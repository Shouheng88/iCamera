package me.shouheng.icamera.manager

import android.content.Context
import me.shouheng.icamera.config.size.AspectRatio
import me.shouheng.icamera.config.size.Size
import me.shouheng.icamera.config.size.SizeMap
import me.shouheng.icamera.enums.*
import me.shouheng.icamera.listener.*
import java.io.File

/**
 * The camera manager interface.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:49
 */
interface CameraManager {

    /** Initialize camera manager. */
    fun initialize(context: Context)

    /** Open camera. */
    fun openCamera(cameraOpenListener: CameraOpenListener?)

    /** Is camera opened */
    val isCameraOpened: Boolean

    /** Get current camera face */
    @get:CameraFace
    var cameraFace: Int

    /** Switch camera face */
    fun switchCamera(@CameraFace cameraFace: Int)

    /** Get current media type */
    @get:MediaType
    var mediaType: Int

    /** Is voice enabled */
    var isVoiceEnable: Boolean

    /** Is current auto focus */
    var isAutoFocus: Boolean

    /** Get current flash mode */
    @get:FlashMode
    var flashMode: Int

    /** Current zoom */
    var zoom: Float

    /** Get max zoom */
    fun getMaxZoom(): Float

    /** Set desired size */
    fun setExpectSize(expectSize: Size)

    /** Sed desired aspect ratio.*/
    fun setExpectAspectRatio(expectAspectRatio: AspectRatio)

    /** Set desired media quality */
    fun setMediaQuality(@MediaQuality mediaQuality: Int)

    /** Get current size for usage. */
    fun getSize(@CameraSizeFor sizeFor: Int): Size?

    /** Get sizes map from aspect ratio to sizes.*/
    fun getSizes(@CameraSizeFor sizeFor: Int): SizeMap?

    /** Get current aspect ratio of preview, picture or video. */
    fun getAspectRatio(@CameraSizeFor sizeFor: Int): AspectRatio?

    /** Set display orientation */
    var displayOrientation: Int

    /** Add camera size change callback */
    fun addCameraSizeListener(cameraSizeListener: CameraSizeListener)

    /** Set the camera preview listener */
    fun setCameraPreviewListener(cameraPreviewListener: CameraPreviewListener)

    /** Take a picture */
    fun takePicture(fileToSave: File, cameraPhotoListener: CameraPhotoListener?)

    /** Get max video file size */
    var videoFileSize: Long

    /** Get max video duration */
    var videoDuration: Int

    /** Start video record */
    fun startVideoRecord(file: File, cameraVideoListener: CameraVideoListener?)

    /** Stop video record */
    fun stopVideoRecord()

    /** Resume preview */
    fun resumePreview()

    /** Close camera */
    fun closeCamera(cameraCloseListener: CameraCloseListener?)

    /** Release camera, destroy thread etc. */
    fun releaseCamera()
}