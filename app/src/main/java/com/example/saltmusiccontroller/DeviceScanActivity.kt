package com.example.saltmusiccontroller

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.saltmusiccontroller.adapter.DeviceListAdapter
import com.example.saltmusiccontroller.model.Device

class DeviceScanActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DeviceListAdapter
    private lateinit var btnRescan: Button
    private lateinit var progressBar: ProgressBar
    private val devices = mutableListOf<Device>()
    private lateinit var lanScanner: LanScanner
    private val REQUEST_PERMISSIONS = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_scan)
        
        recyclerView = findViewById(R.id.recycler_view)
        btnRescan = findViewById(R.id.btn_rescan)
        progressBar = findViewById(R.id.progress_bar)
        
        setupRecyclerView()
        setupScanner()
        setupListeners()
        
        // 检查并请求必要权限
        checkPermissions()
    }
    
    private fun setupRecyclerView() {
        adapter = DeviceListAdapter(devices) { device ->
            val intent = android.content.Intent()
            intent.putExtra("ip", device.ipAddress)
            intent.putExtra("port", device.port)
            setResult(RESULT_OK, intent)
            finish()
        }
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
    
    private fun setupScanner() {
        lanScanner = LanScanner(this) {
            runOnUiThread {
                if (it.isEmpty()) {
                    showToast("未发现设备，请确保设备在同一网络")
                }
            }
        }
    }
    
    private fun setupListeners() {
        btnRescan.setOnClickListener {
            checkPermissions { startScan() }
        }
    }
    
    // 权限检查
    private fun checkPermissions(onGranted: () -> Unit = {}) {
        val requiredPermissions = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SCAN_WIFI) 
            != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.SCAN_WIFI)
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        if (requiredPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                requiredPermissions.toTypedArray(),
                REQUEST_PERMISSIONS
            )
        } else {
            onGranted()
        }
    }
    
    private fun startScan() {
        devices.clear()
        adapter.notifyDataSetChanged()
        lanScanner.startScan()
    }
    
    fun addDevice(device: Device) {
        runOnUiThread {
            val index = devices.indexOfFirst { it.ipAddress == device.ipAddress }
            if (index == -1) {
                devices.add(device)
                adapter.notifyItemInserted(devices.size - 1)
            }
        }
    }
    
    fun updateScanStatus(isScanning: Boolean) {
        runOnUiThread {
            progressBar.visibility = if (isScanning) View.VISIBLE else View.GONE
            btnRescan.isEnabled = !isScanning
            btnRescan.text = if (isScanning) "扫描中..." else "重新扫描"
        }
    }
    
    fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                startScan()
            } else {
                showToast("需要权限才能扫描设备")
            }
        }
    }
    
    override fun onBackPressed() {
        lanScanner.stopScan()
        super.onBackPressed()
    }
    
    override fun onDestroy() {
        lanScanner.stopScan()
        super.onDestroy()
    }
}
    