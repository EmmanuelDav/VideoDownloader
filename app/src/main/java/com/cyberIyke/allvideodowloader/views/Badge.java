package com.cyberIyke.allvideodowloader.views;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import com.cyberIyke.allvideodowloader.R;

public class Badge extends AppCompatTextView {


    public Badge(Context context) {
        super(context);
    }

    public Badge(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Badge(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setNumber(int number) {
        setText(String.valueOf(number));
    }

    public int getNumber(){
        return Integer.parseInt(getText().toString());
    }

    public void tabSelected(boolean isSelected) {
        if (isSelected) {
            setTextColor(ContextCompat.getColor(getContext(), R.color.white));
            setBackground(ContextCompat.getDrawable(getContext(), R.drawable.ic_tab_selected));
        } else {
            setTextColor(ContextCompat.getColor(getContext(), R.color.text_2));
            setBackground(ContextCompat.getDrawable(getContext(), R.drawable.ic_tab_unselected));
        }
    }

    public void hide() {
        setVisibility(GONE);
    }

    public void show() {
        setVisibility(VISIBLE);
    }


}