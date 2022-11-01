package com.simplerapps.videotoaudio

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.simplerapps.videotoaudio.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        //showSelectVideoFragment()
        showConvertInfoFragment()
    }

    fun showSelectVideoFragment() {
        val selectVideoFragment = SelectVideoFragment()
        showFragment(selectVideoFragment)
    }

    fun showConvertInfoFragment() {
        val convertInfoFragment = ConvertInfoFragment()
        showFragment(convertInfoFragment)
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(viewBinding.fragmentContainer.id,fragment)
        }
    }
}