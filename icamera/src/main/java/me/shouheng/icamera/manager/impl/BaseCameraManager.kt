package me.shouheng.icamera.manager.impl

import android.content.Context
import android.media.CamcorderProfile
import android.media.MediaActionSound
import android.media.MediaRecorder
import android.os.*
import me.shouheng.icamera.config.ConfigurationProvider
import me.shouheng.icamera.config.size.AspectRatio
import me.shouheng.icamera.config.size.Size
import me.shouheng.icamera.config.size.SizeMap
import me.shouheng.icamera.enums.*
import me.shouheng.icamera.listener.*
import me.shouheng.icamera.manager.CameraManager
import me.shouheng.icamera.preview.CameraPreview
import me.shouheng.icamera.util.XLog
import me.shouheng.icamera.util.XLog.e
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

/**
 * Base camera manager, the template class for camera1 and camera2 implementation.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:50
 */
abstract class BaseCameraManager<CameraId>(var cameraPreview: CameraPreview) : CameraManager, MediaRecorder.OnInfoListener {

    protected lateinit var context: Context

    @MediaType
    override var mediaType: Int = ConfigurationProvider.get().defaultMediaType
    @CameraFace
    override var cameraFace: Int = ConfigurationProvider.get().defaultCameraFace
    @MediaQuality
    var expectedQuality: Int = ConfigurationProvider.get().defaultMediaQuality

