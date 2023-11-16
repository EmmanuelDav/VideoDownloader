package com.cyberIyke.allvideodowloader.helper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.DisplayMetrics
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.cyberIyke.allvideodowloader.R
import com.google.android.gms.ads.*
import com.google.android.gms.ads.formats.UnifiedNativeAd
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdController {
    var adCounter = 1
    var adDisplayCounter = 7
    var nativeAd: UnifiedNativeAd? = null
    fun initAd(context: Context?) {
        MobileAds.initialize(context!!) { initializationStatus: InitializationStatus? -> }
    }

    private var gadView: AdView? = null
    fun loadBannerAd(context: Context, adContainer: LinearLayout) {
        gadView = AdView(context)
        gadView!!.adUnitId = context.getString(R.string.admob_banner_ad_id)
        adContainer.addView(gadView)
        loadBanner(context)
    }

    private fun loadBanner(context: Context?) {
        val adRequest = AdRequest.Builder().build()
        val adSize = AdController.getAdSize(context as Activity)
      //  gadView!!.adSize = adSize
        gadView!!.loadAd(adRequest)
    }

    fun getAdSize(context: Activity): AdSize {
        val display = context.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        val widthPixels = outMetrics.widthPixels.toFloat()
        val density = outMetrics.density
        val adWidth = (widthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
    }

    fun largeBannerAd(context: Context, adContainer: LinearLayout) {
        val adView = AdView(context)
        val adRequest = AdRequest.Builder().build()
       // adView.adSize = AdSize.LARGE_BANNER
        adView.adUnitId = context.getString(R.string.admob_banner_ad_id)
        adView.loadAd(adRequest)
        adContainer.addView(adView)
    }

    var mInterstitialAd: InterstitialAd? = null
    fun loadInterAd(context: Context) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            context.getString(R.string.admob_interstitial_ad_id),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    // The mInterstitialAd reference will be null until
                    // an ad is loaded.
                    mInterstitialAd = interstitialAd
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    // Handle the error
                    mInterstitialAd = null
                }
            })
    }

    fun showInterAd(context: Activity?, intent: Intent?, requstCode: Int) {
        if (adCounter == adDisplayCounter && mInterstitialAd != null) {
            adCounter = 1
            mInterstitialAd!!.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        // Called when fullscreen content is dismissed.
//                    Log.d("TAG", "The ad was dismissed.");
                        AdController.loadInterAd(context!!)
                        startActivity(context, intent, requstCode)
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
//                     Called when fullscreen content failed to show.
//                    Log.d("TAG", "The ad failed to show.");
                    }

                    override fun onAdShowedFullScreenContent() {
                        // Called when fullscreen content is shown.
                        // Make sure to set your reference to null so you don't
                        // show it a second time.
                        mInterstitialAd = null
                        //                    Log.d("TAG", "The ad was shown.");
                    }
                }
            mInterstitialAd!!.show(context!!)
        } else {
            if (adCounter == adDisplayCounter) {
                adCounter = 1
            }
            if (context != null) {
                startActivity(context, intent, requstCode)
            }
        }
    }

    fun startActivity(context: Activity, intent: Intent?, requestCode: Int) {
        if (intent != null) {
            context.startActivityForResult(intent, requestCode)
        }
    }

    fun showInterAd(context: Fragment, intent: Intent?, requstCode: Int) {
        if (adCounter == adDisplayCounter && mInterstitialAd != null) {
            adCounter = 1
            mInterstitialAd!!.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        // Called when fullscreen content is dismissed.
//                    Log.d("TAG", "The ad was dismissed.");
                        context.activity?.let { AdController.loadInterAd(it) }
                        startActivity(context, intent, requstCode)
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
//                     Called when fullscreen content failed to show.
//                    Log.d("TAG", "The ad failed to show.");
                    }

                    override fun onAdShowedFullScreenContent() {
                        // Called when fullscreen content is shown.
                        // Make sure to set your reference to null so you don't
                        // show it a second time.
                        mInterstitialAd = null
                        //                    Log.d("TAG", "The ad was shown.");
                    }
                }
            context.activity?.let { mInterstitialAd!!.show(it) }
        } else {
            if (adCounter == adDisplayCounter) {
                adCounter = 1
            }
            startActivity(context, intent, requstCode)
        }
    }

    fun startActivity(context: Fragment, intent: Intent?, requestCode: Int) {
        if (intent != null) {
            context.startActivityForResult(intent, requestCode)
        }
    }
}