package com.cyberIyke.allvideodowloader.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.gyf.immersionbar.ImmersionBar;
import com.cyberIyke.allvideodowloader.R;
import com.cyberIyke.allvideodowloader.utils.Utils;

public class FeedbackActivity extends AppCompatActivity {
    private RelativeLayout mainContent;
    private ImageView backBtn;
    private ImageView imgCheckbox1, imgCheckbox2, imgCheckbox3;
    private LinearLayout llBox1, llBox2, llBox3;
    private TextView txtCancel, txtOK;
    private boolean isBox1Selected = false;
    private boolean isBox2Selected = false;
    private boolean isBox3Selected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        ImmersionBar.with(FeedbackActivity.this)
                .statusBarColor(R.color.white)
                .navigationBarColor(R.color.white)
                .statusBarDarkFont(true)
                .navigationBarDarkIcon(true)
                .init();


        mainContent = findViewById(R.id.mainContent);
        backBtn = findViewById(R.id.backBtn);
        txtCancel = findViewById(R.id.txtCancel);
        txtOK = findViewById(R.id.txtOK);

        imgCheckbox1 = findViewById(R.id.imgCheckbox1);
        imgCheckbox2 = findViewById(R.id.imgCheckbox2);
        imgCheckbox3 = findViewById(R.id.imgCheckbox3);

        llBox1 = findViewById(R.id.llBox1);
        llBox2 = findViewById(R.id.llBox2);
        llBox3 = findViewById(R.id.llBox3);

        txtOK.setEnabled(false);
        txtOK.setAlpha(0.5f);

        int actionBarHeight = 0;
        TypedValue tv = new TypedValue();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
                actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        } else if (getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, actionBarHeight);
        params.setMargins(0, Utils.Companion.getStatusBarHeight(this), 0, 0);
        mainContent.setLayoutParams(params);

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        llBox1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBox1Selected) {
                    isBox1Selected = false;
                    Glide.with(FeedbackActivity.this)
                            .load(R.drawable.ic_box_unselect)
                            .into(imgCheckbox1);
                } else {
                    isBox1Selected = true;
                    Glide.with(FeedbackActivity.this)
                            .load(R.drawable.ic_box_selected)
                            .into(imgCheckbox1);
                }
                if (isBox1Selected || isBox2Selected || isBox3Selected) {
                    txtOK.setEnabled(true);
                    txtOK.setAlpha(1);
                } else {
                    txtOK.setEnabled(false);
                    txtOK.setAlpha(0.5f);
                }
            }
        });
        llBox2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBox2Selected) {
                    isBox2Selected = false;
                    Glide.with(FeedbackActivity.this)
                            .load(R.drawable.ic_box_unselect)
                            .into(imgCheckbox2);
                } else {
                    isBox2Selected = true;
                    Glide.with(FeedbackActivity.this)
                            .load(R.drawable.ic_box_selected)
                            .into(imgCheckbox2);
                }
                if (isBox1Selected || isBox2Selected || isBox3Selected) {
                    txtOK.setEnabled(true);
                    txtOK.setAlpha(1);
                } else {
                    txtOK.setEnabled(false);
                    txtOK.setAlpha(0.5f);
                }
            }
        });

        llBox3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBox3Selected) {
                    isBox3Selected = false;
                    Glide.with(FeedbackActivity.this)
                            .load(R.drawable.ic_box_unselect)
                            .into(imgCheckbox3);
                } else {
                    isBox3Selected = true;
                    Glide.with(FeedbackActivity.this)
                            .load(R.drawable.ic_box_selected)
                            .into(imgCheckbox3);
                }

                if (isBox1Selected || isBox2Selected || isBox3Selected) {
                    txtOK.setEnabled(true);
                    txtOK.setAlpha(1);
                } else {
                    txtOK.setEnabled(false);
                    txtOK.setAlpha(0.5f);
                }
            }
        });
        txtCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        txtOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Feedback:");
                if (isBox1Selected) {
                    stringBuilder.append("\n");
                    stringBuilder.append("Can’t browse videos");
                }
                if (isBox2Selected) {
                    stringBuilder.append("\n");
                    stringBuilder.append("No download resources deleted");
                }
                if (isBox3Selected) {
                    stringBuilder.append("\n");
                    stringBuilder.append("Too many ads");
                }

                Intent selectorIntent = new Intent(Intent.ACTION_SENDTO);
                selectorIntent.setData(Uri.parse("mailto:"));

                final Intent emailIntent = new Intent(Intent.ACTION_SEND);
                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.feedback_email)});
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                emailIntent.putExtra(Intent.EXTRA_TEXT, stringBuilder.toString());
                emailIntent.setSelector( selectorIntent );

                startActivity(Intent.createChooser(emailIntent, "Send email..."));
            }
        });
    }
}