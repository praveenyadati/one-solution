package com.praveen.record;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

public class YoutubeClient {
	
	private Context context				= null;
	
	public YoutubeClient(Context context) {
		this.context = context;
	}
	
	public void download(String url) {
		new YouTubePageStreamUriGetter().execute("https://www.youtube.com/watch?v=4GuqB1BQVr4");
	}

	class Meta {
	    public String num;
	    public String type;
	    public String ext;

	    Meta(String num, String ext, String type) {
	        this.num = num;
	        this.ext = ext;
	        this.type = type;
	    }
	}

	class Video {
	    public String ext = "";
	    public String type = "";
	    public String url = "";

	    Video(String ext, String type, String url) {
	        this.ext = ext;
	        this.type = type;
	        this.url = url;
	    }
	}

	public ArrayList<Video> getStreamingUrisFromYouTubePage(String ytUrl)
	        throws IOException {
	    if (ytUrl == null) {
	        return null;
	    }

	    // Remove any query params in query string after the watch?v=<vid> in
	    // e.g.
	    // http://www.youtube.com/watch?v=0RUPACpf8Vs&feature=youtube_gdata_player
	    int andIdx = ytUrl.indexOf('&');
	    if (andIdx >= 0) {
	        ytUrl = ytUrl.substring(0, andIdx);
	    }

	    // Get the HTML response
	    String userAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:8.0.1)";
	    HttpClient client = new DefaultHttpClient();
	    client.getParams().setParameter(CoreProtocolPNames.USER_AGENT,
	            userAgent);
	    HttpGet request = new HttpGet(ytUrl);
	    HttpResponse response = client.execute(request);
	    String html = "";
	    InputStream in = response.getEntity().getContent();
	    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
	    StringBuilder str = new StringBuilder();
	    String line = null;
	    while ((line = reader.readLine()) != null) {
	        str.append(line.replace("\\u0026", "&"));
	    }
	    in.close();
	    html = str.toString();

	    // Parse the HTML response and extract the streaming URIs
	    if (html.contains("verify-age-thumb")) {
	        System.out.println("YouTube is asking for age verification. We can't handle that sorry.");
	        return null;
	    }

	    if (html.contains("das_captcha")) {
	        System.out.println("Captcha found, please try with different IP address.");
	        return null;
	    }

	    Pattern p = Pattern.compile("stream_map\": \"(.*?)?\"");
	    // Pattern p = Pattern.compile("/stream_map=(.[^&]*?)\"/");
	    Matcher m = p.matcher(html);
	    List<String> matches = new ArrayList<String>();
	    while (m.find()) {
	        matches.add(m.group());
	    }

	    if (matches.size() != 1) {
	        System.out.println("Found zero or too many stream maps.");
	        return null;
	    }

	    String urls[] = matches.get(0).split(",");
	    HashMap<String, String> foundArray = new HashMap<String, String>();
	    for (String ppUrl : urls) {
	        String url = URLDecoder.decode(ppUrl, "UTF-8");
	        System.out.println("url = "+url);

	        Pattern p1 = Pattern.compile("itag=([0-9]+?)[&]");
	        Matcher m1 = p1.matcher(url);
	        String itag = null;
	        if (m1.find()) {
	            itag = m1.group(1);
	        }

	        Pattern p2 = Pattern.compile("sig=(.*?)[&]");
	        Matcher m2 = p2.matcher(url);
	        String sig = null;
	        if (m2.find()) {
	            sig = m2.group(1);
	        }

	        Pattern p3 = Pattern.compile("url=(.*?)[&]");
	        Matcher m3 = p3.matcher(ppUrl);
	        String um = null;
	        if (m3.find()) {
	            um = m3.group(1);
	        }

	        if (itag != null && sig != null && um != null) {
	            foundArray.put(itag, URLDecoder.decode(um, "UTF-8") + "&"
	                    + "signature=" + sig);
	        }
	    }

	    if (foundArray.size() == 0) {
	        System.out.println("Couldn't find any URLs and corresponding signatures");
	        return null;
	    }

