package com.cyberIyke.allvideodowloader.activities

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.gyf.immersionbar.ImmersionBar
import com.cyberIyke.allvideodowloader.R
import com.cyberIyke.allvideodowloader.data.Constants


@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    lateinit var imgLoading: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        ImmersionBar.with(this)
            .transparentStatusBar()
            .navigationBarColor(R.color.white)
            .statusBarDarkFont(true)
            .navigationBarDarkIcon(true)
            .init()

        if (savedInstanceState == null) {
            imgLoading = findViewById(R.id.imgLoading)
            val rotate = ObjectAnimator.ofFloat(imgLoading, "rotation", 0f, 360f)
            rotate.setDuration(1000)
            rotate.repeatCount = 500
            rotate.start()

            mainScreen()
        }
    }


    @Suppress("deprecation")
    private fun mainScreen() {
        Handler().postDelayed({
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }, Constants.SPLASH_SCREEN_TIMEOUT.toLong())
    }

    override fun onBackPressed() {

    }
}