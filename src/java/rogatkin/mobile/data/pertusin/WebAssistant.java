package rogatkin.mobile.data.pertusin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class WebAssistant {

	public interface Notifiable<T> {
		void done(T data);
	}

	protected static final String TAG = WebAssistant.class.getSimpleName();
	Context context;

	protected HostnameVerifier hostVerifier;

	public WebAssistant() {

	}

	public WebAssistant(Context ctx) {
		context = ctx;
	}

	public WebAssistant setHostNameVerifier(HostnameVerifier hnv) {
		// if (Main.__debug)
		hostVerifier = hnv;
		return this;
	}

	public <DO> Future<DO> post(final DO pojo, final Notifiable<DO> notf) throws IOException {
		final String query = makeQuery(pojo);
		final URL url = new URL(getURL(pojo));
		return Executors.newSingleThreadExecutor().submit(new Callable<DO>() {

			public DO call() throws Exception {
				try {
					if (Main.__debug)
						Log.d(TAG, "Posting to :" + url + ", query: " + query);
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();
					if (connection instanceof HttpsURLConnection && hostVerifier != null)
						((HttpsURLConnection) connection).setHostnameVerifier(hostVerifier);

					//connection.setRequestProperty("Cookie", cookie);
					applyHeaders(connection, getHeaders(pojo));
					//Set to POST
					connection.setDoOutput(true);
					connection.setRequestMethod("POST");
					connection.setReadTimeout(10000); //?? configure
					Writer writer = new OutputStreamWriter(connection.getOutputStream(), Base64.UTF_8); // TODO configure
					writer.write(query);
					writer.flush();
					writer.close(); // TODO finally ?

					putResponse(connection, pojo);
					if (Main.__debug)
						Log.d(TAG, "Resp code:" + connection.getResponseCode());

				} catch (Exception e) {
					putError(e, pojo);
					if (Main.__debug)
						Log.e(TAG, "", e);
				} finally {
					//connection.disconnect();
					if (notf != null)
						notf.done(pojo);
				}
				return pojo;
			}
		});
	}

	public <DO> Future<DO> post(DO pojo) throws IOException {
		return post(pojo, null);
	}

	public <DO> void get(final DO pojo, final Notifiable<DO> notf) throws IOException {
		final URL url = new URL(getURL(pojo) + "?" + makeQuery(pojo));
		Executors.newSingleThreadExecutor().submit(new Runnable() {

			public void run() {
				HttpURLConnection connection;
				try {
					if (Main.__debug)
						Log.d(TAG, "Getting from :" + url);
					connection = (HttpURLConnection) url.openConnection();
					if (connection instanceof HttpsURLConnection && hostVerifier != null)
						((HttpsURLConnection) connection).setHostnameVerifier(hostVerifier);
					//connection.setRequestProperty("Cookie", cookie);
					applyHeaders(connection, getHeaders(pojo));
					connection.setRequestMethod("GET");
					putResponse(connection, pojo);
				} catch (IOException e) {
					putError(e, pojo);
				} finally {
					//connection.disconnect();
					if (notf != null)
						notf.done(pojo);
				}
			}
		});

	}

	public <DO> String getURL(DO pojo) {
		Class<?> pojoc = pojo.getClass();
		EndpointA ep = pojoc.getAnnotation(EndpointA.class);
		if (!ep.value().isEmpty())
			return ep.value();
		String res = null;
		for (Field f : pojoc.getFields()) {
			ep = f.getAnnotation(EndpointA.class);
			if (!ep.value().isEmpty())
				if (res == null)
					try {
						res = f.get(pojo).toString();
					} catch (Exception e) {

					}
				else
					throw new IllegalArgumentException(
							"More than one field " + f.getName() + " declares end point URL");
		}
		return res;
	}

	public <DO> Map<String, List<String>> getHeaders(DO pojo) {
		Class<?> pojoc = pojo.getClass();
		HashMap<String, List<String>> res = new HashMap<String, List<String>>();
		for (Field f : pojoc.getFields()) {
			WebA a = f.getAnnotation(WebA.class);
			String name = null;
			if (a != null) {
				if (!a.header() || a.response())
					continue;
				if (!a.value().isEmpty())
					name = a.value();
				else
					name = f.getName();
			} else
				continue;
			try {
				Class<?> type = f.getType();
				Object v = f.get(pojo);
				if (v != null) {
					if (type == String.class) {
						putMapList(res, name, v.toString());
					} else if (type == int.class) {
						putMapList(res, name, v.toString());
					} else if (type == boolean.class || type == Boolean.class) {
						putMapList(res, name, v.toString());
					} else if (f.getType() == long.class) {
					} else if (f.getType() == Date.class) {
					} else if (f.getType() == double.class) {
					} else if (f.getType() == float.class) {

					} else {
						throw new IllegalArgumentException("Unsupported type " + f.getType() + " for " + f.getName());
					}
				}
			} catch (Exception e) {
				if (e instanceof IllegalArgumentException)
					throw (IllegalArgumentException) e;
				if (Main.__debug)
					Log.e(TAG, "", e);
			}
		}
		return res;
	}

	protected <DO> void putResponse(HttpURLConnection connection, DO pojo) {
		String res = null;
		InputStream ins = null;
		for (Field f : pojo.getClass().getFields()) {
			WebA a = f.getAnnotation(WebA.class);
			String name = null;
			if (a != null) {
				if (!a.response())
					continue;
				name = a.value().isEmpty() ? f.getName() : a.value();
				if (a.header())
					addHeaders(connection, f, pojo, name);
				Class<?> type = f.getType();
				try {
					if ("code".equals(name)) {
						if (type == int.class)
							f.setInt(pojo, connection.getResponseCode());
						else if (type == String.class) {
							f.set(pojo, connection.getHeaderField(0));
						}
					} else {
						if (type == String.class) {
							if (res != null)
								throw new IllegalArgumentException(
										"Only one field can be annotated as response string : " + name);
							try {
								res = IOAssistant.asString(ins = connection.getInputStream(), 0, null);
								f.set(pojo, res);
							} finally {
								ins.close();
							}
						} else if (type == InputStream.class) {
							if (ins != null)
								throw new IllegalArgumentException(
										"Only one field can be annotated as response stream or string : " + name);
							ins = connection.getInputStream();
							f.set(pojo, ins);
						}
					}
				} catch (Exception e) {
					if (e instanceof IllegalArgumentException)
						throw (IllegalArgumentException) e;
					if (Main.__debug)
						Log.e(TAG, "", e);
				}

			}

		}
	}

	protected <DO> void putError(Throwable e, DO pojo) {
		for (Field f : pojo.getClass().getFields()) {
			if (f.getType().isAssignableFrom(Throwable.class) && f.isAnnotationPresent(WebA.class)
					&& f.getAnnotation(WebA.class).response())
				try {
					DataAssistant.assureAccessible(f).set(pojo, e);
				} catch (Exception ex) {
					if (Main.__debug)
						Log.e(TAG, "", ex);
				}
		}

	}

	protected <DO> void addHeaders(HttpURLConnection connection, Field f, DO pojo, String name) {
		Class<?> type = f.getType();
		try {
			if (type.isAssignableFrom(List.class)) {
				f.set(pojo, connection.getHeaderFields().get(name));
			} else if (type == String.class) {
				f.set(pojo, connection.getHeaderField(name));
			} else
				throw new IllegalArgumentException("Tyype " + type + " isn't supported for response header " + name);
		} catch (Exception e) {
			if (e instanceof IllegalArgumentException)
				throw (IllegalArgumentException) e;
			if (Main.__debug)
				Log.e(TAG, "", e);
		}
	}

	/**
	 * Populates POJO from JSON string accordingly Store A
	 * 
	 * @param jss
	 * @param pojo
	 * @param reverse
	 * @param names
	 * @throws IOException
	 */
	public <DO> void putJSON(String jss, DO pojo, boolean reverse, String... names) throws IOException {
		try {
			JSONObject json = new JSONObject(jss);
			HashSet<String> ks = new HashSet<String>();
			for (String s : names)
				ks.add(s);
			JSONDateUtil du = null;
			for (Field f : pojo.getClass().getFields()) {
				StoreA da = f.getAnnotation(StoreA.class);
				if (da == null)
					continue;
				String n = f.getName();
				if (ks.contains(n) ^ reverse)
					continue;

				n = da.storeName().isEmpty() ? n : da.storeName();
				if (!json.has(n))
					continue;
				Class<?> type = f.getType();
				try {
					if (type.isPrimitive()) {
						if (type == char.class || type == int.class || type == short.class)
							f.setInt(pojo, json.getInt(n));
						else if (type == boolean.class)
							f.setBoolean(pojo, json.getBoolean(n));
						else if (type == long.class)
							f.setLong(pojo, json.getLong(n));
						else if (type == float.class)
							f.setFloat(pojo, (float) json.getDouble(n));
						else if (type == double.class)
							f.setDouble(pojo, json.getDouble(n));
						else if (Main.__debug)
							Log.e(TAG, "Unsupported type of preference value: " + type + " for " + n);
					} else {
						if (type == String.class)
							f.set(pojo, json.getString(n));
						else if (type == Date.class) {
							if (du == null)
								du = new JSONDateUtil();
							String v = json.getString(n);
							if (TextUtils.isEmpty(v))
								f.set(pojo, null);
							else
								f.set(pojo, du.parse(v));
						} else
							f.set(pojo, json.get(n));
					}
				} catch (Exception e) {
					if (Main.__debug)
						Log.e(TAG, "Coudn't populate value to " + n + " " + e);
				}
			}
		} catch (JSONException e) {
			throw new IOException("String " + jss + " isn't JSON", e);
		}

	}

	/**
	 * creates a JSON object reflecting WebA fields of a POJO
	 * 
	 * @param pojo
	 *            to get JSOB from
	 * @param reverse
	 *            name what should be included, excluded
	 * @param names
	 *            in white or black list
	 * @return JSON object
	 */
	public <DO> JSONObject getJSON(DO pojo, boolean reverse, String... names) {
		JSONDateUtil du = null;
		JSONObject res = new JSONObject();
		HashSet<String> ks = new HashSet<String>();
		for (String s : names)
			ks.add(s);
		for (Field f : pojo.getClass().getFields()) {
			WebA a = f.getAnnotation(WebA.class);
			String name = f.getName();
			if (a != null) {
				if (a.header())
					continue;
				if (ks.contains(name) ^ reverse)
					continue;
				if (!a.value().isEmpty())
					name = a.value();
			} else
				continue;

			try {
				Class<?> type = f.getType();
				if (type == String.class) {
					res.put(name, emptyIfNull(f.get(pojo)));
				} else if (type == int.class) {
					res.put(name, f.getInt(pojo));
				} else if (type == boolean.class) {
					res.put(name, f.getBoolean(pojo));
				} else if (type == long.class) {
					res.put(name, f.getLong(pojo));
				} else if (type == float.class) {
					res.put(name, f.getFloat(pojo));
				} else if (type == double.class) {
					res.put(name, f.getDouble(pojo));
				} else if (type == Date.class) {
					if (du == null)
						du = new JSONDateUtil();
					//du.toJSON((Date)f.get(pojo));
					if (f.get(pojo) != null)
						res.put(name, f.get(pojo));
				} else {
					if (Main.__debug)
						Log.e(TAG, "Unsupported type for " + type + " for " + name);
				}
			} catch (Exception e) {
				if (e instanceof IllegalArgumentException)
					throw (IllegalArgumentException) e;
				if (Main.__debug)
					Log.e(TAG, "A problem in filling JSON object", e);
			}
		}
		return res;
	}

	public <DO> String makeQuery(DO pojo) {
		Class<?> pojoc = pojo.getClass();
		StringBuilder c = new StringBuilder(256);
		for (Field f : pojoc.getFields()) {
			WebA a = f.getAnnotation(WebA.class);

			// TODO add processing for collections/arrays
			if (a != null) {
				if (a.header() || a.response())
					continue;
				if (c.length() > 0)
					c.append('&');
				if (!a.value().isEmpty())
					c.append(a.value());
				else
					c.append(f.getName());
			} else
				continue;
			c.append('=');
			try {
				Class<?> type = f.getType();
				if (type == String.class) {
					c.append(URLEncoder.encode(emptyIfNull(f.get(pojo)), Base64.UTF_8));
				} else if (type == int.class) {
					c.append(f.getInt(pojo));
				} else if (type == boolean.class || type == Boolean.class) {
					c.append(f.getBoolean(pojo) ? "true" : "");
				} else if (f.getType() == long.class) {
					c.append(f.getLong(pojo));
				} else if (f.getType() == Date.class) {
				} else if (f.getType() == double.class) {
				} else if (f.getType() == float.class) {

				} else {
					throw new IllegalArgumentException("Unsupported type " + f.getType() + " for " + f.getName());
				}
			} catch (Exception e) {
				if (e instanceof IllegalArgumentException)
					throw (IllegalArgumentException) e;

			}
		}
		return c.toString();
	}

	public static void putMapList(Map<String, List<String>> map, String key, String value) {
		List<String> values = null;
		if (map.containsKey(key))
			values = map.get(key);
		else {
			values = new ArrayList<String>();
			map.put(key, values);
		}
		values.add(value);
	}

	public static String emptyIfNull(Object obj) {
		if (obj == null)
			return "";
		return obj.toString();
	}

	public static void debug(boolean on) {
		Main.__debug = on;
	}

	protected void applyHeaders(HttpURLConnection connection, Map<String, List<String>> headers) {
		if (headers.size() > 0) {
			for (Map.Entry<String, List<String>> es : headers.entrySet()) {
				for (String v : es.getValue())
					connection.addRequestProperty(es.getKey(), v);
			}
		}
	}

	public static class JSONDateUtil {
		SimpleDateFormat JSONISO_8601_FMT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.ENGLISH);

		public Date parse(String jds) throws ParseException {
			return JSONISO_8601_FMT.parse(jds);
		}

		public String toJSON(Date date) {
			return JSONISO_8601_FMT.format(date);
		}
	}
}
