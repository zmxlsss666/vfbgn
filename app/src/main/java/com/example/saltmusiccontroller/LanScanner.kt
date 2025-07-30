package com.example.saltmusiccontroller

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.example.saltmusiccontroller.model.Device
import com.example.saltmusiccontroller.util.Constants
import com.example.saltmusiccontroller.util.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

class LanScanner(
    private val context: DeviceScanActivity,
    private val onScanComplete: (List<Device>) -> Unit
) {
    private val devices = ConcurrentHashMap<String, Device>()
    private var isScanning = false
    private val scope = CoroutineScope(Dispatchers.IO)

    fun startScan() {
        if (isScanning) return
        
        // 检查网络连接
        if (!isNetworkAvailable()) {
            context.showToast("网络不可用，请检查连接")
            return
        }
        
        isScanning = true
        devices.clear()
        context.updateScanStatus(true)
        
        val localIp = NetworkUtils.getLocalIpAddress(context)
        if (localIp.isEmpty() || localIp == "0.0.0.0") {
            context.showToast("无法获取本地IP地址")
            stopScan()
            return
        }
        
        val subnet = NetworkUtils.getSubnet(localIp)
        
        // 并发扫描IP段
        for (i in 1..254) {
            val ip = "$subnet.$i"
            if (ip == localIp) continue
            
            scope.launch {
                scanIpAddress(ip)
            }
        }
        
        // 启动超时任务
        scope.launch {
            kotlinx.coroutines.delay(Constants.SCAN_TIMEOUT.toLong())
            withContext(Dispatchers.Main) {
                stopScan()
                onScanComplete(devices.values.toList())
            }
        }
    }

    private suspend fun scanIpAddress(ip: String) {
        if (!isScanning) return
        
        try {
            Socket().use { socket ->
                // 增加超时时间到2秒，提高检测成功率
                socket.connect(InetSocketAddress(ip, Constants.SCAN_PORT), 2000)
                val device = Device(ip, Constants.SCAN_PORT)
                devices[ip] = device
                
                withContext(Dispatchers.Main) {
                    context.addDevice(device)
                }
            }
        } catch (e: IOException) {
            // 连接失败，忽略此IP
        }
    }
    
    // 检查网络是否可用
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                   capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            return networkInfo.isConnected
        }
    }

    fun stopScan() {
        isScanning = false
        context.updateScanStatus(false)
    }
}
    