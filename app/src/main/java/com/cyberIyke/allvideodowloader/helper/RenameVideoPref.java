package com.cyberIyke.allvideodowloader.helper;

import android.content.Context;
import android.content.SharedPreferences;

public class RenameVideoPref {
    public SharedPreferences.Editor editor;
    public SharedPreferences preferences;


    public RenameVideoPref(Context context) {
        preferences = context.getSharedPreferences("RenameVideoPref", 0);
        this.editor = preferences.edit();
    }

    public void setString(String key, String value) {
        this.editor.putString(key, value).commit();
    }

    public String getString(String key, String defValue) {
        return this.preferences.getString(key, defValue);
    }

}
