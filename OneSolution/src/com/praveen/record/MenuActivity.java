package com.praveen.record;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MenuActivity extends Activity implements OnClickListener {

	private File rootDirectory						= null;

	private TextView mTextPath						= null;

	private ImageView mImageSettings				= null;

	private GridView mGridFolders					= null;

	private ListView mListFolders					= null;

	private Button mButtonRecord					= null;

	private Button mButtonFile						= null;

	private Button mButtonFolder					= null;

	private List<File> directories					= null;

	private MediaPlayer mMediaPlayer				= null;

	private FileAdapter adptr						= null;

	private LinearLayout folder						= null;

	private String[] folderOptions					= null;

	private String[] audioFolderOptions				= null;

	private AppPreferences preferences				= null;

	private ImageView mImageFolderType				= null;

	private String fileCopyPath						= null;

	private String fileMergePath					= null;

	private String[] audioFormats					= null;

	private String[] textFormats					= null;

	private MergeTask mergeTask						= null;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_menu);

		init();
	}

	private void init() {
		initViews();
		initObjects();
		setInitilaData();
		addClickListeners();
	}

	private void initViews() {
		folder = (LinearLayout) findViewById(R.id.image_folder);
		mTextPath = (TextView) findViewById(R.id.path);
		mGridFolders = (GridView) findViewById(R.id.gridview);
		mListFolders = (ListView) findViewById(R.id.listview);
		mButtonRecord = (Button) findViewById(R.id.new_record);
		mButtonFile = (Button) findViewById(R.id.new_file);
		mButtonFolder = (Button) findViewById(R.id.new_folder);
		mImageSettings = (ImageView) findViewById(R.id.image_setting);
		mImageFolderType = (ImageView) findViewById(R.id.image_folder_type);
	}

	private void initObjects() {
		preferences = new AppPreferences(this);
		if(preferences.getPreviousPath() == null) {
			preferences.updatePreviousPath(getRootFileDirectory().getAbsolutePath());
		}
		rootDirectory = new File(preferences.getPreviousPath());
		folderOptions = new String[]{"Rename","Copy","Delete","Zip & Send file","Properties"};
		audioFolderOptions = new String[]{"Rename","Copy","Delete","Zip & Send file", "Properties","Merge with"};
		audioFormats = new String[]{".mp3",".3gp",".mp4",".wav","ogg","flac","aac","amr"};
		textFormats = new String[]{".txt",".doc",".docx"};
	}

	private void setInitilaData() {
		mTextPath.setText(rootDirectory.getAbsolutePath().toString());
		boolean listviewEnable = preferences.isListViewEnable();
		if(listviewEnable) {
			mGridFolders.setVisibility(View.GONE);
			mListFolders.setVisibility(View.VISIBLE);
			mImageFolderType.setImageResource(R.drawable.grid);
		}
	}

	private void addClickListeners() {
		folder.setOnClickListener(this);
		mButtonRecord.setOnClickListener(this);
		mImageSettings.setOnClickListener(this);
		mButtonFile.setOnClickListener(this);
		mButtonFolder.setOnClickListener(this);
		mImageFolderType.setOnClickListener(this);
		mGridFolders.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int pos,long id) {
				doClickItemAction(pos);
			}
		});

		mGridFolders.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
				String path = directories.get(pos).getAbsoluteFile().toString();
				String root = rootDirectory.toString().substring(0, rootDirectory.toString().lastIndexOf("/"));
				if(root == "" || root.length() <= 0) {
					return true;
				}
				showDetailsAlert(path, pos);
				return true;
			}

		});

		mListFolders.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int pos,long id) {
				doClickItemAction(pos);
			}
		});

		mListFolders.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
				String path = directories.get(pos).getAbsoluteFile().toString();
				if(fileMergePath != null) {
					return false;
				}
				showDetailsAlert(path, pos);
				return true;
			}
		});
	}

	private void doClickItemAction(int pos) {
		String path = directories.get(pos).getAbsoluteFile().toString();
		if(isAudioFile(path)) {
			if(fileMergePath != null) {
				File file1 = new File(fileMergePath);
				File file2 = new File(path);
				if(getFileExtension(fileMergePath).equalsIgnoreCase(getFileExtension(path))) {
					String name = file1.getName().substring(0, file1.getName().lastIndexOf(".")) +
							file2.getName().substring(0, file2.getName().lastIndexOf("."))+getFileExtension(path);
					File destination = new File(file2.getParent()+File.separator+name);
					mergeTask = new MergeTask(file1, file2, destination);
					mergeTask.execute();
				} else {
					Toast.makeText(getApplicationContext(), "We cannot merge these two files", Toast.LENGTH_LONG).show();
				}
			} else {
				openFile(path);
				//playAudio(path);
			}
			return;
		} else if((path.endsWith(".txt")) || (!directories.get(pos).isDirectory() && isNormalFile(path))) {
			Intent intent = new Intent(MenuActivity.this, DocumentActivity.class);
			intent.putExtra("path", path);
			startActivity(intent);
			overridePendingTransition(R.anim.right_to_left, R.anim.left_to_right);
			return;
		} else if(directories.get(pos).isDirectory()) {
			if(path == "" || path.length() <= 0) {
				return;
			}
			rootDirectory = directories.get(pos);
			preferences.updatePreviousPath(rootDirectory.getAbsolutePath());
			mTextPath.setText(rootDirectory.getAbsolutePath().toString());
			directories = findDirectories(rootDirectory); 
			Collections.sort(directories);
			FileAdapter adptr = new FileAdapter(MenuActivity.this);
			mGridFolders.setAdapter(adptr);
			mListFolders.setAdapter(adptr);
		} else {
			openFile(path);
		}
	}

	private void DeleteRecursive(String path, int pos) {
		File f = new File(path);
		if (f.isDirectory()) {
			for (File child : f.listFiles()) {
				DeleteRecursive(child.getAbsolutePath(),pos);
			}
		}
		f.delete();
	}

	private File getRootFileDirectory() {
		File SDCardRoot = new File(Environment.getExternalStorageDirectory()+File.separator+"MP3Rec");
		if(!SDCardRoot.exists()) {
			SDCardRoot.mkdir();
		}
		return SDCardRoot.getAbsoluteFile();
	}

	private List<File> findDirectories(File root) {
		System.out.println("rooot "+root);
		List<File> result = new ArrayList<File>();
		for (File file : root.listFiles()) {
			if (file.isDirectory() || isAudioFile(file.getAbsolutePath()) || isTextFile(file.getAbsolutePath()) || isNormalFile(file.getAbsolutePath())) {
				if(isHidenFile(file) ) {
					if(preferences.isHidenFileEnable())
						result.add(file);
				} else {
					result.add(file);
				}
			}
		}
		return result;
	}

	private boolean isHidenFile(File file) {
		if(file.getName().startsWith(".")) {
			return true;
		}
		return false;
	}

	private boolean isAudioFile(String path) {
		File file = new File(path);
		for(int i=0; i<audioFormats.length; i++) {
			if(file.getName().toLowerCase().endsWith(audioFormats[i])) {
				return true;
			}
		}
		return false;
	}

	private boolean isTextFile(String path) {
		File file = new File(path);
		for(int i=0; i<textFormats.length; i++) {
			if(file.getName().toLowerCase().endsWith(textFormats[i])) {
				return true;
			}
		}
		return false;
	}

	private String getFileExtension(String path) {
		File file = new File(path);
		for(int i=0; i<audioFormats.length; i++) {
			if(file.getName().toLowerCase().endsWith(audioFormats[i])) {
				return audioFormats[i];
			}
		}
		return null;
	}

	private boolean isNormalFile(String path) {
		File file = new File(path);
		String name = file.getName();
		return !name.contains(".");
	}

	private class FileAdapter extends BaseAdapter {

		private LayoutInflater inflater			= null;

		private FileAdapter(Context context) {
			inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return directories.size();
		}

		@Override
		public Object getItem(int arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int arg0) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int pos, View convertView, ViewGroup arg2) {
			View view = convertView;
			ViewHolder holder;
			if(convertView==null){
				view = inflater.inflate(R.layout.layout_row, null);
				holder = new ViewHolder();
				initialiseViews(view, holder);
				view.setTag(holder );
			} else { 
				holder=(ViewHolder)view.getTag();
			}
			updateViews(holder, pos);
			return view;
		}

		private void initialiseViews(View view, ViewHolder holder) {
			holder.text = (TextView) view.findViewById(R.id.list_folder_name);
			holder.folder = (ImageView) view.findViewById(R.id.list_folder_icon);
		}

		private void updateViews(ViewHolder holder, int pos) {
			String path = directories.get(pos).getAbsoluteFile().toString();
			String name = directories.get(pos).getName();
			String fileName = name;
			if(mGridFolders.isShown()) {
				fileName = name.length() > 15 ? name.substring(0, 15)+"..." : name; 
			} 

			holder.text.setText(fileName);
			if(isAudioFile(path)) {
				holder.folder.setImageResource(R.drawable.audio);
			} else if(isTextFile(path)) {
				holder.folder.setImageResource(R.drawable.document);
			} else if(!directories.get(pos).isDirectory() && isNormalFile(path)) {
				holder.folder.setImageResource(R.drawable.normal);
			} 


			else {
				holder.folder.setImageResource(R.drawable.file_folder);
			}
		}

		class ViewHolder {
			private TextView text		= null;
			private ImageView folder	= null;
		}


	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.image_folder:
			doFolderButtonAction();
			break;
		case R.id.new_record:
			doRecordButtonAction();
			break;
		case R.id.image_setting:
			doSettingsButtonAction();
			break;
		case R.id.new_file:
			createNewFile();
			break;
		case R.id.new_folder:
			showNewFileAlert(true);
			break;
		case R.id.image_folder_type:
			doFolderTypeButtonAction();
			break;
		}
	}

	private void doFolderTypeButtonAction() {
		if(mGridFolders.isShown()) {
			mImageFolderType.setImageResource(R.drawable.grid);
			mGridFolders.setVisibility(View.GONE);
			mListFolders.setVisibility(View.VISIBLE);
			preferences.updateListViewEnable(true);
		} else {
			mImageFolderType.setImageResource(R.drawable.list);
			mListFolders.setVisibility(View.GONE);
			mGridFolders.setVisibility(View.VISIBLE);
			preferences.updateListViewEnable(false);
		}
	}

	int count = 1;
	private void createNewFile() {
		try {
			System.out.println("root directory = "+rootDirectory.getAbsolutePath());
			String path = rootDirectory.getAbsolutePath()+File.separator;
			File file = new File(path+"New file("+count+")");
			if(file.exists()) {
				count++;
				createNewFile();
				return;
			}
			if(!file.createNewFile()) {
				Toast.makeText(getApplicationContext(), "Cannot create a file here", Toast.LENGTH_LONG).show();
			} else {
				refreshList();
				Intent intent = new Intent(MenuActivity.this, DocumentActivity.class);
				intent.putExtra("path", file.getAbsolutePath());
				startActivity(intent);
				overridePendingTransition(R.anim.right_to_left, R.anim.left_to_right);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}



	private void doFolderButtonAction() {
		String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath();
		if(rootPath.equalsIgnoreCase(rootDirectory.toString())) {
			return;
		}
		String path = rootDirectory.toString().substring(0, rootDirectory.toString().lastIndexOf("/"));
		if(path == "" || path.length() <= 0) {
			return;
		}
		rootDirectory = new File(path);
		preferences.updatePreviousPath(rootDirectory.getAbsolutePath());
		mTextPath.setText(rootDirectory.getAbsolutePath().toString());
		directories = findDirectories(rootDirectory); 
		Collections.sort(directories, new ItemFileNameComparator());
		adptr = new FileAdapter(MenuActivity.this);
		mGridFolders.setAdapter(adptr);
		mListFolders.setAdapter(adptr);
	}

	private void doRecordButtonAction() {
		if(mMediaPlayer != null) {
			mMediaPlayer.stop();
			mMediaPlayer.release();
			mMediaPlayer = null;
			mImageSettings.setImageResource(R.drawable.settings);
		} 
		Intent intent = new Intent(MenuActivity.this, RecordActivity.class);
		intent.putExtra("path", rootDirectory.getAbsolutePath().toString());
		startActivity(intent);
		overridePendingTransition(R.anim.right_to_left, R.anim.left_to_right);
	}

	private void doSettingsButtonAction() {
		if(mMediaPlayer != null) {
			mMediaPlayer.stop();
			mMediaPlayer.release();
			mMediaPlayer = null;
			mImageSettings.setImageResource(R.drawable.settings);
		} else if(fileMergePath != null) {
			fileMergePath = null;
			mImageSettings.setImageResource(R.drawable.settings);
		} else if(fileCopyPath != null) {
			try {
				File source = new File(fileCopyPath);
				File dest = new File(rootDirectory+File.separator+new File(fileCopyPath).getName());
				copyFile(source, dest);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				mImageSettings.setImageResource(R.drawable.settings);
				fileCopyPath = null;
				refreshList();
			}
		}


		else {
			Intent intent = new Intent(MenuActivity.this, SettingsActivity.class);
			startActivity(intent);
			overridePendingTransition(R.anim.right_to_left, R.anim.left_to_right);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		refreshList();
	}

	private void refreshList() {
		directories = findDirectories(rootDirectory); 
		Collections.sort(directories, new ItemFileNameComparator());
		adptr = new FileAdapter(MenuActivity.this);
		mGridFolders.setAdapter(adptr);
		mListFolders.setAdapter(adptr);
	}

	public void playAudio(String path) {
		try {
			Uri uri = Uri.parse(path);
			if(mMediaPlayer != null) {
				mMediaPlayer.release();
				mMediaPlayer = null;
			}
			mImageSettings.setImageResource(R.drawable.audio_play);
			mMediaPlayer = new MediaPlayer();
			mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mMediaPlayer.setDataSource(getApplicationContext(), uri);
			mMediaPlayer.prepare();
			mMediaPlayer.start();
			mMediaPlayer.setOnCompletionListener(new OnCompletionListener() {

				@Override
				public void onCompletion(MediaPlayer mp) {
					mImageSettings.setImageResource(R.drawable.settings);
					mMediaPlayer.release();
					mMediaPlayer = null;
				}
			});

		} catch (Exception e) {
			mMediaPlayer.release();
			mMediaPlayer = null;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(mMediaPlayer != null) {
			mMediaPlayer.release();
			mMediaPlayer = null;
		}
	}

	private class ItemFileNameComparator implements Comparator<File> {
		public int compare(File lhs, File rhs) {
			File lhsFile = new File(lhs.toString().toLowerCase(Locale.ENGLISH));
			File rhsFile= new File( rhs.toString().toLowerCase(Locale.ENGLISH));

			String lhsDir = lhsFile.isDirectory()? lhsFile.getPath() : lhsFile.getParent();
			String rhsDir = rhsFile.isDirectory()? rhsFile.getPath() : rhsFile.getParent();

			int result =  lhsDir.toLowerCase(Locale.ENGLISH).compareTo(rhsDir .toLowerCase(Locale.ENGLISH));    

			if (result != 0) {
				return result;
			}else{              
				if(lhsFile.isDirectory()!= rhsFile.isDirectory()){
					return lhsFile.getParent().toLowerCase(Locale.ENGLISH).compareTo( rhsFile.getParent().toLowerCase(Locale.ENGLISH));
				}
				return lhsFile.getName().toLowerCase(Locale.ENGLISH).compareTo( rhsFile.getName().toLowerCase(Locale.ENGLISH));
			}
		}
	}

	private void showDeleteAlert(final String path, final int pos) {
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCancelable(true);
		dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
		dialog.setContentView(R.layout.dialog_two_button);
		TextView text = (TextView) dialog.findViewById(R.id.text_message);
		text.setText("Do you want to delete this file ?");

		dialog.show();

		Button buttonYes = (Button) dialog.findViewById(R.id.button_yes);
		buttonYes.setText("Yes");
		buttonYes.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
				//directories.remove(pos);
				DeleteRecursive(path, pos);
				//adptr.notifyDataSetChanged();
				refreshList();
			}
		});

		Button buttonNo = (Button) dialog.findViewById(R.id.button_no);
		buttonNo.setText("No");
		buttonNo.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
	}


	private void showNewFileAlert(final boolean isFolder) {
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCancelable(true);
		dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
		dialog.setContentView(R.layout.layout_textfield);
		final EditText mFileNameField = (EditText) dialog.findViewById(R.id.file_name_field);
		String text = isFolder ? "New folder" : "New document";
		mFileNameField.setText("record01", TextView.BufferType.SPANNABLE);
		mFileNameField.selectAll();
		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		dialog.show();

		Button buttonYes = (Button) dialog.findViewById(R.id.button_yes);
		buttonYes.setText("Create");
		buttonYes.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
				try {
					File file = new File(rootDirectory.getAbsolutePath()+File.separator+mFileNameField.getText().toString());
					if(isFolder && !file.exists()) {
						if(!file.mkdir()) {
							Toast.makeText(getApplicationContext(), "Cannot create a folder here", Toast.LENGTH_LONG).show();
						}
					} else {
						file = new File(rootDirectory.getAbsolutePath()+File.separator+mFileNameField.getText().toString());
						file.createNewFile();
					}
					refreshList();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		Button buttonNo = (Button) dialog.findViewById(R.id.button_no);
		buttonNo.setText("Cancle");
		buttonNo.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
	}


	private void showRenameFileAlert(String path) {
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCancelable(true);
		dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
		dialog.setContentView(R.layout.dialog_rename);
		final EditText mFileNameField = (EditText) dialog.findViewById(R.id.file_name_field);
		String extension = "";
		final File file = new File(path);
		String name = file.getName();
		/*if(!file.isDirectory() && !isNormalFile(path)) {
			extension = path.substring(path.lastIndexOf("."));
			name = file.getName().substring(0,file.getName().lastIndexOf("."));
		}*/

		//final String ext = extension;
		mFileNameField.setText(name, TextView.BufferType.SPANNABLE);
		mFileNameField.selectAll();
		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		
		dialog.show();
		Button buttonRename = (Button) dialog.findViewById(R.id.button_rename);
		buttonRename.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
				File renameFile = new File(file.getParentFile().getAbsoluteFile()+File.separator+mFileNameField.getText().toString());
				if(!file.renameTo(renameFile)) {
					Toast.makeText(getApplicationContext(), "Cannot rename a folder/file here", Toast.LENGTH_LONG).show();
				} else {
					refreshList();
				}
			}
		});
	}


	private void showDetailsAlert(final String path, final int pos) {
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCancelable(true);
		dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
		dialog.setContentView(R.layout.layout_details);
		ListView list = (ListView) dialog.findViewById(R.id.file_details);
		String[] options = (isAudioFile(path) ? audioFolderOptions : folderOptions);
		list.setAdapter(new DetailsAdapter(options));
		list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int pos,long id) {
				dialog.dismiss();
				switch(pos) {
				case 0:
					showRenameFileAlert(path);
					break;
				case 1:
					fileCopyPath = path;
					mImageSettings.setImageResource(R.drawable.paste);
					break;
				case 2:
					showDeleteAlert(path, pos);
					break;
				case 3:
					shareFile(path);
					break;
				case 4:
					showPropertiesAlert(path);
					break;
				case 5:
					fileMergePath = path;
					mImageSettings.setImageResource(R.drawable.cancel);
					break;
				}
				// TODO Auto-generated method stub

			}
		});
		dialog.show();
	}

	private void shareFile(String path) {
		Intent shareIntent = new Intent();
		shareIntent.setAction(Intent.ACTION_SEND);
		shareIntent.putExtra(Intent.EXTRA_STREAM,Uri.parse(path));
		shareIntent.setType("application/zip");
		startActivity(Intent.createChooser(shareIntent, "Share File")); 
	}

	private class DetailsAdapter extends BaseAdapter {

		private LayoutInflater inflater			= null;
		private String[] options				= null;

		private DetailsAdapter(String[] options) {
			inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			this.options = options;
		}

		@Override
		public int getCount() {
			return options.length;
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
				text.setText(options[pos]+"");
			} 
			return view;
		}

	}


	private void showPropertiesAlert(final String path) {
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCancelable(true);
		dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
		dialog.setContentView(R.layout.dialog_properties);
		File file = new File(path);
		TextView name = (TextView) dialog.findViewById(R.id.file_name);
		TextView type = (TextView) dialog.findViewById(R.id.file_type);
		TextView size = (TextView) dialog.findViewById(R.id.file_size);
		TextView date = (TextView) dialog.findViewById(R.id.file_date);
		name.setText("Name : "+file.getName());
		type.setText((file.isDirectory()) ? "Type : Folder" : "Type : File");
		size.setText("Size : "+getSize(path));
		String time = new SimpleDateFormat("dd MMM yyyy hh:mm a").format(
				new Date(file.lastModified()) 
				);
		date.setText("Date : "+time);
		Button ok = (Button) dialog.findViewById(R.id.button_ok);
		ok.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		dialog.show();
	}


	private void mergeFiles(File file1, File file2, File destination) {
		try {
			FileInputStream fistream1 = new FileInputStream(file1.getAbsolutePath());
			FileInputStream fistream2 = new FileInputStream(file2.getAbsolutePath());
			SequenceInputStream sistream = new SequenceInputStream(fistream1, fistream2);
			FileOutputStream fostream = new FileOutputStream(destination.getAbsolutePath());
			int temp;
			while( ( temp = sistream.read() ) != -1) {
				fostream.write(temp);   // to write to file
			}
			fostream.close();
			sistream.close();
			fistream1.close();
			fistream2.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			fileMergePath = null;
		}

	}


	private String getSize(String path) {
		File file = new File(path);
		long length = file.length();
		if(file.isDirectory()) {
			length = folderSize(file);
		}
		long kb = length/1024;
		long mb = kb/1024;
		long gb = mb/1024;
		if(gb > 0) {
			return gb+" GB";
		} else if(gb <= 0 && mb > 1) {
			return mb+" MB";
		} else if(mb <= 0 && kb > 1) {
			return kb+" KB";
		}
		return kb+" KB";
	}

	private long folderSize(File directory) {
		long length = 0;
		for (File file : directory.listFiles()) {
			if (file.isFile())
				length += file.length();
			else
				length += folderSize(file);
		}
		return length;
	}

	private void copyFile(File sourceFile, File destFile)
			throws IOException {
		if (!sourceFile.exists()) {
			return;
		}
		if (!destFile.exists()) {
			destFile.createNewFile();
		}
		FileChannel source = null;
		FileChannel destination = null;
		source = new FileInputStream(sourceFile).getChannel();
		destination = new FileOutputStream(destFile).getChannel();
		if (destination != null && source != null) {
			destination.transferFrom(source, 0, source.size());
		}
		if (source != null) {
			source.close();
		}
		if (destination != null) {
			destination.close();
		}

	}

	public class MergeTask extends AsyncTask<Void, Void, String> {

		private File file1,file2,destination;

		private ProgressDialog progress		= null;

		public MergeTask(File file1, File file2, File destination) {
			this.file1 = file1;
			this.file2 = file2;
			this.destination = destination;

			progress = new ProgressDialog(MenuActivity.this);
			progress.setMessage("Mixing...");
			progress.setCancelable(true);
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progress.show();
		}

		@Override
		protected String doInBackground(Void... arg0) {
			mergeFiles(file1, file2, destination);
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			progress.cancel();
			fileMergePath = null;
			mImageSettings.setImageResource(R.drawable.settings);
			refreshList();
		}
	}

	public void openFile(String name) {
		Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
		File file = new File(name);
		String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString());
		String mimetype = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
		if (extension.equalsIgnoreCase("") || mimetype == null)
		{

			// if there is no extension or there is no definite mimetype, still try to open the file
			intent.setDataAndType(Uri.fromFile(file), "text/*");
		}
		else
		{
			intent.setDataAndType(Uri.fromFile(file), mimetype);            
		}
		// custom message for the intent
		startActivity(Intent.createChooser(intent, "Choose an Application:"));
	}



	@Override
	public void onBackPressed() {
		String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath();
		if(mergeTask != null) {
			mergeTask.cancel(true);
			fileMergePath = null;
		} else if(!rootPath.equalsIgnoreCase(rootDirectory.toString())) {
			doFolderButtonAction();
		} else {
			super.onBackPressed();
		}
	}



}
