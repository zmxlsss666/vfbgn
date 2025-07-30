package com.example.saltmusiccontroller

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import com.example.saltmusiccontroller.util.NetworkUtils
import com.example.saltmusiccontroller.util.Constants
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class DeviceScanActivity : AppCompatActivity() {
    private val TAG = "DeviceScanActivity"
    private lateinit var listView: ListView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var btnRescan: Button
    private val devices = ConcurrentHashMap<String, Int>()
    private lateinit var adapter: ArrayAdapter<String>
    private var isScanning = false
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    // 必要权限列表
    private val REQUIRED_PERMISSIONS = arrayOf(
        android.Manifest.permission.INTERNET,
        android.Manifest.permission.ACCESS_WIFI_STATE,
        android.Manifest.permission.CHANGE_WIFI_STATE,
        android.Manifest.permission.ACCESS_NETWORK_STATE,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )
    private val PERMISSION_REQUEST_CODE = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_scan)
        
        initViews()
        setupAdapter()
        checkPermissionsAndScan()
    }

    private fun initViews() {
        listView = findViewById(R.id.list_devices)
        progressBar = findViewById(R.id.progress_bar)
        tvStatus = findViewById(R.id.tv_status)
        btnRescan = findViewById(R.id.btn_rescan)
        
        btnRescan.setOnClickListener {
            checkPermissionsAndScan()
        }
        
        listView.setOnItemClickListener { _, _, position, _ ->
            val item = adapter.getItem(position) ?: return@setOnItemClickListener
            val parts = item.split(":")
            if (parts.size >= 2) {
                val ip = parts[0]
                val port = try {
                    parts[1].toInt()
                } catch (e: NumberFormatException) {
                    Constants.DEFAULT_PORT
                }
                // 显示连接中状态
                tvStatus.text = "正在连接 $ip:$port..."
                // 基于now-playing API验证连接
                verifyApiConnection(ip, port) { isSuccessful ->
                    if (isSuccessful) {
                        tvStatus.text = "连接成功"
                        val intent = Intent()
                        intent.putExtra("ip", ip)
                        intent.putExtra("port", port)
                        setResult(RESULT_OK, intent)
                        finish()
                    } else {
                        tvStatus.text = "连接失败：无法访问now-playing API"
                        Toast.makeText(this, "连接失败，请请确保设备API正常", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /**
     * 验证是否能成功访问设备的now-playing API
     * 只有API返回有效响应才算连接成功
     */
    private fun verifyApiConnection(ip: String, port: Int, callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            var isSuccessful = false
            try {
                // 构建now-playing API地址（根据实际设备API路径调整）
                val url = "http://$ip:$port/now-playing"
                Log.d(TAG, "验证API连接: $url")
                
                val request = Request.Builder()
                    .url(url)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        // 验证响应不为空且包含基本播放信息字段
                        if (!responseBody.isNullOrEmpty() && 
                            (responseBody.contains("title") || 
                             responseBody.contains("artist") || 
                             responseBody.contains("status"))) {
                            isSuccessful = true
                            Log.d(TAG, "API响应有效: $responseBody")
                        } else {
                            Log.e(TAG, "API响应无效: $responseBody")
                        }
                    } else {
                        Log.e(TAG, "API请求失败，状态码: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "API请求异常: ${e.message}")
            }
            withContext(Dispatchers.Main) {
                callback(isSuccessful)
            }
        }
    }

    private fun setupAdapter() {
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listView.adapter = adapter
    }

    private fun checkPermissionsAndScan() {
        if (hasAllPermissions()) {
            startScan()
        } else {
            requestPermissions()
        }
    }

    private fun hasAllPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            REQUIRED_PERMISSIONS,
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startScan()
            } else {
                Toast.makeText(this, "需要所有权限才能扫描和连接设备", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startScan() {
        if (isScanning) return
        
        isScanning = true
        devices.clear()
        adapter.clear()
        progressBar.visibility = View.VISIBLE
        tvStatus.text = "正在扫描设备..."
        btnRescan.isEnabled = false
        
        CoroutineScope(Dispatchers.IO).launch {
            val localIp = NetworkUtils.getLocalIpAddress(this@DeviceScanActivity)
            if (localIp.isEmpty()) {
                withContext(Dispatchers.Main) {
                    tvStatus.text = "无法获取本地IP，请检查WiFi"
                    updateScanState(false)
                }
                return@launch
            }
            
            val subnet = NetworkUtils.getSubnet(localIp)
            Log.d(TAG, "开始扫描子网: $subnet.* (本地IP: $localIp)")
            
            // 扫描子网内的IP (1-254)
            for (i in 1..254) {
                if (!isScanning) break
                
                val ip = "$subnet.$i"
                if (ip == localIp) continue  // 跳过自身
                
                launch {
                    // 扫描阶段仅检查端口开放，连接阶段再验证API
                    checkPortOpen(ip, Constants.DEFAULT_PORT)
                }
                delay(10)  // 控制扫描速度
            }
            
            // 等待扫描完成
            delay(3000)
            withContext(Dispatchers.Main) {
                if (devices.isEmpty()) {
                    tvStatus.text = "未发现设备，请确保设备开启并在同一网络"
                } else {
                    tvStatus.text = "发现 ${devices.size} 台设备（点击连接）"
                }
                updateScanState(false)
            }
        }
    }

    // 扫描阶段仅检查端口是否开放
    private suspend fun checkPortOpen(ip: String, port: Int) {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), 1000)  // 快速端口检查
                if (socket.isConnected) {
                    devices[ip] = port
                    withContext(Dispatchers.Main) {
                        adapter.add("$ip:$port")
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        } catch (e: Exception) {
            // 端口未开放，忽略
        }
    }

    private fun updateScanState(isScanning: Boolean) {
        this.isScanning = isScanning
        progressBar.visibility = if (isScanning) View.VISIBLE else View.GONE
        btnRescan.isEnabled = !isScanning
    }

    override fun onBackPressed() {
        isScanning = false
        super.onBackPressed()
    }
}
    
