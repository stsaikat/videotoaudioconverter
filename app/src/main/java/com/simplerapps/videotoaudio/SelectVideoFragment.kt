package com.simplerapps.videotoaudio

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment

class SelectVideoFragment : Fragment(R.layout.fragment_select_video) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val selectVideoButton = view.findViewById<Button>(R.id.bt_select_video)
        selectVideoButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent,0)
        }
    }
}