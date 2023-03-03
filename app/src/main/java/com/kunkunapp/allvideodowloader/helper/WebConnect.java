package com.kunkunapp.allvideodowloader.helper;

import android.content.SharedPreferences;
import android.util.Patterns;
import android.widget.EditText;

import com.kunkunapp.allvideodowloader.activities.MainActivity;

public class WebConnect {
    private EditText textBox;
    private MainActivity activity;

    public WebConnect(EditText textBox, MainActivity activity) {
        this.textBox = textBox;
        this.activity = activity;
    }

    public void connect() {
        String text = textBox.getText().toString();
        if (Patterns.WEB_URL.matcher(text).matches()) {
            if (!text.startsWith("http")) {
                text = "http://" + text;
            }
            activity.getBrowserManager().newWindow(text);
        } else {
            final SharedPreferences prefs = activity.getSharedPreferences("settings", 0);
            String searchEngine = prefs.getString("searchEngine", "Google");
            switch (searchEngine) {
                case "Google":
                    text = "https://google.com/search?q=" + text;
                    break;
                case "Bing":
                    text = "https://www.bing.com/search?q=" + text;
                    break;
                case "Ask":
                    text = "https://www.ask.com/web?q=" + text;
                    break;
                case "Yahoo":
                    text = "https://www.yahoo.com/search?q=" + text;
                    break;
                case "Baidu":
                    text = "http://www.baidu.com/s?ie=" + text;
                    break;
                case "Yandex":
                    text = "https://yandex.ru/search/?text=" + text;
                    break;
            }
            activity.getBrowserManager().newWindow(text);
        }
    }
}
