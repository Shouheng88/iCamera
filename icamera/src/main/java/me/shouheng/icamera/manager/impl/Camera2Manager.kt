package me.shouheng.icamera.manager.impl

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.media.MediaActionSound
import android.media.MediaRecorder
import android.os.Build
import android.support.annotation.IntDef
import android.support.annotation.RequiresApi
import android.view.Surface
import android.view.SurfaceHolder
import me.shouheng.icamera.config.ConfigurationProvider
import me.shouheng.icamera.config.convert.ImageRawDataConverter
import me.shouheng.icamera.config.size.AspectRatio
import me.shouheng.icamera.config.size.Size
import me.shouheng.icamera.config.size.SizeMap
import me.shouheng.icamera.enums.*
import me.shouheng.icamera.listener.CameraCloseListener
import me.shouheng.icamera.listener.CameraOpenListener
import me.shouheng.icamera.listener.CameraPhotoListener
import me.shouheng.icamera.listener.CameraVideoListener
import me.shouheng.icamera.manager.impl.Camera2Manager.CaptureSessionCallback.CameraState
import me.shouheng.icamera.preview.CameraPreview
import me.shouheng.icamera.preview.CameraPreviewCallback
import me.shouheng.icamera.util.CameraHelper
import me.shouheng.icamera.util.CameraHelper.getJpegOrientation
import me.shouheng.icamera.util.CameraHelper.getSizeMapFromSizes
import me.shouheng.icamera.util.XLog.d
import me.shouheng.icamera.util.XLog.e
import me.shouheng.icamera.util.XLog.i
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.locks.ReentrantLock

