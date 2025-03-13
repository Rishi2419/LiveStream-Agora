package com.example.livestream

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.content.ContextCompat
import java.util.*

class FrontCameraHelper(
    private val context: Context,
    private val surfaceView: SurfaceView,
    private val width: Int = 640,
    private val height: Int = 480
) {
    private val TAG = "FrontCameraHelper"

    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private lateinit var previewRequestBuilder: CaptureRequest.Builder
    private lateinit var previewRequest: CaptureRequest

    private val cameraManager: CameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private lateinit var cameraId: String
    private lateinit var backgroundHandler: Handler
    private lateinit var backgroundThread: HandlerThread

    // Camera state callback
    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createCameraPreviewSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice?.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice?.close()
            cameraDevice = null
            Log.e(TAG, "Camera device error: $error")
        }
    }

    // Initialize camera
    fun startCamera() {
        startBackgroundThread()

        // Find front camera
        try {
            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)

                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    this.cameraId = cameraId
                    break
                }
            }

            // Set up surface for preview
            val surfaceHolder = surfaceView.holder
            surfaceHolder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    openCamera()
                }

                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                    // Handle surface changes if needed
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    closeCamera()
                }
            })

            // If surface is already available, open camera immediately
            if (surfaceHolder.surface.isValid) {
                openCamera()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting camera: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground")
        backgroundThread.start()
        backgroundHandler = Handler(backgroundThread.looper)
    }

    private fun stopBackgroundThread() {
        try {
            backgroundThread.quitSafely()
            backgroundThread.join()
        } catch (e: InterruptedException) {
            Log.e(TAG, "Error stopping background thread: ${e.message}")
        }
    }

    private fun openCamera() {
        try {
            // Check for permission at runtime too
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(cameraId, stateCallback, backgroundHandler)
            } else {
                Log.e(TAG, "Camera permission not granted")
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to open camera: ${e.message}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Camera permission not granted: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening camera: ${e.message}")
        }
    }

    private fun createCameraPreviewSession() {
        try {
            val surface = surfaceView.holder.surface

            // Create preview request
            previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder.addTarget(surface)

            // Create camera capture session
            cameraDevice!!.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (cameraDevice == null) return

                        cameraCaptureSession = session
                        previewRequestBuilder.set(
                            CaptureRequest.CONTROL_AF_MODE,
                            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                        )

                        // Start preview
                        previewRequest = previewRequestBuilder.build()
                        cameraCaptureSession?.setRepeatingRequest(
                            previewRequest,
                            null,
                            backgroundHandler
                        )
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Failed to configure camera")
                    }
                },
                null
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to create camera preview session: ${e.message}")
        }
    }

    fun stopCamera() {
        closeCamera()
        stopBackgroundThread()
    }

    private fun closeCamera() {
        try {
            cameraCaptureSession?.close()
            cameraCaptureSession = null

            cameraDevice?.close()
            cameraDevice = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing camera: ${e.message}")
        }
    }

    // Additional helper methods if needed

    // Get optimal preview size
    private fun getOptimalPreviewSize(sizes: Array<Size>, width: Int, height: Int): Size {
        val aspectRatio = width.toFloat() / height.toFloat()

        return sizes.sortedWith(compareBy {
            Math.abs(it.width.toFloat() / it.height.toFloat() - aspectRatio)
        }).filter {
            it.width <= width && it.height <= height
        }.firstOrNull() ?: sizes[0]
    }

    // Toggle camera flash if available
    fun toggleFlash(enable: Boolean) {
        try {
            if (::previewRequestBuilder.isInitialized) {
                previewRequestBuilder.set(
                    CaptureRequest.FLASH_MODE,
                    if (enable) CaptureRequest.FLASH_MODE_TORCH else CaptureRequest.FLASH_MODE_OFF
                )

                previewRequest = previewRequestBuilder.build()
                cameraCaptureSession?.setRepeatingRequest(
                    previewRequest,
                    null,
                    backgroundHandler
                )
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to toggle flash: ${e.message}")
        }
    }
}