package com.praveen.record.camera;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.praveen.record.R;
import com.praveen.record.utils.OSStringUtils;

public class OSCameraActivity extends Activity {

	private CameraPreview mCameraPreview 		= null;

	private Camera mCamera 						= null;

	private FrameLayout mCameraLayout 			= null;

	private ImageView mImageCapture				= null;

	private String filePath						= null;

	private MediaRecorder mediaRecorder 		= null;
	
	private String mediaType					= null;
	
	private boolean isVideoRecording			= false;
	
	private TextView mTextviewTimer				= null;
	
	private CountDownTimer cdTimer				= null;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera);

		init();
	}

	private void init() {
		initViews();
		initObjects();
		addClickListeners();
	}

	private void initViews() {
		mCameraLayout = (FrameLayout) findViewById(R.id.layout_camera);
		mImageCapture = (ImageView) findViewById(R.id.media_capture);
		mTextviewTimer = (TextView) findViewById(R.id.timer);
	}

	private void initObjects() {
		filePath = getIntent().getExtras().getString("path");
		mediaType = getIntent().getExtras().getString("type");
		if(!mediaType.equalsIgnoreCase("image")) {
			mTextviewTimer.setText(OSStringUtils.getTime(0));
			mImageCapture.setImageResource(R.drawable.video_capture_blue);
		}
	}

	private void addClickListeners() {
		mImageCapture.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(mediaType.equalsIgnoreCase("image")) {
					mCamera.takePicture(null, null, mPicture);
				} else {
					if(!isVideoRecording) {
						startCountDownTimer();
						isVideoRecording = true;
						recordVideo();
					} else {
						cdTimer.cancel();
						isVideoRecording = false;
						stopVideoRecording();
						mTextviewTimer.setText(OSStringUtils.getTime(0));
						mImageCapture.setImageResource(R.drawable.video_capture_blue);
					}
				}
			}
		});

	}


	private void createCamera(final Camera camera) {
		mCamera = camera;// getCameraInstance();
		if (mCamera == null) {
			Toast.makeText(getApplicationContext(), "Something went wrong with your camera", Toast.LENGTH_LONG).show();
			return;
		}
		mCameraPreview = new CameraPreview(this, mCamera);
		final Camera.Parameters parameters = mCamera.getParameters();
		parameters.setPictureSize(640, 480);
		mCamera.setParameters(parameters);
		mCameraLayout.addView(mCameraPreview);
	}


	private void releaseCamera() {
		if (mCamera != null) {
			mCameraLayout.removeAllViews();
			mCamera.release();
			mCamera = null;
		}
	}


	private Camera getCameraInstance() {
		Camera camera = null;
		try {
			camera = Camera.open();
			camera.setDisplayOrientation(90);
		} catch (final Exception e) {
			e.getMessage();
		}
		return camera;
	}

	/**
	 * call back when picture is ready to capture
	 */
	private final PictureCallback mPicture = new PictureCallback() {
		@Override
		public void onPictureTaken(final byte[] data, final Camera camera) {
			try {
				Bitmap image = BitmapFactory.decodeByteArray(data, 0, data.length);
				image = adjustBitmap(getIntent(), image);
				String path = filePath+File.separator+"IMG_"+getFileTimeStamp()+".png";

				FileOutputStream stream = new FileOutputStream(path);
				image.compress(Bitmap.CompressFormat.PNG, 100, stream);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} finally {
				closeActivity();
			}

		}
	};

	private String getFileTimeStamp() {
		return new SimpleDateFormat("dd-MMM-yyyy_HH.mm.ss", Locale.US)
		.format(new Date());
	}

	private Bitmap adjustBitmap(Intent intent, Bitmap mImagebitmap) {
		try {
			if (intent.getIntExtra("orienation", 1)==1) { //for portrait
				if(mImagebitmap != null) {
					return rotation(mImagebitmap,90);
				}
			}
			else if (intent.getIntExtra("orienation", 1)==2) { //for Landscape
				if(mImagebitmap != null) {
					return rotation(mImagebitmap,0);
				}
			}

			else if (intent.getIntExtra("orienation", 1)==3) { //for 180 degrees
				if(mImagebitmap != null) {
					return rotation(mImagebitmap,180);
				}
			}

			else if (intent.getIntExtra("orienation", 1)==4) {//for 270 degrees
				if(mImagebitmap != null) {
					return rotation(mImagebitmap,270);
				}
			}

		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return mImagebitmap;
	}

	private Bitmap rotation(Bitmap imagebitmap,int angle) {
		Matrix mtx = new Matrix();
		mtx.postRotate(angle);
		return Bitmap.createBitmap(imagebitmap, 0, 0, imagebitmap.getWidth(), imagebitmap.getHeight(), mtx, true);
	}


	protected void recordVideo() {
		try {
			mCamera.setPreviewDisplay(mCameraPreview.getHolder());
			mCamera.startPreview();
			mediaRecorder = new MediaRecorder();
			mCamera.unlock();
			mediaRecorder.setCamera(mCamera);
			mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
			mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
			mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
			mediaRecorder.setVideoSize(640, 480);
			mediaRecorder.setVideoEncodingBitRate(500000);
			mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
			
			//CamcorderProfile cpHigh = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
			//mediaRecorder.setProfile(cpHigh);
			
			mediaRecorder.setOrientationHint(90);
			final File mediaFile = new File(filePath+ File.separator+"VID_"+ getFileTimeStamp() + ".mp4");
			mediaRecorder.setOutputFile(mediaFile.getPath());
			mediaRecorder.setPreviewDisplay(mCameraPreview.getHolder().getSurface());
			mediaRecorder.prepare();
			mediaRecorder.start();
		} catch (final IllegalStateException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private void stopVideoRecording() {
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (mediaRecorder != null) {
			mediaRecorder.stop();
			mediaRecorder.reset(); // clear recorder configuration
			mediaRecorder.release(); // release the recorder object
			mediaRecorder = null;
			mCamera.lock();
		}
	}
	
	private long total = 24*60*60*1000;
	private void startCountDownTimer() {
		cdTimer = new CountDownTimer(total, 1000) {
			public void onTick(long millisUntilFinished) {
				long currentTime = (24*60*60)-((millisUntilFinished)/1000);
				if(currentTime % 2 == 0) {
					mImageCapture.setImageResource(R.drawable.video_capture_blue_red);
				} else {
					mImageCapture.setImageResource(R.drawable.video_capture_blue);
				}
				mTextviewTimer.setText(OSStringUtils.getTime(currentTime)+"");
			}
			public void onFinish() {
			}
		}.start();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mCamera = getCameraInstance();
		createCamera(mCamera);
	}

	@Override
	protected void onPause() {
		super.onPause();
		releaseCamera();
	}

	private void closeActivity() {
		finish();
		overridePendingTransition(android.R.anim.slide_in_left,
				android.R.anim.slide_out_right);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		closeActivity();
	}
	
	

}
