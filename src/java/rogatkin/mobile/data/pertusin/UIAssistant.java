package rogatkin.mobile.data.pertusin;

import java.io.File;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;

import rogatkin.mobile.data.pertusin.PresentA.FieldType;
import android.app.Activity;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

public class UIAssistant {
	public final static String RES_ID_PREF = "@+";

	public static final String DATE_FORMAT = "MM/dd/yy";

	Context context;

	public UIAssistant() {

	}

	public UIAssistant(Context c) {
		context = c;
	}

	public <DO> void fillData(Context c, Activity a, DO obj) {
		fillData(c, a.getWindow().getDecorView(), obj, false);
	}

	public <DO> void fillData(Context c, View pv, DO obj, boolean inList) {
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
							if (t.length() == 0)
								t = pf.defaultTo();
							if (f.getType() == String.class)
								try {
									f.set(obj, t);
								} catch (IllegalArgumentException e) {
									System.err.printf("Can't set value for %s, %s%n", f.getName(), e);
								} catch (IllegalAccessException e) {
									System.err.printf("Make field '%s' public, %s%n", f.getName(), e);
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
						} else if (v instanceof RadioButton) {
							try {
								if (f.getType() == boolean.class) {
									f.setBoolean(obj, ((RadioButton) v).isChecked());
								} else if (f.getType() == int.class) {
									f.setInt(obj, ((RadioButton) v).isChecked() ? 1 : 0);
								}
							} catch (IllegalArgumentException e) {
								System.err.printf("Can't set value for %s, %s%n", f.getName(), e);
							} catch (IllegalAccessException e) {
								System.err.printf("Make field '%s' public, %s%n", f.getName(), e);
							}
						} else if (v instanceof ImageView) {
							Object t = v.getTag();
							try {
								f.set(obj, t);
							} catch (IllegalArgumentException e) {
								System.err.printf("Can't set value for %s, %s%n", f.getName(), e);
							} catch (IllegalAccessException e) {
								System.err.printf("Make field '%s' public, %s%n", f.getName(), e);
							}
						} else if (v instanceof Spinner) {
							if (f.getType().isEnum()) {
								int p = ((Spinner) v).getSelectedItemPosition();
								try {
									f.set(obj, f.getType().getEnumConstants()[p]);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}
					} else
						System.err.printf("No view for %d / %s%n", id, f.getName());
				} else {
					id = resolveId(pf.viewTagName(), f.getName(), c);
					if (id > 0)
						try {
							f.set(obj, pv.getTag(id));
						} catch (Exception e) {
							e.printStackTrace();
						}
				}
			}
		}
	}

	public <DO> void fillView(Context c, Activity a, DO obj) {
		fillView(c, a.getWindow().getDecorView(), obj, false);
	}

	public <DO> void fillView(Context c, View pv, DO obj, boolean inList) {
		if (obj == null)
			throw new IllegalArgumentException("No POJO specified");

		Field[] flds = obj.getClass().getFields();
		IllegalArgumentException validationException = null;
		for (Field f : flds) {
			PresentA pf = f.getAnnotation(PresentA.class);
			//System.err.printf("Processing %s %s%n", f.getName(), pf);
			if (pf != null) {
				int id = resolveId(inList ? pf.listViewFieldName() : pf.viewFieldName(), f.getName(), c);
				int resId = resolveId(pf.fillValuesResource(), f.getName(), c);
				int i = 0;
				if (id != 0) {
					View v = pv.findViewById(id);
					if (v != null) {
						try {
							Object d = f.get(obj);
							String t;
							// TODO apply converter
							if (d == null)
								t = "";
							else {
								if (d instanceof Number) {
									if (FieldType.Money.equals(pf.presentType()))
										t = String.format("%1$.2f", d);
									else if (FieldType.Quantity.equals(pf.presentType()))
										t = String.format("%1$.1f", d);
									else if (d instanceof Integer || d instanceof Long)
										t = String.format("%d", d);
									else {
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
									t = df.format((Date)d);
								} else
									t = d.toString();
							}
							// 
							if (v instanceof EditText) {
								if (d instanceof Number == false || ((Number) d).floatValue() != 0)
									((EditText) v).setText(t);
							} else if (v instanceof Spinner && resId > 0) { // resId != 0								
								ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(c, resId,
										android.R.layout.simple_spinner_item);
								adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
								((Spinner) v).setAdapter(adapter);
								((Spinner) v).setSelection(i);
							} else if (v instanceof ImageView) {
								if (d instanceof Boolean)
									v.setVisibility((Boolean) d ? View.VISIBLE : View.INVISIBLE);
								else {
									((ImageView) v).setTag(d);
									if (d instanceof File) {
										// TODO possible bm recycle 
										((ImageView) v).setImageBitmap(BitmapFactory.decodeFile(((File) d).getPath()));
									} else if (d instanceof byte[]) {
										((ImageView) v).setImageBitmap(BitmapFactory.decodeByteArray((byte[]) d, 0,
												((byte[]) d).length));
									}

								}
							} else if (v instanceof RadioButton) {
								if (d instanceof Boolean)
									((RadioButton) v).setChecked((Boolean) d);
							} else if (v instanceof TextView) {
								((TextView) v).setText(t);
							}
						} catch (IllegalArgumentException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}
				} else {
					id = resolveId(pf.viewTagName(), f.getName(), c);
					if (id > 0)
						try {
							pv.setTag(id, f.get(obj));
						} catch (Exception e) {

						}
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
			System.err.printf("Resolving %d for %s%n", id, fn);
		} else
			try {
				id = Integer.parseInt(fn);
			} catch (NumberFormatException ne) {

			}
		return id;
	}
}
