package com.simplerapps.phonic

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
            pickVideo()
            //pickAudio()
        }
    }

    private fun pickVideo() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent,0)
    }

    private fun pickAudio() {
        val audioIntent = Intent();
        audioIntent.type = "audio/*"
        audioIntent.action = Intent.ACTION_OPEN_DOCUMENT
        startActivityForResult(
            Intent.createChooser(
                audioIntent,
                "Select Audio",
            ),
            1
        )
    }
}