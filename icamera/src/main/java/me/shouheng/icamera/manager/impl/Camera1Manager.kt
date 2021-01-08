package me.shouheng.icamera.manager.impl

import android.content.Context
import android.graphics.PixelFormat
import android.hardware.Camera
import android.hardware.Camera.*
import android.media.ExifInterface
import android.media.MediaActionSound
import android.media.MediaRecorder
import android.os.Build
import me.shouheng.icamera.config.ConfigurationProvider
import me.shouheng.icamera.config.size.AspectRatio
import me.shouheng.icamera.config.size.Size
import me.shouheng.icamera.config.size.SizeMap
import me.shouheng.icamera.enums.*
import me.shouheng.icamera.listener.CameraCloseListener
import me.shouheng.icamera.listener.CameraOpenListener
import me.shouheng.icamera.listener.CameraPhotoListener
import me.shouheng.icamera.listener.CameraVideoListener
import me.shouheng.icamera.preview.CameraPreview
import me.shouheng.icamera.preview.CameraPreviewCallback
import me.shouheng.icamera.util.CameraHelper
import me.shouheng.icamera.util.CameraHelper.calDisplayOrientation
import me.shouheng.icamera.util.CameraHelper.getSizeMapFromSizes
import me.shouheng.icamera.util.CameraHelper.getZoomIdxForZoomFactor
import me.shouheng.icamera.util.CameraHelper.onOrientationChanged
import me.shouheng.icamera.util.XLog.d
import me.shouheng.icamera.util.XLog.e
import me.shouheng.icamera.util.XLog.i
import me.shouheng.icamera.util.XLog.w
import java.io.File
import java.io.IOException

/**
 * Camera manager for camera1.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:51
 */
class Camera1Manager(cameraPreview: CameraPreview) : BaseCameraManager<Int?>(cameraPreview) {

    private var camera: Camera? = null
    private var zoomRatios: List<Float>? = null

    @Volatile
    private var showingPreview = false
    private var canDisableShutterSound = false

    override fun initialize(context: Context) {
        super.initialize(context)
        initCameraInfo()
    }

    override fun openCamera(cameraOpenListener: CameraOpenListener?) {
        super.openCamera(cameraOpenListener)
        backgroundHandler?.post {
            d("Camera1Manager", "openCamera")
            try {
                camera = open(currentCameraId!!)
                camera?.setPreviewCallback { bytes, camera ->
                    notifyPreviewFrameChanged(bytes, previewSize!!, camera.parameters.previewFormat)
                }
                prepareCameraOutputs()
                adjustCameraParameters(
                    forceCalculateSizes = false,
                    changeFocusMode = true,
                    changeFlashMode = true
                )
                if (cameraPreview.isAvailable) {
                    setupPreview()
                }
                camera?.startPreview()
                showingPreview = true
                notifyCameraOpened()
            } catch (ex: Exception) {
                e("Camera1Manager", "error : $ex")
                notifyCameraOpenError(ex)
            }
        }
    }

    override val isCameraOpened: Boolean
        get() = camera != null

    override fun switchCamera(cameraFace: Int) {
        d("Camera1Manager", "switchCamera : $cameraFace")
        super.switchCamera(cameraFace)
        if (isCameraOpened) {
            closeCamera(cameraCloseListener)
            openCamera(cameraOpenListener)
        }
    }

    override var mediaType: Int = super.mediaType
        set(mediaType) {
            d("Camera1Manager", "setMediaType : " + mediaType + " with mediaType " + this.mediaType)
            if (this.mediaType == mediaType) return
            field = mediaType
            if (isCameraOpened) {
                backgroundHandler?.post {
                    try {
                        adjustCameraParameters(true, false, false)
                    } catch (ex: Exception) {
                        e("Camera1Manager", "setMediaType : $ex")
                    }
                }
            }
        }

    override var isVoiceEnable: Boolean
        get() = voiceEnabled
        set(voiceEnable) {
            if (voiceEnabled == voiceEnable) return
            voiceEnabled = voiceEnable
        }

