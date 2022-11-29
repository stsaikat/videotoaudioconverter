package com.simplerapps.phonic.splash

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.MobileAds
import com.simplerapps.phonic.common.AdManager
import com.simplerapps.phonic.databinding.ActivitySplashBinding
import com.simplerapps.phonic.servicechooser.ServiceChooserActivity

class SplashActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        supportActionBar?.hide()

        MobileAds.initialize(applicationContext)
        AdManager.loadAd(applicationContext)

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this,ServiceChooserActivity::class.java)
            startActivity(intent)
            finish()
        },2500)
    }
}