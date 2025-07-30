package com.example.saltmusiccontroller.util

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Enumeration

object NetworkUtils {
    // 获取本地IP地址，兼容多种网络类型
    fun getLocalIpAddress(context: Context): String {
        // 尝试通过WiFi管理器获取
        try {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ipAddress = wifiManager.connectionInfo.ipAddress
            if (ipAddress != 0) {
                return Formatter.formatIpAddress(ipAddress)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 尝试通过网络接口获取
        try {
            val interfaces: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf: NetworkInterface = interfaces.nextElement()
                val addresses: Enumeration<InetAddress> = intf.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr: InetAddress = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr.hostAddress.indexOf(':') < 0) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return ""
    }

    fun getSubnet(ipAddress: String): String {
        return ipAddress.substring(0, ipAddress.lastIndexOf('.'))
    }
    
    // 检查IP地址是否有效
    fun isValidIpAddress(ip: String): Boolean {
        return try {
            val parts = ip.split(".")
            if (parts.size != 4) return false
            
            parts.forEach { part ->
                val num = part.toInt()
                if (num < 0 || num > 255) return false
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
    