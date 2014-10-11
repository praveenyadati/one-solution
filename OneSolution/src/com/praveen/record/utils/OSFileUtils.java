package com.praveen.record.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import android.os.Environment;

public class OSFileUtils {


	public static boolean isHidenFile(File file) {
		if(file.getName().startsWith(".")) {
			return true;
		}
		return false;
	}

	public static boolean isAudioFile(String path) {
		String[] audioFormats = {".mp3",".3gp",".mp4",".wav","ogg","flac","aac","amr"};
		File file = new File(path);
		for(int i=0; i<audioFormats.length; i++) {
			if(file.getName().toLowerCase(Locale.ENGLISH).endsWith(audioFormats[i])) {
				return true;
			}
		}
		return false;
	}

	public static boolean isTextFile(String path) {
		String[] textFormats = {".txt",".doc",".docx"};
		File file = new File(path);
		for(int i=0; i<textFormats.length; i++) {
			if(file.getName().toLowerCase(Locale.ENGLISH).endsWith(textFormats[i])) {
				return true;
			}
		}
		return false;
	}

	public static boolean isImageFile(String path) {
		String[] textFormats = {".png",".jpg",".jpeg"};
		File file = new File(path);
		for(int i=0; i<textFormats.length; i++) {
			if(file.getName().toLowerCase(Locale.ENGLISH).endsWith(textFormats[i])) {
				return true;
			}
		}
		return false;
	}

	public static String getFileExtension(String path) {
		String ext = path.substring(path.lastIndexOf("."));
		return ext;
	}

	public static boolean isNormalFile(String path) {
		File file = new File(path);
		String name = file.getName();
		return !name.contains(".");
	}

	public static List<File> listFiles(File root, boolean hiddenFileEnable) {
		List<File> result = new ArrayList<File>();
		for (File file : root.listFiles()) {
			if (file.isDirectory() || isAudioFile(file.getAbsolutePath()) || isTextFile(file.getAbsolutePath()) || isNormalFile(file.getAbsolutePath())  || isImageFile(file.getAbsolutePath())) {
				if(isHidenFile(file) ) {
					if(hiddenFileEnable)
						result.add(file);
				} else {
					result.add(file);
				}
			}
		}
		sortFiles(result);
		return result;
	}

	public static void mergeFiles(File file1, File file2, File destination) {
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
		} 

	}

	public static String getFileSize(String path) {
		File file = new File(path);
		long length = file.length();
		if(file.isDirectory()) {
			length = folderSize(file);
		}
		float kb =  ((float)length/1024.0F);
		float mb =  ((float)kb/1024.0F);
		float gb =  ((float)mb/1024.0F);
		if(gb >= 1) {
			return gb+" GB";
		} else if(gb < 1 && mb >= 1) {
			return mb+" MB";
		} else if(mb < 1 && kb >= 1) {
			return kb+" KB";
		}
		return kb+" KB";
	}

	private static long folderSize(File directory) {
		long length = 0;
		for (File file : directory.listFiles()) {
			if (file.isFile())
				length += file.length();
			else
				length += folderSize(file);
		}
		return length;
	}


	public static void copyFile(File sourceFile, File destFile) throws IOException {
		if (!sourceFile.exists()) {
			return;
		}
		if (!destFile.exists()) {
			destFile.createNewFile();
		}
		FileUtils.copyFile(sourceFile, destFile, false);
	}

	public static void copyFiles(List<File> files, File destDir) {
		try {
			for(File file : files) {
				File source = file;
				File dest = new File(destDir+File.separator+file.getName());
				if(source.isDirectory()) {
					FileUtils.copyDirectory(source, dest);
				} else {
					FileUtils.copyFile(source, dest);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void cutFiles(List<File> files, File destDir) {
		try {
			for(File file : files) {
				File source = file;
				File dest = new File(destDir+File.separator+file.getName());
				if(source.isDirectory()) {
					FileUtils.copyDirectory(source, dest);
				} else {
					FileUtils.copyFile(source, dest);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void deleteFiles(List<File> files) {
		try {
			for(File file : files) {
				if(file.isDirectory()) {
					FileUtils.deleteDirectory(file);
				} else {
					file.delete();
				}
			}
		} catch (IOException e) {

		}
	}


	public static List<File> searchFile(final String name) {
		List<File> files = (List<File>) FileUtils.listFiles(Environment.getExternalStorageDirectory(),
				new RegexFileFilter(".*"+name+".*"),TrueFileFilter.TRUE);
		return files;
	}


	public static void sortFiles(List<File> files) {
		Collections.sort(files, new ItemFileNameComparator());
	}

	private static class ItemFileNameComparator implements Comparator<File> {
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

}
