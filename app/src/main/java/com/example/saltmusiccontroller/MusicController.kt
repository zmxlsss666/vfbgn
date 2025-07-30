package com.example.saltmusiccontroller

import com.example.saltmusiccontroller.model.ApiResponse
import com.example.saltmusiccontroller.model.NowPlaying
import com.example.saltmusiccontroller.model.SongInfoResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicController(
    private val musicApiService: MusicApiService = MusicApiService(),
    private val songInfoService: SongInfoService = SongInfoService()
) {
    var currentIp: String? = null
    var currentPort: Int = 35373

    fun setDevice(ip: String, port: Int = 35373) {
        currentIp = ip
        currentPort = port
    }

    fun getNowPlaying(callback: (NowPlaying?) -> Unit) {
        currentIp?.let { ip ->
            CoroutineScope(Dispatchers.IO).launch {
                val result = musicApiService.getNowPlaying(ip, currentPort)
                withContext(Dispatchers.Main) {
                    callback(result)
                }
            }
        } ?: run {
            callback(null)
        }
    }

    fun togglePlayPause(callback: (ApiResponse?) -> Unit) {
        currentIp?.let { ip ->
            CoroutineScope(Dispatchers.IO).launch {
                val result = musicApiService.togglePlayPause(ip, currentPort)
                withContext(Dispatchers.Main) {
                    callback(result)
                }
            }
        } ?: run {
            callback(null)
        }
    }

    fun nextTrack(callback: (ApiResponse?) -> Unit) {
        currentIp?.let { ip ->
            CoroutineScope(Dispatchers.IO).launch {
                val result = musicApiService.nextTrack(ip, currentPort)
                withContext(Dispatchers.Main) {
                    callback(result)
                }
            }
        } ?: run {
            callback(null)
        }
    }

    fun previousTrack(callback: (ApiResponse?) -> Unit) {
        currentIp?.let { ip ->
            CoroutineScope(Dispatchers.IO).launch {
                val result = musicApiService.previousTrack(ip, currentPort)
                withContext(Dispatchers.Main) {
                    callback(result)
                }
            }
        } ?: run {
            callback(null)
        }
    }

    fun volumeUp(callback: (ApiResponse?) -> Unit) {
        currentIp?.let { ip ->
            CoroutineScope(Dispatchers.IO).launch {
                val result = musicApiService.volumeUp(ip, currentPort)
                withContext(Dispatchers.Main) {
                    callback(result)
                }
            }
        } ?: run {
            callback(null)
        }
    }

    fun volumeDown(callback: (ApiResponse?) -> Unit) {
        currentIp?.let { ip ->
            CoroutineScope(Dispatchers.IO).launch {
                val result = musicApiService.volumeDown(ip, currentPort)
                withContext(Dispatchers.Main) {
                    callback(result)
                }
            }
        } ?: run {
            callback(null)
        }
    }

    fun toggleMute(callback: (ApiResponse?) -> Unit) {
        currentIp?.let { ip ->
            CoroutineScope(Dispatchers.IO).launch {
                val result = musicApiService.toggleMute(ip, currentPort)
                withContext(Dispatchers.Main) {
                    callback(result)
                }
            }
        } ?: run {
            callback(null)
        }
    }

    fun getSongDetails(title: String, artist: String, callback: (SongInfoResponse?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val songId = songInfoService.searchSongId(title, artist)
            val songInfo = songId?.let { songInfoService.getSongInfo(it) }
            withContext(Dispatchers.Main) {
                callback(songInfo)
            }
        }
    }
}
    