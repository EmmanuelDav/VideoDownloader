package com.cyberIyke.allvideodowloader.activities

import android.view.*
import androidx.appcompat.app.AppCompatActivity

import com.cyberIyke.allvideodowloader.R
import android.widget.TextView
import android.os.Bundle
import com.gyf.immersionbar.ImmersionBar
import androidx.viewpager.widget.ViewPager
import com.rd.PageIndicatorView
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import androidx.viewpager.widget.PagerAdapter
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.text.style.ForegroundColorSpan
import android.text.method.LinkMovementMethod

class IntroActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager
    private var myViewPagerAdapter: MyViewPagerAdapter? = null
    private lateinit var layouts: IntArray
    private var dotsIndicator: PageIndicatorView? = null
    private lateinit var txtSkip: TextView
    private lateinit var txtNext: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)
        ImmersionBar.with(this@IntroActivity)
            .statusBarColor(R.color.white)
            .navigationBarColor(R.color.white)
            .statusBarDarkFont(true)
            .navigationBarDarkIcon(true)
            .init()
        layouts = intArrayOf(
            R.layout.slider_1,
            R.layout.slider_2,
            R.layout.slider_3,
            R.layout.slider_4
        )
        txtNext = findViewById(R.id.txtNext)
        txtSkip = findViewById(R.id.txtSkip)
        dotsIndicator = findViewById(R.id.dotsIndicator)
        viewPager = findViewById(R.id.viewPager)
        myViewPagerAdapter = MyViewPagerAdapter()
        viewPager.adapter = myViewPagerAdapter
        dotsIndicator!!.setViewPager(viewPager)
        viewPager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            public override fun onPageSelected(position: Int) {
                if (viewPager.getCurrentItem() == 3) {
                    txtSkip.setVisibility(View.INVISIBLE)
                    txtNext.setText("Got it")
                } else {
                    txtSkip.setVisibility(View.VISIBLE)
                    txtNext.setText("Next")
                }
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
        txtSkip.setOnClickListener { finish() }
        txtNext.setOnClickListener {
            if (viewPager.currentItem == 3) {
                finish()
            } else {
                viewPager.currentItem = viewPager.currentItem + 1
            }
        }
    }

    inner class MyViewPagerAdapter : PagerAdapter() {
        private var layoutInflater: LayoutInflater? = null
        public override fun instantiateItem(container: ViewGroup, position: Int): Any {
            layoutInflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater?
            val view: View = layoutInflater!!.inflate(layouts[position], container, false)
            container.addView(view)
            if (position == 3) {
                val txtTitle1: TextView = view.findViewById(R.id.txtTitle1)
                val spannableStringBuilder: SpannableStringBuilder =
                    SpannableStringBuilder("Please get the PERMISSION from the owner before you repost videos or images")
                spannableStringBuilder.setSpan(StyleSpan(Typeface.BOLD), 15, 25, 0)
                spannableStringBuilder.setSpan(
                    ForegroundColorSpan(getColor(R.color.text_1)),
                    15,
                    25,
                    0
                )
                txtTitle1.text = spannableStringBuilder
                txtTitle1.movementMethod = LinkMovementMethod.getInstance()
            }
            return view
        }

        override fun getCount(): Int {
            return layouts.size
        }

        override fun isViewFromObject(view: View, obj: Any): Boolean {
            return view === obj
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            val view: View = `object` as View
            container.removeView(view)
        }
    }
}