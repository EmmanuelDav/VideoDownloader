package com.kunkunapp.allvideodowloader.helper;

import android.app.IntentService;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.kunkunapp.allvideodowloader.MyApp;
import com.kunkunapp.allvideodowloader.R;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class DownloadManager extends IntentService {
    private static final String CHUNCKED = "chunked";
    private static File downloadFile = null;
    private static long prevDownloaded = 0;
    private static long downloadSpeed = 0;
    private static long totalSize = 0;

    private static final String WEBSITE = "website";
    private static final String TWITTER = "twitter.com";
    private static final String METACAFE = "metacafe.com";
    private static final String MYSPACE = "myspace.com";

    private static boolean chunked;
    private static ByteArrayOutputStream bytesOfChunk;

    private static boolean stop = false;
    private static Thread downloadThread;

    public DownloadManager() {
        super("DownloadManager");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        stop = false;
        downloadThread = Thread.currentThread();
        if (intent != null) {
            chunked = intent.getBooleanExtra(CHUNCKED, false);

            if (chunked) {
                downloadFile = null;
                prevDownloaded = 0;
                downloadSpeed = 0;
                totalSize = 0;
                handleChunkedDownload(intent);
            } else {
                prevDownloaded = 0;
                URLConnection connection;
                try {
                    totalSize = Long.parseLong(intent.getStringExtra("size"));
                    connection = (new URL(intent.getStringExtra("link"))).openConnection();
                    String filename = intent.getStringExtra("name") + "." + intent.getStringExtra("type");
                    File directory = Environment.getExternalStoragePublicDirectory(getString(R.string.app_name));
                    boolean directotryExists;
                    directotryExists = directory.exists() || directory.mkdir() || directory
                            .createNewFile();
                    if (directotryExists) {

                        downloadFile = new File(Environment.getExternalStoragePublicDirectory(getString(R.string.app_name)), filename);
                        if (connection != null) {
                            FileOutputStream out = null;
                            if (downloadFile.exists()) {
                                prevDownloaded = downloadFile.length();
                                connection.setRequestProperty("Range", "bytes=" + downloadFile.length
                                        () + "-");
                                connection.connect();
                                out = new FileOutputStream(Environment.getExternalStoragePublicDirectory(getString(R.string.app_name)) + "/" + filename, true);
                            } else {
                                connection.connect();
                                if (downloadFile.createNewFile()) {
                                    out = new FileOutputStream(downloadFile.getAbsolutePath(), true);
                                }
                            }
                            if (out != null && downloadFile.exists()) {
                                InputStream in = connection.getInputStream();
                                FileChannel fileChannel;
                                try (ReadableByteChannel readableByteChannel = Channels.newChannel(in)) {
                                    fileChannel = out.getChannel();
                                    while (downloadFile.length() < totalSize) {
                                        if (stop) return;
                                        fileChannel.transferFrom(readableByteChannel, 0, 1024);
                                    }
                                }
                                in.close();
                                out.flush();
                                out.close();
                                fileChannel.close();
                                downloadFinished(filename);
                            }

                        }
                    }
                } catch (FileNotFoundException e) {
                    linkNotFound(intent);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void downloadFinished(String filename) {
        if (onDownloadFinishedListener != null) {
            onDownloadFinishedListener.onDownloadFinished();
        } else {
            DownloadQueues queues = DownloadQueues.load(getApplicationContext());
            queues.deleteTopVideo(getApplicationContext());
            CompletedVideos completedVideos = CompletedVideos.load
                    (getApplicationContext());
            completedVideos.addVideo(getApplicationContext(), filename);

            DownloadVideo topVideo = queues.getTopVideo();
            if (topVideo != null) {
                Intent downloadService = MyApp.getInstance().getDownloadService();
                downloadService.putExtra("link", topVideo.link);
                downloadService.putExtra("name", topVideo.name);
                downloadService.putExtra("type", topVideo.type);
                downloadService.putExtra("size", topVideo.size);
                downloadService.putExtra("page", topVideo.page);
                downloadService.putExtra(CHUNCKED, topVideo.chunked);
                downloadService.putExtra(WEBSITE, topVideo.website);
                onHandleIntent(downloadService);
            }
        }
    }

    private void linkNotFound(Intent intent) {
        if (onLinkNotFoundListener != null) {
            onLinkNotFoundListener.onLinkNotFound();
        } else {
            DownloadQueues queues = DownloadQueues.load(getApplicationContext());
            queues.deleteTopVideo(getApplicationContext());
            DownloadVideo inactiveDownload = new DownloadVideo();
            inactiveDownload.name = intent.getStringExtra("name");
            inactiveDownload.link = intent.getStringExtra("link");
            inactiveDownload.type = intent.getStringExtra("type");
            inactiveDownload.size = intent.getStringExtra("size");
            inactiveDownload.page = intent.getStringExtra("page");
            inactiveDownload.website = intent.getStringExtra(WEBSITE);
            inactiveDownload.chunked = intent.getBooleanExtra(CHUNCKED, false);
            InactiveDownloads inactiveDownloads = InactiveDownloads.load(getApplicationContext());
            inactiveDownloads.add(getApplicationContext(), inactiveDownload);

            DownloadVideo topVideo = queues.getTopVideo();
            if (topVideo != null) {
                Intent downloadService = MyApp.getInstance().getDownloadService();
                downloadService.putExtra("link", topVideo.link);
                downloadService.putExtra("name", topVideo.name);
                downloadService.putExtra("type", topVideo.type);
                downloadService.putExtra("size", topVideo.size);
                downloadService.putExtra("page", topVideo.page);
                downloadService.putExtra(CHUNCKED, topVideo.chunked);
                downloadService.putExtra(WEBSITE, topVideo.website);
                onHandleIntent(downloadService);
            }
        }
    }

    private void handleChunkedDownload(Intent intent) {
        try {
            String name = intent.getStringExtra("name");
            String type = intent.getStringExtra("type");
            File directory = Environment.getExternalStoragePublicDirectory(getString(R.string.app_name));

            boolean directotryExists;
            directotryExists = directory.exists() || directory.mkdir() || directory
                    .createNewFile();
            if (directotryExists) {
                File progressFile = new File(getCacheDir(), name + ".dat");
                File videoFile = new File(Environment.getExternalStoragePublicDirectory(getString(R.string.app_name)), name + "." + type);
                long totalChunks = 0;
                if (progressFile.exists()) {
                    FileInputStream in = new FileInputStream(progressFile);
                    DataInputStream data = new DataInputStream(in);
                    totalChunks = data.readLong();
                    data.close();
                    in.close();

                } else if (videoFile.exists()) {
                    downloadFinished(name + "." + type);
                }

                if (videoFile.exists() && progressFile.exists()) {
                    while (true) {
                        prevDownloaded = 0;
                        String website = intent.getStringExtra(WEBSITE);
                        String chunkUrl = null;
                        switch (website) {
                            case "dailymotion.com":
                                chunkUrl = getNextChunkWithDailymotionRule(intent, totalChunks);
                                break;
                            case "vimeo.com":
                                chunkUrl = getNextChunkWithVimeoRule(intent, totalChunks);
                                break;
                            case TWITTER:
                            case METACAFE:
                            case MYSPACE:
                                chunkUrl = getNextChunkWithM3U8Rule(intent, totalChunks);
                                break;
                            default:
                                break;
                        }
                        if (chunkUrl == null) {
                            downloadFinished(name + "." + type);
                        }
                        bytesOfChunk = new ByteArrayOutputStream();
                        try {
                            URLConnection uCon = new URL(chunkUrl).openConnection();
                            if (uCon != null) {
                                InputStream in = uCon.getInputStream();
                                try (ReadableByteChannel readableByteChannel = Channels.newChannel(in)) {
                                    try (WritableByteChannel writableByteChannel = Channels.newChannel(bytesOfChunk)) {
                                        int read;
                                        while (true) {
                                            if (stop) return;

                                            ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
                                            read = readableByteChannel.read(buffer);
                                            if (read != -1) {
                                                buffer.flip();
                                                writableByteChannel.write(buffer);
                                            } else {
                                                try (FileOutputStream vAddChunk = new FileOutputStream
                                                        (videoFile, true)) {
                                                    vAddChunk.write(bytesOfChunk.toByteArray());
                                                }
                                                FileOutputStream outputStream = new FileOutputStream
                                                        (progressFile, false);
                                                try (DataOutputStream dataOutputStream = new DataOutputStream
                                                        (outputStream)) {
                                                    dataOutputStream.writeLong(++totalChunks);
                                                }
                                                outputStream.close();
                                                break;
                                            }
                                        }
                                    }
                                }
                                in.close();
                                bytesOfChunk.close();
                            }
                        } catch (FileNotFoundException e) {
                            downloadFinished(name + "." + type);
                            break;
                        } catch (IOException e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                }

                MediaScannerConnection.scanFile(getApplicationContext(), new String[]{videoFile.getAbsolutePath()}, null, new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        //nada
                    }
                });

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getNextChunkWithDailymotionRule(Intent intent, long totalChunks) {
        String link = intent.getStringExtra("link");
        return link.replace("FRAGMENT", "frag(" + (totalChunks + 1) + ")");
    }

    private String getNextChunkWithVimeoRule(Intent intent, long totalChunks) {
        String link = intent.getStringExtra("link");
        return link.replace("SEGMENT", "segment-" + (totalChunks + 1));
    }

    private String getNextChunkWithM3U8Rule(Intent intent, long totalChunks) {
        String link = intent.getStringExtra("link");
        String website = intent.getStringExtra(WEBSITE);
        String line = null;
        try {
            URLConnection m3u8Con = new URL(link).openConnection();
            InputStream in = m3u8Con.getInputStream();
            InputStreamReader inReader = new InputStreamReader(in);
            BufferedReader buffReader = new BufferedReader(inReader);
            while ((line = buffReader.readLine()) != null) {
                if ((website.equals(TWITTER) || website.equals(MYSPACE)) && line
                        .endsWith(".ts") || website.equals(METACAFE) && line.endsWith(".mp4")) {
                    break;
                }
            }
            if (line != null) {
                long l = 1;
                while (l < (totalChunks + 1)) {
                    line = buffReader.readLine();
                    l++;
                }
            }
            buffReader.close();
            inReader.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (line != null) {
            String prefix;
            switch (website) {
                case TWITTER:
                    Log.i("VDInfo", "downloading chunk " + (totalChunks + 1) + ": " +
                            "https://video.twimg.com" + line);
                    return "https://video.twimg.com" + line;
                case METACAFE:
                case MYSPACE:
                    prefix = link.substring(0, link.lastIndexOf("/") + 1);
                    Log.i("VDInfo", "downloading chunk " + (totalChunks + 1) + ": " + prefix +
                            line);
                    return prefix + line;
                default:
                    return null;
            }
        } else {
            return null;
        }
    }

    public interface OnDownloadFinishedListener {
        void onDownloadFinished();
    }

    private static OnDownloadFinishedListener onDownloadFinishedListener;

    public static void setOnDownloadFinishedListener(OnDownloadFinishedListener listener) {
        onDownloadFinishedListener = listener;
    }


    public interface OnLinkNotFoundListener {
        void onLinkNotFound();
    }

    private static OnLinkNotFoundListener onLinkNotFoundListener;

    public static void setOnLinkNotFoundListener(OnLinkNotFoundListener listener) {
        onLinkNotFoundListener = listener;
    }

    @Override
    public void onDestroy() {
        downloadFile = null;
        Thread.currentThread().interrupt();
        super.onDestroy();
    }


    public static void stop() {
        Log.d("debug", "stop: called");
        Intent downloadService = MyApp.getInstance().getDownloadService();
        MyApp.getInstance().stopService(downloadService);
        forceStopIfNecessary();
    }

    public static void forceStopIfNecessary() {
        if(downloadThread != null){
            Log.d("debug", "force: called");
            downloadThread = Thread.currentThread();
            if (downloadThread.isAlive()) {
                stop = true;
            }
        }
    }

    /**
     * Should be called every second
     *
     * @return download speed in bytes per second
     */
    public static long getDownloadSpeed() {
        if (!chunked) {
            if (downloadFile != null) {
                long downloaded = downloadFile.length();
                downloadSpeed = downloaded - prevDownloaded;
                prevDownloaded = downloaded;
                return downloadSpeed;
            }
            return 0;
        } else {
            if (bytesOfChunk != null) {
                long downloaded = bytesOfChunk.size();
                downloadSpeed = downloaded - prevDownloaded;
                prevDownloaded = downloaded;
                return downloadSpeed;
            }
            return 0;
        }
    }

    /**
     * @return remaining time to download video in milliseconds
     */
    public static long getRemaining() {
        if (!chunked && (downloadFile != null)) {
            long remainingLength = totalSize - prevDownloaded;
            return (1000 * (remainingLength / downloadSpeed));
        } else {
            return 0;
        }
    }
}
