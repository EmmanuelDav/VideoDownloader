package com.kunkunapp.allvideodowloader.browser;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.webkit.URLUtil;

import com.kunkunapp.allvideodowloader.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.HashMap;

public abstract class VideoContentSearch extends Thread {

    private static final String VIDEO = "video";
    private static final String AUDIO = "audio";
    private static final String LENGTH = "content-length";
    private static final String TWITTER = "twitter.com";
    private static final String YOUTUBE = "youtube.com";
    private static final String METACAFE = "metacafe.com";
    private static final String MYSPACE = "myspace.com";
    private Context context;
    private String url;
    private String page;
    private String title;
    private int numLinksInspected;

    public abstract void onStartInspectingURL();

    public abstract void onFinishedInspectingURL(boolean finishedAll);

    public abstract void onVideoFound(String size, String type, String link, String name,
                                      String page, boolean chunked, String website, boolean audio);

    public VideoContentSearch(Context context, String url, String page, String title) {
        this.context = context;
        this.url = url;
        this.page = page;
        //this.title = title;
        this.title = URLUtil.guessFileName(url, null, null);
        numLinksInspected = 0;
    }

    @Override
    public void run() {
        String urlLowerCase = url.toLowerCase();
        String[] filters = context.getResources().getStringArray(R.array.videourl_filters);
        boolean urlMightBeVideo = false;
        for (String filter : filters) {
            if (urlLowerCase.contains(filter)) {
                urlMightBeVideo = true;
                break;
            }
        }
        if (urlMightBeVideo) {
            numLinksInspected++;
            onStartInspectingURL();

            URLConnection uCon = null;
            try {
                uCon = new URL(url).openConnection();
                uCon.connect();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (uCon != null) {
                String contentType = uCon.getHeaderField("content-type");

                if (contentType != null) {
                    contentType = contentType.toLowerCase();
                    if (contentType.contains(VIDEO) || contentType.contains
                            (AUDIO)) {
                        addVideoToList(uCon, page, title, contentType);
                    } else if (contentType.equals("application/x-mpegurl") ||
                            contentType.equals("application/vnd.apple.mpegurl")) {
                        addVideosToListFromM3U8(uCon, page, title);
                    }
                }
            }

            numLinksInspected--;
            boolean finishedAll = false;
            if (numLinksInspected <= 0) {
                finishedAll = true;
            }
            onFinishedInspectingURL(finishedAll);
        }
    }

    private void addVideoToList(URLConnection uCon, String page, String title, String contentType) {
        try {
            String size = uCon.getHeaderField(LENGTH);
            String link = uCon.getHeaderField("Location");
            if (link == null) {
                link = uCon.getURL().toString();
            }

            String host = new URL(page).getHost();
            String website = null;
            boolean chunked = false;
            String type;
            boolean audio = false;

            // Skip twitter video chunks.
            if (host.contains(TWITTER) && contentType.equals("video/mp2t")) {
                return;
            }

            String name = VIDEO;
            if (title != null) {
                if (contentType.contains(AUDIO)) {
                    name = "[AUDIO ONLY]" + title;
                } else {
                    name = title;
                }
            } else if (contentType.contains(AUDIO)) {
                name = AUDIO;
            }

            if (host.contains(YOUTUBE) || (new URL(link).getHost().contains("googlevideo.com")
            )) {

                int r = link.lastIndexOf("&range");
                if (r > 0) {
                    link = link.substring(0, r);
                }
                URLConnection ytCon;
                ytCon = new URL(link).openConnection();
                ytCon.connect();
                size = ytCon.getHeaderField(LENGTH);

                if (host.contains(YOUTUBE)) {
                    URL embededURL = new URL("http://www.youtube.com/oembed?url=" + page +
                            "&format=json");
                    try {
                        String jSonString;
                        InputStream in = embededURL.openStream();

                        InputStreamReader inReader = new InputStreamReader(in, Charset
                                .defaultCharset());
                        StringBuilder sb = new StringBuilder();
                        char[] buffer = new char[1024];
                        int read;
                        while ((read = inReader.read(buffer)) != -1) {
                            sb.append(buffer, 0, read);
                        }
                        jSonString = sb.toString();

                        name = new JSONObject(jSonString).getString("title");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (contentType.contains(VIDEO)) {
                        name = "[VIDEO ONLY]" + name;
                    } else if (contentType.contains(AUDIO)) {
                        name = "[AUDIO ONLY]" + name;
                    }
                    website = YOUTUBE;
                }
            } else if (host.contains("dailymotion.com")) {
                chunked = true;
                website = "dailymotion.com";
                link = link.replaceAll("(frag\\()+(\\d+)+(\\))", "FRAGMENT");
                size = null;
            } else if (host.contains("vimeo.com") && link.endsWith("m4s")) {
                chunked = true;
                website = "vimeo.com";
                link = link.replaceAll("(segment-)+(\\d+)", "SEGMENT");
                size = null;
            } else if (host.contains("facebook.com") && link.contains("bytestart")) {
                int b = link.lastIndexOf("&bytestart");
                int f = link.indexOf("fbcdn");
                if (b > 0) {
                    link = "https://video.xx." + link.substring(f, b);
                }
                URLConnection fbCon;
                fbCon = new URL(link).openConnection();
                fbCon.connect();
                size = fbCon.getHeaderField(LENGTH);
                website = "facebook.com";

                try {
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(link, new HashMap<String, String>());
                    retriever.release();
                    audio = false;
                } catch (RuntimeException ex) {
                    audio = true;
                }
            }else if (host.contains("instagram.com")) {
                try {
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(link, new HashMap<String, String>());
                    retriever.release();
                    audio = false;
                } catch (RuntimeException ex) {
                    audio = true;
                }
            }

            switch (contentType) {
                case "video/mp4":
                    type = "mp4";
                    break;
                case "video/webm":
                    type = "webm";
                    break;
                case "video/mp2t":
                    type = "ts";
                    break;
                case "audio/webm":
                    type = "webm";
                    break;
                default:
                    type = "mp4";
                    break;
            }

            onVideoFound(size, type, link, name, page, chunked, website, audio);
        } catch (IOException e) {
            //nada
        }
    }

    private void addVideosToListFromM3U8(URLConnection uCon, String page, String title) {
        try {
            String host;
            Boolean audio = false;
            host = new URL(page).getHost();
            if (host.contains(TWITTER) || host.contains(METACAFE) || host.contains
                    (MYSPACE)) {
                InputStream in = uCon.getInputStream();
                InputStreamReader inReader = new InputStreamReader(in);
                BufferedReader buffReader = new BufferedReader(inReader);
                String line;
                String prefix = null;
                String type = null;
                String name = VIDEO;
                String website = null;
                if (title != null) {
                    name = title;
                }
                if (host.contains(TWITTER)) {
                    prefix = "https://video.twimg.com";
                    type = "ts";
                    website = TWITTER;
                } else if (host.contains(METACAFE)) {
                    String link = uCon.getURL().toString();
                    prefix = link.substring(0, link.lastIndexOf("/") + 1);
                    website = METACAFE;
                    type = "mp4";
                } else if (host.contains(MYSPACE)) {
                    String link = uCon.getURL().toString();
                    website = MYSPACE;
                    type = "ts";

                    onVideoFound(null, type, link, name, page, true, website, audio);

                    return;
                }
                while ((line = buffReader.readLine()) != null) {
                    if (line.endsWith(".m3u8")) {
                        String link = prefix + line;
                        onVideoFound(null, type, link, name, page, true, website, audio);

                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
