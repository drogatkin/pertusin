package rogatkin.mobile.data.pertusin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map.Entry;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

public class DataAssistant {

	Context context;

	String separator = "\t";

	final static String crln = "\r\n";

	private final static String TAG = "pertusin-DataAssistant";

	public DataAssistant(Context c) {
		context = c;
	}

	public void setSeparator(String s) {
		separator = s;
	}

	public String getCreateQuery(Class<?> pojo) {
		StringBuilder q = new StringBuilder();
		String name = resolveStoreName(pojo);
		q.append("create table IF NOT EXISTS ").append(name).append(" (");
		boolean first = true;
		boolean primary = false;
		StringBuilder c = new StringBuilder(256);
		for (Field f : pojo.getFields()) {
			StoreA da = f.getAnnotation(StoreA.class);
			if (da == null)
				continue;
			if (first)
				first = false;
			else
				q.append(", ");
			name = da.storeName();
			if (name.length() == 0)
				name = f.getName();
			if (da.key() && da.auto() == 0 && primary == false) { // TODO chekc for multipe primary keuys an conflicts with auto
				c.append(", primary key(").append(name).append(')');
				primary = true;
			} else if (da.index()) {
				// TODO work out creation index
				//c.append(", index(").append(name).append(')');
			}
			if (da.unique())
				c.append(", UNIQUE(").append(name).append(')');

			if (da.sql().length() > 0)
				q.append(da.sql());
			else {
				q.append(name).append(' ').append(da.type().length() > 0 ? da.type() : resolveType(f.getType()));
				if (da.size() > 0) {

					q.append('(').append(da.size());
					if (da.precision() > 0)
						q.append(',').append(da.precision());
					q.append(')');
				}
				if (da.auto() != 0 && primary == false) {
					q.append(" PRIMARY KEY AUTOINCREMENT NOT NULL");
					primary = true;
				}
				if (da.nocase())
					q.append(" COLLATE NOCASE");
			}
		}
		// TODO add constraints, check foreign, primary, key, unique
		if (c.length() > 0)
			q.append(c);
		q.append(")");
		if (Main.__debug)
			Log.d(TAG, "Create query:" + q);
		return q.toString();
	}

	public <DO> String[] asProjectionValues(Class<?> pojo, boolean reverse, String... scope) {
		if (pojo == null)
			return scope;
		if (reverse && scope.length == 0)
			throw new IllegalArgumentException("Requested list is empty");
		HashSet<String> result = new HashSet<String>();
		HashSet<String> ks = new HashSet<String>();
		for (String s : scope)
			ks.add(s);
		for (Field f : pojo.getFields()) {
			StoreA da = f.getAnnotation(StoreA.class);
			if (da == null)
				continue;
			String n = f.getName();
			if (ks.contains(n) ^ reverse)
				continue;
			if (da.storeName().length() > 0)
				n = da.storeName();
			result.add(n);
		}
		return result.toArray(new String[result.size()]);
	}

