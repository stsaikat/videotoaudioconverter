package com.simplerapps.phonic.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MyFolderRepo(private val context: Context) {
    companion object {
        const val MY_SHARED_PREF = "my_shared_pref"
        const val MY_AUDIO_LIST = "my_audio_list"
    }

    private val sharedPref = context.getSharedPreferences(MY_SHARED_PREF,Context.MODE_PRIVATE)
    private val gson = Gson()
    private val listTypeToken = object : TypeToken<ArrayList<AudioFileModel>>() {}.type

    fun getMyAudioList() : ArrayList<AudioFileModel>? {
        val list = sharedPref.getString(MY_AUDIO_LIST,null)
        list?.let { audioList ->
            return gson.fromJson<ArrayList<AudioFileModel>>(audioList,listTypeToken)
        }

        return null
    }

    fun addToAudioList(audio: AudioFileModel) {
        var list = getMyAudioList()
        if (list == null) {
            list = ArrayList()
        }

        list.add(audio)

        val listAsString = gson.toJson(list)
        sharedPref.edit().putString(MY_AUDIO_LIST,listAsString).apply()
    }

}