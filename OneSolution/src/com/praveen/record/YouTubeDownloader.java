package com.praveen.record;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.os.Environment;


public class YouTubeDownloader {


	public static String newline = System.getProperty("line.separator");
	private static final Logger log =Logger.getLogger("ONE SOLUTION");//JavaYoutubeDownloader.class.getCanonicalName());
	private static final Level defaultLogLevelSelf = Level.FINER;
	private static final Level defaultLogLevel = Level.WARNING;
	private static final Logger rootlog = Logger.getLogger("");
	private static final String scheme = "http";
	private static final String host = "www.youtube.com";
	private static final Pattern commaPattern = Pattern.compile(",");
	//  private static final Pattern pipePattern = Pattern.compile("\|");
	private static final String[] ILLEGAL_FILENAME_CHARACTERS = { "/", "n", "r", "t", " ", "f", "`", "?", "*", "<", ">", "|", ":" };

	private static void usage(String error) {
		if (error != null) {
			System.err.println("Error: " + error);
		}
		System.err.println("usage: YouTubeDownloader VIDEO_ID DESTINATION_DIRECTORY");
		System.exit(-1);
	}

	private static String getYoutubeVideoId(String youtubeUrl)
	{
		String video_id="";
		if (youtubeUrl != null && youtubeUrl.trim().length() > 0 && youtubeUrl.startsWith("http"))
		{

			String expression = "^.*((youtu.be"+ "\\/)" + "|(v\\/)|(\\/u\\/w\\/)|(embed\\/)|(watch\\?))\\??v?=?([^#\\&\\?]*).*"; // var regExp = /^.*((youtu.be\/)|(v\/)|(\/u\/\w\/)|(embed\/)|(watch\?))\??v?=?([^#\&\?]*).*/;
			CharSequence input = youtubeUrl;
			Pattern pattern = Pattern.compile(expression,Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(input);
			if (matcher.matches())
			{
				String groupIndex1 = matcher.group(7);
				if(groupIndex1!=null && groupIndex1.length()==11)
					video_id = groupIndex1;
			}
		}
		return video_id;
	}



	public  YouTubeDownloader(String url) {
		try {
			setupLogging();

			log.fine("Starting");
			 String videoId = null;
			 String outdir = ".";
			// TODO Ghetto command line parsing
			File SDCardRoot = new File(Environment.getExternalStorageDirectory()+File.separator);
				videoId = getYoutubeVideoId(url);
				outdir = SDCardRoot.getAbsolutePath();
				
				
				System.out.println("video id = "+videoId);

			int format = 18; // http://en.wikipedia.org/wiki/YouTube#Quality_and_codecs
			String encoding = "UTF-8";
			String userAgent = "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.13) Gecko/20101203 Firefox/3.6.13";
			File outputDir = new File(outdir);
			String extension = getExtension(format);

			play(videoId, format, encoding, userAgent, outputDir, extension);

		} catch (Exception t) {
			t.printStackTrace();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.fine("Finished");
	}

	private static String getExtension(int format) {
		// TODO: See reference http://en.wikipedia.org/wiki/YouTube#Quality_and_codecs
		return "mp4";
	}

	private static void play(String videoId, int format, String encoding, String userAgent, File outputdir, String extension) throws Throwable {
		log.fine("Retrieving " + videoId);
		List<NameValuePair> qparams = new ArrayList<NameValuePair>();
		qparams.add(new BasicNameValuePair("video_id", videoId));
		qparams.add(new BasicNameValuePair("fmt", "" + format));
		URI uri = getUri("get_video_info", qparams);

		CookieStore cookieStore = new BasicCookieStore();
		HttpContext localContext = new BasicHttpContext();
		localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet(uri);
		httpget.setHeader("User-Agent", userAgent);

		System.out.println("Executing " + uri);
		HttpResponse response = httpclient.execute(httpget, localContext);
		HttpEntity entity = response.getEntity();
		if (entity != null && response.getStatusLine().getStatusCode() == 200) {
			InputStream instream = entity.getContent();
			String videoInfo = getStringFromInputStream(encoding, instream);
			if (videoInfo != null && videoInfo.length() > 0) {
				List<NameValuePair> infoMap = new ArrayList<NameValuePair>();
				URLEncodedUtils.parse(infoMap, new Scanner(videoInfo), encoding);
				String token = null;
				String downloadUrl = null;
				String filename = videoId;

				for (NameValuePair pair : infoMap) {
					String key = pair.getName();
					String val = pair.getValue();
					//System.out.println(key + "=" + val);
					if (key.equals("token")) {
						token = val;
					} 
					else if (key.equals("title")) {
						filename = val;
					} 
					else if (key.equals("url_encoded_fmt_stream_map")) {
						System.out.println("download url = "+val);
						downloadUrl = URLDecoder.decode(
								val.substring(val.indexOf("http")/*,val.indexOf("&")*/), "UTF-8");
					}
				}

				filename = cleanFilename(filename);
				if (filename.length() == 0) {
					filename = videoId;
				} 
				else {
					filename += "_" + videoId;
				}
				filename += "." + extension;
				File outputfile = new File(outputdir, filename);

				System.out.println("Saving to " + outputfile);
				System.out.println("From " + downloadUrl);

				if (downloadUrl != null) {
					downloadWithHttpClient(userAgent, downloadUrl, outputfile);
				}
			}
		}
	}

	private static void downloadWithHttpClient(String userAgent, String downloadUrl, File outputfile) throws Throwable {
		HttpGet httpget2 = new HttpGet(downloadUrl);
		httpget2.setHeader("User-Agent", userAgent);

		System.out.println("Executing " + httpget2.getURI());
		HttpClient httpclient2 = new DefaultHttpClient();
		HttpResponse response2 = httpclient2.execute(httpget2);
		HttpEntity entity2 = response2.getEntity();
		if (entity2 != null && response2.getStatusLine().getStatusCode() == 200) {
			long length = entity2.getContentLength();
			InputStream instream2 = entity2.getContent();
			System.out.println("Writing " + length + " bytes to " + outputfile);
			if (outputfile.exists()) {
				outputfile.delete();
			}
			FileOutputStream outstream = new FileOutputStream(outputfile);
			try {
				byte[] buffer = new byte[2048];
				int count = -1;
				while ((count = instream2.read(buffer)) != -1) {
					outstream.write(buffer, 0, count);
				}
				outstream.flush();
			} finally {
				outstream.close();
			}
		}
	}

	private static String cleanFilename(String filename) {
		for (String c : ILLEGAL_FILENAME_CHARACTERS) {
			char cr = c.charAt(0);
			filename = filename.replace(cr, '_');
		}
		return filename;
	}

	private static URI getUri(String path, List<NameValuePair> qparams) throws URISyntaxException {
		URI uri = URIUtils.createURI(scheme, host, -1, "/" + path, URLEncodedUtils.format(qparams, "UTF-8"), null);
		return uri;
	}

	private static void setupLogging() {
		changeFormatter(new Formatter() {
			@Override
			public String format(LogRecord arg0) {
				return arg0.getMessage() + newline;
			}
		});
		explicitlySetAllLogging(Level.FINER);
	}

	private static void changeFormatter(Formatter formatter) {
		Handler[] handlers = rootlog.getHandlers();
		for (Handler handler : handlers) {
			handler.setFormatter(formatter);
		}
	}

	private static void explicitlySetAllLogging(Level level) {
		rootlog.setLevel(Level.ALL);
		for (Handler handler : rootlog.getHandlers()) {
			handler.setLevel(defaultLogLevelSelf);
		}
		log.setLevel(level);
		rootlog.setLevel(defaultLogLevel);
	}

	private static String getStringFromInputStream(String encoding, InputStream instream) throws UnsupportedEncodingException, IOException {
		Writer writer = new StringWriter();

		char[] buffer = new char[1024];
		try {
			Reader reader = new BufferedReader(new InputStreamReader(instream, encoding));
			int n;
			while ((n = reader.read(buffer)) != -1) {
				writer.write(buffer, 0, n);
			}
		} finally {
			instream.close();
		}
		String result = writer.toString();
		return result;
	}

}
