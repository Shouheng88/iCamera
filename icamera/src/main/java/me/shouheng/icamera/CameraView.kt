package me.shouheng.icamera

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.support.annotation.IntRange
import android.support.annotation.RequiresApi
import android.support.v4.view.ViewCompat
import android.text.TextUtils
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import me.shouheng.icamera.config.ConfigurationProvider
import me.shouheng.icamera.config.size.AspectRatio
import me.shouheng.icamera.config.size.Size
import me.shouheng.icamera.config.size.SizeMap
import me.shouheng.icamera.enums.*
import me.shouheng.icamera.listener.*
import me.shouheng.icamera.manager.CameraManager
import me.shouheng.icamera.preview.CameraPreview
import me.shouheng.icamera.util.CameraHelper.getDeviceDefaultOrientation
import me.shouheng.icamera.util.XLog.d
import me.shouheng.icamera.widget.FocusMarkerLayout
import java.io.File
import java.util.*

/**
 * It's over :D
 *
 * ___   _______  _______  __   __  _______  ______    _______
 * |   | |       ||   _   ||  |_|  ||       ||    _ |  |   _   |
 * |   | |       ||  |_|  ||       ||    ___||   | ||  |  |_|  |
 * |   | |       ||       ||       ||   |___ |   |_||_ |       |
 * |   | |      _||       ||       ||    ___||    __  ||       |
 * |   | |     |_ |   _   || ||_|| ||   |___ |   |  | ||   _   |
 * |___| |_______||__| |__||_|   |_||_______||___|  |_||__| |__|
 *
 * = WngShhng ==
 *
 * THE ADVANCED CAMERA LIBRARY FOR ANDROID.
 * '
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:43
 */
class CameraView : FrameLayout {
    private var cameraManager: CameraManager? = null
    private var cameraPreview: CameraPreview? = null
    private var clipScreen = false
    private var adjustViewBounds = false

