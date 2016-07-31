package rogatkin.mobile.data.pertusin;

import java.util.regex.Pattern;

public class ValidateAssistant {
	static final String EMAIL_REGEXP = "\\w+([-+.']\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*";
	static final String PASSWD_REGEXP = "\\.*(?=.{8,})(?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).*";
	static final String PHONE_REGEXP_US = "^[0-9\\+]{1,}[0-9\\-]{3,15}";

	Pattern emailP, passP, phoneP;

	public ValidateAssistant() {
		emailP = Pattern.compile(EMAIL_REGEXP);
		passP = Pattern.compile(PASSWD_REGEXP);
		phoneP = Pattern.compile(PHONE_REGEXP_US);
	}

	public boolean validateEmail(String email) {
		return email.length() < 255 && emailP.matcher(email).matches();
	}

	public boolean validatePassword(String password) {
		return password.length() > 7 && password.length() < 40 && passP.matcher(password).matches();
	}

	public boolean validatePhone(String phone, int type) {
		switch (type) {
		case 0:
			return phone.length() > 6 && phone.length() < 16 && phoneP.matcher(phone).matches();
		}
		return true;
	}
}
