package com.example.saltmusiccontroller

import android.util.Log
import com.example.saltmusiccontroller.model.NowPlaying
import com.example.saltmusiccontroller.model.ApiResponse
import com.example.saltmusiccontrollercontroller.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit

class MusicController {
    private val TAG = "MusicController"
    var currentIp: String? = null
    var currentPort: Int = Constants.DEFAULT_PORT // 默认端口35373
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * 设置目标设备的IP和端口
     */
    fun setDevice(ip: String, port: Int) {
        currentIp = ip
        currentPort = port
        Log.d(TAG, "已设置设备: $ip:$port")
    }

    /**
     * 通过访问/api/now-playing验证连接有效性
     */
    suspend fun verifyConnection(): Boolean {
        if (currentIp.isNullOrEmpty()) return false
        
        return withContext(Dispatchers.IO) {
            try {
                val url = "http://$currentIp:$currentPort/api/now-playing"
                val request = Request.Builder().url(url).build()
                
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        // 验证响应是否包含成功状态和必要字段
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

    /**
     * 1. 获取当前播放信息
     * API: GET /api/now-playing
     */
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
                                val nowPlaying = Constants.gson.fromJson(json, NowPlaying::class.java)
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

    /**
     * 2. 播放/暂停切换
     * API: GET /api/play-pause
     */
    suspend fun togglePlayPause(callback: (ApiResponse?) -> Unit) {
        sendCommand("/api/play-pause", callback)
    }

    /**
     * 3. 下一曲
     * API: GET /api/next-track
     */
    suspend fun nextTrack(callback: (ApiResponse?) -> Unit) {
        sendCommand("/api/next-track", callback)
    }

    /**
     * 4. 上一曲
     * API: GET /api/previous-track
     */
    suspend fun previousTrack(callback: (ApiResponse?) -> Unit) {
        sendCommand("/api/previous-track", callback)
    }

    /**
     * 5. 音量增加
     * API: GET /api/volume/up
     */
    suspend fun volumeUp(callback: (ApiResponse?) -> Unit) {
        sendCommand("/api/volume/up", callback)
    }

    /**
     * 6. 音量减少
     * API: GET /api/volume/down
     */
    suspend fun volumeDown(callback: (ApiResponse?) -> Unit) {
        sendCommand("/api/volume/down", callback)
    }

    /**
     * 7. 静音切换
     * API: GET /api/mute
     */
    suspend fun toggleMute(callback: (ApiResponse?) -> Unit) {
        sendCommand("/api/mute", callback)
    }

    /**
     * 发送命令的通用方法
     * @param endpoint API端点路径（与提供的文档完全一致）
     */
    private suspend fun sendCommand(endpoint: String, callback: (ApiResponse?) -> Unit) {
        if (currentIp.isNullOrEmpty()) {
            callback(null)
            return
        }

        withContext(Dispatchers.IO) {
            try {
                // 使用提供的API路径，不做任何额外修改
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
                                val apiResponse = Constants.gson.fromJson(json, ApiResponse::class.java)
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
    