    @PreviewAdjustType
    private var adjustType = PreviewAdjustType.NONE
    private var aspectRatio: AspectRatio? = null
    private var focusMarkerLayout: FocusMarkerLayout? = null
    private var displayOrientationDetector: DisplayOrientationDetector? = null
    private var sensorManager: SensorManager? = null
    private val orientationChangedListeners: MutableList<OnOrientationChangedListener> = ArrayList()
    private val sensorEventListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(sensorEvent: SensorEvent) {
            synchronized(this) {
                if (sensorEvent.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    if (sensorEvent.values[0] < 4 && sensorEvent.values[0] > -4) {
                        if (sensorEvent.values[1] > 0) {
                            // UP
                            ConfigurationProvider.get().sensorPosition =
                                SensorPosition.SENSOR_POSITION_UP
                            ConfigurationProvider.get().degrees =
                                if (ConfigurationProvider.get().deviceDefaultOrientation
                                    == DeviceDefaultOrientation.ORIENTATION_PORTRAIT
                                ) 0 else 90
                        } else if (sensorEvent.values[1] < 0) {
                            // UP SIDE DOWN
                            ConfigurationProvider.get().sensorPosition =
                                SensorPosition.SENSOR_POSITION_UP_SIDE_DOWN
                            ConfigurationProvider.get().degrees =
                                if (ConfigurationProvider.get().deviceDefaultOrientation
                                    == DeviceDefaultOrientation.ORIENTATION_PORTRAIT
                                ) 180 else 270
                        }
                    } else if (sensorEvent.values[1] < 4 && sensorEvent.values[1] > -4) {
                        if (sensorEvent.values[0] > 0) {
                            // LEFT
                            ConfigurationProvider.get().sensorPosition =
                                SensorPosition.SENSOR_POSITION_LEFT
                            ConfigurationProvider.get().degrees =
                                if (ConfigurationProvider.get().deviceDefaultOrientation
                                    == DeviceDefaultOrientation.ORIENTATION_PORTRAIT
                                ) 90 else 180
                        } else if (sensorEvent.values[0] < 0) {
                            // RIGHT
                            ConfigurationProvider.get().sensorPosition =
                                SensorPosition.SENSOR_POSITION_RIGHT
                            ConfigurationProvider.get().degrees =
                                if (ConfigurationProvider.get().deviceDefaultOrientation
                                    == DeviceDefaultOrientation.ORIENTATION_PORTRAIT
                                ) 270 else 0
                        }
                    }
                    // notify screen orientation changed
                    if (orientationChangedListeners.isNotEmpty()) {
                        for (listener in orientationChangedListeners) {
                            listener.onOrientationChanged(ConfigurationProvider.get().degrees)
                        }
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, i: Int) { /*noop*/ }
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : super(context, attrs, defStyleAttr) {
        initCameraView(context, attrs, defStyleAttr, 0)
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        initCameraView(context, attrs, defStyleAttr, defStyleRes)
    }

    private fun initCameraView(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) {
        // set the device default orientation and register orientation change sensor events
        val defaultOrientation = getDeviceDefaultOrientation(getContext())
        if (defaultOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            ConfigurationProvider.get().deviceDefaultOrientation = DeviceDefaultOrientation.ORIENTATION_LANDSCAPE
        } else {
            ConfigurationProvider.get().deviceDefaultOrientation = DeviceDefaultOrientation.ORIENTATION_PORTRAIT
        }
        sensorManager = getContext().getSystemService(Activity.SENSOR_SERVICE) as SensorManager
        sensorManager?.registerListener(
            sensorEventListener,
            sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )
        // initialize camera
        cameraPreview = ConfigurationProvider.get().cameraPreviewCreator.create(context, this)
        cameraManager = ConfigurationProvider.get().cameraManagerCreator.create(context, cameraPreview!!)
        cameraManager?.initialize(context)
        cameraManager?.addCameraSizeListener(object : CameraSizeListener {
            override fun onPreviewSizeUpdated(previewSize: Size) {
                aspectRatio = cameraManager?.getAspectRatio(CameraSizeFor.SIZE_FOR_PREVIEW)
                if (displayOrientationDetector!!.lastKnownDisplayOrientation % 180 == 0) {
                    aspectRatio = aspectRatio?.inverse()
                }
                d(TAG, "onPreviewSizeUpdated : $previewSize")
                requestLayout()
            }

            override fun onVideoSizeUpdated(videoSize: Size) { /*noop*/ }

            override fun onPictureSizeUpdated(pictureSize: Size) { /*noop*/ }
        })
        focusMarkerLayout = FocusMarkerLayout(context)
        focusMarkerLayout?.setCameraView(this)
        focusMarkerLayout?.layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )
        this.addView(focusMarkerLayout)
        val a = context.obtainStyledAttributes(attrs, R.styleable.CameraView, defStyleAttr, R.style.Widget_CameraView)
        adjustViewBounds = a.getBoolean(R.styleable.CameraView_android_adjustViewBounds, false)
        cameraManager?.switchCamera(a.getInt(R.styleable.CameraView_cameraFace, CameraFace.FACE_REAR))
        cameraManager?.mediaType = a.getInt(R.styleable.CameraView_mediaType, MediaType.TYPE_PICTURE)
        cameraManager?.isVoiceEnable = a.getBoolean(R.styleable.CameraView_voiceEnable, true)
        val strAspectRatio = a.getString(R.styleable.CameraView_aspectRatio)
        val defaultRatio = ConfigurationProvider.get().defaultAspectRatio
        aspectRatio = if (TextUtils.isEmpty(strAspectRatio)) defaultRatio else AspectRatio.parse(strAspectRatio!!)
        cameraManager?.setExpectAspectRatio(aspectRatio?:defaultRatio)
        cameraManager?.isAutoFocus = a.getBoolean(R.styleable.CameraView_autoFocus, true)
        cameraManager?.flashMode = a.getInt(R.styleable.CameraView_flash, FlashMode.FLASH_AUTO)
        val zoomString = a.getString(R.styleable.CameraView_zoom)
        zoom = if (!TextUtils.isEmpty(zoomString)) {
            try { java.lang.Float.valueOf(zoomString?:"") } catch (e: Exception) { 1.0f }
        } else { 1.0f }
        clipScreen = a.getBoolean(R.styleable.CameraView_clipScreen, false)
        adjustType = a.getInt(R.styleable.CameraView_cameraAdjustType, adjustType)
        focusMarkerLayout?.setScaleRate(a.getInt(R.styleable.CameraView_scaleRate, FocusMarkerLayout.DEFAULT_SCALE_RATE))
        focusMarkerLayout?.setTouchZoomEnable(a.getBoolean(R.styleable.CameraView_touchRoom, true))
        focusMarkerLayout?.setUseTouchFocus(a.getBoolean(R.styleable.CameraView_touchFocus, true))
        a.recycle()
        displayOrientationDetector = object : DisplayOrientationDetector(context) {
            override fun onDisplayOrientationChanged(displayOrientation: Int) {
                cameraManager?.displayOrientation = displayOrientation
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            displayOrientationDetector?.enable(ViewCompat.getDisplay(this))
        }
    }

    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            displayOrientationDetector?.disable()
        }
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        d(TAG, "onMeasure")
        if (isInEditMode) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }
        d(TAG, "clipScreen: $clipScreen adjustViewBounds: $adjustViewBounds")
        if (clipScreen) {
            var width = MeasureSpec.getSize(widthMeasureSpec)
            var height = MeasureSpec.getSize(heightMeasureSpec)
            d(TAG, "width: $width height: $height")
            when (adjustType) {
                PreviewAdjustType.WIDTH_FIRST ->
                    height = width * aspectRatio!!.heightRatio / aspectRatio!!.widthRatio
                PreviewAdjustType.HEIGHT_FIRST ->
                    width = height * aspectRatio!!.widthRatio / aspectRatio!!.heightRatio
                PreviewAdjustType.SMALLER_FIRST ->
                    if (width * aspectRatio!!.heightRatio < height * aspectRatio!!.widthRatio) {
                        height = width * aspectRatio!!.heightRatio / aspectRatio!!.widthRatio
                    } else {
                        width = height * aspectRatio!!.widthRatio / aspectRatio!!.heightRatio
                    }
                PreviewAdjustType.LARGER_FIRST ->
                    if (width * aspectRatio!!.heightRatio < height * aspectRatio!!.widthRatio) {
                        width = height * aspectRatio!!.widthRatio / aspectRatio!!.heightRatio
                    } else {
                        height = width * aspectRatio!!.heightRatio / aspectRatio!!.widthRatio
                    }
                PreviewAdjustType.NONE -> { }
                else -> { }
            }
            super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
            )
            return
        }
        if (adjustViewBounds) {
            // fix 2020-08-31 : preview is distorted when switch face for camera2 while it's not opened
            val widthMode = MeasureSpec.getMode(widthMeasureSpec)
            val heightMode = MeasureSpec.getMode(heightMeasureSpec)
            d(TAG, "widthMode: $widthMode heightMode: $heightMode")
            if (widthMode == MeasureSpec.EXACTLY && heightMode != MeasureSpec.EXACTLY) {
                val ratio = aspectRatio
                var height = (MeasureSpec.getSize(widthMeasureSpec) * ratio!!.ratio()).toInt()
                if (heightMode == MeasureSpec.AT_MOST) {
                    height = Math.min(height, MeasureSpec.getSize(heightMeasureSpec))
                }
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
            } else if (widthMode != MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
                var width = (MeasureSpec.getSize(heightMeasureSpec) * aspectRatio!!.ratio()).toInt()
                if (widthMode == MeasureSpec.AT_MOST) {
                    width = Math.min(width, MeasureSpec.getSize(widthMeasureSpec))
                }
                super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), heightMeasureSpec)
            } else {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            }
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
        val width = measuredWidth
        val height = measuredHeight
        // Always smaller first! But use the effect to the CameraPreview instead of the CameraView.
        d(TAG, "width: $width height: $height")
        if (height < width * aspectRatio!!.heightRatio / aspectRatio!!.widthRatio) {
            cameraPreview!!.view?.measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(width * aspectRatio!!.heightRatio / aspectRatio!!.widthRatio, MeasureSpec.EXACTLY)
            )
        } else {
            cameraPreview!!.view?.measure(
                MeasureSpec.makeMeasureSpec(height * aspectRatio!!.widthRatio / aspectRatio!!.heightRatio, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
            )
        }
    }

    fun openCamera(cameraOpenListener: CameraOpenListener?) {
        cameraManager?.openCamera(cameraOpenListener)
    }

    val isCameraOpened: Boolean
        get() = cameraManager?.isCameraOpened == true

    @get:CameraFace
    val cameraFace: Int
        get() = cameraManager?.cameraFace?:ConfigurationProvider.get().defaultCameraFace

    /**
     * Switch camera to given face.
     *
     * @param cameraFace camera face
     */
    fun switchCamera(@CameraFace cameraFace: Int) {
        cameraManager?.switchCamera(cameraFace)
    }

    /**
     * Set media type, video, picture etc.
     *
     * @param mediaType media type
     */
    @get:MediaType
    var mediaType: Int
        get() = cameraManager?.mediaType?:ConfigurationProvider.get().defaultMediaType
        set(mediaType) {
            cameraManager?.mediaType = mediaType
        }

    /**
     * Whether use shutter when capture. The final result is not only affected by
     * this value, but also subject to your phone circumstance. If your phone was
     * in SILENT mode, there will be no voice even you set the voiceEnable true.
     *
     * @param voiceEnable true to use the voice
     */
    var isVoiceEnable: Boolean
        get() = cameraManager?.isVoiceEnable == true
        set(voiceEnable) {
            cameraManager?.isVoiceEnable = voiceEnable
        }

    var isAutoFocus: Boolean
        get() = cameraManager?.isAutoFocus == true
        set(autoFocus) {
            cameraManager?.isAutoFocus = autoFocus
        }

    @get:FlashMode
    var flashMode: Int
        get() = cameraManager?.flashMode?:ConfigurationProvider.get().defaultFlashMode
        set(flashMode) {
            cameraManager?.flashMode = flashMode
        }

    /**
     * Set zoom for camera with minimum value 1.0 to [.getMaxZoom]
     *
     * @param zoom zoom ratio
     */
    var zoom: Float
        get() = cameraManager?.zoom?:1.0f
        set(zoom) {
            cameraManager?.zoom = zoom
        }

    val maxZoom: Float
        get() = cameraManager?.getMaxZoom()?:1.0f

    /**
     * Set expect picture or video size, the final output size
     * will be calculated from [me.shouheng.icamera.config.calculator.CameraSizeCalculator].
     * You can implement this interface and set your own implementation by
     * [ConfigurationProvider.setCameraSizeCalculator]
     *
     * @param expectSize expect output size
     */
    fun setExpectSize(expectSize: Size) {
        cameraManager?.setExpectSize(expectSize)
    }

    /**
     * Set expect aspect ratio of output, final result logic was same as [.setExpectSize].
     *
     * @param aspectRatio expect aspect ratio of output
     */
    fun setExpectAspectRatio(aspectRatio: AspectRatio) {
        cameraManager?.setExpectAspectRatio(aspectRatio)
    }

    /**
     * Set expected media quality
     *
     * @param mediaQuality media quality
     */
    fun setMediaQuality(@MediaQuality mediaQuality: Int) {
        cameraManager?.setMediaQuality(mediaQuality)
    }

    /**
     * Current using size of camera
     *
     * @param sizeFor the size for
     * @return        the size
     */
    fun getSize(@CameraSizeFor sizeFor: Int): Size {
        return cameraManager?.getSize(sizeFor)?: Size.of(1, 1)
    }

    /**
     * Get all support sizes of camera
     *
     * @param sizeFor the size for
     * @return        the sizes
     */
    fun getSizes(@CameraSizeFor sizeFor: Int): SizeMap {
        return cameraManager?.getSizes(sizeFor)?: SizeMap()
    }

    /**
     * Get current aspect ratio of preview, picture or video. Since the size might not be calculated
     * when the time trying to get the ratio, the returned value might be null.
     *
     * @param sizeFor the aspect ratio for
     * @return        the aspect ratio
     */
    fun getAspectRatio(@CameraSizeFor sizeFor: Int): AspectRatio {
        return cameraManager?.getAspectRatio(sizeFor)?: ConfigurationProvider.get().defaultAspectRatio
    }

    fun addCameraSizeListener(cameraSizeListener: CameraSizeListener) {
        cameraManager?.addCameraSizeListener(cameraSizeListener)
    }

    /**
     * Set the camera preview listener
     *
     * @param cameraPreviewListener the listener
     */
    fun setCameraPreviewListener(cameraPreviewListener: CameraPreviewListener) {
        cameraManager?.setCameraPreviewListener(cameraPreviewListener)
    }

    /**
     * Add screen orientation change listener. You may listen and update rotate the controls
     * according to the screen change.
     *
     * @param orientationChangedListener the orientation change listener
     */
    fun addOrientationChangedListener(orientationChangedListener: OnOrientationChangedListener) {
        if (!orientationChangedListeners.contains(orientationChangedListener)) {
            orientationChangedListeners.add(orientationChangedListener)
        }
    }

    fun removeOrientationChangedListener(orientationChangedListener: OnOrientationChangedListener?) {
        orientationChangedListeners.remove(orientationChangedListener)
    }

    /**
     * Call to take a picture, you can get the output from the callback.
     *
     * @param fileToSave          the file to save picture
     * @param cameraPhotoListener the result callback
     */
    fun takePicture(
        fileToSave: File,
        cameraPhotoListener: CameraPhotoListener?
    ) {
        cameraManager?.takePicture(fileToSave, cameraPhotoListener)
    }

    /**
     * Set the maximum file size (in bytes) of the recording video.
     *
     * @param videoFileSize the maximum file size in bytes (if zero or negative, the limit will be disabled)
     */
    fun setVideoFileSize(videoFileSize: Long) {
        cameraManager?.videoFileSize = videoFileSize
    }

    /**
     * Set the maximum duration (in ms) of the recording video.
     *
     * @param videoDuration the maximum duration in ms (if zero or negative, the duration limit will be disabled)
     */
    fun setVideoDuration(videoDuration: Int) {
        cameraManager?.videoDuration = videoDuration
    }

    /**
     * Start video record
     *
     * @param file                the file to save video data
     * @param cameraVideoListener the result callback
     */
    fun startVideoRecord(
        file: File,
        cameraVideoListener: CameraVideoListener?
    ) {
        cameraManager?.startVideoRecord(file, cameraVideoListener)
    }

    /**
     * Stop video record
     */
    fun stopVideoRecord() {
        cameraManager?.stopVideoRecord()
    }

    fun resumePreview() {
        cameraManager?.resumePreview()
    }

    fun closeCamera(cameraCloseListener: CameraCloseListener?) {
        cameraManager?.closeCamera(cameraCloseListener)
    }

    fun releaseCamera() {
        cameraManager?.releaseCamera()
        sensorManager?.unregisterListener(sensorEventListener)
        orientationChangedListeners.clear()
    }

    /**
     * Set a callback when the use made a move event on the maker.
     *
     * @param onMoveListener the callback
     */
    fun setOnMoveListener(onMoveListener: OnMoveListener?) {
        focusMarkerLayout?.setOnMoveListener(onMoveListener)
    }

    fun setTouchAngle(touchAngle: Int) {
        focusMarkerLayout?.setTouchAngle(touchAngle)
    }

    /**
     * Set scale rate when user try to scale on maker.
     *
     * @param scaleRate the scale rate
     */
    fun setScaleRate(@IntRange(from = 0) scaleRate: Int) {
        focusMarkerLayout?.setScaleRate(scaleRate)
    }

    fun setTouchZoomEnable(touchZoomEnable: Boolean) {
        focusMarkerLayout?.setTouchZoomEnable(touchZoomEnable)
    }

    fun setUseTouchFocus(useTouchFocus: Boolean) {
        focusMarkerLayout?.setUseTouchFocus(useTouchFocus)
    }

    companion object {
        private const val TAG = "CameraView"
    }
}