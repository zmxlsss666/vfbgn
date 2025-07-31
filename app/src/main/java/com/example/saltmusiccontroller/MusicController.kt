package com.example.saltmusiccontroller

import android.util.Log
import com.example.saltmusiccontroller.model.ApiResponse
import com.example.saltmusiccontroller.model.NowPlaying
import com.example.saltmusiccontroller.util.Constants
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit

class MusicController {
    private val TAG = "MusicController"
    private val gson = Gson()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)  // 连接超时
        .readTimeout(5, TimeUnit.SECONDS)     // 读取超时
        .build()

    // 设备信息
    var currentIp: String? = null
    var currentPort: Int = Constants.DEFAULT_PORT  // 默认端口35373

    // 设置设备IP和端口
    fun setDevice(ip: String, port: Int) {
        currentIp = ip
        currentPort = port
        Log.d(TAG, "已设置设备：$ip:$port")
    }

    // 验证设备连接
    suspend fun verifyConnection(): Boolean {
        val ip = currentIp ?: return false
        return withContext(Dispatchers.IO) {
            try {
                val url = "http://$ip:$currentPort/api/now-playing"
                val request = Request.Builder().url(url).build()
                
                httpClient.newCall(request).execute().use { response ->
                    val isSuccess = response.isSuccessful 
                    Log.d(TAG, "设备验证：${if (isSuccess) "成功" else "失败"}")
                    isSuccess
                }
            } catch (e: Exception) {
                Log.e(TAG, "验证连接失败", e)
                false
            }
        }
    }

    // 获取当前播放信息
    suspend fun getNowPlaying(callback: (NowPlaying?) -> Unit) {
        val ip = currentIp ?: run {
            callback(null)
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val url = "http://$ip:$currentPort/api/now-playing"
                val request = Request.Builder().url(url).build()

                httpClient.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e(TAG, "获取播放信息失败", e)
                        callback(null)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            val json = response.body?.string()
                            try {
                                val result = gson.fromJson(json, NowPlaying::class.java)
                                callback(result)
                            } catch (e: Exception) {
                                Log.e(TAG, "解析播放信息失败", e)
                                callback(null)
                            }
                        } else {
                            Log.e(TAG, "播放信息请求失败，状态码：${response.code}")
                            callback(null)
                        }
                        response.close()
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "获取播放信息异常", e)
                callback(null)
            }
        }
    }

    // 播放/暂停切换
    suspend fun togglePlayPause(callback: (ApiResponse?) -> Unit) {
        sendCommand("/api/play-pause", callback)
    }

    // 下一曲
    suspend fun nextTrack(callback: (ApiResponse?) -> Unit) {
        sendCommand("/api/next-track", callback)
    }

    // 上一曲
    suspend fun previousTrack(callback: (ApiResponse?) -> Unit) {
        sendCommand("/api/previous-track", callback)
    }

    // 音量增加
    suspend fun volumeUp(callback: (ApiResponse?) -> Unit) {
        sendCommand("/api/volume/up", callback)
    }

    // 音量减少
    suspend fun volumeDown(callback: (ApiResponse?) -> Unit) {
        sendCommand("/api/volume/down", callback)
    }

    // 静音切换
    suspend fun toggleMute(callback: (ApiResponse?) -> Unit) {
        sendCommand("/api/mute", callback)
    }

    // 发送命令通用方法
    private suspend fun sendCommand(endpoint: String, callback: (ApiResponse?) -> Unit) {
        val ip = currentIp ?: run {
            callback(null)
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val url = "http://$ip:$currentPort$endpoint"
                val request = Request.Builder().url(url).build()

                httpClient.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e(TAG, "命令发送失败：$endpoint", e)
                        callback(null)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            val json = response.body?.string()
                            try {
                                val result = gson.fromJson(json, ApiResponse::class.java)
                                callback(result)
                            } catch (e: Exception) {
                                Log.e(TAG, "解析命令响应失败", e)
                                callback(null)
                            }
                        } else {
                            Log.e(TAG, "命令请求失败，状态码：${response.code}")
                            callback(null)
                        }
                        response.close()
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "命令处理异常", e)
                callback(null)
            }
        }
    }
}
