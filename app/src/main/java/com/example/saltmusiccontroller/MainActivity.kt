package com.example.saltmusiccontroller

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.saltmusiccontroller.model.NowPlaying
import com.example.saltmusiccontroller.util.Constants

class MainActivity : AppCompatActivity() {
    private lateinit var musicController: MusicController
    private lateinit var tvTitle: TextView
    private lateinit var tvArtist: TextView
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        musicController = MusicController()
        initViews()
        
        // 获取Intent中的IP和端口
        val ip = intent.getStringExtra("ip")
        val port = intent.getIntExtra("port", Constants.DEFAULT_PORT)
        
        if (ip != null) {
            musicController.setDevice(ip, port)
            tvStatus.text = "已连接到: $ip:$port"
            // 加载播放信息（在协程中调用suspend函数）
            lifecycleScope.launch {
                loadNowPlaying()
            }
        } else {
            tvStatus.text = "未连接设备"
        }
        
        setupListeners()
    }

    private fun initViews() {
        tvTitle = findViewById(R.id.tv_title)
        tvArtist = findViewById(R.id.tv_artist)
        tvStatus = findViewById(R.id.tv_status)
    }

    private fun setupListeners() {
        // 播放/暂停按钮
        findViewById<Button>(R.id.btn_play_pause).setOnClickListener {
            // 确保在协程中调用suspend函数
            lifecycleScope.launch {
                musicController.togglePlayPause { response ->
                    runOnUiThread {
                        if (response != null) {
                            Toast.makeText(this@MainActivity, response.message, Toast.LENGTH_SHORT).show()
                            // 刷新播放信息（在协程中）
                            lifecycleScope.launch { loadNowPlaying() }
                        } else {
                            Toast.makeText(this@MainActivity, "操作失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        
        // 下一曲按钮
        findViewById<Button>(R.id.btn_next).setOnClickListener {
            lifecycleScope.launch {
                musicController.nextTrack { response ->
                    runOnUiThread {
                        if (response != null) {
                            Toast.makeText(this@MainActivity, response.message, Toast.LENGTH_SHORT).show()
                            lifecycleScope.launch { loadNowPlaying() }
                        } else {
                            Toast.makeText(this@MainActivity, "操作失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        
        // 上一曲按钮
        findViewById<Button>(R.id.btn_previous).setOnClickListener {
            lifecycleScope.launch {
                musicController.previousTrack { response ->
                    runOnUiThread {
                        if (response != null) {
                            Toast.makeText(this@MainActivity, response.message, Toast.LENGTH_SHORT).show()
                            lifecycleScope.launch { loadNowPlaying() }
                        } else {
                            Toast.makeText(this@MainActivity, "操作失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        
        // 音量增加按钮
        findViewById<Button>(R.id.btn_volume_up).setOnClickListener {
            lifecycleScope.launch {
                musicController.volumeUp { response ->
                    runOnUiThread {
                        response?.message?.let { msg ->
                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                        } ?: run {
                            Toast.makeText(this@MainActivity, "操作失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        
        // 音量减少按钮
        findViewById<Button>(R.id.btn_volume_down).setOnClickListener {
            lifecycleScope.launch {
                musicController.volumeDown { response ->
                    runOnUiThread {
                        response?.message?.let { msg ->
                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                        } ?: run {
                            Toast.makeText(this@MainActivity, "操作失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        
        // 静音按钮
        findViewById<Button>(R.id.btn_mute).setOnClickListener {
            lifecycleScope.launch {
                musicController.toggleMute { response ->
                    runOnUiThread {
                        response?.message?.let { msg ->
                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                        } ?: run {
                            Toast.makeText(this@MainActivity, "操作失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
    
    // 加载当前播放信息（suspend函数）
    private suspend fun loadNowPlaying() {
        musicController.getNowPlaying { nowPlaying ->
            runOnUiThread {
                if (nowPlaying != null) {
                    tvTitle.text = nowPlaying.title
                    tvArtist.text = nowPlaying.artist
                    tvStatus.text = if (nowPlaying.isPlaying) "正在播放" else "已暂停"
                } else {
                    tvStatus.text = "获取播放信息失败"
                }
            }
        }
    }
}
    
