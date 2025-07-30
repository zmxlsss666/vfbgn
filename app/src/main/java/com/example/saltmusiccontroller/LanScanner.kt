package com.example.saltmusiccontroller

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay  // 导入delay函数
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

// 定义接口用于与Activity通信（解决showToast等方法引用问题）
interface ScanCallback {
    fun showToast(message: String)
    fun updateScanStatus(status: String)
    fun addDevice(ip: String, port: Int)
}

class LanScanner(private val callback: ScanCallback) {
    private var isScanning = false

    fun startScan(context: Context, subnet: String, port: Int) {
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
                    delay(10)  // 修复：添加delay导入
                }
                
                // 扫描结束
                delay(2000)
                withContext(Dispatchers.Main) {
                    callback.updateScanStatus("扫描完成")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.showToast("扫描出错: ${e.message}")
                }
            } finally {
                isScanning = false
            }
        }
    }

    private suspend fun checkDevice(ip: String, port: Int) {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), 2000)
                if (socket.isConnected) {
                    withContext(Dispatchers.Main) {
                        callback.addDevice(ip, port)  // 通过接口回调添加设备
                    }
                }
            }
        } catch (e: IOException) {
            // 连接失败忽略
        }
    }

    fun stopScan() {
        isScanning = false
    }
}
    
