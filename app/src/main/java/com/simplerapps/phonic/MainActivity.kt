package com.simplerapps.phonic

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.simplerapps.phonic.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        showSelectVideoFragment()
    }

    private fun showSelectVideoFragment() {
        val selectVideoFragment = SelectVideoFragment()
        showFragment(selectVideoFragment)
    }

    private fun showConvertInfoFragment(uri: Uri) {
        val convertInfoFragmentOnClickListener = object : ConvertInfoFragment.OnClickListener {
            override fun onClick(view: View) {
                when(view.id) {
                    R.id.bt_convert -> showConvertProcessFragment(uri)
                }
            }
        }
        val convertInfoFragment = ConvertInfoFragment(uri,convertInfoFragmentOnClickListener)
        showFragment(convertInfoFragment)
    }

    private fun showConvertProcessFragment(uri: Uri) {
        val processFragmentListener = object : ConvertProcessFragment.Listener {
            override fun onButtonClick(view: View) {
                when(view.id) {
                    R.id.ibt_home -> showSelectVideoFragment()
                }
            }
        }
        val convertProcessFragment = ConvertProcessFragment(uri,processFragmentListener)
        showFragment(convertProcessFragment)
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(viewBinding.fragmentContainer.id, fragment)
        }
    }

    @Deprecated("Deprecated in Java")
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