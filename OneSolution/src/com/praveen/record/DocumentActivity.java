package com.praveen.record;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class DocumentActivity extends Activity implements OnClickListener {

	private ImageView mImageSave					= null;

	private String filePath							= null;
	
	private EditText mEditTextData					= null;
	
	private String fileData							= null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_document);

		init();
	}

	private void init() {
		initViews();
		initObjects();
		addClickListeners();
	}

	private void initViews() {
		mImageSave = (ImageView) findViewById(R.id.image_save);
		mEditTextData = (EditText) findViewById(R.id.document_field);
	}

	private void initObjects() {
		filePath = getIntent().getExtras().getString("path");
		fileData = readFileAsString();
		mEditTextData.setText(fileData);
	}

	private void addClickListeners() {
		mImageSave.setOnClickListener(this);
		mEditTextData.setOnFocusChangeListener(new OnFocusChangeListener(){
			public void onFocusChange(View v, boolean hasFocus){
				if (hasFocus) {
					mEditTextData.setSelection(mEditTextData.length());
					getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
				}
			}
		});
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.image_save:
			doSaveButtonAction();
			break;
		}
	}

	private void doSaveButtonAction() {
		String data = mEditTextData.getText().toString();
		int lastIndex = data.length() > 10 ? 10 : data.length();
		String fileName = data.substring(0,lastIndex);
		if(fileName.contains("\n")) {
			fileName = fileName.substring(0,data.indexOf("\n"));
		}
		//fileName = fileName.replaceAll("\\s+","");
		System.out.println("file name = "+fileName);
		if(data.trim().length() > 0 && fileData.length() != data.length() && !fileData.equals(data)) {
			try {
				File file = new File(filePath);
				FileWriter fileWriter = new FileWriter(file);
		        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
				String[] linesData = data.split("\n");
				for(int i=0; i< linesData.length; i++) {
					bufferedWriter.write(linesData[i]);
					bufferedWriter.newLine();
				}
				bufferedWriter.close();
				String extnsion = filePath.contains(".txt") ? ".txt" : "";
				File newFile = new File(file.getParent()+File.separator+fileName+extnsion);
				file.renameTo(newFile);
			} catch (IOException e) {
				
			}
		}
		closeActivity();
	}
	
	private String readFileAsString() {
        StringBuilder stringBuilder = new StringBuilder();
        try {
        	String line;
        	BufferedReader in = new BufferedReader(new FileReader(new File(filePath)));
            while ((line = in.readLine()) != null) {
            	stringBuilder.append(line);
            	stringBuilder.append('\n');
            }

        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        } 
        return stringBuilder.toString();
    }

	private void showSaveAlert() {
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCancelable(true);
		dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
		dialog.setContentView(R.layout.dialog_two_button);
		TextView text = (TextView) dialog.findViewById(R.id.text_message);
		text.setText("Do you want to save this data ?");

		dialog.show();

		Button buttonYes = (Button) dialog.findViewById(R.id.button_yes);
		buttonYes.setText("Yes");
		buttonYes.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
				doSaveButtonAction();
			}
		});

		Button buttonNo = (Button) dialog.findViewById(R.id.button_no);
		buttonNo.setText("No");
		buttonNo.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
				closeActivity();
			}
		});
	}
	
	private void closeActivity() {
		finish();
		overridePendingTransition(android.R.anim.slide_in_left,
				android.R.anim.slide_out_right);
	}

	@Override
	public void onBackPressed() {
		String data = mEditTextData.getText().toString();
		if(data.trim().length() > 0 && fileData.length() != data.length() && !fileData.equals(data)) {
			doSaveButtonAction();
		} else {
			closeActivity();
		}
	}

}
