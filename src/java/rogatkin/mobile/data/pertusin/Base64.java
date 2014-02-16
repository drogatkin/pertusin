package rogatkin.mobile.data.pertusin;

import java.io.UnsupportedEncodingException;
import android.util.Log;

public class Base64 {
	public static final String ISO_8859_1 = "iso-8859-1"; // the same as in Value

	public static final String UTF_8 = "UTF-8"; // the same as in Value

	protected final static char BASE64ARRAY[] = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
			'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i',
			'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3',
			'4', '5', '6', '7', '8', '9', '+', '/' };

	/**
	 * Translates a Base64 value to either its 6-bit reconstruction value or a negative number indicating some other meaning.
	 */
	protected final static byte[] DECODABET = { -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 0 - 8
			-5, -5, // Whitespace: Tab and Linefeed
			-9, -9, // Decimal 11 - 12
			-5, // Whitespace: Carriage Return
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 14 -
			// 26
			-9, -9, -9, -9, -9, // Decimal 27 - 31
			-5, // Whitespace: Space
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 33 - 42
			62, // Plus sign at decimal 43
			-9, -9, -9, // Decimal 44 - 46
			63, // Slash at decimal 47
			52, 53, 54, 55, 56, 57, 58, 59, 60, 61, // Numbers zero through nine
			-9, -9, -9, // Decimal 58 - 60
			-1, // Equals sign at decimal 61
			-9, -9, -9, // Decimal 62 - 64
			0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, // Letters 'A'
			// through 'N'
			14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, // Letters 'O'
			// through 'Z'
			-9, -9, -9, -9, -9, -9, // Decimal 91 - 96
			26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, // Letters 'a'
			// through 'm'
			39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, // Letters 'n'
			// through 'z'
			-9, -9, -9, -9 // Decimal 123 - 126
	};

	protected final static byte WHITE_SPACE_ENC = -5; // Indicates white space

	// in encoding

	protected final static byte EQUALS_SIGN_ENC = -1; // Indicates equals sign

	// in encoding

	/** The equals sign (=) as a byte. */
	protected final static byte EQUALS_SIGN = (byte) '=';

	/** base 64 encoding, string converted to bytes using specified encoding
	 * @param String <val>_s</val> original string to encode
	 * @param String encoding, can be null, then iso-8859-1 used
	 * @return String result of encoding as iso-8859-1 string<br>
	 * return null in case of invalid encoding or original string null
	 * @exception no exceptions
	 */
	public final static String base64Encode(String s, String enc) {
		if (s == null)
			return null;
		if (enc == null)
			enc = ISO_8859_1;
		try {

			return base64Encode(s.getBytes(enc));
		} catch (Exception e) {
			if (Main.__debug)
				Log.e(TAG, "", e);
		}
		return null;
	}

	public final static String base64Encode(byte[] bytes) {
		return base64Encode(bytes, 0, bytes.length);
	}

	/** base 64 encoding, array of bytes converted to bytes using specified encoding
	 * @param String <val>_s</val> original string to encode
	 * @param String encoding, can be null, then iso-8859-1 used
	 * @return String result of encoding as iso-8859-1 string<br>
	 * 
	 * @exception <code>NullPointerException</code> if input parameter is null
	 */
	public final static String base64Encode(byte[] bytes, int pos, int len) {
		if (pos < 0 || pos > bytes.length - 2)
			throw new IllegalArgumentException("Illegal pos:" + pos);
		if (len <= 0 && (pos + len) > bytes.length)
			throw new IllegalArgumentException("Illegal len:" + len);
		StringBuffer encodedBuffer = new StringBuffer((int) (len * 1.5));
		int i = 0;
		int pad = 0;
		while (i < len) {
			int b1 = (0xFF & bytes[pos + i++]);
			int b2;
			int b3;
			if (i >= len) {
				b2 = 0;
				b3 = 0;
				pad = 2;
			} else {
				b2 = 0xFF & bytes[pos + i++];
				if (i >= len) {
					b3 = 0;
					pad = 1;
				} else
					b3 = (0xFF & bytes[pos + i++]);
			}
			byte c1 = (byte) (b1 >> 2);
			byte c2 = (byte) (((b1 & 0x3) << 4) | (b2 >> 4));
			byte c3 = (byte) (((b2 & 0xf) << 2) | (b3 >> 6));
			byte c4 = (byte) (b3 & 0x3f);
			//try {
			encodedBuffer.append(BASE64ARRAY[c1]).append(BASE64ARRAY[c2]);
			switch (pad) {
			case 0:
				encodedBuffer.append(BASE64ARRAY[c3]).append(BASE64ARRAY[c4]);
				break;
			case 1:
				encodedBuffer.append(BASE64ARRAY[c3]).append('=');
				break;
			case 2:
				encodedBuffer.append("==");
				break;
			}//}catch(ArrayIndexOutOfBoundsException ai) {
			//ai.printStackTrace();
			//System.err.println("b1:"+b1+"b2:"+b2+"b3:"+b3+"c1:"+c1+"c2:"+c2+"c3:"+c3+"c4:"+c4);
			//}
		}
		return encodedBuffer.toString();

		/*if (_s == null)
			return null;
		
		try {		
			if (_enc == null)
				_enc = ISO_8859_1;
			return new BASE64Encoder().encodeBuffer(_s.getBytes(_enc));
		} catch (Exception e) {
			if (debug)
				System.err.println("Encoding exception "+e);
		}
		return null;*/
	}

	/**
	 * base 64 decoding
	 * 
	 * @param encoded
	 *            string
	 * @param encoding
	 *            used to get string bytes
	 * @return result of encoding, or null if encoding invalid or string null, or string is invalid base 64 encoding
	 */
	public final static String base64Decode(String _s, String _enc) {
		if (_s == null)
			return null;
		if (_enc == null)
			_enc = ISO_8859_1;
		try {
			return new String(decode64(_s), _enc);
		} catch (UnsupportedEncodingException uee) {
		}
		return null;
	}

	/**
	 * Decodes data from Base64 notation, automatically detecting gzip-compressed data and decompressing it.
	 * 
	 * @param s
	 *            the string to decode
	 * @return the decoded data
	 * @since 1.4
	 */
	public static byte[] decode64(String s) {
		byte[] bytes;
		try {
			bytes = s.getBytes(ISO_8859_1);
		} // end try
		catch (java.io.UnsupportedEncodingException uee) {
			bytes = s.getBytes();
		} // end catch
		// </change>

		// Decode
		bytes = decode(bytes, 0, bytes.length);

		// Check to see if it's gzip-compressed
		// GZIP Magic Two-Byte Number: 0x8b1f (35615)
		if (bytes != null && bytes.length >= 4) {

			int head = ((int) bytes[0] & 0xff) | ((bytes[1] << 8) & 0xff00);
			if (java.util.zip.GZIPInputStream.GZIP_MAGIC == head) {
				java.io.ByteArrayInputStream bais = null;
				java.util.zip.GZIPInputStream gzis = null;
				java.io.ByteArrayOutputStream baos = null;
				byte[] buffer = new byte[2048];
				int length = 0;

				try {
					baos = new java.io.ByteArrayOutputStream();
					bais = new java.io.ByteArrayInputStream(bytes);
					gzis = new java.util.zip.GZIPInputStream(bais);

					while ((length = gzis.read(buffer)) >= 0) {
						baos.write(buffer, 0, length);
					} // end while: reading input

					// No error? Get new bytes.
					bytes = baos.toByteArray();

				} // end try
				catch (java.io.IOException e) {
					// Just return originally-decoded bytes
				} // end catch
				finally {
					try {
						baos.close();
					} catch (Exception e) {
					}
					try {
						gzis.close();
					} catch (Exception e) {
					}
					try {
						bais.close();
					} catch (Exception e) {
					}
				} // end finally

			} // end if: gzipped
		} // end if: bytes.length >= 2

		return bytes;
	} // end decode

	/**
	 * Very low-level access to decoding ASCII characters in the form of a byte array. Does not support automatically gunzipping or any other "fancy" features.
	 * 
	 * @param source
	 *            The Base64 encoded data
	 * @param off
	 *            The offset of where to begin decoding
	 * @param len
	 *            The length of characters to decode
	 * @return decoded data
	 * @since 1.3
	 */
	public static byte[] decode(byte[] source, int off, int len) {
		int len34 = len * 3 / 4;
		byte[] outBuff = new byte[len34]; // Upper limit on size of output
		int outBuffPosn = 0;

		byte[] b4 = new byte[4];
		int b4Posn = 0;
		int i = 0;
		byte sbiCrop = 0;
		byte sbiDecode = 0;
		for (i = off; i < off + len; i++) {
			sbiCrop = (byte) (source[i] & 0x7f); // Only the low seven bits
			sbiDecode = DECODABET[sbiCrop];

			if (sbiDecode >= WHITE_SPACE_ENC) // Whitesp ace,Eq ualssi gnor be
			// tter
			{
				if (sbiDecode >= EQUALS_SIGN_ENC) {
					b4[b4Posn++] = sbiCrop;
					if (b4Posn > 3) {
						outBuffPosn += decode4to3(b4, 0, outBuff, outBuffPosn);
						b4Posn = 0;

						// If that was the equals sign, break out of 'for' loop
						if (sbiCrop == EQUALS_SIGN)
							break;
					} // end if: quartet built

				} // end if: equals sign or better

			} // end if: white space, equals sign or better
			else {
				if (Main.__debug)
				    Log.e(TAG, "Bad Base64 input character at " + i + ": " + source[i] + "(decimal)");
				return null;
			} // end else:
		} // each input character

		byte[] out = new byte[outBuffPosn];
		System.arraycopy(outBuff, 0, out, 0, outBuffPosn);
		return out;
	} // end decode

	/**
	 * Decodes four bytes from array <var>source</var> and writes the resulting bytes (up to three of them) to <var>destination</var>. The source and
	 * destination arrays can be manipulated anywhere along their length by specifying <var>srcOffset</var> and <var>destOffset</var>. This method does not
	 * check to make sure your arrays are large enough to accomodate <var>srcOffset</var> + 4 for the <var>source</var> array or <var>destOffset</var> + 3
	 * for the <var>destination</var> array. This method returns the actual number of bytes that were converted from the Base64 encoding.
	 * 
	 * 
	 * @param source
	 *            the array to convert
	 * @param srcOffset
	 *            the index where conversion begins
	 * @param destination
	 *            the array to hold the conversion
	 * @param destOffset
	 *            the index where output will be put
	 * @return the number of decoded bytes converted
	 * @since 1.3
	 */
	private static int decode4to3(byte[] source, int srcOffset, byte[] destination, int destOffset) {
		// Example: Dk==
		if (source[srcOffset + 2] == EQUALS_SIGN) {
			// Two ways to do the same thing. Don't know which way I like best.
			// int outBuff = ( ( DECODABET[ source[ srcOffset ] ] << 24 ) >>> 6
			// )
			// | ( ( DECODABET[ source[ srcOffset + 1] ] << 24 ) >>> 12 );
			int outBuff = ((DECODABET[source[srcOffset]] & 0xFF) << 18)
					| ((DECODABET[source[srcOffset + 1]] & 0xFF) << 12);

			destination[destOffset] = (byte) (outBuff >>> 16);
			return 1;
		}

		// Example: DkL=
		else if (source[srcOffset + 3] == EQUALS_SIGN) {
			// Two ways to do the same thing. Don't know which way I like best.
			// int outBuff = ( ( DECODABET[ source[ srcOffset ] ] << 24 ) >>> 6
			// )
			// | ( ( DECODABET[ source[ srcOffset + 1 ] ] << 24 ) >>> 12 )
			// | ( ( DECODABET[ source[ srcOffset + 2 ] ] << 24 ) >>> 18 );
			int outBuff = ((DECODABET[source[srcOffset]] & 0xFF) << 18)
					| ((DECODABET[source[srcOffset + 1]] & 0xFF) << 12)
					| ((DECODABET[source[srcOffset + 2]] & 0xFF) << 6);

			destination[destOffset] = (byte) (outBuff >>> 16);
			destination[destOffset + 1] = (byte) (outBuff >>> 8);
			return 2;
		}

		// Example: DkLE
		else {
			try {
				// Two ways to do the same thing. Don't know which way I like
				// best.
				// int outBuff = ( ( DECODABET[ source[ srcOffset ] ] << 24 )
				// >>> 6 )
				// | ( ( DECODABET[ source[ srcOffset + 1 ] ] << 24 ) >>> 12 )
				// | ( ( DECODABET[ source[ srcOffset + 2 ] ] << 24 ) >>> 18 )
				// | ( ( DECODABET[ source[ srcOffset + 3 ] ] << 24 ) >>> 24 );
				int outBuff = ((DECODABET[source[srcOffset]] & 0xFF) << 18)
						| ((DECODABET[source[srcOffset + 1]] & 0xFF) << 12)
						| ((DECODABET[source[srcOffset + 2]] & 0xFF) << 6)
						| ((DECODABET[source[srcOffset + 3]] & 0xFF));

				destination[destOffset] = (byte) (outBuff >> 16);
				destination[destOffset + 1] = (byte) (outBuff >> 8);
				destination[destOffset + 2] = (byte) (outBuff);

				return 3;
			} catch (Exception e) {
				if (debug) {
					System.out.println("" + source[srcOffset] + ": " + (DECODABET[source[srcOffset]]));
					System.out.println("" + source[srcOffset + 1] + ": " + (DECODABET[source[srcOffset + 1]]));
					System.out.println("" + source[srcOffset + 2] + ": " + (DECODABET[source[srcOffset + 2]]));
					System.out.println("" + source[srcOffset + 3] + ": " + (DECODABET[source[srcOffset + 3]]));
				}
				return -1;
			} // end catch
		}
	} // end decodeToBytes

	private final static boolean debug = false;
        private final static String TAG = "Base64";
}
