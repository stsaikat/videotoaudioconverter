package com.simplerapps.phonic.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.simplerapps.phonic.R
import com.simplerapps.phonic.databinding.DialogMyFolderOptionsBinding
import com.simplerapps.phonic.repository.AudioFileModel

class MyFolderOptionsDialog(
    private val audioFileModel: AudioFileModel,
    val listener: OnMyFolderOptionsClick
) : DialogFragment(R.layout.dialog_my_folder_options) {

    private lateinit var viewBinding: DialogMyFolderOptionsBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let { activity ->
            val builder = AlertDialog.Builder(activity)
            viewBinding = DialogMyFolderOptionsBinding.inflate(activity.layoutInflater)
            initListeners()
            builder.setView(viewBinding.root)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun initListeners() {
        viewBinding.tvbShare.setOnClickListener {
            listener.onShareClick(audioFileModel)
            dismiss()
        }

        viewBinding.tvbEdit.setOnClickListener {
            listener.onEditClick(audioFileModel)
            dismiss()
        }
    }

    interface OnMyFolderOptionsClick {
        fun onEditClick(audioFileModel: AudioFileModel)
        fun onShareClick(audioFileModel: AudioFileModel)
    }
}