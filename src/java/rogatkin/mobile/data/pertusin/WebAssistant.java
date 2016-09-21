package rogatkin.mobile.data.pertusin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class WebAssistant implements AutoCloseable {

	public interface Notifiable<T> {
		void done(T data);
	}

	public interface DataDeployer {
		void deploy(OutputStream target) throws IOException;
	}

	// TODO do not create single thread executor for each request, reuse existing, or define small pool for
	// parallelism

	protected static final String TAG = WebAssistant.class.getSimpleName();
	Context context;

	int timeout = 10 * 1000;

	ExecutorService executor = Executors.newSingleThreadExecutor();

	protected HostnameVerifier hostVerifier;

	public WebAssistant() {

	}

	public WebAssistant(Context ctx) {
		context = ctx;
	}

	/**
	 * allows to override host name verifier for SSL
	 * 
	 * @param hnv
	 * @return self
	 */
	public WebAssistant setHostNameVerifier(HostnameVerifier hnv) {
		// if (Main.__debug)
		hostVerifier = hnv;
		return this;
	}

	/**
	 * posts a request based on POJO values and then fills in response
	 * 
	 * @param pojo
	 *            an object used for forming parameters and taking response
	 * @param notf
	 *            a listener of when a job's done
	 * @return future object to monitor result even in a listener's set
	 * @throws IOException
	 */
	public <DO> Future<DO> post(final DO pojo, final Notifiable<DO> notf) throws IOException {
		final URL url = new URL(getURL(pojo));
		return executor.submit(new Callable<DO>() {

			public DO call() throws Exception {
				try {
					if (Main.__debug)
						Log.d(TAG, "Posting to :" + url);
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();
					if (connection instanceof HttpsURLConnection && hostVerifier != null)
						((HttpsURLConnection) connection).setHostnameVerifier(hostVerifier);
					//connection.setRequestProperty("Cookie", cookie);
					applyHeaders(connection, getHeaders(pojo));
					//Set to POST
					connection.setDoOutput(true);
					connection.setRequestMethod("POST");
					connection.setReadTimeout(timeout); //?? configure
					Writer writer = new OutputStreamWriter(connection.getOutputStream(), Base64.UTF_8); // TODO configure
					writer.write(makeQuery(pojo));
					//if (Main.__debug)
					//	Log.d(TAG, "Posting query :" + query);
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

	/**
	 * similar to post but does get request
	 * 
	 * @param pojo
	 * @param notf
	 * @throws IOException
	 */
	public <DO> void get(final DO pojo, final Notifiable<DO> notf) throws IOException {
		final URL url = new URL(getURL(pojo) + "?" + makeQuery(pojo));
		executor.submit(new Runnable() {

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

	/**
	 * similar to get but does delete request it doesn't add URL query
	 * 
	 * @param pojo
	 * @param notf
	 * @throws IOException
	 */
	public <DO> void delete(final DO pojo, final Notifiable<DO> notf) throws IOException {
		String query = makeQuery(pojo);
		final URL url = new URL(getURL(pojo) + (query.isEmpty() ? query : "?") + query);
		executor.submit(new Runnable() {

			public void run() {
				HttpURLConnection connection;
				try {
					if (Main.__debug)
						Log.d(TAG, "Deleting :" + url);
					connection = (HttpURLConnection) url.openConnection();
					if (connection instanceof HttpsURLConnection && hostVerifier != null)
						((HttpsURLConnection) connection).setHostnameVerifier(hostVerifier);
					//connection.setRequestProperty("Cookie", cookie);
					applyHeaders(connection, getHeaders(pojo));
					connection.setRequestMethod("DELETE");
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

	/** issues DELETE method request and provides JSON body entity
	 * 
	 * @param pojo used generating JSON and taking response
	 * @param notf response notification handler
	 * @param fillterInv specifies how filtering happens white or black list
	 * @param names filter pojo fields used for JSON generation (only first level) accordingly filter
	 * @throws IOException
	 */
	public <DO> void delete(final DO pojo, final Notifiable<DO> notf, boolean fillterInv, String... names)
			throws IOException {
		final JSONObject json = getJSON(pojo, fillterInv, names);
		final URL url = new URL(getURL(pojo));
		executor.submit(new Runnable() {

			public void run() {
				try {
					if (Main.__debug)
						Log.d(TAG, "Deleting :" + url + ", json: " + json);
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();
					if (connection instanceof HttpsURLConnection && hostVerifier != null)
						((HttpsURLConnection) connection).setHostnameVerifier(hostVerifier);

					//connection.setRequestProperty("Cookie", cookie);
					applyHeaders(connection, getHeaders(pojo));
					connection.setDoOutput(true);
					connection.setRequestMethod("DELETE");
					connection.setReadTimeout(timeout);
					Writer writer = new OutputStreamWriter(connection.getOutputStream(), Base64.UTF_8); // TODO configure
					writer.write(json.toString());
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
			}
		});
	}

	/**
	 * performs put request
	 * 
	 * @param pojo
	 *            which will be formed in JSON as the request payload
	 * @param notf
	 * @return
	 * @throws IOException
	 */
	public <DO> Future<DO> put(DO pojo, Notifiable<DO> notf) throws IOException {
		return put(pojo, notf, false);
	}

	/**
	 * performs put request
	 * 
	 * @param pojo
	 *            which will be formed in JSON as the request payload
	 * @param notf
	 * @param flag
	 *            of filtering POJO fields for forming JSON
	 * @param filtering
	 *            fields accordingly flag
	 * @return
	 * @throws IOException
	 */
	public <DO> Future<DO> put(final DO pojo, final Notifiable<DO> notf, boolean fillterInv, String... names)
			throws IOException {
		final JSONObject json = getJSON(pojo, fillterInv, names);
		final URL url = new URL(getURL(pojo));
		return executor.submit(new Callable<DO>() {

			public DO call() throws Exception {
				try {
					if (Main.__debug)
						Log.d(TAG, "Putting to :" + url + ", json: " + json);
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();
					if (connection instanceof HttpsURLConnection && hostVerifier != null)
						((HttpsURLConnection) connection).setHostnameVerifier(hostVerifier);

					//connection.setRequestProperty("Cookie", cookie);
					applyHeaders(connection, getHeaders(pojo));
					connection.setDoOutput(true);
					connection.setRequestMethod("PUT");
					connection.setReadTimeout(timeout);
					Writer writer = new OutputStreamWriter(connection.getOutputStream(), Base64.UTF_8); // TODO configure
					writer.write(json.toString());
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

	public <DO> Future<DO> postMultipart(final DO pojo, final Notifiable<DO> notf) throws IOException {
		final URL url = new URL(getURL(pojo));
		return executor.submit(new Callable<DO>() {

			public DO call() throws Exception {
				try {
					if (Main.__debug)
						Log.d(TAG, "Posting multippart to :" + url);
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();
					if (connection instanceof HttpsURLConnection && hostVerifier != null)
						((HttpsURLConnection) connection).setHostnameVerifier(hostVerifier);

					//connection.setRequestProperty("Cookie", cookie);
					applyHeaders(connection, getHeaders(pojo));
					String boundary = generateBoundary();
					connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
					//Set to POST
					connection.setDoOutput(true);
					connection.setRequestMethod("POST");
					connection.setReadTimeout(timeout);
					OutputStream target = connection.getOutputStream();
					for (Field f : pojo.getClass().getFields()) {
						WebA a = f.getAnnotation(WebA.class);
						String name;
						if (a != null) {
							if (a.header() || a.response())
								continue;
							name = !a.value().isEmpty() ? a.value() : f.getName();
						} else
							continue;

						try {
							Class<?> type = f.getType();
							if (type == String.class) {
								writePart(target, boundary, name, null, Base64.UTF_8, "text/plain",
										new StringDeployer((String) f.get(pojo)));
							} else if (type == JSONObject.class) {
								writePart(target, boundary, name, null, Base64.UTF_8, "application/json",
										new StringDeployer(f.get(pojo).toString()));
							} else if (type == File.class) {
								File file = (File) f.get(pojo);
								writePart(target, boundary, name, f.getName(), Base64.UTF_8, "image/jpg",
										new FileDeployer(file));
							} else if (type == Date.class) {
								//HTTPDate
							} else if (type.isArray() || type.isAssignableFrom(Collection.class)) {
								Log.w(TAG, "Aggregation type " + type + " isn't supported");
							} else {
								// TODO add handling int, long, double, Date, array, Collection
								writePart(target, boundary, name, null, "ascii", "text/plain",
										new StringDeployer(f.get(pojo).toString()));
							}
						} catch (Exception e) {
							if (e instanceof IllegalArgumentException)
								throw (IllegalArgumentException) e;
							if (Main.__debug)
								Log.e(TAG, "A problem in generating query string", e);
						}
					}
					writeEndPart(target, boundary);
					target.flush();
					target.close(); // TODO finally ?

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

	/**
	 * generates URL based on pojo fields
	 * 
	 * @param pojo
	 * @return
	 */
	public <DO> String getURL(DO pojo) {
		Class<?> pojoc = pojo.getClass();
		EndpointA ep = pojoc.getAnnotation(EndpointA.class);
		String res = null;
		if (ep != null && !ep.value().isEmpty())
			res = ep.value();
		String path = null;
		for (Field f : pojoc.getFields()) {
			ep = f.getAnnotation(EndpointA.class);
			boolean epSet = false;
			if (ep != null && !ep.value().isEmpty())
				if (res == null)
					try {
						res = f.get(pojo).toString();
						epSet = true;
					} catch (Exception e) {

					}
				else
					throw new IllegalArgumentException(
							"More than one field " + f.getName() + " declares end point URL");
			WebA w = f.getAnnotation(WebA.class);
			if (w != null && w.path()) {
				if (path == null) {
					if (epSet)
						throw new IllegalArgumentException(
								"Same field can't be annotated as Endpoint and path argument :" + f.getName());
					try {
						path = f.get(pojo).toString();
					} catch (Exception e) {

					}
				} else
					throw new IllegalArgumentException("More than one field " + f.getName() + " declares URL path");
			}
		}
		if (path == null)
			return res;
		return res + "/" + path; // TODO check if / ends res or starts path
	}

	/**
	 * extracts headers from POJO to a map to use in requests like loadURL
	 * 
	 * @param pojo
	 * @return
	 */
	public <DO> Map<String, List<String>> getHeaders(DO pojo) {
		Class<?> pojoc = pojo.getClass();
		HashMap<String, List<String>> res = new HashMap<String, List<String>>();
		for (Field f : pojoc.getFields()) {
			WebA a = f.getAnnotation(WebA.class);
			String name = null;
			if (a != null) {
				if (!a.header() || a.response() || a.path())
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
						putMapList(res, name, v.toString());
					} else if (f.getType() == Date.class) {
						putMapList(res, name, HTTPDate.formatDate((Date) v));
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

	/**
	 * extracts headers from POJO to a map to use in requests like loadURL
	 * despite a fact that a headers can be multiple
	 * 
	 * @param pojo
	 * @return
	 */
	public <DO> Map<String, String> getSingleHeaders(DO pojo) {
		HashMap<String, String> result = new HashMap<String, String>();
		Map<String, List<String>> headers = getHeaders(pojo);
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			result.put(entry.getKey(), entry.getValue().get(0));
		}
		return result;
	}

	protected <DO> void putResponse(HttpURLConnection connection, DO pojo) {
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
						if (ins != null)
							throw new IllegalArgumentException("Only one field can be annotated as response : " + name);
						if (type == String.class) {
							try {
								// TODO get encoding from content-type
								f.set(pojo, IOAssistant.asString(ins = connection.getInputStream(), 0, null));
							} finally {
								if (ins != null)
									ins.close();
							}
						} else if (type == InputStream.class) {
							ins = connection.getInputStream();
							f.set(pojo, ins);
						} else if (type == OutputStream.class) {
							IOAssistant.copy(ins = connection.getInputStream(), (OutputStream) f.get(pojo), 0);
						} else if (type == File.class) {
							IOAssistant.copy(ins = connection.getInputStream(), (File) f.get(pojo));
						} else if (type == byte[].class) {
							ByteArrayOutputStream os = new ByteArrayOutputStream();
							IOAssistant.copy(ins = connection.getInputStream(), os, 0);
							f.set(pojo, os.toByteArray());
							os.close();
						}
					}
				} catch (Exception e) {
					if (e instanceof IllegalArgumentException)
						throw (IllegalArgumentException) e;
					if (Main.__debug)
						Log.e(TAG, "Processing response exception", e);
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
	 * populates JSON array elements to pojo
	 * 
	 * @param jss
	 *            JSON array
	 * @param pojo
	 *            an element POJO
	 * @param noCase
	 *            consider mapping of JSON POJO fields case insensitive
	 * @return an array with POJO elements
	 * @throws IOException
	 *             if any processing exception happened
	 */
	public <DO> DO[] putJSONArray(JSONArray jsarr, DO pojo, boolean noCase) throws Exception {
		JSONDateUtil[] du = new JSONDateUtil[1];
		Class<?> pojoCl = pojo.getClass();
		DO[] result = (DO[]) Array.newInstance(pojoCl, jsarr.length());

		for (int j = 0, s = jsarr.length(); j < s; j++) {
			JSONObject jso = jsarr.getJSONObject(j);
			HashMap<String, String> map = null;
			if (noCase) {
				map = new HashMap<String, String>();
				for (Iterator<String> i = jso.keys(); i.hasNext();) {
					String k = i.next();
					String mk = k.toUpperCase();
					if (Main.__debug)
						if (map.containsKey(mk))
							Log.w(TAG, "Conflicting field name for no case:" + k);
					map.put(mk, k);
				}
			}
			result[j] = (DO) pojoCl.newInstance();
			pojo = result[j];
			for (Field f : pojoCl.getFields()) {
				StoreA da = f.getAnnotation(StoreA.class);
				if (da == null)
					continue;
				String n = f.getName();
				n = da.storeName().isEmpty() ? n : da.storeName();
				if (map != null)
					n = map.get(n.toUpperCase());
				if (!jso.has(n))
					continue;
				Class<?> type = f.getType();
				try {
					if (type.isArray() || type.isAssignableFrom(Collection.class)) {
						if (Main.__debug)
							Log.w(TAG, "Collections are not supported for " + n);
						continue;
					}
					setDataToField(f, n, pojo, jso, du);
					/*if (type.isPrimitive()) {
						if (type == char.class || type == int.class || type == short.class)
							f.setInt(pojo, jso.getInt(n));
						else if (type == boolean.class)
							f.setBoolean(pojo, jso.getBoolean(n));
						else if (type == long.class)
							f.setLong(pojo, jso.getLong(n));
						else if (type == float.class)
							f.setFloat(pojo, (float) jso.getDouble(n));
						else if (type == double.class)
							f.setDouble(pojo, jso.getDouble(n));
						else if (Main.__debug)
							Log.e(TAG, "Unsupported type of preference value: " + type + " for " + n);
					} else {
						if (type == String.class)
							f.set(pojo, jso.getString(n));
						else if (type == Date.class) {
							if (du == null)
								du = new JSONDateUtil();
							String v = jso.getString(n);
							if (TextUtils.isEmpty(v))
								f.set(pojo, null);
							else
								f.set(pojo, du.parse(v));
						} else
							f.set(pojo, jso.get(n));
					}*/
				} catch (Exception e) {
					if (Main.__debug)
						Log.e(TAG, "Coudn't populate value to " + n + " " + e);
				}
			}
		}
		return result;
	}

	/**
	 * populates JSON array elements to pojo
	 * 
	 * @param jss
	 *            JSON string
	 * @param pojo
	 *            an element POJO
	 * @param noCase
	 *            consider mapping of JSON POJO fields case insensitive
	 * @return an array with POJO elements
	 * @throws IOException
	 *             if any processing exception happened
	 */
	public <DO> DO[] putJSONArray(String jss, DO pojo, boolean noCase) throws IOException {
		try {
			return putJSONArray(new JSONArray(jss), pojo, noCase);
		} catch (JSONException e) {
			throw new IOException("String " + jss + " isn't JSON array", e);
		} catch (Exception e) {
			throw new IOException("Couldn't fill array", e);
		}
	}

	/**
	 * Populates POJO from JSON string accordingly StoreA
	 * 
	 * @param jss
	 * @param pojo
	 * @param reverse
	 * @param names
	 * @throws IOException
	 */
	public <DO> void putJSON(String jss, DO pojo, boolean reverse, String... names) throws IOException {
		// TODO add no case sensitive
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
					if (type.isArray() || type.isAssignableFrom(Collection.class)) {
						//putJSONArray(json.getJSONArray(n), type.getComponentType(), false);
						if (Main.__debug)
							Log.w(TAG, "Collections are not supported for " + n);
						continue;
					}
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
						Log.e(TAG, "Couldn't populate value to " + n + " " + e);
				}
			}
		} catch (JSONException e) {
			throw new IOException("String " + jss + " isn't JSON", e);
		}
	}

	public OutputStream writePart(OutputStream parts, String boundary, String name, String file, String encoding,
			String contentType, DataDeployer dp) throws IOException {
		parts.write("--".getBytes("ascii"));
		parts.write(boundary.getBytes("ascii"));
		parts.write("\r\n".getBytes("ascii"));
		parts.write("Content-Disposition: form-data; name=\"".getBytes("ascii"));
		parts.write(name.getBytes("ascii"));
		if (file != null) {
			parts.write("\"; filename=\"".getBytes("ascii"));
			parts.write(file.getBytes("ascii"));
		}
		parts.write("\"\r\n".getBytes("ascii"));
		parts.write("Content-Type: ".getBytes("ascii"));

		parts.write(contentType.getBytes("ascii"));
		if (encoding != null) {
			parts.write("; charset=".getBytes("ascii"));
			parts.write(encoding.getBytes("ascii"));
			parts.write("\r\n".getBytes("ascii"));
		} else
			parts.write("Content-Transfer-Encoding: binary\r\n".getBytes("ascii"));
		parts.write("\r\n".getBytes("ascii"));
		dp.deploy(parts);
		parts.write("\r\n".getBytes("ascii"));
		return parts;
	}

	public void writeEndPart(OutputStream parts, String boundary) throws IOException {
		parts.write("--".getBytes("ascii"));
		parts.write(boundary.getBytes("ascii"));
		parts.write("--\r\n".getBytes("ascii"));
	}

	public String generateBoundary() {
		return "--=" + UUID.randomUUID().toString();
	}

	public <DO> DO setDataToField(Field f, String n, DO pojo, JSONObject json, JSONDateUtil[] du) throws Exception {
		Class<?> type = f.getType();
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
				if (du[0] == null)
					du[0] = new JSONDateUtil();
				String v = json.getString(n);
				if (TextUtils.isEmpty(v))
					f.set(pojo, null);
				else
					f.set(pojo, du[0].parse(v));
			} else
				f.set(pojo, json.get(n));
		}
		return pojo;
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
				if (a.header() || a.response())
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
				if (a.header() || a.response() || a.path())
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
				if (Main.__debug)
					Log.e(TAG, "A problem in generating query string", e);
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

	public static class FileDeployer implements DataDeployer {
		File file;

		public FileDeployer(File f) {
			file = f;
		}

		public void deploy(OutputStream target) throws IOException {
			IOAssistant.copy(file, target);
		}

	}

	public static class StringDeployer implements DataDeployer {
		String str;

		public StringDeployer(String s) {
			str = s;
		}

		public void deploy(OutputStream target) throws IOException {
			if (str != null)
				target.write(str.getBytes("ascii"));
		}

	}

	public static class JSONDateUtil {
		SimpleDateFormat JSONISO_8601_FMT = android.os.Build.VERSION.SDK_INT > 23
				? new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.ENGLISH)
				: new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZ", Locale.ENGLISH);

		public Date parse(String jds) throws ParseException {
			if (jds == null || jds.isEmpty())
				return null;
			return JSONISO_8601_FMT.parse(jds);
		}

		public String toJSON(Date date) {
			if (date == null)
				return "";
			return JSONISO_8601_FMT.format(date);
		}
	}

	public static class HTTPDate {
		private static final TimeZone GMT = TimeZone.getTimeZone("GMT");
		/**
		 * Date format pattern used to parse HTTP date headers in RFC 1123
		 * format.
		 */
		public static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";

		/**
		 * Formats the given date according to the RFC 1123 pattern.
		 *
		 * @param date
		 *            The date to format.
		 * @return An RFC 1123 formatted date string.
		 * @see #PATTERN_RFC1123
		 */
		public static String formatDate(Date date) {
			return formatDate(date, PATTERN_RFC1123);
		}

		/**
		 * Formats the given date according to the specified pattern. The
		 * pattern must conform to that used by the {@link SimpleDateFormat
		 * simple date format} class.
		 *
		 * @param date
		 *            The date to format.
		 * @param pattern
		 *            The pattern to use for formatting the date.
		 * @return A formatted date string.
		 * @throws IllegalArgumentException
		 *             If the given date pattern is invalid.
		 * @see SimpleDateFormat
		 */
		public static String formatDate(Date date, String pattern) {
			if (date == null) {
				throw new IllegalArgumentException("date is null");
			}
			if (pattern == null) {
				throw new IllegalArgumentException("pattern is null");
			}

			SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.US);
			formatter.setTimeZone(GMT);
			return formatter.format(date);
		}
	}

	public void close() throws Exception {
		executor.shutdown();
		executor = null;
	}
}
