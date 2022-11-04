package com.simplerapps.videotoaudio.splash

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.simplerapps.videotoaudio.databinding.ActivitySplashBinding
import com.simplerapps.videotoaudio.servicechooser.ServiceChooserActivity

class SplashActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this,ServiceChooserActivity::class.java)
            startActivity(intent)
            finish()
        },1000)
    }
}