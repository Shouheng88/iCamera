package me.shouheng.icamera

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
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
    private lateinit var aspectRatio: AspectRatio
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
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
            : super(context, attrs, defStyleAttr) {
        initCameraView(context, attrs, defStyleAttr, 0)
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int)
            : super(context, attrs, defStyleAttr, defStyleRes) {
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
                aspectRatio = cameraManager?.getAspectRatio(CameraSizeFor.SIZE_FOR_PREVIEW)!!
                if (displayOrientationDetector!!.lastKnownDisplayOrientation % 180 == 0) {
                    aspectRatio = aspectRatio.inverse()
                }
                d("CameraView", "onPreviewSizeUpdated : $previewSize")
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
        var cameraFace = a.getInt(R.styleable.CameraView_cameraFace, -1)
        cameraFace = if (cameraFace == -1) ConfigurationProvider.get().defaultCameraFace else cameraFace
        cameraManager?.switchCamera(cameraFace)
        cameraManager?.mediaType = a.getInt(R.styleable.CameraView_mediaType, ConfigurationProvider.get().defaultMediaType)
        cameraManager?.isVoiceEnable = a.getBoolean(R.styleable.CameraView_voiceEnable, true)
        val strAspectRatio = a.getString(R.styleable.CameraView_aspectRatio)
        val defaultRatio = ConfigurationProvider.get().defaultAspectRatio
        aspectRatio = if (TextUtils.isEmpty(strAspectRatio)) defaultRatio else AspectRatio.parse(strAspectRatio!!)
        cameraManager?.setExpectAspectRatio(aspectRatio)
        cameraManager?.isAutoFocus = a.getBoolean(R.styleable.CameraView_autoFocus, true)
        cameraManager?.flashMode = a.getInt(R.styleable.CameraView_flash, ConfigurationProvider.get().defaultFlashMode)
        val zoomString = a.getString(R.styleable.CameraView_zoom)
        zoom = try { (zoomString?:"").toFloat() } catch (e: Exception) { 1.0f }
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
        d("CameraView", "onMeasure")
        if (isInEditMode) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }
        d("CameraView", "clipScreen: $clipScreen adjustViewBounds: $adjustViewBounds")
        if (clipScreen) {
            var w = MeasureSpec.getSize(widthMeasureSpec)
            var h = MeasureSpec.getSize(heightMeasureSpec)
            d("CameraView", "width: $w height: $h")
            when (adjustType) {
                PreviewAdjustType.WIDTH_FIRST -> h = w * aspectRatio.heightRatio / aspectRatio.widthRatio
                PreviewAdjustType.HEIGHT_FIRST -> w = h * aspectRatio.widthRatio / aspectRatio.heightRatio
                PreviewAdjustType.SMALLER_FIRST ->
                    if (w * aspectRatio.heightRatio < h * aspectRatio.widthRatio) {
                        h = w * aspectRatio.heightRatio / aspectRatio.widthRatio
                    } else {
                        w = h * aspectRatio.widthRatio / aspectRatio.heightRatio
                    }
                PreviewAdjustType.LARGER_FIRST ->
                    if (w * aspectRatio.heightRatio < h * aspectRatio.widthRatio) {
                        w = h * aspectRatio.widthRatio / aspectRatio.heightRatio
                    } else {
                        h = w * aspectRatio.heightRatio / aspectRatio.widthRatio
                    }
                PreviewAdjustType.NONE -> { }
                else -> { }
            }
            super.onMeasure(
                    MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY)
            )
            return
        }
        if (adjustViewBounds) {
            // fix 2020-08-31 : preview is distorted when switch face for camera2 while it's not opened
            val wm = MeasureSpec.getMode(widthMeasureSpec)
            val hm = MeasureSpec.getMode(heightMeasureSpec)
            d("CameraView", "widthMode: $wm heightMode: $hm")
            if (wm == MeasureSpec.EXACTLY && hm != MeasureSpec.EXACTLY) {
                val ratio = aspectRatio
                var h = (MeasureSpec.getSize(widthMeasureSpec) * ratio.ratio()).toInt()
                if (hm == MeasureSpec.AT_MOST) h = h.coerceAtMost(MeasureSpec.getSize(heightMeasureSpec))
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY))
            } else if (wm != MeasureSpec.EXACTLY && hm == MeasureSpec.EXACTLY) {
                var w = (MeasureSpec.getSize(heightMeasureSpec) * aspectRatio.ratio()).toInt()
                if (wm == MeasureSpec.AT_MOST) w = w.coerceAtMost(MeasureSpec.getSize(widthMeasureSpec))
                super.onMeasure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), heightMeasureSpec)
            } else {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            }
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
        val w = measuredWidth; val h = measuredHeight
        // Always smaller first! But use the effect to the CameraPreview instead of the CameraView.
        d("CameraView", "width: $w height: $h")
        if (h < w * aspectRatio.heightRatio / aspectRatio.widthRatio) {
            cameraPreview?.view?.measure(
                    MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(w * aspectRatio.heightRatio / aspectRatio.widthRatio, MeasureSpec.EXACTLY)
            )
        } else {
            cameraPreview?.view?.measure(
                    MeasureSpec.makeMeasureSpec(h * aspectRatio.widthRatio / aspectRatio.heightRatio, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY)
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

    /** Switch camera to given face. */
    fun switchCamera(@CameraFace cameraFace: Int) {
        cameraManager?.switchCamera(cameraFace)
    }

    /** Set media type, video, picture etc. */
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

    /** Set zoom for camera with minimum value 1.0 to [maxZoom] */
    var zoom: Float
        get() = cameraManager?.zoom?:1.0f
        set(zoom) {
            cameraManager?.zoom = zoom
        }

    /** Get max zoom of camera */
    val maxZoom: Float
        get() = cameraManager?.getMaxZoom()?:1.0f

    /**
     * Set expect picture or video size, the final output size
     * will be calculated from [me.shouheng.icamera.config.calculator.CameraSizeCalculator].
     * You can implement this interface and set your own implementation by
     * [ConfigurationProvider.cameraSizeCalculator]
     */
    fun setExpectSize(expectSize: Size) {
        cameraManager?.setExpectSize(expectSize)
    }

    /** Set expect aspect ratio of output, final result logic was same as [setExpectSize].*/
    fun setExpectAspectRatio(aspectRatio: AspectRatio) {
        cameraManager?.setExpectAspectRatio(aspectRatio)
    }

    /** Set expected media quality */
    fun setMediaQuality(@MediaQuality mediaQuality: Int) {
        cameraManager?.setMediaQuality(mediaQuality)
    }

    /** Current using size of camera */
    fun getSize(@CameraSizeFor sizeFor: Int): Size {
        return cameraManager?.getSize(sizeFor)?: Size.of(1, 1)
    }

    /** Get all support sizes of camera */
    fun getSizes(@CameraSizeFor sizeFor: Int): SizeMap {
        return cameraManager?.getSizes(sizeFor)?: SizeMap()
    }

    /**
     * Get current aspect ratio of preview, picture or video. Since the size might not be calculated
     * when the time trying to get the ratio, the returned value might be null.
     */
    fun getAspectRatio(@CameraSizeFor sizeFor: Int): AspectRatio {
        return cameraManager?.getAspectRatio(sizeFor)?: ConfigurationProvider.get().defaultAspectRatio
    }

    fun addCameraSizeListener(cameraSizeListener: CameraSizeListener) {
        cameraManager?.addCameraSizeListener(cameraSizeListener)
    }

    /** Set the camera preview listener */
    fun setCameraPreviewListener(cameraPreviewListener: CameraPreviewListener) {
        cameraManager?.setCameraPreviewListener(cameraPreviewListener)
    }

    /**
     * Add screen orientation change listener. You may listen and update rotate the controls
     * according to the screen change.
     */
    fun addOrientationChangedListener(orientationChangedListener: OnOrientationChangedListener) {
        if (!orientationChangedListeners.contains(orientationChangedListener)) {
            orientationChangedListeners.add(orientationChangedListener)
        }
    }

    fun removeOrientationChangedListener(orientationChangedListener: OnOrientationChangedListener?) {
        orientationChangedListeners.remove(orientationChangedListener)
    }

    /** Call to take a picture, you can get the output from the callback. */
    fun takePicture(fileToSave: File, cameraPhotoListener: CameraPhotoListener?) {
        cameraManager?.takePicture(fileToSave, cameraPhotoListener)
    }

    /** Set the maximum file size (in bytes) of the recording video. */
    fun setVideoFileSize(videoFileSize: Long) {
        cameraManager?.videoFileSize = videoFileSize
    }

    /** Set the maximum duration (in ms) of the recording video. */
    fun setVideoDuration(videoDuration: Int) {
        cameraManager?.videoDuration = videoDuration
    }

    /** Start video record */
    fun startVideoRecord(file: File, cameraVideoListener: CameraVideoListener?) {
        cameraManager?.startVideoRecord(file, cameraVideoListener)
    }

    /** Stop video record */
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

    /** Set a callback when the use made a move event on the maker. */
    fun setOnMoveListener(onMoveListener: OnMoveListener?) {
        focusMarkerLayout?.setOnMoveListener(onMoveListener)
    }

    fun setTouchAngle(touchAngle: Int) {
        focusMarkerLayout?.setTouchAngle(touchAngle)
    }

    /** Set scale rate when user try to scale on maker. */
    fun setScaleRate(@IntRange(from = 0) scaleRate: Int) {
        focusMarkerLayout?.setScaleRate(scaleRate)
    }

    fun setTouchZoomEnable(touchZoomEnable: Boolean) {
        focusMarkerLayout?.setTouchZoomEnable(touchZoomEnable)
    }

    fun setUseTouchFocus(useTouchFocus: Boolean) {
        focusMarkerLayout?.setUseTouchFocus(useTouchFocus)
    }
}