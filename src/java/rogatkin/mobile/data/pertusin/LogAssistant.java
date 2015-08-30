package rogatkin.mobile.data.pertusin;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class LogAssistant {

	public static LogAssistant log;

	private PrintStream logStream;

	protected LogAssistant(String path) throws FileNotFoundException {
		logStream = new PrintStream(new FileOutputStream(path));
	}

	synchronized public static void init(String path) {
		if (log != null)
			return;
		try {
			log = new LogAssistant(path);
		} catch (FileNotFoundException e) {

		}
	}

	public void log(String tag, String format, Object... params) {
		try {
			logStream.println("" + tag + ":" + String.format(format, params));
			logStream.flush();
		} catch (Exception e) {
			logStream.println("Error in log message " + e);
		}
	}
}
