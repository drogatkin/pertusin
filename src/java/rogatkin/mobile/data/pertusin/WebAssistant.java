package rogatkin.mobile.data.pertusin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

public class WebAssistant {

	public interface Notifiable<T> {
		void done(T data);
	}

	protected static final String TAG = WebAssistant.class.getSimpleName();
	Context context;

	public WebAssistant() {

	}

	public WebAssistant(Context ctx) {
		context = ctx;
	}

	public <DO> Future<String> post(final DO pojo) throws IOException {
		final String query = makeQuery(pojo);
		final URL url = new URL(getURL(pojo));
		return Executors.newSingleThreadExecutor().submit(new Callable<String>() {

			public String call() throws Exception {
				String res = null;
				try {
					if (Main.__debug)
						Log.d(TAG, "Posting to :" + url + ", query: " + query);
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();
					//connection.setRequestProperty("Cookie", cookie);
					applyHeaders(connection, getHeaders(pojo));
					//Set to POST
					connection.setDoOutput(true);
					connection.setRequestMethod("POST");
					connection.setReadTimeout(10000); //?? configure
					Writer writer = new OutputStreamWriter(connection.getOutputStream());
					writer.write(query);
					writer.flush();
					writer.close(); // TODO finally ?
					int respCode = connection.getResponseCode();
					if (respCode == HttpURLConnection.HTTP_OK) {
						//res = connection.getContent().toString();
						InputStream ins;
						res = IOAssistant.asString(ins = connection.getInputStream(), 0, null);
						ins.close();
					}
					if (Main.__debug)
						Log.d(TAG, "Resp code:" + respCode + ", content:" + res);
				} catch (Exception e) {
					res = e.toString();
					if (Main.__debug)
						Log.e(TAG, "", e);
				}
				return res;
			}
		});
	}
	
	public <DO> void post(final DO pojo, final Notifiable<String> notf) throws IOException {
		
	}

	public <DO> void get(final DO pojo, final Notifiable<String> notf) throws IOException {
		final URL url = new URL(getURL(pojo) + "?" + makeQuery(pojo));
		Executors.newSingleThreadExecutor().submit(new Runnable() {

			public void run() {
				HttpURLConnection connection;
				try {
					connection = (HttpURLConnection) url.openConnection();
					//connection.setRequestProperty("Cookie", cookie);
					applyHeaders(connection, getHeaders(pojo));
					connection.setRequestMethod("GET");
					int respCode = connection.getResponseCode();
					if (respCode == HttpURLConnection.HTTP_OK) {
						//res = connection.getContent().toString();
						InputStream ins;
						notf.done(IOAssistant.asString(ins = connection.getInputStream(), 0, null));
						ins.close();
					}
				} catch (IOException e) {
					notf.done(null);
				}

			}
		});

	}

	public <DO> void putJSON(DO pojo) throws IOException {

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
				if (!a.header())
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
				if (type == String.class) {
					if (v != null)
						putMapList(res, name, v.toString());
				} else if (type == int.class) {
					if (v != null)
						putMapList(res, name, v.toString());
				} else if (type == boolean.class || type == Boolean.class) {
					if (v != null)
						putMapList(res, name, v.toString());
				} else if (f.getType() == long.class) {
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
		return res;
	}

	public <DO> JSONObject makeJSON(DO pojo) {
		JSONObject res = new JSONObject();
		Class<?> pojoc = pojo.getClass();
		for (Field f : pojoc.getFields()) {
			WebA a = f.getAnnotation(WebA.class);
			String name;
			if (a != null) {
				if (a.header())
					continue;
				if (!a.value().isEmpty())
					name = a.value();
				else
					name = f.getName();
			} else
				continue;

			try {
				Class<?> type = f.getType();
				if (type == String.class) {
					res.put(name, emptyIfNull(f.get(pojo)));
				} else if (type == int.class) {
					res.put(name, f.getInt(pojo));
				} else {

				}
			} catch (Exception e) {
				if (e instanceof IllegalArgumentException)
					throw (IllegalArgumentException) e;

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
				if (a.header())
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
}
