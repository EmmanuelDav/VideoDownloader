package com.cyberIyke.allvideodowloader.views;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

public class BadgeRed extends AppCompatTextView {


    public BadgeRed(Context context) {
        super(context);
    }

    public BadgeRed(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BadgeRed(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setNumber(int number) {
        if (number > 0) {
            show();
        } else {
            hide();
        }
        setText(String.valueOf(number));
    }

    public void hide() {
        setVisibility(GONE);
    }

    public void show() {
        setVisibility(VISIBLE);

    }


}