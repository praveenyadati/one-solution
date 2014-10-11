package com.praveen.record;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.praveen.record.utils.OSStringUtils;

public class RecordActivity extends Activity implements OnClickListener{

	private RecMicToMp3 mRecMicToMp3				= null;

	private ImageView mImageStart					= null;

	private ImageView mImageStop					= null;

	private ImageView mImagePause					= null;

	private String filePath							= null;

	private TextView mTextTimer						= null;

	private boolean recordingStarted				= false;

	private AppPreferences preferences				= null;

	private CountDownTimer cdTimer					= null;

	private ImageView audioProgress					= null;

	private ImageView mImageDelete					= null;

	private int progressCount						= 0;

	private File recordFile							= null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_record);

		init();

	}

	private void init() {
		initViews();
		initObjects();
		addClickListeners();
	}

	private void initViews() {
		mImageStart = (ImageView) findViewById(R.id.button_record);
		mImageStop = (ImageView) findViewById(R.id.button_stop);
		mTextTimer = (TextView) findViewById(R.id.text_timer);
		mImagePause = (ImageView) findViewById(R.id.button_pause);
		audioProgress = (ImageView) findViewById(R.id.audio_progress);
		mImageDelete = (ImageView) findViewById(R.id.image_trash);
	}

	private void initObjects() {
		preferences = new AppPreferences(this);
		filePath = getIntent().getExtras().getString("path");
		int sampleRate = 44100;
		int sampleFormat = preferences.getSampleFormat();
		int bitRate = preferences.getSampleRate();
		mRecMicToMp3 = new RecMicToMp3(sampleRate,sampleFormat,bitRate);
		mRecMicToMp3.setHandle(new Handler() {
			@Override
			public void handleMessage(Message msg) {
			}
		});
	}

	private void addClickListeners() {
		mImageStart.setOnClickListener(this);
		mImageStop.setOnClickListener(this);
		mImagePause.setOnClickListener(this);
		mImageDelete.setOnClickListener(this);
	}

	private long total = 24*60*60*1000;
	private void startCountDownTimer() {
		cdTimer = new CountDownTimer(total, 1000) {
			public void onTick(long millisUntilFinished) {
				total = millisUntilFinished;
				long currentTime = (24*60*60)-((total)/1000);
				mTextTimer.setText(OSStringUtils.getTime(currentTime)+"");
				updateProgress();
				progressCount++;
				if(progressCount >= 7) {
					progressCount = 1;
				}
			}
			public void onFinish() {
			}
		}.start();
	}

	private void updateProgress() {
		switch(progressCount) {
		case 0:
			audioProgress.setImageResource(R.drawable.audio_progress_1);
			break;
		case 1:
			audioProgress.setImageResource(R.drawable.audio_progress_2);
			break;
		case 2:
			audioProgress.setImageResource(R.drawable.audio_progress_3);
			break;
		case 3:
			audioProgress.setImageResource(R.drawable.audio_progress_4);
			break;
		case 4:
			audioProgress.setImageResource(R.drawable.audio_progress_3);
			break;
		case 5:
			audioProgress.setImageResource(R.drawable.audio_progress_2);
			break;
		case 6:
			audioProgress.setImageResource(R.drawable.audio_progress_1);
			break;
		}
	}

	private boolean pause = false;

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.button_stop:
			doStopButtonAction();
			showNewFileAlert();
			break;
		case R.id.button_record:
			doRecordButtonAction();
			break;
		case R.id.button_pause :
			doPauseButtonAction();
			break;
		case R.id.image_trash:
			doDeleteButtonAction();
			break;
		}
	}

	private void doDeleteButtonAction() {
		if(recordingStarted) {
			audioProgress.setImageResource(R.drawable.audio_progress_1);
			if(recordFile.exists()) {
				recordFile.delete();
			}
			doStopButtonAction();
			mTextTimer.setText(OSStringUtils.getTime(0)+"");
			total = 24*60*60*1000;
		}
	}

	private void doRecordButtonAction() {
		if(!recordingStarted) {
			mTextTimer.setText(OSStringUtils.getTime(1)+"");
			recordFile = createNewFile();
			mRecMicToMp3.setFilePath(recordFile.getAbsolutePath());
			mImagePause.setImageResource(R.drawable.orange_pause);
			recordingStarted = true;
			mRecMicToMp3.start();
			startCountDownTimer();
			hideKeyboard();
		}
	}

	private void doStopButtonAction() {
		if(recordingStarted) {
			recordingStarted = false;
			mRecMicToMp3.stop();
			if(cdTimer != null) {
				cdTimer.cancel();
				cdTimer = null;
			}
		}
		progressCount = 0;
	}

	private void doPauseButtonAction() {
		if(!recordingStarted) {
			return;
		}
		pause = !pause;
		if(!pause) {
			mImagePause.setImageResource(R.drawable.orange_pause);
			startCountDownTimer();
		} else {
			mImagePause.setImageResource(R.drawable.orange_play);
			cdTimer.cancel();
		}
		mRecMicToMp3.setPauseEnable();
	}


	private void hideKeyboard() {
		InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);

		// check if no view has focus:
		View view = this.getCurrentFocus();
		if (view != null) {
			inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
		}
	}

	
	private void showNewFileAlert() {
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCancelable(false);
		dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
		dialog.setContentView(R.layout.orange_dialog_field);
		final EditText mFileNameField = (EditText) dialog.findViewById(R.id.file_name_field);
		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		mFileNameField.setText("record01", TextView.BufferType.SPANNABLE);
		mFileNameField.selectAll();

		dialog.show();

		Button buttonYes = (Button) dialog.findViewById(R.id.button_yes);
		buttonYes.setText("Create");
		buttonYes.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mFileNameField.getText().toString().trim().length() > 0) {
					dialog.dismiss();
					File newFile = new File(filePath + File.separator+mFileNameField.getText().toString()+".mp3");
					recordFile.renameTo(newFile);
					closeActivity();
				} else {
					Toast.makeText(getApplicationContext(), "This field cannot be left blank", Toast.LENGTH_LONG).show();
				}
			}
		});

	}

	int count = 1;
	private File createNewFile() {
		try {
			File file = new File(filePath+File.separator +"record("+count+").mp3");
			if(file.exists()) {
				count++;
				createNewFile();
			} else {
				file.createNewFile();
			}
			return file;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}


	private void closeActivity() {
		finish();
		overridePendingTransition(android.R.anim.slide_in_left,
				android.R.anim.slide_out_right);
	}

	@Override
	public void onBackPressed() {
		if(recordingStarted) {
			doStopButtonAction();
			showNewFileAlert();
		} else {
			closeActivity();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(mRecMicToMp3 != null) {
			mRecMicToMp3.stop();
		}
		if(cdTimer != null) {
			cdTimer.cancel();
			cdTimer = null;
		}
	}

}