    var numberOfCameras = 0
    var rearCameraId: CameraId? = null
    var frontCameraId: CameraId? = null
    var currentCameraId: CameraId? = null
    var rearCameraOrientation = 0
    var frontCameraOrientation = 0
    var previewSizes: List<Size>? = null
    var pictureSizes: List<Size>? = null
    var videoSizes: List<Size>? = null
    var expectedRatio: AspectRatio = ConfigurationProvider.get().defaultAspectRatio
    var previewSizeMap: SizeMap? = null
    var pictureSizeMap: SizeMap? = null
    var videoSizeMap: SizeMap? = null
    var expectedSize: Size? = null
    var previewSize: Size? = null
    var pictureSize: Size? = null
    var videoSize: Size? = null
    var camcorderProfile: CamcorderProfile? = null
    var pictureFile: File? = null
    var videoOutFile: File? = null
    var videoRecorder: MediaRecorder? = null
    var mediaActionSound: MediaActionSound? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            MediaActionSound()
        } else null
    var voiceEnabled: Boolean = ConfigurationProvider.get().isVoiceEnable

    override var isAutoFocus: Boolean = ConfigurationProvider.get().isAutoFocus

    @FlashMode
    override var flashMode: Int = ConfigurationProvider.get().defaultFlashMode
    override var zoom = 1.0f
    var maxZoomValue = 0f
    override var displayOrientation = 0
    override var videoFileSize: Long = ConfigurationProvider.get().defaultVideoFileSize
    override var videoDuration: Int = ConfigurationProvider.get().defaultVideoDuration

    var cameraOpenListener: CameraOpenListener? = null
    var cameraCloseListener: CameraCloseListener? = null
    var previewListener: CameraPreviewListener? = null
    private var cameraPhotoListener: CameraPhotoListener? = null
    private var cameraVideoListener: CameraVideoListener? = null
    private val cameraSizeListeners: MutableList<CameraSizeListener> = mutableListOf()

    @Volatile
    var takingPicture = false
    @Volatile
    var videoRecording = false

    private var backgroundThread: HandlerThread? = null
    var backgroundHandler: Handler? = null
    var uiHandler = Handler(Looper.getMainLooper())

    override fun initialize(context: Context) {
        this.context = context
        startBackgroundThread()
    }

    override fun openCamera(cameraOpenListener: CameraOpenListener?) {
        this.cameraOpenListener = cameraOpenListener
    }

    override fun switchCamera(cameraFace: Int) {
        if (cameraFace == this.cameraFace) return
        this.cameraFace = cameraFace
        currentCameraId = if (cameraFace == CameraFace.FACE_FRONT) frontCameraId else rearCameraId
    }

    override fun getMaxZoom(): Float = maxZoomValue

    override fun setExpectSize(expectSize: Size) {
        if (expectSize == this.expectedSize) return
        this.expectedSize = expectSize
        expectedRatio = AspectRatio.of(this.expectedSize!!)
        ConfigurationProvider.get().cameraSizeCalculator.changeExpectSize(expectSize)
        ConfigurationProvider.get().cameraSizeCalculator.changeExpectAspectRatio(expectedRatio)
    }

    override fun setExpectAspectRatio(expectAspectRatio: AspectRatio) {
        if (this.expectedRatio == expectAspectRatio) return
        this.expectedRatio = expectAspectRatio
        ConfigurationProvider.get().cameraSizeCalculator.changeExpectAspectRatio(expectAspectRatio)
    }

    override fun setMediaQuality(@MediaQuality mediaQuality: Int) {
        if (this.expectedQuality == mediaQuality) return
        this.expectedQuality = mediaQuality
        ConfigurationProvider.get().cameraSizeCalculator.changeMediaQuality(mediaQuality)
    }

    override fun getAspectRatio(@CameraSizeFor sizeFor: Int): AspectRatio? {
        return when (sizeFor) {
            CameraSizeFor.SIZE_FOR_PICTURE -> if (pictureSize == null) null else AspectRatio.of(pictureSize!!)
            CameraSizeFor.SIZE_FOR_VIDEO -> if (videoSize == null) null else AspectRatio.of(videoSize!!)
            CameraSizeFor.SIZE_FOR_PREVIEW -> if (previewSize == null) null else AspectRatio.of(previewSize!!)
            else -> if (previewSize == null) null else AspectRatio.of(previewSize!!)
        }
    }

    override fun addCameraSizeListener(cameraSizeListener: CameraSizeListener) {
        cameraSizeListeners.add(cameraSizeListener)
    }

    override fun setCameraPreviewListener(cameraPreviewListener: CameraPreviewListener) {
        this.previewListener = cameraPreviewListener
    }

    override fun takePicture(fileToSave: File, cameraPhotoListener: CameraPhotoListener?) {
        pictureFile = fileToSave
        this.cameraPhotoListener = cameraPhotoListener
    }

    override fun startVideoRecord(file: File, cameraVideoListener: CameraVideoListener?) {
        videoOutFile = file
        this.cameraVideoListener = cameraVideoListener
    }

    override fun releaseCamera() {
        stopBackgroundThread()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mediaActionSound?.release()
        }
    }

    override fun closeCamera(cameraCloseListener: CameraCloseListener?) {
        this.cameraCloseListener = cameraCloseListener
    }

    override fun onInfo(mr: MediaRecorder, what: Int, extra: Int) {
        if (MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED == what) {
            onMaxDurationReached()
        } else if (MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED == what) {
            onMaxFileSizeReached()
        }
    }

    /*----------------------------------- Protected Methods Region -----------------------------------*/
    fun handlePictureTakenResult(bytes: ByteArray) {
        if (pictureFile == null) {
            notifyCameraCaptureFailed(RuntimeException("Error creating media file, check storage permissions."))
            XLog.d("BaseCameraManager", "Error creating media file, check storage permissions.")
            return
        }
        // do write
        try {
            val fos = FileOutputStream(pictureFile)
            fos.write(bytes)
            fos.close()
        } catch (error: FileNotFoundException) {
            e("BaseCameraManager", "File not found: " + error.message)
            notifyCameraCaptureFailed(RuntimeException("File not found: " + error.message))
        } catch (error: IOException) {
            e("BaseCameraManager", "Error accessing file: " + error.message)
            notifyCameraCaptureFailed(RuntimeException("Error accessing file: " + error.message))
        } catch (error: Throwable) {
            e("BaseCameraManager", "Error saving file: " + error.message)
            notifyCameraCaptureFailed(RuntimeException("Error saving file: " + error.message))
        }
    }

    fun notifyCameraOpened() {
        uiHandler.post { cameraOpenListener?.onCameraOpened(cameraFace) }
    }

    fun notifyCameraOpenError(throwable: Throwable) {
        uiHandler.post { cameraOpenListener?.onCameraOpenError(throwable) }
    }

    fun notifyCameraPictureTaken(data: ByteArray) {
        uiHandler.post { cameraPhotoListener?.onPictureTaken(data, pictureFile!!) }
    }

    fun notifyCameraCaptureFailed(throwable: Throwable) {
        uiHandler.post { cameraPhotoListener?.onCaptureFailed(throwable) }
    }

    fun notifyVideoRecordStart() {
        uiHandler.post { cameraVideoListener?.onVideoRecordStart() }
    }

    fun notifyVideoRecordStop(file: File) {
        uiHandler.post { cameraVideoListener?.onVideoRecordStop(file) }
    }

    fun notifyVideoRecordError(throwable: Throwable) {
        uiHandler.post { cameraVideoListener?.onVideoRecordError(throwable) }
    }

    fun safeStopVideoRecorder() {
        try {
            videoRecorder?.stop()
        } catch (ex: Exception) {
            notifyVideoRecordError(RuntimeException(ex))
        }
    }

    fun releaseVideoRecorder() {
        try {
            videoRecorder?.reset()
            videoRecorder?.release()
        } catch (ex: Exception) {
            notifyVideoRecordError(RuntimeException(ex))
        } finally {
            videoRecorder = null
        }
    }

    fun notifyPreviewSizeUpdated(previewSize: Size) {
        uiHandler.post {
            cameraSizeListeners.forEach {
                it.onPreviewSizeUpdated(previewSize)
            }
        }
    }

    fun notifyPictureSizeUpdated(pictureSize: Size) {
        uiHandler.post {
            cameraSizeListeners.forEach {
                it.onPictureSizeUpdated(pictureSize)
            }
        }
    }

    fun notifyVideoSizeUpdated(videoSize: Size) {
        uiHandler.post {
            cameraSizeListeners.forEach {
                it.onVideoSizeUpdated(videoSize)
            }
        }
    }

    fun notifyPreviewFrameChanged(data: ByteArray, size: Size, format: Int) {
        uiHandler.post { previewListener?.onPreviewFrame(data, size, format) }
    }

    fun notifyCameraClosed() {
        uiHandler.post { cameraCloseListener?.onCameraClosed(cameraFace) }
    }

    /*----------------------------------- Private Methods Region -----------------------------------*/
    private fun onMaxDurationReached() {
        stopVideoRecord()
    }

    private fun onMaxFileSizeReached() {
        stopVideoRecord()
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("BaseCameraManager", Process.THREAD_PRIORITY_BACKGROUND)
        backgroundThread!!.start()
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        if (Build.VERSION.SDK_INT > 17) {
            backgroundThread?.quitSafely()
        } else {
            backgroundThread?.quit()
        }
        try {
            backgroundThread?.join()
        } catch (e: InterruptedException) {
            XLog.e("BaseCameraManager", "stopBackgroundThread: $e")
        } finally {
            backgroundThread = null
            backgroundHandler = null
        }
    }
}