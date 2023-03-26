package com.cyberIyke.allvideodowloader.activities

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.cyberIyke.allvideodowloader.R
import com.cyberIyke.allvideodowloader.utils.Utils.Companion.getStatusBarHeight
import com.gyf.immersionbar.ImmersionBar

class FeedbackActivity : AppCompatActivity() {

    private lateinit var mainContent: RelativeLayout
    private lateinit var backBtn: ImageView
    private lateinit var imgCheckbox1: ImageView
    private lateinit var imgCheckbox2: ImageView
    private lateinit var imgCheckbox3: ImageView
    private lateinit var llBox1: LinearLayout
    private lateinit var llBox2: LinearLayout
    private lateinit var llBox3: LinearLayout
    private lateinit var txtCancel: TextView
    private lateinit var txtOK: TextView
    private var isBox1Selected: Boolean = false
    private var isBox2Selected: Boolean = false
    private var isBox3Selected: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)
        ImmersionBar.with(this@FeedbackActivity)
            .statusBarColor(R.color.white)
            .navigationBarColor(R.color.white)
            .statusBarDarkFont(true)
            .navigationBarDarkIcon(true)
            .init()
        mainContent = findViewById(R.id.mainContent)
        backBtn = findViewById(R.id.backBtn)
        txtCancel = findViewById(R.id.txtCancel)
        txtOK = findViewById(R.id.txtOK)
        imgCheckbox1 = findViewById(R.id.imgCheckbox1)
        imgCheckbox2 = findViewById(R.id.imgCheckbox2)
        imgCheckbox3 = findViewById(R.id.imgCheckbox3)
        llBox1 = findViewById(R.id.llBox1)
        llBox2 = findViewById(R.id.llBox2)
        llBox3 = findViewById(R.id.llBox3)
        txtOK.isEnabled = false
        txtOK.alpha = 0.5f
        var actionBarHeight: Int = 0
        val tv: TypedValue = TypedValue()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (theme.resolveAttribute(
                    android.R.attr.actionBarSize,
                    tv,
                    true
                )
            ) actionBarHeight =
                TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)
        } else if (theme.resolveAttribute(R.attr.actionBarSize, tv, true)) {
            actionBarHeight =
                TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)
        }
        val params: LinearLayout.LayoutParams =
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, actionBarHeight)
        params.setMargins(0, getStatusBarHeight(this), 0, 0)
        mainContent.setLayoutParams(params)
        backBtn.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                onBackPressed()
            }
        })
        llBox1.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                if (isBox1Selected) {
                    isBox1Selected = false
                    Glide.with(this@FeedbackActivity)
                        .load(R.drawable.ic_box_unselect)
                        .into(imgCheckbox1)
                } else {
                    isBox1Selected = true
                    Glide.with(this@FeedbackActivity)
                        .load(R.drawable.ic_box_selected)
                        .into(imgCheckbox1)
                }
                if (isBox1Selected || isBox2Selected || isBox3Selected) {
                    txtOK.isEnabled = true
                    txtOK.alpha = 1f
                } else {
                    txtOK.isEnabled = false
                    txtOK.alpha = 0.5f
                }
            }
        })
        llBox2.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                if (isBox2Selected) {
                    isBox2Selected = false
                    Glide.with(this@FeedbackActivity)
                        .load(R.drawable.ic_box_unselect)
                        .into(imgCheckbox2)
                } else {
                    isBox2Selected = true
                    Glide.with(this@FeedbackActivity)
                        .load(R.drawable.ic_box_selected)
                        .into(imgCheckbox2)
                }
                if (isBox1Selected || isBox2Selected || isBox3Selected) {
                    txtOK.isEnabled = true
                    txtOK.alpha = 1f
                } else {
                    txtOK.isEnabled = false
                    txtOK.alpha = 0.5f
                }
            }
        })
        llBox3.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                if (isBox3Selected) {
                    isBox3Selected = false
                    Glide.with(this@FeedbackActivity)
                        .load(R.drawable.ic_box_unselect)
                        .into(imgCheckbox3)
                } else {
                    isBox3Selected = true
                    Glide.with(this@FeedbackActivity)
                        .load(R.drawable.ic_box_selected)
                        .into(imgCheckbox3)
                }
                if (isBox1Selected || isBox2Selected || isBox3Selected) {
                    txtOK.isEnabled = true
                    txtOK.alpha = 1f
                } else {
                    txtOK.isEnabled = false
                    txtOK.alpha = 0.5f
                }
            }
        })
        txtCancel.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                onBackPressed()
            }
        })
        txtOK.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val stringBuilder: StringBuilder = StringBuilder()
                stringBuilder.append("Feedback:")
                if (isBox1Selected) {
                    stringBuilder.append("\n")
                    stringBuilder.append("Can’t browse videos")
                }
                if (isBox2Selected) {
                    stringBuilder.append("\n")
                    stringBuilder.append("No download resources deleted")
                }
                if (isBox3Selected) {
                    stringBuilder.append("\n")
                    stringBuilder.append("Too many ads")
                }
                val selectorIntent: Intent = Intent(Intent.ACTION_SENDTO)
                selectorIntent.data = Uri.parse("mailto:")
                val emailIntent: Intent = Intent(Intent.ACTION_SEND)
                emailIntent.putExtra(
                    Intent.EXTRA_EMAIL,
                    arrayOf(getString(R.string.feedback_email))
                )
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                emailIntent.putExtra(Intent.EXTRA_TEXT, stringBuilder.toString())
                emailIntent.selector = selectorIntent
                startActivity(Intent.createChooser(emailIntent, "Send email..."))
            }
        })
    }
}