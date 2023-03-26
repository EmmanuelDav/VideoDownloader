package com.cyberIyke.allvideodowloader.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "ShortcutTable")
public class ShortcutTable {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public Integer imgLogo;
    public String strTitle;
    public String strURL;

    public ShortcutTable(Integer imgLogo, String strTitle, String strURL) {
        this.imgLogo = imgLogo;
        this.strTitle = strTitle;
        this.strURL = strURL;
    }
}
