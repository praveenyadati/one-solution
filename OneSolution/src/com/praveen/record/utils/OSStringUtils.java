package com.praveen.record.utils;

public class OSStringUtils {

	public static String getTime(long time) {
		String val = time+"";
		if(val.length() == 1) {
			val = "00:00:0"+val;
		} else if(val.length() >= 2) {
			if(time > 59) {
				int min = (int) (time/60);
				int sec = (int) (time%60);
				int hour = 0;
				if(min > 59) {
					hour = (int) (min/60);
					min = (int) (hour%60);
					sec = (int) (min/60);
				}
				String seconds = sec+"";
				String minuits = min+"";
				String hours = hour+"";
				if(seconds.length() == 1) {
					seconds = "0"+sec;
				}
				if(minuits.length() == 1) {
					minuits = "0"+min;
				}
				if(hours.length() == 1) {
					hours = "0"+hour;
				}
				val = hour+":"+minuits+":"+seconds;
			} else {
				val = "00:00:"+val;
			}
		}
		return val;
	}

}
