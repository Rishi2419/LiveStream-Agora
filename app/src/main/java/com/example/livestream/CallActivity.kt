package com.example.livestream

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceView
import android.view.View.GONE
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.livestream.databinding.ActivityCallBinding
import io.agora.rtc2.*
import io.agora.rtc2.video.CameraCapturerConfiguration
import io.agora.rtc2.video.VideoCanvas

class CallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallBinding
    private var agoraEngine: RtcEngine? = null
    private var isHost = false
    private var localSurfaceView: SurfaceView? = null
    private var remoteSurfaceView: SurfaceView? = null

    private val appId = "2298ff7865d14062afec8e8cedd5daf5"
    private val channelName = "rishi"
    private val token = "007eJxTYDjhtfmqlIm/wu6Gg8sZd3PdzHyQtLblSTXfG5/YfQ+498YrMKSmJadYGlkmJZkkG5ikpJhYplgYJSclAhkGhiZmySmyDy+mNwQyMjz+84CRkQECQXxWhqLM4oxMBgYA7EoiwA=="
    private val uid = 0

    private val REQUESTED_PERMISSION = arrayOf(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.CAMERA
    )
    private val PERMISSION_ID = 22

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        isHost = intent.getBooleanExtra("isHost", false)

        if (!checkSelfPermission()) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSION, PERMISSION_ID)
        }

        setupAgoraEngine()

        binding.btnLeave.setOnClickListener {
            leaveCall()
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

                override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {}

                override fun onUserOffline(uid: Int, reason: Int) {
                    runOnUiThread {
                        remoteSurfaceView?.visibility = GONE
                    }
                }
            }

            agoraEngine = RtcEngine.create(config)
            agoraEngine!!.enableVideo()

            if (isHost) {
                joinChannelAsHost()
            } else {
                joinChannelAsAudience()
            }

        } catch (e: Exception) {}
    }

    private fun joinChannelAsHost() {
        if (!checkSelfPermission()) return

        agoraEngine!!.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        agoraEngine!!.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)

        // Force Agora to use the **rear camera**
        val cameraConfig = CameraCapturerConfiguration(
            CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_REAR
        )
        agoraEngine!!.setCameraCapturerConfiguration(cameraConfig)

        setupLocalVideo()  // Setup the local video AFTER setting camera configuration

        localSurfaceView!!.visibility = SurfaceView.VISIBLE
        agoraEngine!!.startPreview()
        agoraEngine!!.joinChannel(token, channelName, uid, ChannelMediaOptions())
    }



    private fun joinChannelAsAudience() {
        agoraEngine!!.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        agoraEngine!!.setClientRole(Constants.CLIENT_ROLE_AUDIENCE)
        agoraEngine!!.joinChannel(token, channelName, uid, ChannelMediaOptions())
    }

    private fun setupLocalVideo() {
        if (localSurfaceView == null) {
            localSurfaceView = SurfaceView(this)
            binding.fullscreenFrameLayout.addView(localSurfaceView)
        }

        agoraEngine!!.setupLocalVideo(
            VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_FIT, 0)
        )
    }

    private fun setupRemoteVideo(uid: Int) {
        remoteSurfaceView = SurfaceView(this)
        binding.fullscreenFrameLayout.removeAllViews()
        binding.fullscreenFrameLayout.addView(remoteSurfaceView)

        agoraEngine!!.setupRemoteVideo(
            VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_FIT, uid)
        )
    }

    private fun leaveCall() {
        agoraEngine?.stopPreview()
        agoraEngine?.leaveChannel()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        Thread {
            RtcEngine.destroy()
            agoraEngine = null
        }.start()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
