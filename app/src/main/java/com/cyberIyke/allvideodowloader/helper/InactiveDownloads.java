package com.cyberIyke.allvideodowloader.helper;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class InactiveDownloads implements Serializable {
    private List<DownloadVideo> list;

    public InactiveDownloads() {
        list = new ArrayList<>();
    }

    public void add(Context context, DownloadVideo inactiveDownload) {
        list.add(inactiveDownload);
        save(context);
    }

    public static InactiveDownloads load(Context context) {
        File file = new File(context.getFilesDir(), "inactive.dat");
        InactiveDownloads inactiveDownloads = new InactiveDownloads();
        if (file.exists()) {
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                try (ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
                    inactiveDownloads = (InactiveDownloads) objectInputStream.readObject();
                }
                fileInputStream.close();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return inactiveDownloads;
    }

    public void save(Context context) {
        try {
            File file = new File(context.getFilesDir(), "inactive.dat");
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {
                objectOutputStream.writeObject(this);
            }
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<DownloadVideo> getList() {
        return list;
    }
}
