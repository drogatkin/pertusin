package rogatkin.mobile.data.pertusin;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOAssistant {
	final static int BUF_SIZE = 16 * 1024;

	public static long copy(InputStream is, OutputStream os, long maxLen) throws IOException {
		byte[] buffer = new byte[maxLen > 0 && maxLen < BUF_SIZE ? (int) maxLen : BUF_SIZE];
		int len = buffer.length;
		long result = 0;
		while ((len = is.read(buffer, 0, len)) > 0) {
			os.write(buffer, 0, len);
			result += len;
			if (maxLen > 0) {
				if (result >= maxLen)
					break;
				len = Math.min((int) (maxLen - result), buffer.length);
			} else
				len = buffer.length;
		}
		return result;
	}

	public static long copy(File inf, OutputStream outs) throws IOException {
		BufferedInputStream is = new BufferedInputStream(new FileInputStream(inf), BUF_SIZE);
		try {
			return copy(is, outs, -1);
		} finally {
			if (is != null)
				is.close();
		}
	}

	public static long copy(File inf, File outf) throws IOException {
		if (!inf.equals(outf)) {
			BufferedOutputStream bos = null;
			try {
				long result = copy(inf, bos = new BufferedOutputStream(new FileOutputStream(outf), BUF_SIZE));
				bos.flush();
				return result;
			} finally {
				if (bos != null)
					bos.close();
			}
		} else
			throw new IOException("An attempt to copy file " + inf + " to itself.");
	}

}
