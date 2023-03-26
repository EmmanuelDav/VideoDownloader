package com.cyberIyke.allvideodowloader.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gyf.immersionbar.ImmersionBar;
import com.rd.PageIndicatorView;
import com.cyberIyke.allvideodowloader.R;

public class IntroActivity extends AppCompatActivity {
    private ViewPager viewPager;
    private MyViewPagerAdapter myViewPagerAdapter;
    private int[] layouts;
    private PageIndicatorView dotsIndicator;
    private TextView txtSkip;
    private TextView txtNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        ImmersionBar.with(IntroActivity.this)
                .statusBarColor(R.color.white)
                .navigationBarColor(R.color.white)
                .statusBarDarkFont(true)
                .navigationBarDarkIcon(true)
                .init();

        layouts = new int[]{
                R.layout.slider_1,
                R.layout.slider_2,
                R.layout.slider_3,
                R.layout.slider_4
        };

        txtNext = findViewById(R.id.txtNext);
        txtSkip = findViewById(R.id.txtSkip);
        dotsIndicator = findViewById(R.id.dotsIndicator);
        viewPager = findViewById(R.id.viewPager);
        myViewPagerAdapter = new MyViewPagerAdapter();
        viewPager.setAdapter(myViewPagerAdapter);
        dotsIndicator.setViewPager(viewPager);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (viewPager.getCurrentItem() == 3) {
                    txtSkip.setVisibility(View.INVISIBLE);
                    txtNext.setText("Got it");
                } else {
                    txtSkip.setVisibility(View.VISIBLE);
                    txtNext.setText("Next");
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        txtSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        txtNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (viewPager.getCurrentItem() == 3) {
                    finish();
                } else {
                    viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
                }
            }
        });
    }

    public class MyViewPagerAdapter extends PagerAdapter {
        private LayoutInflater layoutInflater;

        public MyViewPagerAdapter() {
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = layoutInflater.inflate(layouts[position], container, false);
            container.addView(view);

            if (position == 3) {
                TextView txtTitle1 = view.findViewById(R.id.txtTitle1);
                SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder("Please get the PERMISSION from the owner before you repost videos or images");
                spannableStringBuilder.setSpan(new StyleSpan(Typeface.BOLD), 15, 25, 0);
                spannableStringBuilder.setSpan(new ForegroundColorSpan(getColor(R.color.text_1)), 15, 25, 0);
                txtTitle1.setText(spannableStringBuilder);
                txtTitle1.setMovementMethod(LinkMovementMethod.getInstance());
            }
            return view;
        }

        @Override
        public int getCount() {
            return layouts.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object obj) {
            return view == obj;
        }


        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View view = (View) object;
            container.removeView(view);
        }
    }
}