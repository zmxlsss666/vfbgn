package com.example.saltmusiccontroller

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_scan)
        
        recyclerView = findViewById(R.id.recycler_view)
        btnRescan = findViewById(R.id.btn_rescan)
        progressBar = findViewById(R.id.progress_bar)
        
        setupRecyclerView()
        setupScanner()
        setupListeners()
        
        startScan()
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
            if (it.isEmpty()) {
                showToast("未发现设备")
            }
        }
    }
    
    private fun setupListeners() {
        btnRescan.setOnClickListener {
            startScan()
        }
    }
    
    private fun startScan() {
        devices.clear()
        adapter.notifyDataSetChanged()
        lanScanner.startScan()
    }
    
    fun addDevice(device: Device) {
        runOnUiThread {
            devices.add(device)
            adapter.notifyItemInserted(devices.size - 1)
        }
    }
    
    fun updateScanStatus(isScanning: Boolean) {
        runOnUiThread {
            progressBar.visibility = if (isScanning) View.VISIBLE else View.GONE
            btnRescan.isEnabled = !isScanning
            btnRescan.text = if (isScanning) "扫描中..." else "重新扫描"
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
    