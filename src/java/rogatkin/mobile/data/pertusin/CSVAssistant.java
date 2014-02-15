package rogatkin.mobile.data.pertusin;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import android.util.Log;

public class CSVAssistant {
	char[] s;

	String delims;

	boolean trim;

	int fixedWidth;

	boolean sawAdlim = false;

	protected BufferedReader r;

	private int pos;

	private boolean eol, eof;

	// TODO add a version using Appendable

	/**
	 * Creates a tokenizer for specified string
	 * 
	 * @param a
	 *            string to create tokenizer for A default token separator ','
	 */
	public CSVAssistant(String _s) {
		this(_s, null, ",", true, 0);
	}

	/**
	 * Creates a tokenizer for specified Reader
	 * 
	 * @param _r
	 *            Reader A default token separator ','
	 */
	public CSVAssistant(BufferedReader _r) {
		this(null, _r, ",", true, 0);
	}

	/**
	 * Creates a tokenizer for string or reader
	 * 
	 * @param _s
	 *            string
	 * @param _r
	 *            reader
	 * @param _delims
	 *            token separators
	 * @param _trim
	 *            true to trim tokens
	 * @param _fixedWidth
	 *            fixed width tokens with specified width
	 */
	public CSVAssistant(String _s, BufferedReader _r, String _delims, boolean _trim, int _fixedWidth) {
		if (_s != null)
			s = _s.toCharArray();
		delims = _delims;
		trim = _trim;
		fixedWidth = _fixedWidth;
		r = _r;
	}

	/**
	 * Check if more tokens available
	 * 
	 * @return true if more tokens
	 * @throws IOException
	 *             can't read more
	 */
	public boolean hasMoreTokens() throws IOException {
		if (eol == true || eof) {
			return false;
		}
		if (sawAdlim == true) {
			// Previous token ended at a delim. So, even if there is no
			// charcter after it, it can be considered as an empty token
			// at the end of line, e.g. as in line "token1,token2," (empty
			// token after second coma)
			return true;
		} // Else see if there is any non white space character left
		int i = 0;
		try {
			do {
				r.mark(2);
				i = r.read();
				if (i == '\r' || i == '\n') {
					r.reset();
					return false;

				} else {
					if (i < 0) {
						eof = eol = true;
						return false;
					}
					r.reset();
					return true;
				}
			} while (i >= 0);
		} catch (Exception e) {
			throw new IOException("Error in processing input file stream: " + e.getMessage());
		}
	}

	/**
	 * Returns next token using specified delimitter
	 * 
	 * @param _delim
	 * @return next token
	 * @exception NoSuchElementException
	 *                , IllegalArgumentException
	 */
	public String nextToken(String _delim) {
		if (s != null)
			return nextTokenS(_delim);
		else if (r != null)
			try {
				return nextTokenR(_delim);
			} catch (IOException ioe) {
				//Logger.error(CLASS_ID, "IO error in reading next token.",
				// ioe);
				throw new NoSuchElementException("IO error in reading next token." + ioe);
			}
		else
			throw new IllegalArgumentException(
					"The tokenizer was initialized not properly, both token sources are undefined.");
	}

	/**
	 * Reads next token from reader
	 * 
	 * @param _delim
	 *            used delimiter
	 * @return next token
	 * @throws IOException
	 */
	public String nextTokenR(String _delim) throws IOException {
		sawAdlim = false; // Reset this
		if (eof)
			throw new NoSuchElementException();
		char c;
		int i;
		StringBuffer result = new StringBuffer(20);
		if (eol == true)
			while ((i = r.read()) >= 0 && (i == '\r' || i == '\n'))
				;
		else
			i = r.read();

		if (i < 0) {
			eof = true;
			return "";
		}

		if (i == '\r' || i == '\n') {
			eol = true;
			if (debug_)
				Log.d(TAG, "Found '', EOL:true");
			return "";
		}

		eol = false;

		if (trim && Character.isWhitespace((char) i) && _delim.indexOf(i) < 0 && i != '\r' && i != '\n')
			while ((i = r.read()) >= 0 && Character.isWhitespace((char) i) && _delim.indexOf(i) < 0 && i != '\r'
					&& i != '\n')
				;
		if (i < 0) {
			eof = true;
			return "";
		}
		c = (char) i;
		char quoted = 0;
		if (c == '"' || c == '\'')
			quoted = c;
		else if (_delim.indexOf(c) >= 0)
			return "";
		else
			result.append(c);
		boolean metQ = false;
		StringBuffer whites = trim ? new StringBuffer(20) : null;
		while ((i = r.read()) >= 0) {
			c = (char) i;
			if (debug_)
				Log.d(TAG, "Read '" + c + "'");
			if (c == quoted)
				if (metQ) {
					result.append(c);
					metQ = false;
				} else
					metQ = true;
			else if (_delim.indexOf(c) >= 0 || c == '\r' || c == '\n') {
				if (_delim.indexOf(c) >= 0) { // Delimiter found
					sawAdlim = true;
				}
				if (quoted == 0 || (quoted != 0 && metQ)) {
					eol = c == '\r' || c == '\n';
					if (debug_)
						Log.d(TAG, "Found " + result + ",EOL:" + eol);
					return result.toString();
				} else
					result.append(c);
			} else {
				if (quoted != 0)
					result.append(c);
				else if (Character.isWhitespace(c) && _delim.indexOf(c) < 0) {
					if (trim)
						whites.append(c);
					else
						result.append(c);
				} else {
					if (trim) {
						result.append(whites);
						whites.setLength(0);
					}
					result.append(c);
				}
			}
		}
		return result.toString();
	}

