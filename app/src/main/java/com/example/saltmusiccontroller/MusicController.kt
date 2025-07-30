package com.example.saltmusiccontroller

import android.util.Log
import com.example.saltmusiccontroller.model.NowPlaying
import com.example.saltmusiccontroller.model.ApiResponse
import com.example.saltmusiccontroller.util.Constants
import com.google.gson.Gson  // 导入Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit

class MusicController {
    private val TAG = "MusicController"
    var currentIp: String? = null
    var currentPort: Int = Constants.DEFAULT_PORT
    // 直接创建Gson实例，避免依赖Constants
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun setDevice(ip: String, port: Int) {
        currentIp = ip
        currentPort = port
        Log.d(TAG, "已设置设备: $ip:$port")
    }

    suspend fun verifyConnection(): Boolean {
        if (currentIp.isNullOrEmpty()) return false
        
        return withContext(Dispatchers.IO) {
            try {
                val url = "http://$currentIp:$currentPort/api/now-playing"
                val request = Request.Builder().url(url).build()
                
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        val isValid = !responseBody.isNullOrEmpty() && 
                                      responseBody.contains("status") && 
                                      responseBody.contains("title")
                        Log.d(TAG, "连接验证: ${if (isValid) "成功" else "失败"}")
                        isValid
                    } else {
                        false
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "连接验证失败: ${e.message}")
                false
            }
        }
    }

    suspend fun getNowPlaying(callback: (NowPlaying?) -> Unit) {
        if (currentIp.isNullOrEmpty()) {
            callback(null)
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val url = "http://$currentIp:$currentPort/api/now-playing"
                val request = Request.Builder().url(url).build()
                
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e(TAG, "获取播放信息失败: ${e.message}")
                        callback(null)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            val json = response.body?.string()
                            Log.d(TAG, "播放信息响应: $json")
                            try {
                                // 使用本地创建的gson实例
                                val nowPlaying = gson.fromJson(json, NowPlaying::class.java)
                                callback(nowPlaying)
                            } catch (e: Exception) {
                                Log.e(TAG, "解析播放信息失败: ${e.message}")
                                callback(null)
                            }
                        } else {
                            Log.e(TAG, "获取播放信息失败，状态码: ${response.code}")
                            callback(null)
                        }
                        response.close()
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "获取播放信息异常: ${e.message}")
                callback(null)
            }
        }
    }

    suspend fun togglePlayPause(callback: (ApiResponse?) -> Unit) {
        sendCommand("/api/play-pause", callback)
    }

    suspend fun nextTrack(callback: (ApiResponse?) -> Unit) {
        sendCommand("/api/next-track", callback)
    }

    suspend fun previousTrack(callback: (ApiResponse?) -> Unit) {
        sendCommand("/api/previous-track", callback)
    }

    suspend fun volumeUp(callback: (ApiResponse?) -> Unit) {
        sendCommand("/api/volume/up", callback)
    }

    suspend fun volumeDown(callback: (ApiResponse?) -> Unit) {
        sendCommand("/api/volume/down", callback)
    }

    suspend fun toggleMute(callback: (ApiResponse?) -> Unit) {
        sendCommand("/api/mute", callback)
    }

    private suspend fun sendCommand(endpoint: String, callback: (ApiResponse?) -> Unit) {
        if (currentIp.isNullOrEmpty()) {
            callback(null)
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val url = "http://$currentIp:$currentPort$endpoint"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Connection", "close")
                    .build()
                
                Log.d(TAG, "发送命令: $url")
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e(TAG, "命令发送失败 ($endpoint): ${e.message}")
                        callback(null)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            val json = response.body?.string()
                            Log.d(TAG, "命令响应 ($endpoint): $json")
                            try {
                                // 使用本地创建的gson实例
                                val apiResponse = gson.fromJson(json, ApiResponse::class.java)
                                callback(apiResponse)
                            } catch (e: Exception) {
                                Log.e(TAG, "解析响应失败: ${e.message}")
                                callback(null)
                            }
                        } else {
                            Log.e(TAG, "命令响应错误 ($endpoint): ${response.code}")
                            callback(null)
                        }
                        response.close()
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "命令处理异常 ($endpoint): ${e.message}")
                callback(null)
            }
        }
    }
}
    
