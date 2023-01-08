package com.simplerapps.phonic.fragment

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.simplerapps.phonic.R

class ConvertInfoFragment(private val uri: Uri,private val onClickListener: OnClickListener) : Fragment(
    R.layout.fragment_convert_info
) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val audioNameEditText = view.findViewById<EditText>(R.id.et_audio_name)
        val videoThumbImageView = view.findViewById<ImageView>(R.id.iv_video_thumb)
        val videoNameTextView = view.findViewById<TextView>(R.id.tv_video_name)

        val fileName = uri.getName(requireContext())!!

        audioNameEditText.setText(getNameWithoutExtension(fileName))
        videoNameTextView.text = fileName

        val thumb = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(context, uri)
            mmr.frameAtTime
        } else {
            null
        }

        thumb?.let {
            videoThumbImageView.setImageBitmap(it)
        }

        view.findViewById<Button>(R.id.bt_convert).setOnClickListener {
            onClickListener.onClick(it)
        }
    }

    private fun getNameWithoutExtension(nameWithExt: String): String {
        val lastDot = nameWithExt.lastIndexOf('.')
        if (lastDot == -1) return nameWithExt
        return nameWithExt.substring(0, lastDot)
    }

    fun Uri.getName(context: Context): String? {
        val returnCursor = context.contentResolver.query(this, null, null, null, null)
        val nameIndex = returnCursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        returnCursor?.moveToFirst()
        val fileName = nameIndex?.let { returnCursor.getString(it) }
        returnCursor?.close()
        return fileName
    }

    interface OnClickListener {
        fun onClick(view: View)
    }
}