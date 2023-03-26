package com.cyberIyke.allvideodowloader.helper;

import android.content.Context;
import android.os.Environment;

import com.cyberIyke.allvideodowloader.MyApp;
import com.cyberIyke.allvideodowloader.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DownloadQueues implements Serializable {
    private List<DownloadVideo> downloads;

    public DownloadQueues() {
        downloads = new ArrayList<>();
    }

    public static DownloadQueues load(Context context) {
        File file = new File(context.getFilesDir(), "downloads.dat");
        DownloadQueues queues = new DownloadQueues();
        if (file.exists()) {
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                try (ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
                    queues = (DownloadQueues) objectInputStream.readObject();
                }
                fileInputStream.close();
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
        }
        return queues;
    }

    public void save(Context context) {
        try {
            File file = new File(context.getFilesDir(), "downloads.dat");
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {
                objectOutputStream.writeObject(this);
            }
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void insertToTop(String size, String type, String link, String name, String page, boolean
            chunked, String website) {
        name = getValidName(name, type);

        DownloadVideo video = new DownloadVideo();
        video.link = link;
        video.name = name;
        video.page = page;
        video.size = size;
        video.type = type;
        video.chunked = chunked;
        video.website = website;
        downloads.add(0, video);
    }


    private String getValidName(String name, String type) {
        name = name.replaceAll("[^\\w ()'!\\[\\]\\-]", "");
        name = name.trim();
        if (name.length() > 127) {//allowed filename length is 127
            name = name.substring(0, 127);
        } else if (name.equals("")) {
            name = "video";
        }
        int i = 0;
        File file = new File(Environment.getExternalStoragePublicDirectory(MyApp.getInstance().getApplicationContext().getString(R.string.app_name)), name + "." + type);
        StringBuilder nameBuilder = new StringBuilder(name);
        while (file.exists()) {
            i++;
            nameBuilder = new StringBuilder(name);
            nameBuilder.append(" ").append(i);
            file = new File(Environment.getExternalStoragePublicDirectory(MyApp.getInstance().getApplicationContext().getString(R.string.app_name)), nameBuilder + "." + type);
        }
        while (nameAlreadyExists(nameBuilder.toString())) {
            i++;
            nameBuilder = new StringBuilder(name);
            nameBuilder.append(" ").append(i);
        }
        return nameBuilder.toString();
    }

    public List<DownloadVideo> getList() {
        return downloads;
    }

    public DownloadVideo getTopVideo() {
        if (!downloads.isEmpty()) {
            return downloads.get(0);
        } else {
            return null;
        }
    }

    public void deleteTopVideo(Context context) {
        if (!downloads.isEmpty()) {
            downloads.remove(0);
            save(context);
        }
    }

    private boolean nameAlreadyExists(String name) {
        for (DownloadVideo video : downloads) {
            if (video.name.equals(name)) return true;
        }
        return false;
    }

    public void renameItem(int index, String newName) {
        if (!downloads.get(index).name.equals(newName)) {
            downloads.get(index).name = getValidName(newName, downloads.get(index).type);
        }
    }
}

