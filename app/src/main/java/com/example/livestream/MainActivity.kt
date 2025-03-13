package com.example.livestream

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceView
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.livestream.databinding.ActivityMainBinding
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var agoraEngine: RtcEngine? = null
    private var isHost = false
    private var isHostPresent = false
    private var localSurfaceView: SurfaceView? = null
    private var remoteSurfaceView: SurfaceView? = null
    private val appId = "2298ff7865d14062afec8e8cedd5daf5"
    private val channelName = "rishi"
    private val token = "007eJxTYDjhtfmqlIm/wu6Gg8sZd3PdzHyQtLblSTXfG5/YfQ+498YrMKSmJadYGlkmJZkkG5ikpJhYplgYJSclAhkGhiZmySmyDy+mNwQyMjz+84CRkQECQXxWhqLM4oxMBgYA7EoiwA=="

    private val uid = 0
    private val PERMISSION_ID = 22

    private val REQUESTED_PERMISSION = arrayOf(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.CAMERA
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Keep the screen awake
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Request Permissions
        if (!checkSelfPermission()) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSION, PERMISSION_ID)
        }

        setupAgoraEngine()

        binding.btnJoinAsHost.setOnClickListener {
            if (!isHostPresent) {
                isHost = true
                isHostPresent = true
                joinChannelAsHost()
            } else {
                showHostAlreadyPresentDialog()
            }
        }

        binding.btnJoinAsAudience.setOnClickListener {
            if (!isHost) {
                joinChannelAsAudience()
            }
        }
    }
    private fun checkSelfPermission(): Boolean {
        return !(ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSION[0]) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSION[1]) != PackageManager.PERMISSION_GRANTED)
    }


    private fun setupAgoraEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = applicationContext
            config.mAppId = appId
            config.mEventHandler = object : IRtcEngineEventHandler() {
                override fun onUserJoined(uid: Int, elapsed: Int) {
                    runOnUiThread {
                        if (!isHost) {
                            setupRemoteVideo(uid)
                        }
                    }
                }

                override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                    showMessage("Joined channel: $channel")
                }

                override fun onUserOffline(uid: Int, reason: Int) {
                    runOnUiThread {
                        remoteSurfaceView?.visibility = GONE
                    }
                }
            }

            agoraEngine = RtcEngine.create(config)
            agoraEngine!!.enableVideo()

        } catch (e: Exception) {
            showMessage(e.message.toString())
        }
    }

    private fun joinChannelAsHost() {
        agoraEngine!!.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        agoraEngine!!.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)

        setupLocalVideo()
        localSurfaceView!!.visibility = VISIBLE
        agoraEngine!!.startPreview()
        agoraEngine!!.joinChannel(token, channelName, uid, ChannelMediaOptions())
    }

    private fun joinChannelAsAudience() {
        agoraEngine!!.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        agoraEngine!!.setClientRole(Constants.CLIENT_ROLE_AUDIENCE)
        agoraEngine!!.joinChannel(token, channelName, uid, ChannelMediaOptions())
    }

    private fun setupLocalVideo() {
        localSurfaceView = SurfaceView(this)
        binding.frameLayout.addView(localSurfaceView)

        agoraEngine!!.setupLocalVideo(
            VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_FIT, 0)
        )
    }

    private fun setupRemoteVideo(uid: Int) {
        remoteSurfaceView = SurfaceView(this)
        binding.frameLayout.removeAllViews()
        binding.frameLayout.addView(remoteSurfaceView)

        agoraEngine!!.setupRemoteVideo(
            VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_FIT, uid)
        )
    }

    private fun showHostAlreadyPresentDialog() {
        AlertDialog.Builder(this)
            .setTitle("Host Already Present")
            .setMessage("Someone is already hosting the stream. Join as an audience.")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showMessage(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        agoraEngine?.stopPreview()
        agoraEngine?.leaveChannel()

        Thread {
            RtcEngine.destroy()
            agoraEngine = null
        }.start()

        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

}
