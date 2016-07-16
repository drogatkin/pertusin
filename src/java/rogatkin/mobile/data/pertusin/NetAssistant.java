package rogatkin.mobile.data.pertusin;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.net.ssl.SSLSocketFactory;

//import android.util.Base64;

public class NetAssistant {
	protected final static int DEF_TIMEOUT = 2; // minutes

	protected final static int DEF_MAILPORT = 25;

	/**
	 * SMTP server connection timeout in minutes
	 * 
	 */
	public final static String PROP_MAILTIMEOUT = "MAILTIMEOUT";

	/**
	 * SMTP server host
	 */
	public final static String PROP_MAILHOST = "MAILHOST";

	/**
	 * SMTP server port
	 */
	public final static String PROP_MAILPORT = "MAILPORT";

	/**
	 * SMTP server account required for authentication
	 */
	public final static String PROP_POPACCNT = "MAILACCOUNT";

	/**
	 * SMTP server password
	 */
	public final static String PROP_PASSWORD = "MAILPASSWORD";

	/**
	 * Using secure layer flag
	 */
	public final static String PROP_SECURE = "MAILSECURELAYER";

	protected Properties properties;

	protected static final SimpleDateFormat PROTOCOL_GMTDATE = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss 'GMT'",
			Locale.US);
	protected static final SimpleDateFormat PROTOCOL_LOCALDATE = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z",
			Locale.US);

	static {
		TimeZone tz = TimeZone.getTimeZone("GMT");
		tz.setID("GMT");
		PROTOCOL_GMTDATE.setTimeZone(tz);
	}

	protected static String fixDataLines(String s) {
		StringBuffer data = new StringBuffer(s.length());
		StringBuffer line = new StringBuffer(128);

		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);

			switch (ch) {
			case '\r':
				break;

			case '\n':
				data.append(line);
				data.append("\r\n");
				line.setLength(0);
				break;
			case '.':
				if (line.length() == 0)
					line.append(ch);
			default:
				line.append(ch);
				break;
			}
		}

		if (line.length() > 0) {
			data.append(line);
			data.append("\r\n");
		}

		return data.toString();
	}

	protected static String escapeSpecials(String s) {
		StringBuffer result = new StringBuffer(s.length());
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '<' || c == '>')
				result.append('\\');
			result.append(c);
		}
		return result.toString();
	}

	public static String splitLine(String line, int size) {
		if (line.length() <= size)
			return line;
		int l = line.length();
		int chunks = l / size;
		StringBuffer result = new StringBuffer(l + chunks * 2);
		int n = 0;
		for (int i = 0; i < chunks; i++) {
			result.append(line.substring(n, n + size));
			n += size;
			result.append("\r\n");
		}
		result.append(line.substring(n));
		return result.toString();
	}

	/**
	 * Creates SendMail object and set base SMTP server protocol parameters
	 * 
	 * @param _properties
	 *            SMTP server connection properties
	 */
	public NetAssistant(Properties _properties) {
		properties = _properties;
	}

	/**
	 * Initiates of mail send.
	 * <p>
	 * An example of use:
	 * 
	 * <pre>
	 * Properties emailHeaders = new Properties();
	 * emailHeaders.put(&quot;To&quot;, &quot;undisclosed-recipients:;&quot;);
	 * emailHeaders.put(&quot;Mime-Version&quot;, &quot;1.0&quot;);
	 * emailHeaders.put(&quot;Content-Type&quot;, &quot;text/plain; format=flowed; charset=UTF-8&quot;);
	 * try {
	 * 	sm.send(adminAddress, up.getField(&quot;userEmail&quot;), &quot;Activation notice&quot;,
	 * 			Substitutor.substitute(&quot;activationemail.txt&quot;, map, properties, locale, tz), emailHeaders);
	 * } catch (IOException ioe) {
	 * }
	 * </pre>
	 * 
	 * @param _mailFrom
	 *            address from
	 * @param _mailTo
	 *            address to, can be overridden to appear in to field
	 *            differently if "TO" property specified in extra headers
	 * @param _subject
	 *            subject of mail
	 * @param _body
	 *            body of mail, can be html or have attachments if extra headers
	 *            specified
	 * @param _extraHeaders
	 *            allow to customize e-mail using different display TO values,
	 *            MIME types and so on
	 * @throws IOException
	 *             if there is a problem of sending e-mail including error
	 *             responses from SMTP server
	 */
	public void send(String _mailFrom, String _mailTo, String _subject, String _body, Properties _extraHeaders)
			throws IOException {
		if (properties == null)
			properties = System.getProperties();

		boolean ssl = "true".equals(properties.getProperty(PROP_SECURE, "false"));
		String mailHost = properties.getProperty(PROP_MAILHOST, "Unknown");
		int mailPort = DEF_MAILPORT;
		try {
			mailPort = Integer.parseInt(properties.getProperty(PROP_MAILPORT, "" + DEF_MAILPORT));
		} catch (Exception e) {
		}
		String popAccount = properties.getProperty(PROP_POPACCNT, _mailFrom);
		String myDomain = popAccount;
		int atp = myDomain.indexOf('@');
		if (atp >= 0)
			myDomain = myDomain.substring(atp + 1);
		TextSocket s = null;
		try {
			s = new TextSocket(mailHost, mailPort, ssl);
			int timeout = DEF_TIMEOUT;
			try {
				timeout = Integer.parseInt(properties.getProperty(PROP_MAILTIMEOUT));
			} catch (Exception e) {
			}
			s.setSoTimeout(timeout * 60 * 1000);

			s.getResult();

			s.write("HELO ");
			s.writeLine(myDomain);
			s.flush();

			if (s.getResult() != 250)
				throw new IOException("At presenting POP account " + myDomain + ", a mail server returned code "
						+ s.lastResult());
			String password = properties.getProperty(PROP_PASSWORD);
			if (password != null && password.length() > 0) {
				s.writeLine("AUTH PLAIN");
				s.flush();
				if (s.getResult() != 334)
					throw new IOException("At plain authentication of " + popAccount + ", a mail server returned code "
							+ s.lastResult());
				byte[] acntBytes = popAccount.getBytes();
				byte[] passwdBytes = password.getBytes();
				byte[] credentialBytes = new byte[acntBytes.length + passwdBytes.length + 2];
				System.arraycopy(acntBytes, 0, credentialBytes, 1, acntBytes.length);
				System.arraycopy(passwdBytes, 0, credentialBytes, acntBytes.length + 2, passwdBytes.length);
				//String ss;
				//s.writeLine(ss=Base64.encodeToString(credentialBytes, Base64.DEFAULT));
				s.writeLine(Base64.base64Encode(credentialBytes));
				s.flush();
				if (s.getResult() != 235)
					throw new IOException("At credentials check of " + popAccount + ", a mail server returned code "
							+ s.lastResult());// + "\n"+ss+"\n"+password);
				// 535 5.7.1 Credentials Rejected 12sm5390241nzn
			}

			s.write("MAIL FROM:<");
			// TODO: check that args[2] is correct domain name
			// s.write(escapeSpecials(_mailFrom));

			s.write(_mailFrom);

			if (_mailFrom.indexOf('@') < 0) {
				s.write("@");
				s.write(mailHost);
			}
			s.writeLine(">");
			s.flush();

			if (s.getResult() != 250)
				throw new IOException(String.format("From: address '%s' (%s) was rejected with code %s", _mailFrom,
						mailHost, s.lastResult()));

			StringTokenizer t = new StringTokenizer(_mailTo, ";,");
			while (t.hasMoreTokens()) {
				s.write("RCPT TO:<");
				String toAddr = t.nextToken();
				s.write(escapeSpecials(toAddr));
				if (toAddr.indexOf('@') < 0) {
					s.write("@");
					s.write(mailHost);
				}
				s.writeLine(">");
				s.flush();

				if (s.getResult() != 250)
					throw new IOException(String.format("Recipient to: address '%s' was rejected with code %s", toAddr,
							s.lastResult()));
			}

			s.writeLine("DATA");
			s.flush();

			s.getResult();

			s.write("Subject: ");
			s.writeLine(_subject);
			s.write("From: ");
			s.writeLine(_mailFrom);
			String charSet = null;
			boolean wasTo = false;
			boolean wasMailer = false;
			if (_extraHeaders != null) {
				Enumeration e = _extraHeaders.keys();
				while (e.hasMoreElements()) {
					String name = (String) e.nextElement();
					wasTo |= name.regionMatches(true, 0, "TO", 0, 2);
					wasMailer |= name.regionMatches(true, 0, "X-Mailer", 0, "X-Mailer".length());
					s.writeLine(name + ": " + _extraHeaders.getProperty(name));
					if (charSet == null && "Content-Type".equalsIgnoreCase(name)) {
						String cts = _extraHeaders.getProperty(name);
						int csp = cts.indexOf("charset=");
						if (csp > 0)
							charSet = cts.substring(csp + "charset=".length()).trim();
					}
				}
			}
			if (wasTo == false) {
				s.write("To: ");
				s.writeLine(_mailTo);
			}
			s.write("Date: ");
			synchronized (PROTOCOL_GMTDATE) {
				s.writeLine(PROTOCOL_GMTDATE.format(new Date()));
			}
			if (wasMailer == false)
				s.writeLine("X-Mailer: Pertusin - Android MVC library by D Rogatkin");

			s.writeLine("");

			if (_body != null && _body.length() > 0) {
				if (charSet == null)
					s.writeBytes(fixDataLines(_body).getBytes());
				else
					s.writeBytes(fixDataLines(_body).getBytes(charSet));
			}
			s.writeLine(".");
			s.flush();
			if (s.getResult() != 250)
				throw new IOException("At writing body the mail server returned code " + s.lastResult());

			s.writeLine("QUIT");
			s.flush();
		} catch (IOException ioe) {
			throw ioe;
		} catch (Exception ex) {
			throw (IOException) new IOException("By: " + ex).initCause(ex);
		} finally {
			if (s != null)
				s.close(); // TODO should catch IOException to do not mask an original exception
		}
	}

	public static void main(String... args) {
		if (args.length < 3) {
			System.out
					.println("Use: SendMail to subject body [host] [port] [account] [pass] [ssl]"
							+ "\nFor example: SendMail jaddressbook@gmail.com Simple \"no body\" smtp.gmail.com 465 jaddressbook@gmail.com cu1cu1 true");
			System.exit(255);
			return;
		}

		testSend(0, args);
	}

	static void testSend(int s, String... args) {
		try {
			new NetAssistant(argsArrayToProps(s + 3, args)).send(args[s], args[s], args[s + 1], args[s + 2], null);
		} catch (IOException e) {
			System.out.println("Error in send:" + e);
			e.printStackTrace();
		}
	}

	static Properties argsArrayToProps(int shift, String... args) {
		Properties properties = null;
		if (args.length > shift) {
			properties = new Properties();
			properties.setProperty(PROP_MAILHOST, args[shift]);
			if (args.length > shift + 1) {
				properties.setProperty(PROP_MAILPORT, args[shift + 1]);
				if (args.length > shift + 2) {
					properties.setProperty(PROP_POPACCNT, args[shift + 2]);
					if (args.length > shift + 3) {
						properties.setProperty(PROP_PASSWORD, args[shift + 3]);
						if (args.length > shift + 4) {
							properties.setProperty(PROP_SECURE, args[shift + 4]);
						}
					}
				}
			}
		}
		return properties;
	}

	public <T extends Message> int receive(int truncateTo, Visitor<T> messageVisitor) throws IOException {
		if (properties == null)
			properties = System.getProperties();
		int result = 0;
		boolean ssl = "true".equals(properties.getProperty(PROP_SECURE, "false"));
		String mailHost = properties.getProperty(PROP_MAILHOST, "Unknown");
		int mailPort = DEF_MAILPORT;
		try {
			mailPort = Integer.parseInt(properties.getProperty(PROP_MAILPORT, "" + DEF_MAILPORT));
		} catch (Exception e) {
		}
		TextSocket s = null;
		try {
			s = new TextSocket(mailHost, mailPort, ssl, true);
			int timeout = parseNumber(properties.getProperty(PROP_MAILTIMEOUT), DEF_TIMEOUT);
			s.setSoTimeout(timeout * 60 * 1000);
			if (s.getPopResult() != 0)
				throw new IOException(String.format("No positive greetings from server %s", s.lastResult()));
			s.write("USER ");
			s.writeLine(properties.getProperty(PROP_POPACCNT, "Guest"));
			if (s.getPopResult() != 0)
				throw new IOException(String.format("User wasn't accepeted %s", s.lastResult()));
			s.write("PASS ");
			s.writeLine(properties.getProperty(PROP_PASSWORD, ""));
			if (s.getPopResult() != 0)
				throw new IOException(String.format("Auth failed %s", s.lastResult()));
			s.writeLine("STAT");
			if (s.getPopResult() != 0)
				throw new IOException(String.format("Stat failed %s", s.lastResult()));
			String parts[] = s.lastResult().split(" ");
			if (parts.length != 3)
				throw new IOException(String.format("Unrecognized Stat response %s", s.lastResult()));
			int nm = parseNumber(parts[1], -1);
			if (nm <= 0)
				return result;

			for (int i = 0; i < nm; i++) {
				Message m = new Message();
				if (truncateTo < 0) {
					s.writeLine("RETR " + (i + 1));
					if (s.getPopResult() != 0)
						throw new IOException(String.format("Retr failed %s", s.lastResult()));
					StringBuilder sb = new StringBuilder();
					String l = s.readLine();
					while (l.equals(".") == false) {
						if (l.length() > 0 && l.charAt(0) == '.')
							l = l.substring(1);
						m.build(l);
						l = s.readLine();
					}
				} else {
					s.write("TOP ");
					s.write("" + (i + 1));
					s.writeLine(" " + truncateTo);
					if (s.getPopResult() != 0)
						throw new IOException(String.format("Top failed %s", s.lastResult()));
					for (int j = 0; j < truncateTo + 1; j++)
						m.build(s.readLine());
				}
				if (messageVisitor.visit((T) m)) {
					s.writeLine("DELE " + (i + 1));
					if (s.getPopResult() != 0)
						throw new IOException(String.format("Dele failed %s", s.lastResult()));
					result++;
				}
			}
			s.writeLine("QUIT");
			s.getPopResult();
		} catch (IOException ioe) {
			throw ioe;
		} catch (Exception ex) {
			throw (IOException) new IOException("By: " + ex).initCause(ex);
		} finally {
			if (s != null) {
				try {
					s.close();
				} catch (IOException ioe) {

				}
			}
		}
		return result;
	}

	/*Message receive(String subjRegExp, long id, int truncate) throws IOException {
		Message result = null;
		return result;
	}*/

	static int parseNumber(String n, int def) {
		try {
			return Integer.parseInt(n);
		} catch (Exception e) {
		}
		return def;
	}

	public static class Message {
		public long size;

		HashMap<String, String> headers;

		StringBuilder body;

		private String folding;

		protected void build(String l) {
			//System.out.println(l);
			if (body != null) {
				body.append(l).append("\r\n");
			} else {
				if (headers == null)
					headers = new HashMap<String, String>();

				if (l.length() > 0 && Character.isWhitespace(l.charAt(0)))
					if (folding != null)
						folding += l;
					else
						throw new IllegalArgumentException("First line requested folding:" + l);
				else {
					if (l.length() == 0) {
						body = new StringBuilder();
					}
					// if line pending?
					if (folding != null) {
						int c = folding.indexOf(':', 0);
						if (c > 0) {
							String key = folding.substring(0, c).trim().toLowerCase();
							String value = folding.substring(c + 1).trim();
							headers.put(key.toUpperCase(), value);
						} else
							throw new IllegalArgumentException("Unparseable header line:" + folding);
					}
					folding = l;
				}
			}
		}

		public String getSubject() {
			if (headers != null && headers.containsKey("SUBJECT"))
				return headers.get("SUBJECT");
			return "";
		}

		public String getBody() {
			if (body == null)
				return "";
			return body.toString();
		}

		public Date getDate() {
			if (headers != null && headers.containsKey("DATE")) {
				synchronized (PROTOCOL_LOCALDATE) {
					try {
						return PROTOCOL_LOCALDATE.parse(headers.get("DATE"));
					} catch (ParseException e) {
						//System.err.println("Can't parse " + headers.get("DATE") + " " + e);
					}
				}
			}
			return null;
		}

		public String getFrom() {
			if (headers != null && headers.containsKey("FROM"))
				return headers.get("FROM");
			return "";
		}

		public String getTo() {
			if (headers != null && headers.containsKey("TO"))
				return headers.get("TO");
			return "";
		}

		public String getHeader(String key) {
			if (headers != null && headers.containsKey(key.toUpperCase()))
				return headers.get(key.toUpperCase());
			return null;
		}

		@Override
		public String toString() {
			return "Message [size=" + size + ", subject=" + getSubject() + ", from " + getFrom() + ", on " + getDate()
					+ ", body=" + body + "]";
		}
	}

	public static interface Visitor<T extends Message> {
		boolean visit(T t);
	}

	public static class TextSocket {
		protected String m_lastResult;

		protected int m_code;

		protected BufferedReader m_input;

		protected OutputStream m_output;

		protected boolean flushOnEOL;

		protected Socket socket;

		public static final byte[] LS = new byte[] { (byte) 13, (byte) 10 };

		protected void createStreams() throws IOException {
			m_input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			m_output = new BufferedOutputStream(socket.getOutputStream());
		}

		TextSocket(String host, int port, boolean ssl) throws UnknownHostException, IOException {
			this(host, port, ssl, false);
		}

		TextSocket(String host, int port, boolean ssl, boolean feol) throws UnknownHostException, IOException {
			if (ssl)
				socket = SSLSocketFactory.getDefault().createSocket(host, port);
			else
				socket = new Socket(host, port);
			flushOnEOL = feol;
			createStreams();
		}

		void setSoTimeout(int timeout) throws SocketException {
			socket.setSoTimeout(timeout);
		}

		void close() throws IOException {
			socket.close();
		}

		public BufferedReader getBufferedInputStream() {
			return m_input;
		}

		public OutputStream getBufferedOutputStream() {
			return m_output;
		}

		public String readLine() throws IOException {
			//String s = m_input.readLine();
			//System.out.print(s);
			return m_input.readLine();//s;
		}

		public void write(String s) throws IOException {
			//System.out.print(s);
			m_output.write(s.getBytes());
		}

		public void writeBytes(byte[] bytes) throws IOException {
			m_output.write(bytes);
		}

		public void writeLine(String s) throws IOException {
			write(s);
			m_output.write(LS);
			if (flushOnEOL)
				m_output.flush();
		}

		public void flush() throws IOException {
			m_output.flush();
		}

		public String lastResult() {
			if (m_lastResult == null)
				return "COnnection lost";
			return m_lastResult;
		}

		public int lastResultCode() {
			return m_code;
		}

		public int getPopResult() throws IOException {
			m_lastResult = m_input.readLine();
			if (m_lastResult == null || m_lastResult.length() == 0) {
				return -1;
			}

			if (m_lastResult.startsWith("-ERR")) {
				try {
					m_code = Integer.parseInt(m_lastResult.substring(4, 6));
				} catch (Exception e) {
					m_code = -2;
				}
			} else if (m_lastResult.startsWith("+OK"))
				m_code = 0;
			else
				m_code = -3;
			return m_code;
		}

		public int getResult() throws IOException {
			m_lastResult = m_input.readLine();

			if (m_lastResult == null || m_lastResult.length() == 0) {
				return -1;
			}

			m_code = resultCode(m_lastResult);

			// See if it's a multiline response (nnn-...)
			if (m_code != -1 && m_lastResult.length() > 3 && m_lastResult.charAt(3) == '-') {

				do {
					String res = m_input.readLine();

					if (res.length() == 0) {
						break;
					}

					m_lastResult = m_lastResult + "\r\n" + res;

					// If it's a normal, matching reply code, we're done
					if (resultCode(res) == m_code && res.length() > 3 && res.charAt(3) == ' ') {
						break;
					}
				} while (true);
			}

			return m_code;
		}

		// Parse the first three digits of s; return -1
		// if malformed
		public int resultCode(String s) {
			int code = 0;
			int n;

			for (n = 0; n < 3 && n < s.length(); n++) {
				char ch = s.charAt(n);

				if (ch < '0' || ch > '9') {
					return -1;
				}
				code = 10 * code + ch - '0';
			}
			return code;
		}

		public boolean isMultilineResult(String s) {
			int n;

			for (n = 0; n < 3 && n < s.length(); n++) {
				if (s.charAt(n) < '0' || s.charAt(n) > '9') {
					return true;
				}
			}

			if (n < s.length() && s.charAt(n) != ' ') {
				return true;
			}
			return false;
		}
	}
}
