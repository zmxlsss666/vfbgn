package com.example.saltmusiccontroller.util

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter

object NetworkUtils {
    fun getLocalIpAddress(context: Context): String {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipAddress = wifiManager.connectionInfo.ipAddress
        return Formatter.formatIpAddress(ipAddress)
    }

    fun getSubnet(ipAddress: String): String {
        return ipAddress.substring(0, ipAddress.lastIndexOf('.'))
    }
}
    