    override var isAutoFocus: Boolean = super.isAutoFocus
        set(autoFocus) {
            if (isAutoFocus == autoFocus) return
            field = autoFocus
            if (isCameraOpened) {
                backgroundHandler?.post { setFocusModeInternal(null) }
            }
        }

    @get:FlashMode
    override var flashMode: Int = super.flashMode
        set(flashMode) {
            if (this.flashMode == flashMode) return
            field = flashMode
            if (isCameraOpened) {
                backgroundHandler?.post { setFlashModeInternal(null) }
            }
        }

    override var zoom: Float = super.zoom
        set(zoom) {
            if (zoom == this.zoom || zoom > getMaxZoom() || zoom < 1f) return
            field = zoom
            if (isCameraOpened) {
                backgroundHandler?.post { setZoomInternal(null) }
            }
        }

    override fun getMaxZoom(): Float {
        if (zoomRatios == null) {
            w("Camera1Manager", "Try to get max zoom while it's not ready.")
            return 0f
        }
        if (maxZoomValue == 0f) {
            maxZoomValue = zoomRatios!![zoomRatios!!.size - 1]
        }
        return maxZoomValue
    }

    override fun setExpectSize(expectSize: Size) {
        super.setExpectSize(expectSize)
        if (isCameraOpened) {
            adjustCameraParameters(
                forceCalculateSizes = true,
                changeFocusMode = false,
                changeFlashMode = false
            )
        }
    }

    override fun setExpectAspectRatio(expectAspectRatio: AspectRatio) {
        super.setExpectAspectRatio(expectAspectRatio)
        if (isCameraOpened) {
            adjustCameraParameters(
                forceCalculateSizes = true,
                changeFocusMode = false,
                changeFlashMode = false
            )
        }
    }

    override fun getSize(@CameraSizeFor sizeFor: Int): Size? {
        return when (sizeFor) {
            CameraSizeFor.SIZE_FOR_PREVIEW -> previewSize
            CameraSizeFor.SIZE_FOR_PICTURE -> pictureSize
            CameraSizeFor.SIZE_FOR_VIDEO -> videoSize
            else -> null
        }
    }

    override fun getSizes(@CameraSizeFor sizeFor: Int): SizeMap? {
        return when (sizeFor) {
            CameraSizeFor.SIZE_FOR_PREVIEW -> {
                if (previewSizeMap == null) {
                    previewSizeMap = getSizeMapFromSizes(previewSizes!!)
                }
                previewSizeMap
            }
            CameraSizeFor.SIZE_FOR_PICTURE -> {
                if (pictureSizeMap == null) {
                    pictureSizeMap = getSizeMapFromSizes(pictureSizes!!)
                }
                pictureSizeMap
            }
            CameraSizeFor.SIZE_FOR_VIDEO -> {
                if (videoSizeMap == null) {
                    videoSizeMap = getSizeMapFromSizes(videoSizes!!)
                }
                videoSizeMap
            }
            else -> null
        }
    }

    override var displayOrientation: Int = super.displayOrientation
        get() = super.displayOrientation
        set(displayOrientation) {
            d("Camera1Manager", "displayOrientation : $displayOrientation")
            if (this.displayOrientation == displayOrientation) return
            field = displayOrientation
            if (isCameraOpened) {
                val parameters = camera?.parameters ?: return
                onOrientationChanged(currentCameraId!!, displayOrientation, parameters)
                camera?.parameters = parameters
                if (showingPreview) {
                    camera?.stopPreview()
                    showingPreview = false
                }
                camera?.setDisplayOrientation(
                    calDisplayOrientation(context, cameraFace,
                        if (cameraFace == CameraFace.FACE_FRONT) frontCameraOrientation else rearCameraOrientation
                    )
                )
                if (!showingPreview) {
                    camera?.startPreview()
                    showingPreview = true
                }
            }
        }

