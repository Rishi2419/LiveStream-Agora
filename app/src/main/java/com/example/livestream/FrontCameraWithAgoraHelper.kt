//package com.example.livestream
//
//import android.content.Context
//import android.graphics.ImageFormat
//import android.media.Image
//import android.media.ImageReader
//import android.view.SurfaceView
//import io.agora.rtc2.RtcEngine
//import io.agora.rtc2.video.ExternalVideoFrame
//import java.nio.ByteBuffer
//
//
//class FrontCameraWithAgoraHelper(
//    context: Context,
//    surfaceView: SurfaceView,
//    width: Int = 640,
//    height: Int = 480,
//    private val agoraEngine: RtcEngine,
//    private val sourceId: Int
//) : FrontCameraHelper(context, surfaceView, width, height) {
//
//    // Override the createCameraPreviewSession method to capture frames
//    // and push them to Agora
//    override fun createCameraPreviewSession() {
//        // First call the parent method to set up the preview
//        super.createCameraPreviewSession()
//
//        // Then set up the image reader to capture frames
//        setupImageReader()
//    }
//
//    private fun setupImageReader() {
//        // Create an image reader to capture frames
//        val imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 2)
//
//        // Set up the image reader listener
//        imageReader.setOnImageAvailableListener({ reader ->
//            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
//
//            // Convert the image to a format that Agora can use
//            val frame = convertImageToAgoraVideoFrame(image)
//
//            // Push the frame to Agora
//            agoraEngine.pushExternalVideoFrame(frame)
//
//            // Release the image
//            image.close()
//        }, backgroundHandler)
//
//        // Add the image reader as a target to the camera capture session
//        previewRequestBuilder.addTarget(imageReader.surface)
//    }
//
//    private fun convertImageToAgoraVideoFrame(image: Image): VideoFrame {
//        // Convert the image to an Agora video frame
//        // This is a simplified version - you'll need to implement the actual conversion
//        val frame = VideoFrame()
//        frame.format = VideoFrame.FORMAT_I420
//        frame.timeStamp = System.currentTimeMillis()
//        frame.stride = width
//        frame.height = height
//        frame.sourceId = sourceId
//
//        // Fill the frame data from the image
//        // This is where you'd convert YUV_420_888 to I420 format
//
//        return frame
//    }
//}