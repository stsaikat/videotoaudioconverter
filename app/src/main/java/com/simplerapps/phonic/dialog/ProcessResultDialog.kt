package com.simplerapps.phonic.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.simplerapps.phonic.R

class ProcessResultDialog(private val title: String?, private val message: String?) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val view = requireActivity().layoutInflater.inflate(R.layout.dialog_process_result,null)
            val tvTitle = view.findViewById<TextView>(R.id.tv_rd_title)
            val tvMessage = view.findViewById<TextView>(R.id.tv_rd_message)
            if (title != null) {
                tvTitle.visibility = View.VISIBLE
                tvTitle.text = title
            }
            else {
                tvTitle.visibility = View.GONE
            }

            if (message != null) {
                tvMessage.visibility = View.VISIBLE
                tvMessage.text = message
            }
            else {
                tvMessage.visibility = View.GONE
            }

            view.findViewById<TextView>(R.id.tv_ok).setOnClickListener {
                dismiss()
            }
            builder.setView(view)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}