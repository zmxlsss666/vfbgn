package com.example.saltmusiccontroller

import com.example.saltmusiccontroller.model.SongDetails.*
import com.example.saltmusiccontroller.util.Constants
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class SongInfoService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
        
    private val gson = Gson()

    suspend fun searchSongId(title: String, artist: String): Long? {
        val query = URLEncoder.encode("$title-$artist", "UTF-8")
        val url = "${Constants.SEARCH_SONG_API}$query"
        
        return try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = response.body?.string()
                    json?.let {
                        val searchResponse = gson.fromJson(it, SongSearchResponse::class.java)
                        searchResponse.result.songs.firstOrNull()?.id
                    }
                } else {
                    null
                }
            }
        } catch (e: IOException) {
            null
        }
    }

    suspend fun getSongInfo(songId: Long): SongInfoResponse? {
        val url = "${Constants.SONG_INFO_API}$songId"
        
        return try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = response.body?.string()
                    json?.let {
                        // 响应是JSON数组，取第一个元素
                        val jsonArray = gson.fromJson(it, Array<SongInfoResponse>::class.java)
                        jsonArray.firstOrNull()
                    }
                } else {
                    null
                }
            }
        } catch (e: IOException) {
            null
        }
    }
}
    