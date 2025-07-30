package com.example.saltmusiccontroller

import com.example.saltmusiccontroller.model.ApiResponse
import com.example.saltmusiccontroller.model.NowPlaying
import com.example.saltmusiccontroller.util.Constants
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class MusicApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()
        
    private val gson = Gson()

    suspend fun getNowPlaying(ip: String, port: Int): NowPlaying? {
        val url = "http://$ip:$port${Constants.API_NOW_PLAYING}"
        return try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = response.body?.string()
                    json?.let { gson.fromJson(it, NowPlaying::class.java) }
                } else {
                    null
                }
            }
        } catch (e: IOException) {
            null
        }
    }

    suspend fun togglePlayPause(ip: String, port: Int): ApiResponse? {
        val url = "http://$ip:$port${Constants.API_PLAY_PAUSE}"
        return executeRequest(url)
    }

    suspend fun nextTrack(ip: String, port: Int): ApiResponse? {
        val url = "http://$ip:$port${Constants.API_NEXT_TRACK}"
        return executeRequest(url)
    }

    suspend fun previousTrack(ip: String, port: Int): ApiResponse? {
        val url = "http://$ip:$port${Constants.API_PREVIOUS_TRACK}"
        return executeRequest(url)
    }

    suspend fun volumeUp(ip: String, port: Int): ApiResponse? {
        val url = "http://$ip:$port${Constants.API_VOLUME_UP}"
        return executeRequest(url)
    }

    suspend fun volumeDown(ip: String, port: Int): ApiResponse? {
        val url = "http://$ip:$port${Constants.API_VOLUME_DOWN}"
        return executeRequest(url)
    }

    suspend fun toggleMute(ip: String, port: Int): ApiResponse? {
        val url = "http://$ip:$port${Constants.API_MUTE}"
        return executeRequest(url)
    }
    
    private suspend fun executeRequest(url: String): ApiResponse? {
        return try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = response.body?.string()
                    json?.let { gson.fromJson(it, ApiResponse::class.java) }
                } else {
                    null
                }
            }
        } catch (e: IOException) {
            null
        }
    }
}
    