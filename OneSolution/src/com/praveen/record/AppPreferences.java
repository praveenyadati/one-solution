package com.praveen.record;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.MediaRecorder.AudioEncoder;

public class AppPreferences
{
	private SharedPreferences myPrefs;
	public AppPreferences(Context context) {
		myPrefs = context.getSharedPreferences("MP3Rec", Context.MODE_PRIVATE);
	}

	public synchronized void updateHidenFileShow(boolean val) {
		myPrefs.edit().putBoolean("hiden", val).commit();
	}
	
	

	public boolean isHidenFileEnable() {
		return myPrefs.getBoolean("hiden", false);
	}
	
	public synchronized void updateSampleRate(int sampleRate) {
		myPrefs.edit().putInt("sampleRate", sampleRate).commit();
	}

	public int getSampleRate() {
		return myPrefs.getInt("sampleRate", 32);
	}
	
	public synchronized void updateSampleFormat(int sampleFormat) {
		myPrefs.edit().putInt("sampleFormat", sampleFormat).commit();
	}

	public int getSampleFormat() {
		return myPrefs.getInt("sampleFormat", AudioFormat.ENCODING_PCM_16BIT);
	}
	
	public synchronized void updatePreviousPath(String previousPath) {
		myPrefs.edit().putString("previousPath", previousPath).commit();
	}

	public String getPreviousPath() {
		return myPrefs.getString("previousPath", null);
	}
	
	public synchronized void updateListViewEnable(boolean val) {
		myPrefs.edit().putBoolean("listView", val).commit();
	}

	public boolean isListViewEnable() {
		return myPrefs.getBoolean("listView", false);
	}




}