	public <DO> ContentValues asContentValues(DO obj, boolean reverse, String... scope) {
		// TODO this method has overhead in loop so type action calculation can be done in specific field:code pairs and then switched
		// by code for fast operation		
		if (reverse && scope.length == 0)
			throw new IllegalArgumentException("Requested list is empty");
		ContentValues result = new ContentValues();
		HashSet<String> ks = new HashSet<String>();
		for (String s : scope)
			ks.add(s);
		for (Field f : obj.getClass().getFields()) {
			StoreA da = f.getAnnotation(StoreA.class);
			if (da == null)
				continue;
			String n = f.getName();
			if (ks.contains(n) ^ reverse)
				continue;
			if (da.storeName().length() > 0)
				n = da.storeName();
			Class<?> type = f.getType();
			Class<? extends ConverterI> cc = da.converter();
			try {
				if (cc != ConverterI.class) {
					ConverterI ci = cc.newInstance();
					// TODO inject values 
					inject(ci, (Class<ConverterI>)cc, obj);
					result.put(n, ci.to(f.get(obj)));
				} if (type.isPrimitive()) {
					if (type == char.class || type == int.class || type == short.class)
						result.put(n, f.getInt(obj));
					else if (type == boolean.class)
						result.put(n, f.getBoolean(obj)?1:0);
					else if (type == long.class)
						result.put(n, f.getLong(obj));
					else if (type == float.class)
						result.put(n, f.getFloat(obj));
					else if (type == double.class)
						result.put(n, f.getDouble(obj));
					else
						throw new IllegalArgumentException("Primitive type " + type + " isn't supported");
				} else if (type == Date.class) {
					Date dt = (Date) f.get(obj);
					if (dt == null)
						result.putNull(n);
					else
						result.put(n, (int) (dt.getTime() / 1000));
				} else if (type == Boolean.class) {
					Boolean bo = (Boolean) f.get(obj);
					if (bo == null)
						result.putNull(n);
					else
						result.put(n, bo ? 1 : 0);
				} else if (type.isEnum()) {
					Object vo = f.get(obj);
					if (vo == null)
						result.putNull(n);
					else {
						int i = 0;
						for (Object e : f.getType().getEnumConstants()) {
							if (e.equals(vo)) {
								result.put(n, i);
								break;
							}
							i++;
						}
						// TODO if no one matches
					}
				} else if (type.isArray() && type.getComponentType() == byte.class) {
					byte[] vo = (byte[]) f.get(obj);
					if (vo == null)
						result.putNull(n);
					else {
						result.put(n, vo);
					}
				} else if (type == File.class) {
					File fv = (File) f.get(obj);
					if (fv == null)
						result.putNull(n);
					else {
						result.put(n, fv.getPath());
					}
				} else {
					Object vo = f.get(obj);
					if (vo == null)
						result.putNull(n);
					else
						result.put(n, vo.toString());
				}
			} catch (Exception e) {
				if (Main.__debug)
					Log.e(TAG, "Exception for " + f, e);
			}

		}
		return result;
	}

	public <DO> String getSelectQuery(Class<?> pojo, ContentValues keys, boolean reverse, String... scope) {
		return getSelectQuery(pojo, keys, null, null, reverse, scope);
	}

	public <DO> String getSelectQuery(Class<?> pojo, ContentValues keys, String orderBy, String limit, boolean reverse,
			String... scope) {
		return SQLiteQueryBuilder.buildQueryString(false, resolveStoreName(pojo),
				asProjectionValues(pojo, reverse, scope), asWhere(keys), null, null, orderBy, limit);
	}

	public String asWhere(ContentValues keys) {
		if (keys == null || keys.size() == 0)
			return null;
		StringBuilder result = new StringBuilder();
		boolean first = true;
		for (Entry<String, Object> e : keys.valueSet()) {
			if (first == false)
				result.append(" AND ");
			else
				first = false;
			String k = e.getKey();
			boolean useLike = false;
			if (k.charAt(0) == '%') {
				useLike = true;
				k = k.substring(1);
			}
			result.append(k);
			Object v = e.getValue();
			if (v == null)
				result.append("ISNULL");
			else if (useLike)
				result.append(" LIKE ");
			else
				result.append('=');
			if (v instanceof String)
				result.append(DatabaseUtils.sqlEscapeString((String) v));
			else if (useLike)
				throw new IllegalArgumentException("Wildcard clause is used with non String value");
			else
				result.append(v);
		}
		return result.toString();
	}

	public <DO> void fillDO(Cursor c, DO obj, boolean reverse, String... scope) {
		if (reverse && scope.length == 0)
			throw new IllegalArgumentException("Requested list is empty");
		//ContentValues result = new ContentValues();
		HashSet<String> ks = new HashSet<String>();
		for (String s : scope)
			ks.add(s);

		for (Field f : obj.getClass().getFields()) {
			StoreA da = f.getAnnotation(StoreA.class);
			if (da == null)
				continue;
			String n = f.getName();
			if (ks.contains(n) ^ reverse)
				continue;
			if (da.storeName().length() > 0)
				n = da.storeName();
			Class<?> type = f.getType();
			Class<?> cc = da.converter();
			// TODO need optimization set value by type for predetected types
			try {
				int ci = c.getColumnIndex(n);
				if (cc != ConverterI.class) {
					ConverterI cci = (ConverterI) cc.newInstance();
					// TODO inject values 
					inject(cci, (Class<ConverterI>)cc, obj);
					f.set(obj, cci.from(c.getString(ci)));
				} else	if (type.isPrimitive()) {
					if (type == char.class || type == int.class || type == short.class)
						f.setInt(obj, c.getInt(ci));
					else if (type == boolean.class)
						f.setBoolean(obj, c.getInt(ci) == 1);
					else if (type == long.class)
						f.setLong(obj, c.getLong(ci));
					else if (type == float.class)
						f.setFloat(obj, c.getFloat(ci));
					else if (type == double.class)
						f.setDouble(obj, c.getDouble(ci));
					else
						throw new IllegalArgumentException("Primitive type " + type + " isn't supported");
				} else if (type == Date.class) {
					int time = c.getInt(ci);
					if (time > 0)
						f.set(obj, new Date(time * 1000l));
					else
						f.set(obj, null);
				} else if (type == Boolean.class) {
					f.set(obj, c.getInt(ci) == 1 ? Boolean.TRUE : Boolean.FALSE);
				} else if (type.isEnum()) {
					f.set(obj, f.getType().getEnumConstants()[c.getInt(ci)]);
				} else if (type.isArray() && type.getComponentType() == byte.class) {
					f.set(obj, c.getBlob(ci));
				} else if (type == File.class) {
					String fn = c.getString(ci);
					if (fn != null && fn.length() > 0)
						f.set(obj, new File(fn));
					else
						f.set(obj, null);
				} else {
					f.set(obj, c.getString(ci));
				}
			} catch (Exception e) {
				if (Main.__debug)
					Log.e(TAG, "Exception for " + f, e);
			}
		}

	}

