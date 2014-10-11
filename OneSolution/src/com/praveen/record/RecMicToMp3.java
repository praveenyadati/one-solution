/* 
 * Copyright (c) 2011-2012 Yuichi Hirano
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.praveen.record;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidParameterException;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.widget.Toast;

import com.uraroji.garage.android.lame.SimpleLame;

public class RecMicToMp3 {

	static {
		System.loadLibrary("mp3lame");
	}

	private String mFilePath;
	private int mSampleRate;
	private boolean mIsRecording = false;
	private Handler mHandler;
	private boolean isPause=false;
	private int sampleFormat;
	private int bitRate;

	public static final int MSG_REC_STARTED = 0;
	public static final int MSG_REC_STOPPED = 1;
	public static final int MSG_ERROR_GET_MIN_BUFFERSIZE = 2;

	public static final int MSG_ERROR_CREATE_FILE = 3;
	public static final int MSG_ERROR_REC_START = 4;

	public static final int MSG_ERROR_AUDIO_RECORD = 5;

	public static final int MSG_ERROR_AUDIO_ENCODE = 6;

	public static final int MSG_ERROR_WRITE_FILE = 7;

	public static final int MSG_ERROR_CLOSE_FILE = 8;

	public RecMicToMp3(int sampleRate, int sampleFormat, int bitRate) {
		if (sampleRate <= 0) {
			throw new InvalidParameterException(
					"Invalid sample rate specified.");
		}
		this.mSampleRate = sampleRate;
		this.sampleFormat = sampleFormat;
		this.bitRate = bitRate;
	}

	public void start() {
		if (mIsRecording) {
			return;
		}

		new Thread() {
			@Override
			public void run() {
				android.os.Process
				.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
				final int minBufferSize = AudioRecord.getMinBufferSize(mSampleRate, AudioFormat.CHANNEL_IN_MONO,sampleFormat);
				if (minBufferSize < 0) {
					if (mHandler != null) {
						mHandler.sendEmptyMessage(MSG_ERROR_GET_MIN_BUFFERSIZE);
					}
					return;
				}
				AudioRecord audioRecord = new AudioRecord(
						MediaRecorder.AudioSource.MIC, mSampleRate,
						AudioFormat.CHANNEL_IN_MONO,sampleFormat, minBufferSize * 2);

				// PCM buffer size (5sec)
				short[] buffer = new short[mSampleRate * (16 / 8) * 1 * 5]; // SampleRate[Hz] * 16bit * Mono * 5sec
				byte[] mp3buffer = new byte[(int) (7200 + buffer.length * 2 * 1.25)];

				FileOutputStream output = null;
				try {
					output = new FileOutputStream(new File(mFilePath));
				} catch (FileNotFoundException e) {
					if (mHandler != null) {
						mHandler.sendEmptyMessage(MSG_ERROR_CREATE_FILE);
					}
					return;
				}

				// Lame init
				System.out.println("bit rate = "+bitRate);
				SimpleLame.init(mSampleRate, 1, mSampleRate, bitRate);

				mIsRecording = true; 
				try {
					try {
						audioRecord.startRecording();
					} catch (IllegalStateException e) {
						if (mHandler != null) {
							mHandler.sendEmptyMessage(MSG_ERROR_REC_START);
						}
						return;
					}

					try {
						if (mHandler != null) {
							mHandler.sendEmptyMessage(MSG_REC_STARTED);
						}

						int readSize = 0;
						while (mIsRecording) {
							if(!isPause) {
								readSize = audioRecord.read(buffer, 0, minBufferSize);
								if (readSize < 0) {
									if (mHandler != null) {
										mHandler.sendEmptyMessage(MSG_ERROR_AUDIO_RECORD);
									}
									break;
								}
								else if (readSize == 0) {
									;
								}
								else {
									int encResult = SimpleLame.encode(buffer,
											buffer, readSize, mp3buffer);
									if (encResult < 0) {
										if (mHandler != null) {
											mHandler.sendEmptyMessage(MSG_ERROR_AUDIO_ENCODE);
										}
										break;
									}
									if (encResult != 0) {
										try {
											output.write(mp3buffer, 0, encResult);
										} catch (IOException e) {
											if (mHandler != null) {
												mHandler.sendEmptyMessage(MSG_ERROR_WRITE_FILE);
											}
											break;
										}
									}
								}
							}
						}

						int flushResult = SimpleLame.flush(mp3buffer);
						if (flushResult < 0) {
							if (mHandler != null) {
								mHandler.sendEmptyMessage(MSG_ERROR_AUDIO_ENCODE);
							}
						}
						if (flushResult != 0) {
							try {
								output.write(mp3buffer, 0, flushResult);
							} catch (IOException e) {
								if (mHandler != null) {
									mHandler.sendEmptyMessage(MSG_ERROR_WRITE_FILE);
								}
							}
						}

						try {
							output.close();
						} catch (IOException e) {
							if (mHandler != null) {
								mHandler.sendEmptyMessage(MSG_ERROR_CLOSE_FILE);
							}
						}
					} finally {
						audioRecord.stop();
						audioRecord.release();
					}
				} finally {
					SimpleLame.close();
				}

				if (mHandler != null) {
					mHandler.sendEmptyMessage(MSG_REC_STOPPED);
				}
			}
		}.start();
	}

	public void stop() {
		mIsRecording = false;
	}


	public boolean isRecording() {
		return mIsRecording;
	}


	public void setHandle(Handler handler) {
		this.mHandler = handler;
	}

	public void setFilePath(String filePath) {
		this.mFilePath = filePath;
	}

	public void setSampleRate(int mSampleRate) {
		this.mSampleRate = mSampleRate;
	}

	public void setSampleFormat(int sampleFormat) {
		this.sampleFormat = sampleFormat;
	}

	public boolean isPauseEnable() {
		return isPause;
	}

	public void setPauseEnable() {
		if(isPause) {
			isPause = false;
		} else {
			isPause = true;
		}
	}
}
