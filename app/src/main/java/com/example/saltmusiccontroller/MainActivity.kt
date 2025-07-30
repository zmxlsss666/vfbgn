package com.example.saltmusiccontroller

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.example.saltmusiccontroller.model.NowPlaying
import com.example.saltmusiccontroller.model.SongInfoResponse
import com.example.saltmusiccontroller.util.Constants

class MainActivity : AppCompatActivity() {
    private lateinit var musicController: MusicController
    private lateinit var tvTitle: TextView
    private lateinit var tvArtist: TextView
    private lateinit var tvAlbum: TextView
    private lateinit var ivCover: ImageView
    private lateinit var tvStatus: TextView
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var btnPrevious: ImageButton
    private lateinit var btnVolumeUp: ImageButton
    private lateinit var btnVolumeDown: ImageButton
    private lateinit var btnMute: ImageButton
    private lateinit var btnScan: ImageButton
    private lateinit var btnCustomIp: ImageButton
    private lateinit var progressBar: ProgressBar
    
    private var updateHandler: Handler? = null
    private var updateRunnable: Runnable? = null
    private val UPDATE_INTERVAL = 3000L // 3秒更新一次

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        setupMusicController()
        setupUpdateHandler()
        setupListeners()
        
        // 尝试连接上次使用的设备
        connectToLastDevice()
    }
    
    private fun initViews() {
        tvTitle = findViewById(R.id.tv_title)
        tvArtist = findViewById(R.id.tv_artist)
        tvAlbum = findViewById(R.id.tv_album)
        ivCover = findViewById(R.id.iv_cover)
        tvStatus = findViewById(R.id.tv_status)
        btnPlayPause = findViewById(R.id.btn_play_pause)
        btnNext = findViewById(R.id.btn_next)
        btnPrevious = findViewById(R.id.btn_previous)
        btnVolumeUp = findViewById(R.id.btn_volume_up)
        btnVolumeDown = findViewById(R.id.btn_volume_down)
        btnMute = findViewById(R.id.btn_mute)
        btnScan = findViewById(R.id.btn_scan)
        btnCustomIp = findViewById(R.id.btn_custom_ip)
        progressBar = findViewById(R.id.progress_bar)
    }
    
    private fun setupMusicController() {
        musicController = MusicController()
    }
    
    private fun setupUpdateHandler() {
        updateHandler = Handler(Looper.getMainLooper())
        updateRunnable = object : Runnable {
            override fun run() {
                updateNowPlaying()
                updateHandler?.postDelayed(this, UPDATE_INTERVAL)
            }
        }
    }
    
    private fun setupListeners() {
        btnPlayPause.setOnClickListener {
            musicController.togglePlayPause { response ->
                if (response == null) {
                    showToast("操作失败，请检查连接")
                } else {
                    updatePlayPauseButton(response.isPlaying ?: false)
                }
            }
        }
        
        btnNext.setOnClickListener {
            musicController.nextTrack { response ->
                if (response == null) {
                    showToast("操作失败，请检查连接")
                } else {
                    showToast(response.message)
                    updateNowPlaying()
                }
            }
        }
        
        btnPrevious.setOnClickListener {
            musicController.previousTrack { response ->
                if (response == null) {
                    showToast("操作失败，请检查连接")
                } else {
                    showToast(response.message)
                    updateNowPlaying()
                }
            }
        }
        
        btnVolumeUp.setOnClickListener {
            musicController.volumeUp { response ->
                response?.message?.let { showToast(it) }
            }
        }
        
        btnVolumeDown.setOnClickListener {
            musicController.volumeDown { response ->
                response?.message?.let { showToast(it) }
            }
        }
        
        btnMute.setOnClickListener {
            musicController.toggleMute { response ->
                response?.message?.let { showToast(it) }
            }
        }
        
        btnScan.setOnClickListener {
            val intent = android.content.Intent(this, DeviceScanActivity::class.java)
            startActivityForResult(intent, 1001)
        }
        
        btnCustomIp.setOnClickListener {
            showCustomIpDialog()
        }
    }
    
    private fun connectToLastDevice() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val lastIp = prefs.getString(Constants.PREF_LAST_IP, null)
        val lastPort = prefs.getInt(Constants.PREF_LAST_PORT, Constants.DEFAULT_PORT)
        
        lastIp?.let {
            musicController.setDevice(it, lastPort)
            tvStatus.text = "已连接到: $it:$lastPort"
            updateNowPlaying()
        } ?: run {
            tvStatus.text = "未连接设备，请扫描或输入IP"
        }
    }
    
    private fun updateNowPlaying() {
        if (musicController.currentIp.isNullOrEmpty()) return
        
        progressBar.visibility = View.VISIBLE
        musicController.getNowPlaying { nowPlaying ->
            progressBar.visibility = View.GONE
            
            if (nowPlaying == null) {
                tvStatus.text = "连接失败，请检查设备"
                return@getNowPlaying
            }
            
            if (nowPlaying.status != "success") {
                tvStatus.text = "获取信息失败"
                return@getNowPlaying
            }
            
            tvTitle.text = nowPlaying.title
            tvArtist.text = nowPlaying.artist
            tvAlbum.text = nowPlaying.album
            updatePlayPauseButton(nowPlaying.isPlaying)
            tvStatus.text = "已连接到: ${musicController.currentIp}:${musicController.currentPort}"
            
            // 获取歌曲封面和歌词
            musicController.getSongDetails(nowPlaying.title, nowPlaying.artist) { songInfo: SongInfoResponse? ->
                songInfo?.let {
                    loadCoverImage(it.pic)
                }
            }
        }
    }
    
    private fun updatePlayPauseButton(isPlaying: Boolean) {
        if (isPlaying) {
            btnPlayPause.setImageResource(R.drawable.ic_pause)
        } else {
            btnPlayPause.setImageResource(R.drawable.ic_play)
        }
    }
    
    private fun loadCoverImage(url: String) {
        Glide.with(this)
            .load(url)
            .placeholder(R.mipmap.ic_launcher)
            .error(R.mipmap.ic_launcher)
            .into(ivCover)
    }
    
    private fun showCustomIpDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_custom_ip)
        
        val etIp = dialog.findViewById<EditText>(R.id.et_ip)
        val etPort = dialog.findViewById<EditText>(R.id.et_port)
        val btnConnect = dialog.findViewById<Button>(R.id.btn_connect)
        
        // 显示当前连接的IP和端口
        musicController.currentIp?.let { etIp.setText(it) }
        etPort.setText(musicController.currentPort.toString())
        
        btnConnect.setOnClickListener {
            val ip = etIp.text.toString().trim()
            val portStr = etPort.text.toString().trim()
            
            if (ip.isEmpty() || portStr.isEmpty()) {
                showToast("请输入IP和端口")
                return@setOnClickListener
            }
            
            val port = try {
                portStr.toInt()
            } catch (e: NumberFormatException) {
                showToast("端口格式错误")
                return@setOnClickListener
            }
            
            // 保存到偏好设置
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            prefs.edit()
                .putString(Constants.PREF_LAST_IP, ip)
                .putInt(Constants.PREF_LAST_PORT, port)
                .apply()
            
            musicController.setDevice(ip, port)
            tvStatus.text = "已连接到: $ip:$port"
            updateNowPlaying()
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            val ip = data?.getStringExtra("ip")
            val port = data?.getIntExtra("port", Constants.DEFAULT_PORT)
            
            if (ip != null && port != null) {
                // 保存到偏好设置
                val prefs = PreferenceManager.getDefaultSharedPreferences(this)
                prefs.edit()
                    .putString(Constants.PREF_LAST_IP, ip)
                    .putInt(Constants.PREF_LAST_PORT, port)
                    .apply()
                
                musicController.setDevice(ip, port)
                tvStatus.text = "已连接到: $ip:$port"
                updateNowPlaying()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateRunnable?.let { updateHandler?.post(it) }
    }
    
    override fun onPause() {
        super.onPause()
        updateRunnable?.let { updateHandler?.removeCallbacks(it) }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        updateHandler?.removeCallbacksAndMessages(null)
        updateHandler = null
        updateRunnable = null
    }
}
    