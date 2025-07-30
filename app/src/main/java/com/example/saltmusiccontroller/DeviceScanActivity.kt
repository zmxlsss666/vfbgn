package com.example.saltmusiccontroller

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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
import kotlinx.coroutines.delay  // 导入协程delay函数
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import com.example.saltmusiccontroller.util.NetworkUtils
import com.example.saltmusiccontroller.util.Constants

class DeviceScanActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var btnRescan: Button
    private val devices = ConcurrentHashMap<String, Int>()
    private lateinit var adapter: ArrayAdapter<String>
    private var isScanning = false

    // 修复：移除不存在的SCAN_WIFI权限，使用标准WiFi权限
    private val REQUIRED_PERMISSIONS = arrayOf(
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
        // 修复：确保布局中存在id为list_devices的ListView
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
                val intent = Intent()
                intent.putExtra("ip", ip)
                intent.putExtra("port", port)
                setResult(RESULT_OK, intent)
                finish()
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
                Toast.makeText(this, "需要所有权限才能扫描设备", Toast.LENGTH_LONG).show()
                finish()
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
                    tvStatus.text = "无法获取本地IP地址，请检查网络连接"
                    updateScanState(false)
                }
                return@launch
            }
            
            val subnet = NetworkUtils.getSubnet(localIp)
            
            // 扫描子网内的IP (1-254)
            for (i in 1..254) {
                if (!isScanning) break
                
                val ip = "$subnet.$i"
                // 跳过本地IP
                if (ip == localIp) continue
                
                launch {
                    checkDevice(ip, Constants.DEFAULT_PORT)
                }
                // 修复：添加协程delay函数的正确引用
                delay(10)  // 稍微延迟避免网络拥塞
            }
            
            // 等待所有扫描扫描任务完成后更新UI
            delay(2000)
            withContext(Dispatchers.Main) {
                if (devices.isEmpty()) {
                    tvStatus.text = "未发现设备，请确保设备在同一网络"
                } else {
                    tvStatus.text = "发现 ${devices.size} 台设备"
                }
                updateScanState(false)
            }
        }
    }

    private suspend fun checkDevice(ip: String, port: Int) {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), 2000)  // 2秒超时
                if (socket.isConnected) {
                    devices[ip] = port
                    withContext(Dispatchers.Main) {
                        adapter.add("$ip:$port")
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        } catch (e: IOException) {
            // 连接失败，忽略
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
    
