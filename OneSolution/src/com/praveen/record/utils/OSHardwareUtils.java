package com.praveen.record.utils;

import android.content.Context;
import android.content.pm.PackageManager;

public class OSHardwareUtils {

	 public static boolean checkCameraHardware(Context c) {
	        if (c.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
	            return true;
	        }
	        return false;
	    }
}
