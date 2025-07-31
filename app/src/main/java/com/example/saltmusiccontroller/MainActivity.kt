package com.example.saltmusiccontroller

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.example.saltmusiccontroller.model.NowPlaying
import com.example.saltmusiccontroller.util.Constants

class MainActivity : AppCompatActivity() {
    // 日志标签
    private val TAG = "MusicMain"
    
    // 控制器与视图变量
    private var musicController: MusicController? = null
    private lateinit var tvTitle: TextView
    private lateinit var tvArtist: TextView
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 全局异常捕获，防止闪退无日志
        setupGlobalExceptionHandler()
        
        try {
            // 初始化布局与视图
            setContentView(R.layout.activity_main)
            initViews()
            
            // 初始化控制器与设备连接
            initMusicController()
            connectToDeviceFromIntent()
            
            // 设置按钮监听
            setupButtonListeners()
        } catch (e: Exception) {
            Log.e(TAG, "初始化失败", e)
            showError("应用初始化失败，请重启")
        }
    }

    // 初始化视图组件
    private fun initViews() {
        tvTitle = findViewById(R.id.tv_title)
        tvArtist = findViewById(R.id.tv_artist)
        tvStatus = findViewById(R.id.tv_status)
    }

    // 初始化音乐控制器
    private fun initMusicController() {
        musicController = MusicController().apply {
            // 初始化默认配置
            currentPort = Constants.DEFAULT_PORT
        }
    }

    // 从Intent获取设备信息并连接
    private fun connectToDeviceFromIntent() {
        val deviceIp = intent.getStringExtra("ip")
        val devicePort = intent.getIntExtra("port", Constants.DEFAULT_PORT)

        // 校验IP有效性
        if (deviceIp.isNullOrEmpty() || !isValidIp(deviceIp)) {
            tvStatus.text = "未获取到有效设备，请重新扫描"
            return
        }

        // 连接设备
        musicController?.setDevice(deviceIp, devicePort)
        tvStatus.text = "正在连接 $deviceIp:$devicePort..."
        
        // 验证连接并加载播放信息
        lifecycleScope.launch {
            try {
                val isConnected = musicController?.verifyConnection() ?: false
                if (isConnected) {
                    loadCurrentPlayingInfo()
                } else {
                    tvStatus.text = "连接失败：设备无响应"
                }
            } catch (e: Exception) {
                Log.e(TAG, "连接过程出错", e)
                tvStatus.text = "连接异常：${e.message ?: "未知错误"}"
            }
        }
    }

    // 设置按钮点击事件
    private fun setupButtonListeners() {
        // 播放/暂停按钮
        findViewById<Button>(R.id.btn_play_pause).setOnClickListener {
            handlePlayPause()
        }
        
        // 下一曲按钮
        findViewById<Button>(R.id.btn_next).setOnClickListener {
            handleNextTrack()
        }
        
        // 上一曲按钮
        findViewById<Button>(R.id.btn_previous).setOnClickListener {
            handlePreviousTrack()
        }
        
        // 音量控制按钮
        findViewById<Button>(R.id.btn_volume_up).setOnClickListener {
            handleVolumeUp()
        }
        findViewById<Button>(R.id.btn_volume_down).setOnClickListener {
            handleVolumeDown()
        }
        
        // 静音按钮
        findViewById<Button>(R.id.btn_mute).setOnClickListener {
            handleMuteToggle()
        }
    }

    // 播放/暂停处理
    private fun handlePlayPause() {
        executeWithDeviceCheck { controller ->
            lifecycleScope.launch {
                controller.togglePlayPause { response ->
                    runOnUiThread {
                        response?.let { showToast(it.message) } 
                            ?: showToast("操作失败")
                        loadCurrentPlayingInfo()
                    }
                }
            }
        }
    }

    // 下一曲处理
    private fun handleNextTrack() {
        executeWithDeviceCheck { controller ->
            lifecycleScope.launch {
                controller.nextTrack { response ->
                    runOnUiThread {
                        response?.let { showToast(it.message) } 
                            ?: showToast("操作失败")
                        loadCurrentPlayingInfo()
                    }
                }
            }
        }
    }

    // 上一曲处理
    private fun handlePreviousTrack() {
        executeWithDeviceCheck { controller ->
            lifecycleScope.launch {
                controller.previousTrack { response ->
                    runOnUiThread {
                        response?.let { showToast(it.message) } 
                            ?: showToast("操作失败")
                        loadCurrentPlayingInfo()
                    }
                }
            }
        }
    }

    // 音量增加处理
    private fun handleVolumeUp() {
        executeWithDeviceCheck { controller ->
            lifecycleScope.launch {
                controller.volumeUp { response ->
                    runOnUiThread {
                        response?.let { showToast(it.message) } 
                            ?: showToast("操作失败")
                    }
                }
            }
        }
    }

    // 音量减少处理
    private fun handleVolumeDown() {
        executeWithDeviceCheck { controller ->
            lifecycleScope.launch {
                controller.volumeDown { response ->
                    runOnUiThread {
                        response?.let { showToast(it.message) } 
                            ?: showToast("操作失败")
                    }
                }
            }
        }
    }

    // 静音切换处理
    private fun handleMuteToggle() {
        executeWithDeviceCheck { controller ->
            lifecycleScope.launch {
                controller.toggleMute { response ->
                    runOnUiThread {
                        response?.let { showToast(it.message) } 
                            ?: showToast("操作失败")
                    }
                }
            }
        }
    }

    // 加载当前播放信息
    private suspend fun loadCurrentPlayingInfo() {
        try {
            musicController?.getNowPlaying { playingInfo ->
                runOnUiThread {
                    playingInfo?.let {
                        tvTitle.text = it.title ?: "未知标题"
                        tvArtist.text = it.artist ?: "未知艺术家"
                        tvStatus.text = if (it.isPlaying) "状态：播放中" else "状态：已暂停"
                    } ?: run {
                        tvStatus.text = "获取播放信息失败"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载播放信息出错", e)
            tvStatus.text = "加载失败：${e.message?.take(10)}"
        }
    }

    // 设备连接检查通用方法
    private fun executeWithDeviceCheck(action: (MusicController) -> Unit) {
        val controller = musicController
        if (controller == null || controller.currentIp.isNullOrEmpty()) {
            showToast("请先连接设备")
            return
        }
        try {
            action(controller)
        } catch (e: CancellationException) {
            // 忽略协程取消异常
        } catch (e: Exception) {
            Log.e(TAG, "操作执行失败", e)
            showToast("操作出错：${e.message?.take(10)}")
        }
    }

    // 简单IP验证
    private fun isValidIp(ip: String): Boolean {
        val parts = ip.split(".")
        return parts.size == 4 && parts.all { it.toIntOrNull() in 0..255 }
    }

    // 显示提示信息
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // 显示错误信息
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        tvStatus.text = message
    }

    // 全局异常处理
    private fun setupGlobalExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            Log.e(TAG, "发生未捕获异常", e)
            runOnUiThread {
                showError("应用出错，请重启：${e.message?.take(20)}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理资源
        musicController = null
        Thread.setDefaultUncaughtExceptionHandler(null)
    }
}
