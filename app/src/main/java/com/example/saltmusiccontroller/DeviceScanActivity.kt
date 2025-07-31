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
import androidx.activity.OnBackPressedCallback
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

class DeviceScanActivity : AppCompatActivity(), ScanCallback {
    private val TAG = "DeviceScanActivity"
    private lateinit var listView: ListView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var btnRescan: Button
    private val devices = ConcurrentHashMap<String, Int>()
    private lateinit var adapter: ArrayAdapter<String>
    private var isScanning = false
    private var lanScanner: LanScanner? = null
    private val REQUEST_LOCATION_PERMISSION = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_scan)
        
        // 修复返回键回调实现
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                stopScan()
                finish()
            }
        })
        
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
            if (!isScanning) {
                startScan()
            }
        }
    }

    private fun setupAdapter() {
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            val item = adapter.getItem(position) ?: return@setOnItemClickListener
            val ip = item.split(":")[0]
            val port = item.split(":")[1].toIntOrNull() ?: Constants.DEFAULT_PORT
            
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("ip", ip)
            intent.putExtra("port", port)
            startActivity(intent)
        }
    }

    private fun checkPermissionsAndScan() {
        if (ContextCompat.checkSelfPermission(this, 
                android.Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            startScan()
        }
    }

    private fun startScan() {
        devices.clear()
        adapter.clear()
        tvStatus.text = "准备扫描..."
        progressBar.visibility = View.VISIBLE
        
        val ipAddress = NetworkUtils.getLocalIpAddress(this)
        if (ipAddress.isEmpty()) {
            tvStatus.text = "无法获取本地IP"
            progressBar.visibility = View.GONE
            return
        }
        
        val subnet = NetworkUtils.getSubnet(ipAddress)
        tvStatus.text = "正在扫描 $subnet.1-254 ..."
        
        lanScanner = LanScanner(this)
        lanScanner?.startScan(subnet, Constants.DEFAULT_PORT)
    }

    private fun stopScan() {
        isScanning = false
        lanScanner?.stopScan()
        progressBar.visibility = View.GONE
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScan()
            } else {
                Toast.makeText(this, "需要位置权限才能扫描设备", Toast.LENGTH_SHORT).show()
                tvStatus.text = "请授予位置权限"
            }
        }
    }

    // ScanCallback实现
    override fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun updateScanStatus(status: String) {
        runOnUiThread {
            tvStatus.text = status
            if (status == "扫描完成") {
                progressBar.visibility = View.GONE
            }
        }
    }

    override fun addDevice(ip: String, port: Int) {
        val deviceInfo = "$ip:$port"
        if (!devices.contains(deviceInfo)) {
            devices[deviceInfo] = port
            runOnUiThread {
                adapter.add(deviceInfo)
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScan()
    }
}
