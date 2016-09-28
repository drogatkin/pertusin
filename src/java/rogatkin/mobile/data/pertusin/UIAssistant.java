package rogatkin.mobile.data.pertusin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import rogatkin.mobile.data.pertusin.PresentA.FieldType;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

public class UIAssistant {
	public final static String RES_ID_PREF = "@+";

	public static final String DATE_FORMAT = "MM/dd/yy";

	private static final String TAG = null;

	Context context;

	public UIAssistant() {

	}

	public UIAssistant(Context c) {
		context = c;
	}

	public <DO> void fillModel(Context c, Activity a, DO obj) {
		fillModel(c, a.getWindow().getDecorView(), obj, false);
	}

	// TODO decide about validation

	public <DO> void fillModel(Context c, View pv, DO obj, boolean inList) {
		if (obj == null)
			throw new IllegalArgumentException("No POJO specified");

		Field[] flds = obj.getClass().getFields();
		IllegalArgumentException validationException = null;
		for (Field f : flds) {
			PresentA pf = f.getAnnotation(PresentA.class);
			//System.err.printf("Processing %s %s%n", f.getName(), pf);
			if (pf != null) {
				int id = resolveId(inList ? pf.listViewFieldName() : pf.viewFieldName(), f.getName(), c);
				if (id != 0) {
					View v = pv.findViewById(id);
					if (v != null) {
						if (v instanceof EditText) {
							String t = ((EditText) v).getText().toString();
							if (!pf.normalize().isEmpty())
								t = normalize(t, pf.normalize());
							if (t.length() == 0) {
								t = pf.defaultTo();
								if (t.length() == 0 && pf.required())
									validationException = new IllegalArgumentException(
											"Required field " + f.getName() + " is missed ");
							}
							if (f.getType() == String.class)
								try {
									f.set(obj, t);
								} catch (IllegalArgumentException e) {
									if (Main.__debug)
										Log.e(TAG, String.format("Can't set value for %s, %s%n", f.getName(), e), e);

								} catch (IllegalAccessException e) {
									if (Main.__debug)
										Log.e(TAG, String.format("Make field '%s' public, %s%n", f.getName(), e));
								}
							else if (f.getType() == int.class) {
								try {
									f.setInt(obj, Integer.parseInt(t.trim()));
								} catch (Exception e) {

								}
							} else if (f.getType() == float.class) {
								try {
									f.setFloat(obj, Float.parseFloat(t.replace(',', '.').trim()));
								} catch (Exception e) {

								}
							} else if (f.getType() == double.class) {
								try {
									f.setDouble(obj, Double.parseDouble(t.replace(',', '.').trim()));
								} catch (Exception e) {

								}
							} else if (f.getType() == Date.class) {
								try {
									if (t.length() > 0) {
										SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);
										f.set(obj, df.parse(t));
									} else
										f.set(obj, null);
								} catch (Exception e) {

								}
							}
						} else if (v instanceof RadioButton || v instanceof CheckBox || v instanceof ToggleButton) {
							try {
								if (f.getType() == boolean.class) {
									f.setBoolean(obj, ((CompoundButton) v).isChecked());
								} else if (f.getType() == int.class) {
									f.setInt(obj, ((CompoundButton) v).isChecked() ? 1 : 0);
								} else if (f.getType() == String.class) {
									f.set(obj, ((CompoundButton) v).isChecked() ? "true" : Boolean.FALSE.toString());
								}
							} catch (IllegalArgumentException e) {
								if (Main.__debug)
									Log.e(TAG, String.format("Can't set value for %s, %s", f.getName(), e));
							} catch (IllegalAccessException e) {
								if (Main.__debug)
									Log.e(TAG, String.format("Make field '%s' public, %s", f.getName(), e));
							}
						} else if (v instanceof ImageView) {
							Object t = v.getTag();
							try {
								if (t != null)
									f.set(obj, t);
								else if (f.getType() == byte[].class) {
									Bitmap bm = ((ImageView) v).getDrawingCache();
									if (bm != null) {
										ByteArrayOutputStream bos = new ByteArrayOutputStream();
										bm.compress(Bitmap.CompressFormat.PNG, 85, bos);
										f.set(obj, bos.toByteArray());
									}
								}
							} catch (IllegalArgumentException e) {
								if (Main.__debug)
									Log.e(TAG, String.format("Can't set value for %s, %s", f.getName(), e));
							} catch (IllegalAccessException e) {
								if (Main.__debug)
									Log.e(TAG, String.format("Make field '%s' public, %s", f.getName(), e));
							}
						} else if (v instanceof Spinner) {
							Spinner sp = (Spinner) v;
							//Log.e(TAG, String.format("Load spinner at %d %s", sp.getSelectedItemPosition(),  sp.getSelectedItem()));
							if (f.getType().isEnum()) {
								try {
									f.set(obj, f.getType().getEnumConstants()[sp.getSelectedItemPosition()]);
								} catch (Exception e) {
									if (Main.__debug)
										Log.e(TAG, "", e);
								}
							} else if (f.getType() == String.class) {
								try {
									f.set(obj, sp.getSelectedItem());
								} catch (Exception e) {
									if (Main.__debug)
										Log.e(TAG, "" + sp.getSelectedItem(), e);
								}
							} else if (f.getType() == int.class || f.getType() == Integer.class) {
								try {
									f.set(obj, sp.getSelectedItemPosition());
								} catch (IllegalArgumentException e) {
									if (Main.__debug)
										Log.e(TAG, String.format("Can't set value for %s, %s", f.getName(), e));
								} catch (IllegalAccessException e) {
									if (Main.__debug)
										Log.e(TAG, String.format("Make field '%s' public, %s", f.getName(), e));
								}
							} else if (Main.__debug)
								Log.e(TAG, "Unsupported type for Spinner " + f.getType() + " for " + f.getName());
						} else if (v instanceof RatingBar) {
							float r = ((RatingBar) v).getRating();
							try {
								if (f.getType() == int.class) {
									f.setInt(obj, (int) (r * 100));
								} else if (f.getType() == float.class) {
									f.setFloat(obj, r);
								}
							} catch (Exception e) {
								if (Main.__debug)
									Log.e(TAG, "", e);
							}
						} else if (v instanceof Switch) {
							Switch sw = (Switch)v;
							try {
								if (f.getType() == boolean.class) {
									f.setBoolean(obj, sw.isChecked());
								} else  if (Main.__debug) {
									Log.e(TAG, "Unsupported type "+f.getType()+" for Switch "+f.getName());
								}
							} catch (Exception e) {
								if (Main.__debug)
									Log.e(TAG, "", e);
							}
						} else if (Main.__debug)
							Log.e(TAG, String.format("Unsupported widget type %s for field %s", v, f.getName()));
					} else if (Main.__debug)
						Log.e(TAG, String.format("(D)No view for %d / %s in %s for %s", id, f.getName(), pv, obj));
				}
				if (pf.viewTagName().length() > 0) {
					id = resolveId(pf.viewTagName(), f.getName(), c);
					if (id > 0)
						try {
							f.set(obj, pv.getTag(id));
						} catch (Exception e) {
							if (Main.__debug)
								Log.e(TAG, "", e);
						}
				} else if (pf.presentType() == FieldType.Hidden) {
					Object tags = pv.getTag();
					/*if (tags == null) {
						tags = new HiddenFieldsHolder();
						pv.setTag(tags);
						((HiddenFieldsHolder)tags).put(, arg1);
					}*/
					if (tags instanceof HiddenFieldsHolder) {
						try {
							f.set(obj, ((HiddenFieldsHolder) tags).get(f.getName()));
						} catch (Exception e) {
							if (Main.__debug)
								Log.e(TAG, "", e);
						}
					}
				}
			}
		}
	}

	protected String normalize(String t, String normalize) {
		if (t != null) {
			for (int i = 0; i < normalize.length(); i++) {
				switch (normalize.charAt(i)) {
				case 't':
					t = t.trim();
					break;
				case 'U':
					t = t.toUpperCase();
					break;
				case 'L':
					t = t.toLowerCase();
					break;
				case 'c':
					if (t.length() > 0) {
						char[] ts = t.toCharArray();
						boolean wasBl = true;
						for (int j = 0; j < ts.length; j++) {
							if (ts[j] != ' ') {
								if (wasBl) {
									ts[j] = Character.toUpperCase(ts[j]);
									wasBl = false;
								}
							} else
								wasBl = true;
						}
						t = new String(ts);
					}
					break;
				case 'C':
					if (t.length() > 0)
						t = Character.toUpperCase(t.charAt(0)) + t.substring(1);
					break;
				}
			}
		}
		return t;
	}

	public <DO> void fillViewRO(Context c, Activity a, DO obj) {
		fillView(c, a.getWindow().getDecorView(), obj, true);
	}

	public <DO> void fillView(Context c, Activity a, DO obj) {
		fillView(c, a.getWindow().getDecorView(), obj, false);
	}

	public <DO> void fillView(Context c, View pv, DO obj, boolean inList) {
		// TODO add dynamically generated cache of Ids
		if (obj == null)
			throw new IllegalArgumentException("No POJO specified");
		if (c == null)
			c = context;
		Field[] flds = obj.getClass().getFields();
		IllegalArgumentException validationException = null;
		for (Field f : flds) {
			PresentA pf = f.getAnnotation(PresentA.class);
			//System.err.printf("Processing %s %s%n", f.getName(), pf);
			if (pf != null) {
				int id = resolveId(inList ? pf.listViewFieldName().isEmpty()?pf.viewFieldName():pf.listViewFieldName() : pf.viewFieldName(), f.getName(), c);
				int resId = resolveId(pf.fillValuesResource(), f.getName(), c);
				int i = 0;
				if (id != 0) {
					View v = pv.findViewById(id);
					if (v != null) {
						try {
							Object d = f.get(obj);
							String t;
							Class<? extends ConverterI> convCl = (Class<? extends ConverterI>) pf.viewConvertor();
							if (convCl != ConverterI.class) {
								try {
									t = convCl.newInstance().to(d);
								} catch (Exception e) {
									t = "error";
									if (Main.__debug)
										Log.e(TAG, "Exception", e);
								}
							} else {
								if (d == null) {
									t = pf.defaultTo();
									if (v instanceof ImageView) {
										int imRes = resolveId(t, null, c);
										if (imRes != 0)
											((ImageView) v).setImageResource(imRes);
										v.setTag(null);
										continue;
									}
								} else {
									if (d instanceof Number) {
										if (FieldType.Money.equals(pf.presentType()))
											t = String.format("%2$s%1$.2f", d, Currency.getInstance (Locale.getDefault()).getSymbol());
										else if (FieldType.Quantity.equals(pf.presentType()))
											t = String.format("%1$.1f", d);
										else if (FieldType.Percent.equals(pf.presentType()))
											t = String.format("%1$.1f%%", d);
										else if (d instanceof Integer || d instanceof Long) {
											t = String.format("%d", d);
											i = ((Number) d).intValue();
										} else {
											int p = pf.presentPrecision();
											if (p > 0)
												t = String.format("%." + p + "f", d);
											else
												t = String.format("%f", d);
										}
									} else if (f.getType().isEnum()) {
										t = "";
										for (Object en : f.getType().getEnumConstants()) {
											if (d.equals(en)) {
												if (resId > 0)
													t = c.getResources().getStringArray(resId)[i];
												break;
											}
											i++;
										}
									} else if (d instanceof Date) {
										SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);
										t = df.format((Date) d);
									} else
										t = d.toString();
								}
							}
							// 
							if (v instanceof EditText) {
								if (d instanceof Number == false || ((Number) d).floatValue() != 0)
									((EditText) v).setText(t);
							} else if (v instanceof Spinner) { // resId != 0
								SpinnerAdapter adapter = null;
								if (resId > 0) {
									/*ArrayAdapter<CharSequence>*/ adapter = ArrayAdapter.createFromResource(c, resId,
											android.R.layout.simple_spinner_item);
									((ArrayAdapter<CharSequence>)adapter).setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
									((Spinner) v).setAdapter(adapter);
								} else {
									adapter = ((Spinner) v).getAdapter();
								}
								if (d instanceof String && adapter != null) {
									for (int n = adapter.getCount(); i < n; i++)
										if (d.equals(adapter.getItem(i))) {
											((Spinner) v).setSelection(i);
											break;
										}
								} else
									((Spinner) v).setSelection(i);
							} else if (v instanceof ImageView) {
								if (d instanceof Boolean)
									v.setVisibility((Boolean) d ? View.VISIBLE : View.INVISIBLE);
								else {
									((ImageView) v).setTag(d);
									if (d instanceof File) {  // instanceof Uri
										File imf = (File) d;
										if (imf.exists() == false) {
											int imRes = resolveId(t, null, c);
											if (imRes != 0)
												((ImageView) v).setImageResource(imRes);
										} else
											// TODO possible bm recycle 
											// TODO look also for scale attributes
											((ImageView) v)
													.setImageBitmap(BitmapFactory.decodeFile(((File) d).getPath()));
									} else if (d instanceof byte[]) {
										((ImageView) v).setImageBitmap(
												BitmapFactory.decodeByteArray((byte[]) d, 0, ((byte[]) d).length));
									} else if (d instanceof Integer) {
										((ImageView) v).setImageResource(((Integer)d).intValue());
									} else if (d instanceof String) { // TODO a string can be barcode, add a specific attribute
										((ImageView) v).setImageResource(resolveId((String)d, "", c));
									} else if (d instanceof Bitmap) {
										if (Main.__debug)
											Log.d(TAG, "Setting bitmap:"+d);
										((ImageView) v).setImageBitmap((Bitmap)d);
									}
								}
							} else if (v instanceof RadioButton || v instanceof CheckBox || v instanceof ToggleButton || v instanceof Switch) {
								if (d instanceof Boolean)
									((CompoundButton) v).setChecked((Boolean) d);
								else if (d instanceof Number)
									((CompoundButton) v).setChecked(((Number) d).intValue() != 0);
								else if (d instanceof String)
									((CompoundButton) v).setChecked("true".equalsIgnoreCase((String) d));
							} else if (v instanceof RatingBar) {
								float r = 0;
								if (d instanceof Float)
									r = ((Float) d).floatValue();
								else if (d instanceof Integer)
									r = ((Number) d).floatValue() / 100f;
								((RatingBar) v).setRating(r);
							} else if (v instanceof TextView) {
								((TextView) v).setText(t);
							} else if (Main.__debug) {
								Log.e(TAG, "Unsupported widget "+v);
							}
						} catch (IllegalArgumentException e) {
							if (Main.__debug)
								Log.e(TAG, "Exception", e);
						} catch (IllegalAccessException e) {
							if (Main.__debug)
								Log.e(TAG, "Exception for " + v, e);
						}

					} else if (Main.__debug)
						Log.e(TAG, String.format("(V)No view for %d / %s in %s for %s", id, f.getName(), pv, obj));
				}
				if (pf.viewTagName().length() > 0) {
					id = resolveId(pf.viewTagName(), f.getName(), c);
					if (id > 0)
						try {
							pv.setTag(id, f.get(obj));
						} catch (Exception e) {
							if (Main.__debug)
								Log.e(TAG, "", e);
						}
					//else if (Main.__debug)
					//Log.e(TAG, String.format("Id can't be resolved for %s in %s", f.getName(), pv));
				} else  if (pf.presentType() == FieldType.Hidden) {
					Object tags = pv.getTag();
					if (tags == null) {
						tags = new HiddenFieldsHolder();
						pv.setTag(tags);
					}
					if (tags instanceof HiddenFieldsHolder) {
						try {
							((HiddenFieldsHolder)tags).put(f.getName(), f.get(obj));
						} catch (Exception e) {
							if (Main.__debug)
								Log.e(TAG, "", e);
						}
					}
				}
			}
		}
	}

	/** stores image in model received as on activity result
	 * 
	 * @param c app context
	 * @param pv view with imageView field
	 * @param obj pojo to fill image file
	 * @param field field name to fill with image
	 * @param data intent passed to onActivityResult
	 */
	public <DO> void fillModel(Context c, View pv, DO obj, String field, Intent data) {
		if (data == null) {
			return;
		}
		Bundle extras = data.getExtras();
		if (extras != null) {
			if (data.getAction() != null) {
				try {
					Field f = obj.getClass().getField(field);
					PresentA pf = f.getAnnotation(PresentA.class);
					int id = resolveId(pf.viewFieldName(), f.getName(), c);
					if (id > 0) {
						Bitmap myBitmap = (Bitmap) extras.get("data");

						ImageView myImage = (ImageView) pv.findViewById(id);
						if (myImage != null) {
							myImage.setImageBitmap(myBitmap);
							if (f.getType() == File.class) {
								File imgFile = (File) f.get(obj);
								if (imgFile == null)
									throw new IllegalArgumentException(
											"Caller didn't set value of type File for " + field);
								FileOutputStream fos = null;
								try {
									fos = new FileOutputStream(imgFile);
									myBitmap.compress(Bitmap.CompressFormat.PNG, 85, fos);
									fos.flush();
									myImage.setTag(imgFile);
								} finally {
									try {
										fos.close();
									} catch (Exception e) {
									}
								}
							} else if (f.getType() == byte[].class) {
								ByteArrayOutputStream bOut = new ByteArrayOutputStream();
								try {
									myBitmap.compress(Bitmap.CompressFormat.PNG, 85, bOut);
									bOut.flush();
									myImage.setTag(bOut.toByteArray());
								} finally {
									try {
										bOut.close();
									} catch (Exception e) {
									}
								}
							}
						}
					}
				} catch (Exception e) {
					if (Main.__debug)
						Log.e(TAG, "", e);
				}
			}
		}
	}

	static int resolveId(String fn, String n, Context c) {
		int id = 0;

		if (fn == null || fn.length() == 0) {
			fn = RES_ID_PREF + "id/" + n;
		} //else
			//System.err.printf("vi:%s%n",fn);

		if (fn.startsWith(RES_ID_PREF)) {
			id = c.getResources().getIdentifier(fn.substring(2), null, c.getPackageName());
			//System.err.printf("Resolving %d for %s%n", id, fn);
		} else if (fn.startsWith("@")) {
			id = c.getResources().getIdentifier(fn, null, c.getPackageName());
		} else
			try {
				id = Integer.parseInt(fn);
			} catch (NumberFormatException ne) {

			}
		return id;
	}

	static class HiddenFieldsHolder extends HashMap<String, Object> {

	}
}
