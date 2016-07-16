package rogatkin.mobile.data.pertusin;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

import android.util.Log;

public class VoldParser {
	
	final static String TAG = "Pertusin-VoldParser";
	
	public static final File VOLD_FSTAB = new File("/etc/vold.fstab");
	
	File[] storages;
	
	VoldParser parse() {
		//LogAssistant.init("/sdcard/log.txt");
		//LogAssistant.log.log("--", "parse");
		if (VOLD_FSTAB.exists() == false)
			return this; // more likely no extra SD
		storages = new File[0];
		try {
			RandomAccessFile raf = new RandomAccessFile(VOLD_FSTAB, "r");
			String l;
			while ((l=raf.readLine()) != null) {
				if (l.length() == 0 || l.startsWith("#"))
					continue;
				if (l.startsWith("dev_mount")) {
					//  Format: dev_mount <label> <mount_point[:[asec_point]:[lun_point]]> <part> <sysfs_path1...> 
					String parts[] = l.split("\\s");
					if (parts.length > 2) {
						String mp = parts[2];
						int cp = mp.indexOf(':');
						if (cp > 0)
							mp = mp.substring(0, cp);
						File f = new File(mp);
						//LogAssistant.log.log("--", "parse %s", f);
						if (f.exists() && f.isDirectory()) {
							storages = Arrays.copyOf(storages, storages.length+1);
							storages[storages.length-1] = f;
							//LogAssistant.log.log("--", "good %s", f);
						}
					}
				}
			}
			raf.close();
		} catch (IOException e) {
			if (Main.__debug)
				Log.e(TAG, "Exception Vold parsing", e);
		}
		return this;
	}
	
	File[] getStorages() {
		return storages;
	}
}