	/**
	 * Gets next token from string
	 * 
	 * @param _delim
	 * @return next token
	 * @exception NoSuchElementException
	 */
	public String nextTokenS(String _delim) {
		if (eol)
			throw new NoSuchElementException();
		else if (pos >= s.length) {
			eol = true;
			return "";
		}
		int st = pos;
		if (trim)
			while (st < s.length && Character.isWhitespace(s[st]) && _delim.indexOf(s[st]) < 0)
				st++;

		char quoted = 0;
		boolean quotedTwice = false;
		List quotedTwiceVector = new ArrayList();
		if (s[st] == '"' || s[st] == '\'')
			quoted = s[st++];
		for (pos = st; pos < s.length; pos++)
			if (quoted != 0) {
				if (s[pos] == quoted) {
					//make sure the next char is not quoted
					// if the next char is also quoted then its not
					// end of token. Its a double quote within data
					if (pos < s.length - 1 && s[pos + 1] == quoted) {
						quotedTwiceVector.add("" + (pos - st));
						pos++;
						quotedTwice = true;
						continue;
					} else
						break;
				}
			} else if (_delim.indexOf(s[pos]) >= 0)
				break;
		int et = pos;
		if (quoted != 0 && s[pos] == quoted)
			pos++;
		pos++;
		if (debug_)
			System.out.println("Tokenizer: st=" + st + " et=" + et + " pos=" + pos);
		String token = new String(s, st, et - st);
		if (quotedTwice) {
			if (debug_)
				Log.d(TAG, "Token before removing double quotes " + token);
			StringBuffer sbToken = new StringBuffer(token);
			for (int i = quotedTwiceVector.size() - 1; i >= 0; i--) {
				if (debug_)
					Log.d(TAG, "Deleting char at " + Integer.parseInt((String) quotedTwiceVector.get(i)));
				sbToken.deleteCharAt(Integer.parseInt((String) quotedTwiceVector.get(i)));
			}
			token = sbToken.toString();
			if (debug_)
				Log.d(TAG, "Token after removing double quotes " + token);
		}
		return token;
	}

	/**
	 * returns next token using default delimiter
	 * 
	 * @return next token
	 * @exception NoSuchElementException
	 */
	public String nextToken() {
		return nextToken(delims);
	}

	/**
	 * Moves to next line in multiline tokens
	 * 
	 * @return true if there is a next line
	 */
	public boolean advanceToNextLine() {
		if (eof)
			return false; // should be an exception
		sawAdlim = false; // Reset this
		int i = -1;
		if (r != null) {
			try {
				do {
					// Skips all characters that comes before the next first
					// new
					// line character
					if (!eol) {
						while ((i = r.read()) >= 0 && (i != '\r') && (i != '\n'))
							//System.err.println("--"+i)
							;
						if (i < 0) {
							eof = eol = true;
							return false;
						}
					}

					// Skips the next new line character too
					do {
						r.mark(2);
						i = r.read();
						//System.err.println("-]-"+i);
					} while (i >= 0 && ((i == '\r') || (i == '\n')));
					if (i >= 0) {
						r.reset();
					} else
						eof = true;
					eol = false;
				} while (!hasMoreTokens()); // Skips empty lines
			} catch (Exception e) {
				Log.e(TAG, "Error reading stream" + e);
				eof = true;
				eol = true;
				return false;
			}
		}
		if (i >= 0) {
			eol = false;
			eof = false;
			return true;
		} else {
			eol = true;
			eof = true;
			return false;
		}
	}

	protected final static boolean debug_ = false;

	private final static String TAG = "CsvAssistant";
}
