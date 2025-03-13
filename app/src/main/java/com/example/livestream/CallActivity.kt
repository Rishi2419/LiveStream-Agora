package com.example.livestream

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.livestream.databinding.ActivityCallBinding
import io.agora.rtc2.*
import io.agora.rtc2.video.CameraCapturerConfiguration
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration
import io.agora.rtc2.video.VideoEncoderConfiguration.*
import io.agora.rtc2.video.VideoEncoderConfiguration.FRAME_RATE.*

class CallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallBinding
    private var agoraEngine: RtcEngine? = null
    private var isHost = false
    private var rearCameraView: SurfaceView? = null
    private var frontCameraView: SurfaceView? = null
    private var remoteSurfaceView: SurfaceView? = null

    // Front camera helper
    private var frontCameraHelper: FrontCameraHelper? = null

    private val appId = "2298ff7865d14062afec8e8cedd5daf5"
    private val channelName = "rishi"
    private val token = "007eJxTYDjhtfmqlIm/wu6Gg8sZd3PdzHyQtLblSTXfG5/YfQ+498YrMKSmJadYGlkmJZkkG5ikpJhYplgYJSclAhkGhiZmySmyDy+mNwQyMjz+84CRkQECQXxWhqLM4oxMBgYA7EoiwA=="
    private val uid = 0

    private val REQUESTED_PERMISSION = arrayOf(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.CAMERA
    )
    private val PERMISSION_ID = 22

    // Track current camera direction
    private var currentCameraDirection = CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_REAR

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        isHost = intent.getBooleanExtra("isHost", false)

        if (!checkSelfPermission()) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSION, PERMISSION_ID)
        }

        // Initialize views for cameras
        rearCameraView = SurfaceView(this)
        binding.fullscreenFrameLayout.addView(rearCameraView)

        frontCameraView = SurfaceView(this)
        binding.frontCameraFrameLayout.addView(frontCameraView)

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
                        setupRemoteVideo(uid)
                    }
                }

                override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                    Log.d("AgoraRTC", "Join channel success, uid: $uid")
                }

                override fun onUserOffline(uid: Int, reason: Int) {
                    runOnUiThread {
                        remoteSurfaceView?.visibility = View.GONE
                    }
                }
            }

            agoraEngine = RtcEngine.create(config)
            agoraEngine!!.enableVideo()

            // Set up video encoder configuration
            val videoEncoderConfig = VideoEncoderConfiguration()
            videoEncoderConfig.dimensions = VD_640x360
            //videoEncoderConfig.frameRate = FRAME_RATE_FPS_15
            videoEncoderConfig.bitrate = STANDARD_BITRATE
            agoraEngine!!.setVideoEncoderConfiguration(videoEncoderConfig)

            if (isHost) {
                joinChannelAsHost()
            } else {
                joinChannelAsAudience()
            }

        } catch (e: Exception) {
            Log.e("AgoraRTC", "Error setting up Agora engine: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun joinChannelAsHost() {
        if (!checkSelfPermission()) return

        agoraEngine!!.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        agoraEngine!!.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)

        // Set up rear camera (main camera for Agora)
        setupRearCamera()

        // Set up front camera (using Camera2 API)
        setupFrontCamera()

        agoraEngine!!.startPreview()

        // Join channel
        val options = ChannelMediaOptions()
        options.publishCameraTrack = true
        options.publishMicrophoneTrack = true
        agoraEngine!!.joinChannel(token, channelName, uid, options)
    }

    private fun setupRearCamera() {
        // Set rear camera configuration for Agora
        val cameraConfig = CameraCapturerConfiguration(
            CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_REAR
        )
        agoraEngine!!.setCameraCapturerConfiguration(cameraConfig)
        currentCameraDirection = CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_REAR

        // Set up local video preview for rear camera
        agoraEngine!!.setupLocalVideo(
            VideoCanvas(rearCameraView, VideoCanvas.RENDER_MODE_FIT, 0)
        )
    }

    private fun setupFrontCamera() {
        // Initialize the front camera helper to handle the front camera separately
        frontCameraHelper = FrontCameraHelper(
            context = this,
            surfaceView = frontCameraView!!,
            width = 640,
            height = 480
        )

        // Start the front camera capture
        frontCameraHelper?.startCamera()
    }

    private fun switchCamera() {
        // Only switch the main Agora camera
        try {
            agoraEngine!!.switchCamera()

            // Update current camera direction
            currentCameraDirection = if (currentCameraDirection == CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_FRONT) {
                CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_REAR
            } else {
                CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_FRONT
            }

        } catch (e: Exception) {
            Log.e("AgoraRTC", "Failed to switch camera: ${e.message}")
        }
    }

    private fun joinChannelAsAudience() {
        agoraEngine!!.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        agoraEngine!!.setClientRole(Constants.CLIENT_ROLE_AUDIENCE)

        // Join channel as audience
        agoraEngine!!.joinChannel(token, channelName, uid, ChannelMediaOptions())
    }

    private fun setupRemoteVideo(uid: Int) {
        // Set up remote view for the main stream
        remoteSurfaceView = SurfaceView(this)
        binding.fullscreenFrameLayout.removeAllViews()
        binding.fullscreenFrameLayout.addView(remoteSurfaceView)

        agoraEngine!!.setupRemoteVideo(
            VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_FIT, uid)
        )
    }

    private fun leaveCall() {
        // Clean up front camera helper
        frontCameraHelper?.stopCamera()
        frontCameraHelper = null

        // Clean up Agora resources
        agoraEngine?.stopPreview()
        agoraEngine?.leaveChannel()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clean up front camera helper
        frontCameraHelper?.stopCamera()
        frontCameraHelper = null

        // Clean up Agora resources
        Thread {
            RtcEngine.destroy()
            agoraEngine = null
        }.start()

        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}