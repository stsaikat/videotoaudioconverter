package com.simplerapps.videotoaudio

import android.content.Intent
import android.net.Uri
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

        showSelectVideoFragment()
    }

    fun showSelectVideoFragment() {
        val selectVideoFragment = SelectVideoFragment()
        showFragment(selectVideoFragment)
    }

    fun showConvertInfoFragment(uri: Uri) {
        val convertInfoFragment = ConvertInfoFragment(uri)
        showFragment(convertInfoFragment)
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(viewBinding.fragmentContainer.id, fragment)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            val uri = data?.data
            uri?.let {
                showConvertInfoFragment(it)
            }
        }
    }
}