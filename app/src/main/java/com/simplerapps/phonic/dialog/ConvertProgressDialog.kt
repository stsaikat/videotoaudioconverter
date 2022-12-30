package com.simplerapps.phonic.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.simplerapps.phonic.R
import kotlin.math.max

class ConvertProgressDialog() : DialogFragment() {

    private lateinit var progressBar: ProgressBar
    private lateinit var progressPercent: TextView
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val view = requireActivity().layoutInflater.inflate(R.layout.dialog_convert_progress,null)
            progressBar = view.findViewById(R.id.pb_convert_progress)
            progressPercent = view.findViewById(R.id.tv_progress_percent)
            builder.setView(view)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    fun setProgress(progress: Int) {
        var modProgress = progress
        if (this::progressBar.isInitialized) {
            modProgress = max(modProgress,progress)
            progressBar.progress = modProgress
        }

        if (this::progressPercent.isInitialized) {
            handler.post {
                "$modProgress%".also { progressPercent.text = it }
            }
        }
    }
}