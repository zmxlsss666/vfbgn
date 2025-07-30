package com.example.saltmusiccontroller

import android.os.AsyncTask
import com.example.saltmusiccontroller.model.Device
import com.example.saltmusiccontroller.util.Constants
import com.example.saltmusiccontroller.util.NetworkUtils
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

class LanScanner(
    private val context: DeviceScanActivity,
    private val onScanComplete: (List<Device>) -> Unit
) {
    private val devices = mutableListOf<Device>()
    private var isScanning = false

    fun startScan() {
        if (isScanning) return
        
        isScanning = true
        devices.clear()
        context.updateScanStatus(true)
        
        val localIp = NetworkUtils.getLocalIpAddress(context)
        val subnet = NetworkUtils.getSubnet(localIp)
        
        for (i in 1..254) {
            val ip = "$subnet.$i"
            if (ip == localIp) continue
            
            ScanTask().execute(ip)
        }
        
        // 启动超时任务
        android.os.Handler().postDelayed({
            isScanning = false
            onScanComplete(devices.distinctBy { it.ipAddress })
            context.updateScanStatus(false)
        }, Constants.SCAN_TIMEOUT.toLong())
    }

    private inner class ScanTask : AsyncTask<String, Void, Device?>() {
        override fun doInBackground(vararg params: String): Device? {
            val ip = params[0]
            return try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, Constants.SCAN_PORT), 1000)
                    Device(ip, Constants.SCAN_PORT)
                }
            } catch (e: IOException) {
                null
            }
        }

        override fun onPostExecute(result: Device?) {
            result?.let {
                devices.add(it)
                context.addDevice(it)
            }
        }
    }
    
    fun stopScan() {
        isScanning = false
    }
}
    