package com.example.saltmusiccontroller

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

interface ScanCallback {
    fun showToast(message: String)
    fun updateScanStatus(status: String)
    fun addDevice(ip: String, port: Int)
}

class LanScanner(private val callback: ScanCallback) {
    private var isScanning = false
    private val TAG = "LanScanner"

    fun startScan(subnet: String, port: Int) {
        if (isScanning) return
        isScanning = true
        
        callback.updateScanStatus("开始扫描...")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 扫描子网内的IP
                for (i in 1..254) {
                    if (!isScanning) break
                    
                    val ip = "$subnet.$i"
                    launch {
                        checkDevice(ip, port)
                    }
                    delay(10) // 降低扫描速度，避免网络拥堵
                }
                
                // 等待所有扫描任务完成
                delay(2000)
                withContext(Dispatchers.Main) {
                    callback.updateScanStatus("扫描完成")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.showToast("扫描出错: ${e.message ?: "未知错误"}")
                    callback.updateScanStatus("扫描出错")
                }
            } finally {
                isScanning = false
            }
        }
    }

    private suspend fun checkDevice(ip: String, port: Int) {
        if (!isScanning) return
        
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), 2000) // 2秒超时
                if (socket.isConnected) {
                    withContext(Dispatchers.Main) {
                        callback.addDevice(ip, port)
                        callback.showToast("发现设备: $ip:$port")
                    }
                }
            }
        } catch (e: IOException) {
            // 连接失败，忽略
            Log.d(TAG, "设备不可达: $ip:$port")
        }
    }

    fun stopScan() {
        isScanning = false
    }
}
