package rogatkin.mobile.data.pertusin;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map.Entry;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

public class DataAssistant {
	private final static String TAG = "pertusin-DataAssistant";

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
			// TODO apply converter
			Class<?> type = f.getType();
			try {
				if (type.isPrimitive()) {
					if (type == char.class || type == int.class || type == short.class)
						result.put(n, f.getInt(obj));
					else if (type == boolean.class)
						result.put(n, f.getBoolean(obj));
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
	
	public <DO> String getSelectQuery(Class<?> pojo, ContentValues keys, String orderBy, String limit, boolean reverse, String... scope) {
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
			result.append(e.getKey());
			Object v = e.getValue();
			if (v == null)
				result.append("ISNULL");
			else
				result.append('=');
			if (v instanceof String)
				result.append('\'').append(DatabaseUtils.sqlEscapeString((String) v)).append('\'');
			else
				result.append(v);
		}
		return result.toString();
	}

	public <DO> void fillDO(Cursor c, DO obj, boolean reverse, String... scope) {
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
			// TODO apply converter
			Class<?> type = f.getType();
			// TODO need optimization set value by type for predetected types
			try {

				int ci = c.getColumnIndex(n);
				if (type.isPrimitive()) {
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

	public <DO> Collection<DO> select(SQLiteDatabase db, Class<?> pojo, ContentValues keys, String orderBy, String limit, boolean reverse,
			String... scope) {
		try {
			String q = getSelectQuery(pojo, keys, orderBy, limit, reverse, scope);
			if (Main.__debug)
				Log.d(TAG, "Select query:"+q); 
			Cursor c = db.rawQuery(q, null);
			if (c.moveToFirst()) {
				ArrayList<DO> result = new ArrayList<DO>();
				do {
					DO instance = (DO) pojo.newInstance();
					fillDO(c, instance, reverse, scope);
					result.add(instance);
				} while (c.moveToNext());
				return result;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public String getDropQuery(Class<?> pojo) {
		return "DROP TABLE IF EXISTS " + resolveStoreName(pojo);
	}

	public String resolveStoreName(Class<?> pojo) {
		// TODO check from annotation first
		String name = pojo.getName();
		int ld = name.lastIndexOf('.');
		if (ld > 0)
			name = name.substring(ld + 1);
		return name;
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