    override fun takePicture(fileToSave: File, cameraPhotoListener: CameraPhotoListener?) {
        super.takePicture(fileToSave, cameraPhotoListener)
        if (!isCameraOpened) {
            notifyCameraCaptureFailed(RuntimeException("Camera not open yet!"))
            return
        }
        backgroundHandler?.post {
            try {
                if (!takingPicture) {
                    takingPicture = true
                    setCameraPhotoQualityInternal(camera)
                    // Disable shutter default and play it by custom MediaActionSound.
                    // Make it consistent with camera 2. The shutter of camera1
                    // was not based on the phone mode, so even when the phone is in silent mode
                    // it makes sound still.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && canDisableShutterSound) {
                        camera?.enableShutterSound(false)
                        if (voiceEnabled && mediaActionSound != null) {
                            mediaActionSound?.play(MediaActionSound.SHUTTER_CLICK)
                        }
                    } // else, let it go ... there is nothing we could do.
                    camera?.takePicture(null, null, PictureCallback { bytes, _ ->
                        onPictureTakenInternal(bytes)
                        takingPicture = false
                    })
                } else {
                    i("Camera1Manager", "takePicture : taking picture")
                }
            } catch (ex: Exception) {
                takingPicture = false
                e("Camera1Manager", "takePicture error : $ex")
                notifyCameraCaptureFailed(RuntimeException(ex))
            }
        }
    }

    override fun startVideoRecord(file: File, cameraVideoListener: CameraVideoListener?) {
        super.startVideoRecord(file, cameraVideoListener)
        if (videoRecording) return
        if (isCameraOpened) {
            backgroundHandler?.post {
                if (prepareVideoRecorder()) {
                    videoRecorder?.start()
                    videoRecording = true
                    notifyVideoRecordStart()
                }
            }
        }
    }

    override fun stopVideoRecord() {
        if (videoRecording && isCameraOpened) {
            backgroundHandler?.post {
                safeStopVideoRecorder()
                releaseVideoRecorder()
                videoRecording = false
                notifyVideoRecordStop(videoOutFile!!)
            }
        }
    }

    override fun resumePreview() {
        camera?.startPreview()
    }

    override fun closeCamera(cameraCloseListener: CameraCloseListener?) {
        if (isCameraOpened) {
            camera?.setPreviewCallback(null)
            camera?.stopPreview()
        }
        showingPreview = false
        uiHandler.removeCallbacksAndMessages(null)
        backgroundHandler?.removeCallbacksAndMessages(null)
        releaseCameraInternal()
        notifyCameraClosed()
    }

    /*-------------------------------------- Inner Methods Region -----------------------------------------*/
    private fun initCameraInfo() {
        numberOfCameras = getNumberOfCameras()
        for (i in 0 until numberOfCameras) {
            val cameraInfo = CameraInfo()
            getCameraInfo(i, cameraInfo)
            if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
                rearCameraId = i
                rearCameraOrientation = cameraInfo.orientation
            } else if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
                frontCameraId = i
                frontCameraOrientation = cameraInfo.orientation
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                canDisableShutterSound = cameraInfo.canDisableShutterSound
            }
        }
        currentCameraId = if (cameraFace == CameraFace.FACE_REAR) rearCameraId else frontCameraId
    }

    private fun prepareCameraOutputs() {
        try {
            val start = System.currentTimeMillis()
            previewSizes = ConfigurationProvider.get()
                .getSizes(camera!!, cameraFace, CameraSizeFor.SIZE_FOR_PREVIEW)
            pictureSizes = ConfigurationProvider.get()
                .getSizes(camera!!, cameraFace, CameraSizeFor.SIZE_FOR_PICTURE)
            videoSizes = ConfigurationProvider.get()
                .getSizes(camera!!, cameraFace, CameraSizeFor.SIZE_FOR_VIDEO)
            zoomRatios = ConfigurationProvider.get().getZoomRatios(camera!!, cameraFace)
            ConfigurationProvider.get().cameraSizeCalculator.init(
                expectedRatio, expectedSize, expectedQuality, previewSizes!!, pictureSizes!!, videoSizes!!
            )
            d("Camera1Manager", "prepareCameraOutputs cost : " + (System.currentTimeMillis() - start) + " ms")
        } catch (ex: Exception) {
            e("Camera1Manager", "error : $ex")
            notifyCameraOpenError(RuntimeException(ex))
        }
    }

    private fun adjustCameraParameters(forceCalculateSizes: Boolean, changeFocusMode: Boolean, changeFlashMode: Boolean) {
        val oldPreview = previewSize
        var start = System.currentTimeMillis()
        val calculator = ConfigurationProvider.get().cameraSizeCalculator
        val parameters = camera?.parameters ?: return
        if (mediaType == MediaType.TYPE_PICTURE && (pictureSize == null || forceCalculateSizes)) {
            pictureSize = calculator.getPictureSize(CameraType.TYPE_CAMERA1)
            previewSize = calculator.getPicturePreviewSize(CameraType.TYPE_CAMERA1)
            parameters.setPictureSize(pictureSize!!.width, pictureSize!!.height)
            notifyPictureSizeUpdated(pictureSize!!)
        }
        // fixed 2020-08-29 : the video size might be null if quickly switched
        // from media types while first time launch the camera.
        if (camcorderProfile == null || forceCalculateSizes) {
            camcorderProfile = CameraHelper.getCamcorderProfile(expectedQuality, currentCameraId!!)
        }
        if (videoSize == null || forceCalculateSizes) {
            videoSize = calculator.getVideoSize(CameraType.TYPE_CAMERA1)
            previewSize = calculator.getVideoPreviewSize(CameraType.TYPE_CAMERA1)
            notifyVideoSizeUpdated(videoSize!!)
        }
        if (previewSize != oldPreview) {
            parameters.setPreviewSize(previewSize!!.width, previewSize!!.height)
            notifyPreviewSizeUpdated(previewSize!!)
        }
        d("Camera1Manager", "adjustCameraParameters size cost : " + (System.currentTimeMillis() - start) + " ms")
        start = System.currentTimeMillis()
        if (changeFocusMode) {
            setFocusModeInternal(parameters)
        }
        d("Camera1Manager", "adjustCameraParameters focus cost : " + (System.currentTimeMillis() - start) + " ms")
        start = System.currentTimeMillis()
        if (changeFlashMode) {
            setFlashModeInternal(parameters)
        }
        d("Camera1Manager", "adjustCameraParameters flash cost : " + (System.currentTimeMillis() - start) + " ms")
        start = System.currentTimeMillis()
        setZoomInternal(parameters)
        d("Camera1Manager", "adjustCameraParameters zoom cost : " + (System.currentTimeMillis() - start) + " ms")
        start = System.currentTimeMillis()
        if (showingPreview) {
            showingPreview = false
            camera?.stopPreview()
        }
        camera?.parameters = parameters
        if (!showingPreview) {
            showingPreview = true
            camera?.startPreview()
        }
        d("Camera1Manager", "adjustCameraParameters restart preview cost : " + (System.currentTimeMillis() - start) + " ms")
    }

    private fun setCameraPhotoQualityInternal(camera: Camera?) {
        val parameters = camera?.parameters ?: return
        parameters.pictureFormat = PixelFormat.JPEG
        when (expectedQuality) {
            MediaQuality.QUALITY_LOWEST -> { parameters.jpegQuality = 25 }
            MediaQuality.QUALITY_LOW -> { parameters.jpegQuality = 50 }
            MediaQuality.QUALITY_MEDIUM -> { parameters.jpegQuality = 75 }
            MediaQuality.QUALITY_HIGH -> { parameters.jpegQuality = 100 }
            MediaQuality.QUALITY_HIGHEST -> { parameters.jpegQuality = 100 }
        }
        camera.parameters = parameters
    }

    private val photoOrientationInternal: Int
        get() {
            val rotate: Int = if (cameraFace == CameraFace.FACE_FRONT) {
                (360 + frontCameraOrientation + ConfigurationProvider.get().degrees) % 360
            } else {
                (360 + rearCameraOrientation - ConfigurationProvider.get().degrees) % 360
            }
            return when (rotate) {
                90 -> { ExifInterface.ORIENTATION_ROTATE_90 }
                180 -> { ExifInterface.ORIENTATION_ROTATE_180 }
                270 -> { ExifInterface.ORIENTATION_ROTATE_270 }
                else -> ExifInterface.ORIENTATION_NORMAL
            }
        }

    private val videoOrientationInternal: Int
        get() {
            return if (cameraFace == CameraFace.FACE_FRONT) {
                (360 + frontCameraOrientation + ConfigurationProvider.get().degrees) % 360
            } else {
                (360 + rearCameraOrientation - ConfigurationProvider.get().degrees) % 360
            }
        }

    private fun onPictureTakenInternal(bytes: ByteArray) {
        handlePictureTakenResult(bytes)
        // rotate
        try {
            val exif = ExifInterface(pictureFile!!.absolutePath)
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, "" + photoOrientationInternal)
            exif.saveAttributes()
            notifyCameraPictureTaken(bytes)
        } catch (error: Throwable) {
            e("Camera1Manager", "Can't save exif info: " + error.message)
        }
    }

    private fun setupPreview() {
        try {
            if (cameraPreview.previewType == PreviewViewType.SURFACE_VIEW) {
                if (showingPreview) {
                    camera?.stopPreview()
                    showingPreview = false
                }
                camera?.setPreviewDisplay(cameraPreview.surfaceHolder)
                if (!showingPreview) {
                    camera?.startPreview()
                    showingPreview = true
                }
            } else {
                val surfaceTexture = cameraPreview.surfaceTexture
                camera?.setPreviewTexture(surfaceTexture)
            }
            camera?.setDisplayOrientation(
                calDisplayOrientation(context, cameraFace,
                    if (cameraFace == CameraFace.FACE_FRONT) frontCameraOrientation else rearCameraOrientation
                )
            )
        } catch (e: IOException) {
            notifyCameraOpenError(RuntimeException(e))
        }
    }

    private fun prepareVideoRecorder(): Boolean {
        videoRecorder = MediaRecorder()
        try {
            camera?.lock()
            camera?.unlock()
            videoRecorder!!.setCamera(camera)
            videoRecorder!!.setAudioSource(MediaRecorder.AudioSource.DEFAULT)
            videoRecorder!!.setVideoSource(MediaRecorder.VideoSource.DEFAULT)
            videoRecorder!!.setOutputFormat(camcorderProfile!!.fileFormat)
            videoRecorder!!.setVideoFrameRate(camcorderProfile!!.videoFrameRate)
            videoRecorder!!.setVideoSize(videoSize!!.width, videoSize!!.height)
            videoRecorder!!.setVideoEncodingBitRate(camcorderProfile!!.videoBitRate)
            videoRecorder!!.setVideoEncoder(camcorderProfile!!.videoCodec)
            videoRecorder!!.setAudioEncodingBitRate(camcorderProfile!!.audioBitRate)
            videoRecorder!!.setAudioChannels(camcorderProfile!!.audioChannels)
            videoRecorder!!.setAudioSamplingRate(camcorderProfile!!.audioSampleRate)
            videoRecorder!!.setAudioEncoder(camcorderProfile!!.audioCodec)
            videoRecorder!!.setOutputFile(videoOutFile.toString())
            if (videoFileSize > 0) {
                videoRecorder!!.setMaxFileSize(videoFileSize)
                videoRecorder!!.setOnInfoListener(this)
            }
            if (videoDuration > 0) {
                videoRecorder!!.setMaxDuration(videoDuration)
                videoRecorder!!.setOnInfoListener(this)
            }
            videoRecorder!!.setPreviewDisplay(cameraPreview.surface)
            videoRecorder!!.setOrientationHint(videoOrientationInternal)
            videoRecorder!!.prepare()
            return true
        } catch (error: IllegalStateException) {
            e("Camera1Manager", "IllegalStateException preparing MediaRecorder: " + error.message)
            notifyVideoRecordError(error)
        } catch (error: IOException) {
            e("Camera1Manager", "IOException preparing MediaRecorder: " + error.message)
            notifyVideoRecordError(error)
        } catch (error: Throwable) {
            e("Camera1Manager", "Error during preparing MediaRecorder: " + error.message)
            notifyVideoRecordError(error)
        }
        releaseVideoRecorder()
        return false
    }

    private fun setFocusModeInternal(parameters: Parameters?) {
        var parms = parameters
        val nullParameters = parms == null
        parms = (if (nullParameters) camera?.parameters else parms) ?: return
        if (mediaType == MediaType.TYPE_VIDEO) {
            if (!turnVideoCameraFeaturesOn(parms)) {
                setAutoFocusInternal(parms)
            }
        } else if (mediaType == MediaType.TYPE_PICTURE) {
            if (!turnPhotoCameraFeaturesOn(parms)) {
                setAutoFocusInternal(parms)
            }
        }
        if (nullParameters) {
            camera?.parameters = parms
        }
    }

    private fun turnPhotoCameraFeaturesOn(parameters: Parameters): Boolean {
        if (parameters.supportedFocusModes.contains(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.focusMode = Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
            return true
        }
        return false
    }

    private fun turnVideoCameraFeaturesOn(parameters: Parameters): Boolean {
        if (parameters.supportedFocusModes.contains(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parameters.focusMode = Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
            return true
        }
        return false
    }

    private fun setAutoFocusInternal(parameters: Parameters) {
        try {
            val modes = parameters.supportedFocusModes
            if (isAutoFocus && modes.contains(Parameters.FOCUS_MODE_AUTO)) {
                parameters.focusMode = Parameters.FOCUS_MODE_AUTO
            } else if (modes.contains(Parameters.FOCUS_MODE_FIXED)) {
                parameters.focusMode = Parameters.FOCUS_MODE_FIXED
            } else if (modes.contains(Parameters.FOCUS_MODE_INFINITY)) {
                parameters.focusMode = Parameters.FOCUS_MODE_INFINITY
            } else {
                parameters.focusMode = modes[0]
            }
        } catch (ex: Exception) {
            e("Camera1Manager", "setAutoFocusInternal $ex")
        }
    }

    private fun setFlashModeInternal(parameters: Parameters?) {
        var params = parameters
        val nullParameters = params == null
        params = if (nullParameters) camera?.parameters else params
        val modes = params?.supportedFlashModes ?: return
        try {
            when (flashMode) {
                FlashMode.FLASH_ON -> setFlashModeOrAuto(params, modes, Parameters.FLASH_MODE_ON)
                FlashMode.FLASH_OFF -> setFlashModeOrAuto(params, modes, Parameters.FLASH_MODE_OFF)
                FlashMode.FLASH_AUTO -> if (modes.contains(Parameters.FLASH_MODE_AUTO)) { params.flashMode = Parameters.FLASH_MODE_AUTO }
                else -> if (modes.contains(Parameters.FLASH_MODE_AUTO)) { params.flashMode = Parameters.FLASH_MODE_AUTO }
            }
            if (nullParameters) {
                camera?.parameters = params
            }
        } catch (ex: Exception) {
            e("Camera1Manager", "setFlashModeInternal : $ex")
        }
    }

    private fun setFlashModeOrAuto(parameters: Parameters?, supportModes: List<String>, mode: String) {
        if (supportModes.contains(mode)) {
            parameters?.flashMode = mode
        } else {
            if (supportModes.contains(Parameters.FLASH_MODE_AUTO)) {
                parameters?.flashMode = Parameters.FLASH_MODE_AUTO
            }
        }
    }

    private fun setZoomInternal(parameters: Parameters?) {
        var params = parameters
        val nullParameters = params == null
        params = (if (nullParameters) camera?.parameters else params) ?: return
        if (params.isZoomSupported) {
            params.zoom = getZoomIdxForZoomFactor(params.zoomRatios, zoom)
            if (nullParameters) {
                camera?.parameters = params
            }
        }
    }

    private fun releaseCameraInternal() {
        camera?.release()
        camera = null
        previewSize = null
        pictureSize = null
        videoSize = null
        maxZoomValue = 0f
    }

    init {
        cameraPreview.setCameraPreviewCallback(object : CameraPreviewCallback {
            override fun onAvailable(cameraPreview: CameraPreview) {
                d("Camera1Manager", "onAvailable : " + cameraPreview.isAvailable)
                if (isCameraOpened) {
                    setupPreview()
                }
            }
        })
    }
}