	    HashMap<String, Meta> typeMap = new HashMap<String, Meta>();
	    typeMap.put("13", new Meta("13", "3GP", "Low Quality - 176x144"));
	    typeMap.put("17", new Meta("17", "3GP", "Medium Quality - 176x144"));
	    typeMap.put("36", new Meta("36", "3GP", "High Quality - 320x240"));
	    typeMap.put("5", new Meta("5", "FLV", "Low Quality - 400x226"));
	    typeMap.put("6", new Meta("6", "FLV", "Medium Quality - 640x360"));
	    typeMap.put("34", new Meta("34", "FLV", "Medium Quality - 640x360"));
	    typeMap.put("35", new Meta("35", "FLV", "High Quality - 854x480"));
	    typeMap.put("43", new Meta("43", "WEBM", "Low Quality - 640x360"));
	    typeMap.put("44", new Meta("44", "WEBM", "Medium Quality - 854x480"));
	    typeMap.put("45", new Meta("45", "WEBM", "High Quality - 1280x720"));
	    typeMap.put("18", new Meta("18", "MP4", "Medium Quality - 480x360"));
	    typeMap.put("22", new Meta("22", "MP4", "High Quality - 1280x720"));
	    typeMap.put("37", new Meta("37", "MP4", "High Quality - 1920x1080"));
	    typeMap.put("33", new Meta("38", "MP4", "High Quality - 4096x230"));

	    ArrayList<Video> videos = new ArrayList<Video>();

	    for (String format : typeMap.keySet()) {
	        Meta meta = typeMap.get(format);

	        if (foundArray.containsKey(format)) {
	            Video newVideo = new Video(meta.ext, meta.type,
	                    foundArray.get(format));
	            videos.add(newVideo);
	           System.out.println("YouTube Video streaming details: ext:" + newVideo.ext
	                    + ", type:" + newVideo.type + ", url:" + newVideo.url);
	        }
	    }

	    return videos;
	}

	private class YouTubePageStreamUriGetter extends
	        AsyncTask<String, String, String> {
	    ProgressDialog progressDialog;

	    @Override
	    protected void onPreExecute() {
	        super.onPreExecute();
	        progressDialog = ProgressDialog.show(context, "",
	                "Connecting to YouTube...", true);
	    }

	    @Override
	    protected String doInBackground(String... params) {
	        String url = params[0];
	        try {
	            ArrayList<Video> videos = getStreamingUrisFromYouTubePage(url);
	            if (videos != null && !videos.isEmpty()) {
	                String retVidUrl = null;
	                for (Video video : videos) {
	                    if (video.ext.toLowerCase().contains("mp4")
	                            && video.type.toLowerCase().contains("medium")) {
	                        retVidUrl = video.url;
	                        break;
	                    }
	                }
	                if (retVidUrl == null) {
	                    for (Video video : videos) {
	                        if (video.ext.toLowerCase().contains("3gp")
	                                && video.type.toLowerCase().contains(
	                                        "medium")) {
	                            retVidUrl = video.url;
	                            break;

	                        }
	                    }
	                }
	                if (retVidUrl == null) {

	                    for (Video video : videos) {
	                        if (video.ext.toLowerCase().contains("mp4")
	                                && video.type.toLowerCase().contains("low")) {
	                            retVidUrl = video.url;
	                            break;

	                        }
	                    }
	                }
	                if (retVidUrl == null) {
	                    for (Video video : videos) {
	                        if (video.ext.toLowerCase().contains("3gp")
	                                && video.type.toLowerCase().contains("low")) {
	                            retVidUrl = video.url;
	                            break;
	                        }
	                    }
	                }

	                return retVidUrl;
	            }
	        } catch (Exception e) {
	            System.out.println("Couldn't get YouTube streaming URL "+ e);
	        }
	        System.out.println("Couldn't get stream URI for " + url);
	        return null;
	    }

	    @Override
	    protected void onPostExecute(String streamingUrl) {
	        super.onPostExecute(streamingUrl);
	        progressDialog.dismiss();
	        if (streamingUrl != null) {
	        	
	        	Toast.makeText(context, "url = "+streamingUrl, Toast.LENGTH_LONG).show();
	        	System.out.println("streamingUrl = "+streamingUrl);
	                         /* Do what ever you want with streamUrl */
	        }
	    }
	}
	
}






























