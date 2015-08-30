package rogatkin.mobile.data.pertusin;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.res.AssetManager;
import android.os.Environment;

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

	public static long copy(InputStream ins, File file) throws IOException {
		FileOutputStream fos = null;
		try {
			return copy(ins, fos = new FileOutputStream(file), -1);
		} finally {
			if (fos != null)
				fos.close();
		}
	}

	public static long copy(File inf, File outf) throws IOException {
		if (!inf.equals(outf)) {
			if (inf.isDirectory())
				throw new IOException("Use copyDir() for copy directories");
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

	public static long copyDir(File ind, File outd, boolean recurs) throws IOException {
		if (ind.equals(outd))
			throw new IOException("An attempt to copy file " + ind + " to itself.");
		if (ind.isDirectory() == false)
			if (ind.exists())
				throw new IOException("Use copy() to copy single files");
			else
				throw new IOException("Source directory doesn't exist");
		if (outd.exists()) {
			if (outd.isDirectory() == false)
				throw new IOException("Output location isn't directory");
		} else if (outd.mkdirs() == false)
			throw new IOException("Output location can't be assured");
		long result = 0;
		String files[] = ind.list();
		if (files == null)
			throw new IOException("Input location can't be assured");
		for (String n : files) {
			File inf = new File(ind, n);
			if (inf.isDirectory()) {
				if (recurs)
					result += copyDir(inf, new File(outd, n), true);
			} else {
				if (copy(inf, new File(outd, n)) > 0)
					result++;
			}
		}

		return result;
	}

	public static long copyDir(AssetManager assetManager, String ind, File outd, boolean recurs) throws IOException {
		if (outd.exists()) {
			if (outd.isDirectory() == false)
				throw new IOException("Output location isn't directory");
		} else if (outd.mkdirs() == false)
			throw new IOException("Output location can't be assured");
		long result = 0;
		String files[] = assetManager.list(ind);
		if (files == null)
			throw new IOException("Input location can't be assured");
		for (String n : files) {
			File inf = new File(ind, n);
			if (inf.isDirectory()) {
				if (recurs)
					result += copyDir(assetManager, inf.getPath(), new File(outd, n), true);
			} else {
				InputStream iss;
				if (copy(iss = assetManager.open(ind + "/" + n), new File(outd, n)) > 0)
					result++;
				iss.close();
			}
		}

		return result;
	}

	public static File getExternalDir(boolean removable) {
		File extFile = Environment.getExternalStorageDirectory();
		if (!removable)
			return extFile;

		try {
			extFile = extFile.getCanonicalFile();
		} catch (IOException e1) {
		}
		File[] ss = new VoldParser().parse().getStorages();
		if (ss != null) {
			for (File f : ss) {
				try {
					if (f.getCanonicalPath().equals(extFile) == false)
						return f;
				} catch (IOException e) {

				}
			}
		}
		return extFile;
	}

	public static String MD5_Hash(String s) {
		MessageDigest m = null;

		try {
			m = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException("No such algorithm", e);
		}

		m.update(s.getBytes(), 0, s.length());
		String hash = new BigInteger(1, m.digest()).toString(16);
		return hash;
	}
}