	public <DO> Collection<DO> select(SQLiteDatabase db, Class<DO> pojo, ContentValues keys, String orderBy,
			String limit, boolean reverse, String... scope) {
		Cursor c = null;
		try {
			String q = getSelectQuery(pojo, keys, orderBy, limit, reverse, scope);
			if (Main.__debug)
				Log.d(TAG, "Select query:" + q);
			c = db.rawQuery(q, null);
			if (c.moveToFirst()) {
				ArrayList<DO> result = new ArrayList<DO>(c.getCount());
				do {
					DO instance = (DO) pojo.newInstance();
					fillDO(c, instance, reverse, scope);
					result.add(instance);
				} while (c.moveToNext());
				return result;
			}
		} catch (Exception e) {
			if (Main.__debug)
				Log.e(TAG, "Exception for " + pojo, e);
		} finally {
			if (c != null)
				c.close();
		}
		return null;
	}

	public <DO> DO select(SQLiteDatabase db, DO pojo, ContentValues keys, boolean reverse, String... scope) {
		Cursor c = null;
		try {
			String q = getSelectQuery(pojo.getClass(), keys, null, null, reverse, scope);
			if (Main.__debug)
				Log.d(TAG, "Select query (1):" + q);
			c = db.rawQuery(q, null);
			if (c.getCount() == 1 && c.moveToFirst()) {
				fillDO(c, pojo, reverse, scope);
				return pojo;
			} else if (c.getCount() > 1)
				throw new IllegalArgumentException("Query " + q + " returned more than 1 record");
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (Exception e) {
			if (Main.__debug)
				Log.e(TAG, "Exception for " + pojo, e);
		} finally {
			if (c != null)
				c.close();
		}
		return null;
	}

	public String getDropQuery(Class<?> pojo) {
		return "DROP TABLE IF EXISTS " + resolveStoreName(pojo);
	}

	public String resolveStoreName(Class<?> pojo) {
		// TODO check from annotation first
		StoreA sa = pojo.getAnnotation(StoreA.class);
		if (sa != null) {
			if (sa.sql() .length() > 0)
				return sa.sql();
			if (sa.storeName().length() > 0)
				return sa.storeName();
		}
		String name = pojo.getName();
		int ld = name.lastIndexOf('.');
		if (ld > 0)
			name = name.substring(ld + 1);
		return name;
	}

	public <DO> void storePreferences(DO obj, boolean reverse, String... scope) {
		SharedPreferences prefs = context.getSharedPreferences(resolveStoreName(obj.getClass()), 0);
		ContentValues cv = asContentValues(obj, reverse, scope);
		Editor ed = prefs.edit();
		for (Entry<String, Object> e : cv.valueSet()) {
			Object v = e.getValue();
			if (v instanceof String)
				ed.putString(e.getKey(), (String) v);
			else if (v instanceof Long)
				ed.putLong(e.getKey(), ((Long) v).longValue());
			else if (v instanceof Integer)
				ed.putInt(e.getKey(), ((Integer) v).intValue());
			else if (v instanceof Float)
				ed.putFloat(e.getKey(), ((Float) v).floatValue());
			//else if (v instanceof Double)
				//ed.putDouble(e.getKey(), ((Double) v).doubleValue());
			else if (v instanceof Boolean)
				ed.putBoolean(e.getKey(), ((Boolean) v).booleanValue());
			else {
				if (v == null)
					ed.remove(e.getKey());
				else if (Main.__debug)
					Log.e(TAG, "Unsupported type of preference value: " + v.getClass() + " for " + e.getKey());
			}
		}
		ed.commit();
	}

	public <DO> DO loadPreferences(DO obj, boolean reverse, String... scope) {
		SharedPreferences prefs = context.getSharedPreferences(resolveStoreName(obj.getClass()), 0);
		HashSet<String> ks = new HashSet<String>();
		for (String s : scope)
			ks.add(s);
		for (Field f : obj.getClass().getFields()) {
			StoreA da = f.getAnnotation(StoreA.class);
			if (da == null)
				continue;
			String n = f.getName();
			if (ks.contains(n) ^ reverse)
				continue;

			Class<?> type = f.getType();
			try {
				if (type.isPrimitive()) {
					if (type == char.class || type == int.class || type == short.class)
						f.setInt(obj, prefs.getInt(n, 0));
					else if (type == boolean.class)
						//f.setBoolean(obj, prefs.getBoolean(n, false));
					f.setBoolean(obj, prefs.getInt(n, 0) == 1);
					else if (type == long.class)
						f.setLong(obj, prefs.getLong(n, 0));
					else if (type == float.class)
						f.setDouble(obj, prefs.getFloat(n, 0));
					else if (Main.__debug)
						Log.e(TAG, "Unsupported type of preference value: " + type + " for " + n);
				} else {
					if (type == String.class)
						f.set(obj, prefs.getString(n, null));
					else if (Main.__debug)
						Log.e(TAG, "Unsupported type of preference value: " + type + " for " + n);
				}
			} catch (Exception e) {
				if (Main.__debug)
					Log.e(TAG, "Exception for " + obj, e);
				return null;
			}
		}
		return obj;
	}

	public <DO> void storeCSV(Collection<DO> values, Appendable a, boolean reverse, String... scope) throws IOException {
		HashSet<String> ks = new HashSet<String>();
		for (String s : scope)
			ks.add(s);
		Field[] fs = null;
		Iterator<DO> vi = values.iterator();
		while (vi.hasNext()) {
			DO obj = vi.next();
			if (obj == null)
				continue;
			if (fs == null) {
				fs = obj.getClass().getFields();
				boolean first = true;
				for (Field f : fs) {
					StoreA da = f.getAnnotation(StoreA.class);
					if (da == null)
						continue;
					String n = f.getName();
					if (ks.contains(n) ^ reverse)
						continue;
					if (first)
						first = false;
					else
						a.append(separator);
					a.append(n);
				}
				a.append(crln);
			}
			boolean first = true;
			for (Field f : fs) {
				StoreA da = f.getAnnotation(StoreA.class);
				if (da == null)
					continue;
				String n = f.getName();
				if (ks.contains(n) ^ reverse)
					continue;
				if (first)
					first = false;
				else
					a.append(separator);
				Class<?> type = f.getType();
				try {
					if (type == String.class) {
						String s = (String) f.get(obj);
						if (s != null)
							a.append(s);
					} else if (type == Date.class) {
						Date d = (Date) f.get(obj);
						if (d == null)
							a.append("0");
						else
							a.append(String.format("%d", d.getTime()));
					} else if (type == File.class) {
						File file = (File) f.get(obj);
						if (file != null)
							a.append(file.getPath());
					} else {
						if (type.isPrimitive()) {
							if (type == char.class || type == int.class || type == short.class)
								a.append(String.format("%d", f.getInt(obj)));
							else if (type == boolean.class)
								a.append(f.getBoolean(obj) ? "true" : "false");
							else if (type == long.class)
								a.append(String.format("%d", f.getLong(obj)));
							else if (type == float.class)
								a.append(String.format(Locale.US, "%f", f.getFloat(obj)));
							else if (type == double.class)
								a.append(String.format(Locale.US, "%f", f.getDouble(obj)));
							else if (Main.__debug)
								Log.e(TAG, "Unsupported type of preference " + type);
						} else if (type.isEnum()) {
							int i = 0;
							Enum vo = (Enum) f.get(obj);
							for (Object e : f.getType().getEnumConstants()) {
								if (e.equals(vo)) {
									a.append(String.format("%d", i));
									break;
								}
								i++;
							}
						} else if (Main.__debug)
							Log.e(TAG, "Unsupported type of preference " + type);
					}
				} catch (IllegalArgumentException e) {
					if (Main.__debug)
						Log.e(TAG, "Exception for " + obj, e);
				} catch (IllegalAccessException e) {
					if (Main.__debug)
						Log.e(TAG, "Exception for " + obj, e);
				}
			}
			a.append(crln);
		}
	}

	public <DO> Collection<DO> loadCSV(Class<DO> pojo, Reader r, boolean reverse, String... scope) throws IOException {
		CSVAssistant tk = new CSVAssistant(null, r instanceof BufferedReader ? (BufferedReader) r : new BufferedReader(
				r), "\t", false, 0);
		ArrayList<String> columns = new ArrayList<String>();

		while (tk.hasMoreTokens())
			columns.add(tk.nextToken());
		ArrayList<DO> result = new ArrayList<DO>();
		HashMap<String, Field> fieldsMap = new HashMap<String, Field>();
		Field[] fields = pojo.getFields();
		HashSet<String> ks = new HashSet<String>();
		for (String s : scope)
			ks.add(s);
		for (Field f : fields) {
			StoreA da = f.getAnnotation(StoreA.class);
			if (da == null)
				continue;
			String n = f.getName();
			if (ks.contains(n) ^ reverse)
				continue;
			fieldsMap.put(n, f);
		}
		while (tk.advanceToNextLine()) {
			try {
				DO row = pojo.newInstance();
				for (String c : columns) {
					String v = tk.nextToken();
					Field f = fieldsMap.get(c);
					if (f == null)
						continue;
					Class<?> type = f.getType();
					if (type == String.class) {
						f.set(row, v);
					} else if (type == Date.class) {
						long d;
						if (v.length() == 0)
							d = 0;
						else
							d = Long.parseLong(v);
						f.set(row, new Date(d));
					} else if (type == File.class) {
						if (v.length() == 0)
							f.set(row, null);
						else
							f.set(row, new File(v));
					} else {
						if (type.isPrimitive()) {
							if (type == char.class || type == int.class || type == short.class)
								f.setInt(row, Integer.parseInt(v));
							else if (type == boolean.class)
								f.setBoolean(row, "true".equalsIgnoreCase(v));
							else if (type == long.class)
								f.setLong(row, Long.parseLong(v));
							else if (type == float.class)
								f.setFloat(row, Float.parseFloat(v));
							else if (type == double.class)
								f.setDouble(row, Double.parseDouble(v));
							else if (Main.__debug)
								Log.e(TAG, "Unsupported type of preference " + type);
						} else if (type.isEnum()) {
							if (v.length() == 0)
								f.set(row, null);
							else
								f.set(row, f.getType().getEnumConstants()[Integer.parseInt(v)]);
						} else if (Main.__debug)
							Log.e(TAG, "Unsupported type of preference " + type);
					}
				}
				result.add(row);
			} catch (Exception e) {
				if (Main.__debug)
					Log.e(TAG, "Exception in CSV parsing", e);
			}
		}

		return result;
	}
	
	public AssetManager getAssetManager() {
		return context.getAssets();
	}
	
	protected <T> T inject(T pojo, Class<T> cl, Object host) {
		for (Field fl : cl.getDeclaredFields()) { // use cl.getFields() for public with inheritance
			if (fl.getAnnotation(InjectA.class) != null) {
				try {
					// TODO lookup for registered types
					Class<?> type = fl.getType();
					if (type == Context.class) {
						assureAccessible(fl).set(pojo, context);
						
					} else if (type == host.getClass() ) {
						assureAccessible(fl).set(pojo, host);
					}
				} catch (Exception e) {
					if (Main.__debug)
						Log.e(TAG, "Exception in ijections", e);
				}
			}
		}
		return pojo;
	}

	static protected Field assureAccessible(Field fl) {
		if (fl.isAccessible())
			return fl;
		fl.setAccessible(true);
		return fl;
	}
	
	protected String resolveType(Class<?> type) {
		if (type.isPrimitive()) {
			if (type == char.class || type == boolean.class || type == int.class || type == long.class)
				return "INTEGER";
			return "REAL";
		} else if (type == Date.class)
			return "INTEGER";
		else if (type == Boolean.class || type.isEnum())
			return "INTEGER";
		else if (type.isArray() && type.getComponentType() == byte.class || type == File.class)
			return "BLOB";
		return "TEXT";
	}
}