/**
 * Camera manager for camera2.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:52
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
@SuppressLint("MissingPermission")
class Camera2Manager(cameraPreview: CameraPreview) : BaseCameraManager<String>(cameraPreview), OnImageAvailableListener {
    private var cameraManager: CameraManager? = null
    private var cameraDevice: CameraDevice? = null
    private var frontCameraCharacteristics: CameraCharacteristics? = null
    private var rearCameraCharacteristics: CameraCharacteristics? = null
    private var frontStreamConfigurationMap: StreamConfigurationMap? = null
    private var rearStreamConfigurationMap: StreamConfigurationMap? = null
    private var imageReader: ImageReader? = null
    private var previewReader: ImageReader? = null
    private var surfaceHolder: SurfaceHolder? = null
    private var surfaceTexture: SurfaceTexture? = null
    private var workingSurface: Surface? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null
    private var previewRequest: CaptureRequest? = null
    private val captureSessionCallback: CaptureSessionCallback = object : CaptureSessionCallback() {
        private fun processCaptureResultInternal(result: CaptureResult, @CameraState cameraPreviewState: Int) {
            when (cameraPreviewState) {
                CameraState.STATE_PREVIEW -> { }
                CameraState.STATE_WAITING_LOCK -> {
                    val afState = result.get(CaptureResult.CONTROL_AF_STATE)
                    if (afState == null) {
                        captureStillPicture()
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState
                        || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState
                        || CaptureResult.CONTROL_AF_STATE_INACTIVE == afState
                        || CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN == afState
                    ) {
                        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                        if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            setCameraPreviewState(CameraState.STATE_PICTURE_TAKEN)
                            captureStillPicture()
                        } else {
                            runPreCaptureSequence()
                        }
                    }
                }
                CameraState.STATE_WAITING_PRE_CAPTURE -> {
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null
                        || aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE
                        || aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED
                    ) {
                        setCameraPreviewState(CameraState.STATE_WAITING_NON_PRE_CAPTURE)
                    }
                }
                CameraState.STATE_WAITING_NON_PRE_CAPTURE -> {
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        setCameraPreviewState(CameraState.STATE_PICTURE_TAKEN)
                        captureStillPicture()
                    }
                }
                CameraState.STATE_PICTURE_TAKEN -> { }
                else -> { }
            }
        }

        override fun processCaptureResult(result: CaptureResult, @CameraState cameraPreviewState: Int) {
            processCaptureResultInternal(result, cameraPreviewState)
        }
    }
    private val onPreviewImageAvailableListener: OnImageAvailableListener =
        object : OnImageAvailableListener {
            private val lock = ReentrantLock()
            private val converter: ImageRawDataConverter = ConfigurationProvider.get().imageRawDataConverter

            override fun onImageAvailable(reader: ImageReader) {
                try {
                    reader.acquireNextImage().use { image ->
                        // Y:U:V == 4:2:2
                        if (previewListener != null && image.format == ImageFormat.YUV_420_888) {
                            // lock to ensure that all data from same Image object
                            lock.lock()
                            notifyPreviewFrameChanged(converter.convertToNV21(image), previewSize!!, ImageFormat.NV21)
                            lock.unlock()
                        }
                    }
                } catch (ex: Exception) {
                    e("Camera2Manager", "error for image preview : $ex")
                }
            }
        }

    override fun initialize(context: Context) {
        super.initialize(context)
        initCameraInfo(context)
    }

    override fun openCamera(cameraOpenListener: CameraOpenListener?) {
        super.openCamera(cameraOpenListener)
        backgroundHandler!!.post {
            val start0 = System.currentTimeMillis()
            prepareCameraOutputs()
            adjustCameraConfiguration(false)
            try {
                val start1 = System.currentTimeMillis()
                cameraManager?.openCamera(
                    currentCameraId!!,
                    object : CameraDevice.StateCallback() {
                        override fun onOpened(camera: CameraDevice) {
                            val start2 = System.currentTimeMillis()
                            cameraDevice = camera
                            if (cameraPreview.isAvailable) {
                                createPreviewSession()
                            }
                            notifyCameraOpened()
                            d("Camera2Manager", "Camera opened cost : "
                                        + (System.currentTimeMillis() - start2) + "ms "
                                        + (System.currentTimeMillis() - start1) + "ms "
                                        + (System.currentTimeMillis() - start0) + "ms."
                            )
                        }

                        override fun onDisconnected(camera: CameraDevice) {
                            e("Camera2Manager", "Camera disconnected.")
                            camera.close()
                            cameraDevice = null
                            notifyCameraOpenError(RuntimeException("Camera disconnected."))
                        }

                        override fun onError(camera: CameraDevice, error: Int) {
                            e("Camera2Manager", "Camera open error : $error")
                            camera.close()
                            cameraDevice = null
                            notifyCameraOpenError(RuntimeException("Camera error : $error"))
                        }
                    },
                    backgroundHandler
                )
            } catch (ex: Exception) {
                e("Camera2Manager", "error : $ex")
                notifyCameraOpenError(ex)
            }
        }
    }

    override val isCameraOpened: Boolean
        get() = cameraDevice != null

    override fun switchCamera(cameraFace: Int) {
        super.switchCamera(cameraFace)
        if (isCameraOpened) {
            closeCamera(cameraCloseListener)
            openCamera(cameraOpenListener)
        }
    }

    override var mediaType: Int = super.mediaType
        set(mediaType) {
            d("Camera2Manager", "setMediaType : " + mediaType + " with mediaType " + this.mediaType)
            if (this.mediaType == mediaType) return
            field = mediaType
            if (isCameraOpened) {
                backgroundHandler?.post {
                    try {
                        adjustCameraConfiguration(false)
                    } catch (ex: Exception) {
                        e("Camera2Manager", "setMediaType : $ex")
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
            if (isAutoFocus == autoFocus) {
                return
            }
            field = autoFocus
            if (isCameraOpened && previewRequestBuilder != null) {
                setAutoFocusInternal()
                previewRequest = previewRequestBuilder!!.build()
                if (captureSession != null) {
                    try {
                        captureSession!!.setRepeatingRequest(
                            previewRequest!!,
                            captureSessionCallback,
                            backgroundHandler
                        )
                    } catch (e: CameraAccessException) {
                        e("Camera2Manager", "setAutoFocus error : $e")
                    }
                } else {
                    i("Camera2Manager", "setAutoFocus captureSession is null.")
                }
            } else {
                i("Camera2Manager", "setAutoFocus camera not open or previewRequestBuilder is null")
            }
        }

    override var flashMode: Int = super.flashMode
        set(flashMode) {
            if (this.flashMode == flashMode) {
                return
            }
            field = flashMode
            if (isCameraOpened && previewRequestBuilder != null) {
                val succeed = setFlashModeInternal()
                if (succeed) {
                    previewRequest = previewRequestBuilder!!.build()
                    if (captureSession != null) {
                        try {
                            captureSession?.setRepeatingRequest(
                                previewRequest!!,
                                captureSessionCallback,
                                backgroundHandler
                            )
                        } catch (e: CameraAccessException) {
                            e("Camera2Manager", "setFlashMode error : $e")
                        }
                    } else {
                        i("Camera2Manager", "setFlashMode captureSession is null.")
                    }
                } else {
                    i("Camera2Manager", "setFlashMode failed.")
                }
            } else {
                i("Camera2Manager", "setFlashMode camera not open or previewRequestBuilder is null")
            }
        }

    override var zoom: Float = super.zoom
        set(zoom) {
            if (zoom == this.zoom || zoom > getMaxZoom() || zoom < 1f) {
                return
            }
            field = zoom
            if (isCameraOpened) {
                val succeed = setZoomInternal(previewRequestBuilder)
                if (succeed) {
                    previewRequest = previewRequestBuilder!!.build()
                    if (captureSession != null) {
                        try {
                            captureSession!!.setRepeatingRequest(
                                previewRequest!!,
                                captureSessionCallback,
                                backgroundHandler
                            )
                        } catch (e: CameraAccessException) {
                            e("Camera2Manager", "setZoom error : $e")
                        }
                    } else {
                        i("Camera2Manager", "setZoom captureSession is null.")
                    }
                } else {
                    i("Camera2Manager", "setZoom failed : setZoomInternal failed.")
                }
            } else {
                i("Camera2Manager", "setZoom failed : camera not open.")
            }
        }

    override fun getMaxZoom(): Float {
        if (maxZoomValue == 0f) {
            val cameraCharacteristics = if (cameraFace == CameraFace.FACE_FRONT) frontCameraCharacteristics else rearCameraCharacteristics
            maxZoomValue = cameraCharacteristics!!.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f
        }
        return maxZoomValue
    }

    override fun setExpectSize(expectSize: Size) {
        super.setExpectSize(expectSize)
        if (isCameraOpened) {
            backgroundHandler?.post {
                adjustCameraConfiguration(true)
                createPreviewSession()
            }
        }
    }

    override fun setExpectAspectRatio(expectAspectRatio: AspectRatio) {
        super.setExpectAspectRatio(expectAspectRatio)
        if (isCameraOpened) {
            backgroundHandler?.post {
                adjustCameraConfiguration(true)
                createPreviewSession()
            }
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
                if (previewSizeMap == null && previewSizes != null) {
                    previewSizeMap = getSizeMapFromSizes(previewSizes!!)
                }
                previewSizeMap
            }
            CameraSizeFor.SIZE_FOR_PICTURE -> {
                if (pictureSizeMap == null && pictureSizes != null) {
                    pictureSizeMap = getSizeMapFromSizes(pictureSizes!!)
                }
                pictureSizeMap
            }
            CameraSizeFor.SIZE_FOR_VIDEO -> {
                if (videoSizeMap == null && videoSizes != null) {
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
            if (this.displayOrientation == displayOrientation) {
                return
            }
            field = displayOrientation
        }

    override fun takePicture(fileToSave: File, cameraPhotoListener: CameraPhotoListener?) {
        super.takePicture(fileToSave, cameraPhotoListener)
        backgroundHandler?.post {
            try {
                if (voiceEnabled) {
                    mediaActionSound?.play(MediaActionSound.SHUTTER_CLICK)
                }
                lockFocus()
            } catch (ex: Exception) {
                notifyCameraCaptureFailed(ex)
            }
        }
    }

    override fun startVideoRecord(file: File, cameraVideoListener: CameraVideoListener?) {
        super.startVideoRecord(file, cameraVideoListener)
        if (videoRecording) return
        if (isCameraOpened) {
            if (voiceEnabled) {
                mediaActionSound?.play(MediaActionSound.START_VIDEO_RECORDING)
            }
            backgroundHandler?.post {
                closePreviewSession()
                if (prepareVideoRecorder()) {
                    createRecordSession()
                } else {
                    i("Camera2Manager", "startVideoRecord : failed when prepare video recorder.")
                    notifyVideoRecordError(RuntimeException("Failed when prepare video recorder."))
                }
            }
        }
    }

    override fun stopVideoRecord() {
        if (videoRecording && isCameraOpened) {
            backgroundHandler?.post {
                closePreviewSession()
                safeStopVideoRecorder()
                releaseVideoRecorder()
                videoRecording = false
                notifyVideoRecordStop(videoOutFile!!)
                if (voiceEnabled) {
                    mediaActionSound?.play(MediaActionSound.STOP_VIDEO_RECORDING)
                }
                createPreviewSession()
            }
        }
    }

    override fun resumePreview() {
        if (isCameraOpened) {
            unlockFocus()
        }
    }

    override fun closeCamera(cameraCloseListener: CameraCloseListener?) {
        super.closeCamera(cameraCloseListener)
        if (isCameraOpened) {
            cameraDevice?.close()
            cameraDevice = null
        }
        closePreviewSession()
        //        releaseTexture();
        closeImageReader()
        releaseVideoRecorder()
        releaseCameraInternal()
        uiHandler.removeCallbacksAndMessages(null)
        backgroundHandler?.removeCallbacksAndMessages(null)
        notifyCameraClosed()
    }

    override fun onImageAvailable(reader: ImageReader) {
        try {
            try {
                reader.acquireNextImage().use { image ->
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer[bytes]
                    handlePictureTakenResult(bytes)
                    notifyCameraPictureTaken(bytes)
                }
            } catch (e: IllegalStateException) {
                e("Camera2Manager", "onImageAvailable error$e")
                notifyCameraCaptureFailed(e)
            }
        } catch (e: Exception) {
            e("Camera2Manager", "onImageAvailable error $e")
            notifyCameraCaptureFailed(e)
        } finally {
            unlockFocus()
        }
    }

    /*---------------------------------inner methods------------------------------------*/

    /**
     * This method cost more but little time (10ms) to finish, we could get the params here
     * before initialize camera information here to accelerate the camera launch. For
     * example get them from [ConfigurationProvider.get] and then use theme here.
     *
     * @param context context
     */
    private fun initCameraInfo(context: Context) {
        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val start = System.currentTimeMillis()
        try {
            numberOfCameras = ConfigurationProvider.get().getNumberOfCameras(context)
            frontCameraId = ConfigurationProvider.get().getCameraId(context, CameraFace.FACE_FRONT)
            rearCameraId = ConfigurationProvider.get().getCameraId(context, CameraFace.FACE_REAR)
            frontCameraOrientation = ConfigurationProvider.get().getCameraOrientation(context, CameraFace.FACE_FRONT)
            rearCameraOrientation = ConfigurationProvider.get().getCameraOrientation(context, CameraFace.FACE_REAR)
            frontCameraCharacteristics = ConfigurationProvider.get().getCameraCharacteristics(context, CameraFace.FACE_FRONT)
            rearCameraCharacteristics = ConfigurationProvider.get().getCameraCharacteristics(context, CameraFace.FACE_REAR)
        } catch (e: Exception) {
            e("Camera2Manager", "initCameraInfo error $e")
        }
        d("Camera2Manager", "initCameraInfo basic cost : " + (System.currentTimeMillis() - start) + " ms")
        currentCameraId = if (cameraFace == CameraFace.FACE_REAR) rearCameraId else frontCameraId
    }

    private fun prepareCameraOutputs() {
        val isFrontCamera = cameraFace == CameraFace.FACE_REAR
        var start = System.currentTimeMillis()
        try {
            if (isFrontCamera && frontStreamConfigurationMap == null) {
                frontStreamConfigurationMap = ConfigurationProvider.get()
                    .getStreamConfigurationMap(context, CameraFace.FACE_REAR)
            } else if (!isFrontCamera && rearStreamConfigurationMap == null) {
                rearStreamConfigurationMap = ConfigurationProvider.get()
                    .getStreamConfigurationMap(context, CameraFace.FACE_FRONT)
            }
        } catch (ex: Exception) {
            e("Camera2Manager", "initCameraInfo error $ex")
            notifyCameraOpenError(RuntimeException(ex))
        }
        d("Camera2Manager", "initCameraInfo get map cost : " + (System.currentTimeMillis() - start) + " ms")
        start = System.currentTimeMillis()
        try {
            val map = if (isFrontCamera) frontStreamConfigurationMap else rearStreamConfigurationMap
            previewSizes = ConfigurationProvider.get().getSizes(map!!, cameraFace, CameraSizeFor.SIZE_FOR_PREVIEW)
            pictureSizes = ConfigurationProvider.get().getSizes(map, cameraFace, CameraSizeFor.SIZE_FOR_PICTURE)
            videoSizes = ConfigurationProvider.get().getSizes(map, cameraFace, CameraSizeFor.SIZE_FOR_VIDEO)
            ConfigurationProvider.get().cameraSizeCalculator.init(
                expectedRatio,
                expectedSize,
                expectedQuality,
                previewSizes?: emptyList(),
                pictureSizes?: emptyList(),
                videoSizes?: emptyList()
            )
        } catch (ex: Exception) {
            e("Camera2Manager", "error : $ex")
            notifyCameraOpenError(RuntimeException(ex))
        }
        d("Camera2Manager", "prepareCameraOutputs cost : " + (System.currentTimeMillis() - start) + " ms")
    }

    private fun adjustCameraConfiguration(forceCalculate: Boolean) {
        val oldPreviewSize = previewSize
        val calculator = ConfigurationProvider.get().cameraSizeCalculator
        if (pictureSize == null || forceCalculate) {
            pictureSize = calculator.getPictureSize(CameraType.TYPE_CAMERA2)
            previewSize = calculator.getPicturePreviewSize(CameraType.TYPE_CAMERA2)
            notifyPictureSizeUpdated(pictureSize!!)

            // fix: CaptureRequest contains un-configured Input/Output Surface!
            imageReader = ImageReader.newInstance(
                pictureSize!!.width,
                pictureSize!!.height,
                ImageFormat.JPEG,  /*maxImages*/
                2
            )
            imageReader!!.setOnImageAvailableListener(this, backgroundHandler)
            previewReader = ImageReader.newInstance(
                previewSize!!.width,
                previewSize!!.height,
                ImageFormat.YUV_420_888,
                2
            )
            previewReader!!.setOnImageAvailableListener(
                onPreviewImageAvailableListener,
                backgroundHandler
            )
        }
        // fixed 2020-08-29 : the video size might be null if quickly switched
        // from media types while first time launch the camera.
        if (videoSize == null || forceCalculate) {
            camcorderProfile = CameraHelper.getCamcorderProfile(expectedQuality, currentCameraId!!)
            videoSize = calculator.getVideoSize(CameraType.TYPE_CAMERA2)
            previewSize = calculator.getVideoPreviewSize(CameraType.TYPE_CAMERA2)
            videoSize?.let { notifyVideoSizeUpdated(it) }
        }
        d("Camera2Manager", "previewSize: $previewSize oldPreviewSize:$oldPreviewSize")
        if (previewSize != oldPreviewSize) {
            previewSize?.let { notifyPreviewSizeUpdated(it) }
        }
    }

    private fun createPreviewSession() {
        try {
            val sessionCreationTask = Runnable {
                try {
                    previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    previewRequestBuilder?.addTarget(workingSurface!!)
                    previewRequestBuilder?.addTarget(previewReader!!.surface)
                    cameraDevice!!.createCaptureSession(
                        listOf(workingSurface, imageReader!!.surface, previewReader!!.surface),
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                                if (isCameraOpened) {
                                    captureSession = cameraCaptureSession
                                    setFlashModeInternal()
                                    previewRequest = previewRequestBuilder!!.build()
                                    try {
                                        captureSession!!.setRepeatingRequest(
                                            previewRequest!!,
                                            captureSessionCallback,
                                            backgroundHandler
                                        )
                                    } catch (ex: Exception) {
                                        e("Camera2Manager", "create preview session error $ex")
                                        notifyCameraOpenError(ex)
                                    }
                                }
                            }

                            override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                                d("Camera2Manager", "onConfigureFailed")
                                notifyCameraOpenError(Throwable("Camera capture session configure failed."))
                            }
                        }, backgroundHandler
                    )
                } catch (ex: Exception) {
                    e("Camera2Manager", "createPreviewSession error $ex")
                    notifyCameraOpenError(ex)
                }
            }
            if (cameraPreview.previewType == PreviewViewType.TEXTURE_VIEW) {
                surfaceTexture = cameraPreview.surfaceTexture
                surfaceTexture!!.setDefaultBufferSize(previewSize!!.width, previewSize!!.height)
                workingSurface = cameraPreview.surface
                sessionCreationTask.run()
            } else if (cameraPreview.previewType == PreviewViewType.SURFACE_VIEW) {
                // only ui thread can touch surface view
                uiHandler.post {
                    surfaceHolder = cameraPreview.surfaceHolder
                    surfaceHolder!!.setFixedSize(previewSize!!.width, previewSize!!.height)
                    workingSurface = cameraPreview.surface
                    // keep the initialization procedure
                    sessionCreationTask.run()
                }
            }
        } catch (ex: Exception) {
            e("Camera2Manager", "createPreviewSession error $ex")
            notifyCameraOpenError(ex)
        }
    }

    private fun captureStillPicture() {
        if (isCameraOpened) {
            try {
                val cameraCharacteristics = if (cameraFace == CameraFace.FACE_FRONT)
                    frontCameraCharacteristics else rearCameraCharacteristics
                val captureBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                captureBuilder.addTarget(imageReader!!.surface)
                captureBuilder.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
                captureBuilder.set(
                    CaptureRequest.JPEG_ORIENTATION,
                    getJpegOrientation(cameraCharacteristics!!, displayOrientation)
                )

                // calculate zoom for capture request
                setZoomInternal(captureBuilder)
                captureSession?.stopRepeating()
                captureSession?.capture(captureBuilder.build(), object : CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        d("Camera2Manager", "onCaptureCompleted: ")
                    }
                }, null)
            } catch (e: Exception) {
                e("Camera2Manager", "Error during capturing picture")
                notifyCameraCaptureFailed(e)
            }
        } else {
            notifyCameraCaptureFailed(RuntimeException("Camera not open."))
        }
    }

    private fun createRecordSession() {
        val sessionCreationTask = Runnable {
            try {
                val surfaces: MutableList<Surface> = ArrayList()
                previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
                val previewSurface = workingSurface
                surfaces.add(previewSurface!!)
                previewRequestBuilder?.addTarget(previewSurface)
                workingSurface = videoRecorder!!.surface
                surfaces.add(workingSurface!!)
                previewRequestBuilder?.addTarget(workingSurface!!)
                cameraDevice?.createCaptureSession(
                    surfaces,
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                            captureSession = cameraCaptureSession
                            previewRequestBuilder?.set(
                                CaptureRequest.CONTROL_MODE,
                                CameraMetadata.CONTROL_MODE_AUTO
                            )
                            try {
                                captureSession?.setRepeatingRequest(
                                    previewRequestBuilder!!.build(),
                                    null,
                                    backgroundHandler
                                )
                            } catch (e: Exception) {
                                e("Camera2Manager", "videoRecorder.start(): $e")
                                notifyVideoRecordError(e)
                            }
                            try {
                                videoRecorder?.start()
                                videoRecording = true
                                notifyVideoRecordStart()
                            } catch (e: Exception) {
                                e("Camera2Manager", "videoRecorder.start(): $e")
                                notifyVideoRecordError(e)
                            }
                        }

                        override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                            i("Camera2Manager", "onConfigureFailed")
                            notifyVideoRecordError(RuntimeException("Video record configure failed."))
                        }
                    },
                    backgroundHandler
                )
            } catch (e: Exception) {
                e("Camera2Manager", "startVideoRecord: $e")
                notifyVideoRecordError(e)
            }
        }
        if (cameraPreview.previewType == PreviewViewType.TEXTURE_VIEW) {
            val texture = surfaceTexture
            texture?.setDefaultBufferSize(videoSize!!.width, videoSize!!.height)
            sessionCreationTask.run()
        } else if (cameraPreview.previewType == PreviewViewType.SURFACE_VIEW) {
            uiHandler.post {
                val holder = surfaceHolder
                holder?.setFixedSize(previewSize!!.width, previewSize!!.height)
                sessionCreationTask.run()
            }
        }
    }

    private fun runPreCaptureSequence() {
        try {
            previewRequestBuilder?.set(
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START
            )
            captureSessionCallback.setCameraPreviewState(CameraState.STATE_WAITING_PRE_CAPTURE)
            captureSession?.capture(
                previewRequestBuilder!!.build(),
                captureSessionCallback,
                backgroundHandler
            )
        } catch (e: CameraAccessException) {
            e("Camera2Manager", "runPreCaptureSequence error $e")
        }
    }

    private fun setFlashModeInternal(): Boolean {
        try {
            val cameraCharacteristics = if (cameraFace == CameraFace.FACE_FRONT)
                frontCameraCharacteristics else rearCameraCharacteristics
            val isFlashAvailable = cameraCharacteristics?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
            if (isFlashAvailable == null || !isFlashAvailable) {
                i("Camera2Manager", "Flash is not available.")
                return false
            }
            when (flashMode) {
                FlashMode.FLASH_ON -> {
                    previewRequestBuilder?.set(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON
                    )
                    previewRequestBuilder?.set(
                        CaptureRequest.FLASH_MODE,
                        CameraMetadata.FLASH_MODE_SINGLE
                    )
                }
                FlashMode.FLASH_OFF -> {
                    previewRequestBuilder?.set(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON
                    )
                    previewRequestBuilder?.set(
                        CaptureRequest.FLASH_MODE,
                        CameraMetadata.FLASH_MODE_OFF
                    )
                }
                FlashMode.FLASH_AUTO -> {
                    previewRequestBuilder?.set(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                    )
                    previewRequestBuilder?.set(
                        CaptureRequest.FLASH_MODE,
                        CameraMetadata.FLASH_MODE_SINGLE
                    )
                }
                else -> {
                    previewRequestBuilder?.set(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                    )
                    previewRequestBuilder?.set(
                        CaptureRequest.FLASH_MODE,
                        CameraMetadata.FLASH_MODE_SINGLE
                    )
                }
            }
            return true
        } catch (ex: Exception) {
            e("Camera2Manager", "setFlashMode error : $ex")
        }
        return false
    }

    private fun setAutoFocusInternal() {
        if (isAutoFocus) {
            val cameraCharacteristics = if (cameraFace == CameraFace.FACE_FRONT)
                frontCameraCharacteristics else rearCameraCharacteristics
            val modes = cameraCharacteristics?.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
            if (modes == null || modes.isEmpty() ||
                modes.size == 1 && modes[0] == CameraCharacteristics.CONTROL_AF_MODE_OFF
            ) {
                isAutoFocus = false
                previewRequestBuilder?.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_OFF
                )
            } else if (mediaType == MediaType.TYPE_PICTURE) {
                previewRequestBuilder?.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
            } else if (mediaType == MediaType.TYPE_VIDEO) {
                previewRequestBuilder?.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
                )
            }
        } else {
            previewRequestBuilder?.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_OFF
            )
        }
    }

    private fun setZoomInternal(builder: CaptureRequest.Builder?): Boolean {
        val maxZoom = getMaxZoom()
        if (maxZoom == 1.0f || builder == null) {
            return false
        }
        val cameraCharacteristics = if (cameraFace == CameraFace.FACE_FRONT)
            frontCameraCharacteristics else rearCameraCharacteristics
        val rect = cameraCharacteristics?.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE) ?: return false
        zoom = zoom.coerceAtLeast(1f).coerceAtMost(maxZoom)
        val cropW = (rect.width() - (rect.width().toFloat() / zoom).toInt()) / 2
        val cropH = (rect.height() - (rect.height().toFloat() / zoom).toInt()) / 2
        val zoomRect = Rect(cropW, cropH, rect.width() - cropW, rect.height() - cropH)
        builder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect)
        return true
    }

    private fun lockFocus() {
        try {
            previewRequestBuilder?.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
            captureSessionCallback.setCameraPreviewState(CameraState.STATE_WAITING_LOCK)
            captureSession?.capture(
                previewRequestBuilder!!.build(),
                captureSessionCallback,
                backgroundHandler
            )
        } catch (e: Exception) {
            e("Camera2Manager", "lockFocus : error during focus locking")
        }
    }

    private fun unlockFocus() {
        try {
            previewRequestBuilder?.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL)
            captureSession?.capture(previewRequestBuilder!!.build(), captureSessionCallback, backgroundHandler)
            captureSessionCallback.setCameraPreviewState(CameraState.STATE_PREVIEW)
            captureSession?.setRepeatingRequest(previewRequest!!, captureSessionCallback, backgroundHandler)
        } catch (e: Exception) {
            e("Camera2Manager", "unlockFocus : error during focus unlocking")
        }
    }

    private fun prepareVideoRecorder(): Boolean {
        videoRecorder = MediaRecorder()
        try {
            videoRecorder?.setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
            videoRecorder?.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            videoRecorder?.setOutputFormat(camcorderProfile!!.fileFormat)
            videoRecorder?.setVideoFrameRate(camcorderProfile!!.videoFrameRate)
            videoRecorder?.setVideoSize(videoSize!!.width, videoSize!!.height)
            videoRecorder?.setVideoEncodingBitRate(camcorderProfile!!.videoBitRate)
            videoRecorder?.setVideoEncoder(camcorderProfile!!.videoCodec)
            videoRecorder?.setAudioEncodingBitRate(camcorderProfile!!.audioBitRate)
            videoRecorder?.setAudioChannels(camcorderProfile!!.audioChannels)
            videoRecorder?.setAudioSamplingRate(camcorderProfile!!.audioSampleRate)
            videoRecorder?.setAudioEncoder(camcorderProfile!!.audioCodec)
            videoRecorder?.setOutputFile(videoOutFile.toString())
            if (videoFileSize > 0) {
                videoRecorder?.setMaxFileSize(videoFileSize)
                videoRecorder?.setOnInfoListener(this)
            }
            if (videoDuration > 0) {
                videoRecorder?.setMaxDuration(videoDuration)
                videoRecorder?.setOnInfoListener(this)
            }
            val cameraCharacteristics = if (cameraFace == CameraFace.FACE_FRONT)
                frontCameraCharacteristics else rearCameraCharacteristics
            videoRecorder?.setOrientationHint(getJpegOrientation(cameraCharacteristics!!, displayOrientation))
            videoRecorder?.setPreviewDisplay(cameraPreview.surface)
            videoRecorder?.prepare()
            return true
        } catch (error: IllegalStateException) {
            e("Camera2Manager", "IllegalStateException preparing MediaRecorder: " + error.message)
            notifyVideoRecordError(error)
        } catch (error: IOException) {
            e("Camera2Manager", "IOException preparing MediaRecorder: " + error.message)
            notifyVideoRecordError(error)
        } catch (error: Exception) {
            e("Camera2Manager", "Error during preparing MediaRecorder: " + error.message)
            notifyVideoRecordError(error)
        }
        releaseVideoRecorder()
        return false
    }

    private fun closePreviewSession() {
        if (captureSession != null) {
            captureSession?.close()
            try {
                captureSession?.abortCaptures()
            } catch (e: Exception) {
                e("Camera2Manager", "closePreviewSession error : $e")
            } finally {
                captureSession = null
            }
        }
    }

    private fun releaseCameraInternal() {
        previewSize = null
        pictureSize = null
        videoSize = null
        maxZoomValue = 0f
    }

    private fun releaseTexture() {
        surfaceTexture?.release()
        surfaceTexture = null
    }

    private fun closeImageReader() {
        imageReader?.close()
        imageReader = null
        previewReader?.close()
        previewReader = null
    }

    internal abstract class CaptureSessionCallback : CaptureCallback() {

        @CameraState
        private var cameraPreviewState = 0

        fun setCameraPreviewState(@CameraState cameraPreviewState: Int) {
            this.cameraPreviewState = cameraPreviewState
        }

        abstract fun processCaptureResult(result: CaptureResult, @CameraState cameraPreviewState: Int)

        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {
            processCaptureResult(partialResult, cameraPreviewState)
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            processCaptureResult(result, cameraPreviewState)
        }

        @IntDef(CameraState.STATE_PREVIEW,
            CameraState.STATE_WAITING_LOCK,
            CameraState.STATE_WAITING_PRE_CAPTURE,
            CameraState.STATE_WAITING_NON_PRE_CAPTURE,
            CameraState.STATE_PICTURE_TAKEN)
        @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
        internal annotation class CameraState {
            companion object {
                /** Camera state: Showing camera preview.  */
                const val STATE_PREVIEW = 0

                /** Camera state: Waiting for the focus to be locked.  */
                const val STATE_WAITING_LOCK = 1

                /** Camera state: Waiting for the exposure to be precapture state.  */
                const val STATE_WAITING_PRE_CAPTURE = 2

                /** Camera state: Waiting for the exposure state to be something other than precapture.  */
                const val STATE_WAITING_NON_PRE_CAPTURE = 3

                /** Camera state: Picture was taken.  */
                const val STATE_PICTURE_TAKEN = 4
            }
        }
    }

    init {
        cameraPreview.setCameraPreviewCallback(object : CameraPreviewCallback {
            override fun onAvailable(cameraPreview: CameraPreview) {
                if (isCameraOpened) {
                    createPreviewSession()
                }
            }
        })
    }
}