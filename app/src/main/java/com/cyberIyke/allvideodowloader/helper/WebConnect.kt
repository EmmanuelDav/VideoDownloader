package com.cyberIyke.allvideodowloader.helper

import android.util.Patterns
import android.widget.EditText
import com.cyberIyke.allvideodowloader.activities.MainActivity

class WebConnect(private val textBox: EditText, private val activity: MainActivity) {
    fun connect() {
        var text = textBox.text.toString()
        if (Patterns.WEB_URL.matcher(text).matches()) {
            if (!text.startsWith("http")) {
                text = "http://$text"
            }
            activity.browserManager.newWindow(text)
        } else {
            val prefs = activity.getSharedPreferences("settings", 0)
            val searchEngine = prefs.getString("searchEngine", "Google")
            when (searchEngine) {
                "Google" -> text = "https://google.com/search?q=$text"
                "Bing" -> text = "https://www.bing.com/search?q=$text"
                "Ask" -> text = "https://www.ask.com/web?q=$text"
                "Yahoo" -> text = "https://www.yahoo.com/search?q=$text"
                "Baidu" -> text = "http://www.baidu.com/s?ie=$text"
                "Yandex" -> text = "https://yandex.ru/search/?text=$text"
            }
            activity.browserManager.newWindow(text)
        }
    }
}