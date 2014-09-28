package com.praveen.record;

import java.lang.Character.UnicodeBlock;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioFormat;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity implements OnClickListener {
	
	private RelativeLayout sampleRate					= null;
	
	private RelativeLayout sampleFormat					= null;
	
	private AppPreferences preferences					= null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		init();

	}
	
	private void init() {
		initViews();
		initObjects();
		addClickLiteners();
	}
	
	private void initViews() {
		sampleRate = (RelativeLayout) findViewById(R.id.samplerate_layout);
		sampleFormat = (RelativeLayout) findViewById(R.id.sampleformate_layout);
	}
	
	private void initObjects() {
		preferences = new AppPreferences(this);
	}
	
	private void addClickLiteners() {
		sampleRate.setOnClickListener(this);
		sampleFormat.setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.samplerate_layout:
			int[]  sampleRates= {8,16,32,64,128,192,256,320};
			String untis = " khz";
			showDetailsAlert(sampleRates, untis);
			break;
		case R.id.sampleformate_layout:
			int[]  sampleFormats= {8, 16};
			untis = "bit";
			//showDetailsAlert(sampleFormats, untis);
			break;
		}
	}
	
	private void showDetailsAlert(final int[]  sampleRates, final String untis) {
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCancelable(true);
		dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
		dialog.setContentView(R.layout.layout_details);
		ListView list = (ListView) dialog.findViewById(R.id.file_details);
		list.setAdapter(new DetailsAdapter(sampleRates,untis));
		list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int pos,long id) {
				dialog.dismiss();
				if(untis.equalsIgnoreCase("bit")) {
					switch(pos) {
					case 0 :
						preferences.updateSampleFormat(AudioFormat.ENCODING_PCM_8BIT);
						break;
					case 1:
						preferences.updateSampleFormat(AudioFormat.ENCODING_PCM_16BIT);
						break;
					}
				} else {
					preferences.updateSampleRate(sampleRates[pos]);
				}
			}
		});
		dialog.show();
	}
	
	
	
	private class DetailsAdapter extends BaseAdapter {
		
		private LayoutInflater inflater			= null;
		private int[] sampleRates				= null;
		private String untis					= null;

		private DetailsAdapter(int[] sampleRates, String untis) {
			inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			this.sampleRates = sampleRates;
			this.untis = untis;
		}
		
		@Override
		public int getCount() {
			return sampleRates.length;
		}

		@Override
		public Object getItem(int arg0) {
			return null;
		}

		@Override
		public long getItemId(int arg0) {
			return 0;
		}

		@Override
		public View getView(int pos, View convertView, ViewGroup arg2) {
			View view = convertView;
			if(convertView==null){
				view = inflater.inflate(R.layout.details_row, null);
				TextView text = (TextView) view.findViewById(R.id.folder_option);
				text.setText(sampleRates[pos]+untis);
			} 
			return view;
		}
		
	}
	
	
	private void closeActivity() {
		finish();
		overridePendingTransition(android.R.anim.slide_in_left,
				android.R.anim.slide_out_right);
	}

	@Override
	public void onBackPressed() {
		closeActivity();
	}
}
