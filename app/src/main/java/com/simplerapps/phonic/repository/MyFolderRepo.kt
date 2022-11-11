package com.simplerapps.phonic.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MyFolderRepo(private val context: Context) {
    companion object {
        const val MY_SHARED_PREF = "my_shared_pref"
        const val MY_AUDIO_LIST = "my_audio_list"
    }

    private val sharedPref = context.getSharedPreferences(MY_SHARED_PREF, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val listTypeToken = object : TypeToken<ArrayList<AudioFileModel>>() {}.type

    fun getMyAudioList(): ArrayList<AudioFileModel>? {
        val list = sharedPref.getString(MY_AUDIO_LIST, null)
        list?.let { audioList ->
            return getExistenceVerifiedList(
                gson.fromJson(
                    audioList,
                    listTypeToken
                )
            )
        }

        return null
    }

    fun addToAudioList(audio: AudioFileModel) {
        var list = getMyAudioList()
        if (list == null) {
            list = ArrayList()
        }

        list.add(0, audio)

        saveList(list)
    }

    private fun getExistenceVerifiedList(list: ArrayList<AudioFileModel>): ArrayList<AudioFileModel> {
        val finalList = ArrayList<AudioFileModel>()
        list.forEach {
            if (isFileExists(Uri.parse(it.uri))) {
                finalList.add(it)
            }
        }

        return finalList
    }

    private fun isFileExists(uri: Uri) : Boolean {
        try {
            val input = context.contentResolver.openInputStream(uri)
            if (input != null) {
                input.close()
                return true
            }
            return false
        }
        catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun saveList(list: ArrayList<AudioFileModel>) {
        val listAsString = gson.toJson(list)
        sharedPref.edit().putString(MY_AUDIO_LIST, listAsString).apply()
    }
}