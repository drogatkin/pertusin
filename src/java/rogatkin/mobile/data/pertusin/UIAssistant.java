package rogatkin.mobile.data.pertusin;

import java.lang.reflect.Field;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

public class UIAssistant {
	public final static String RES_ID_PREF = "@+id/";

	public <DO> void fillContent(Context c, Activity a, DO obj) {
		if (obj == null)
			throw new IllegalArgumentException("No POJO specified");

		Field[] flds = obj.getClass().getFields();
		IllegalArgumentException validationException = null;
		for (Field f : flds) {
			PresentA pf = f.getAnnotation(PresentA.class);
			//System.err.printf("Processing %s %s%n", f.getName(), pf);
			if (pf != null) {
				String fn = pf.viewFieldName();
				
				if (fn == null || fn.length() == 0) {
					fn = RES_ID_PREF + f.getName();
				} //else
					//System.err.printf("vi:%s%n",fn);
				int id = 0;
				if (fn.startsWith(RES_ID_PREF)) {
					id = c.getResources().getIdentifier(fn.substring(2), null, c.getPackageName());
System.err.printf("Resolving %d for %s%n", id, fn);
				} else
					try {
						id = Integer.parseInt(fn);
					} catch (NumberFormatException ne) {

					}
				if (id != 0) {
					View v = a.findViewById(id);
					if (v != null) {
						if (v instanceof EditText) {
							String t  = ((EditText) v).getText().toString();
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
						} else if (v instanceof ImageView) {
							
						}
					}
				}
			}
		}
	}
}
