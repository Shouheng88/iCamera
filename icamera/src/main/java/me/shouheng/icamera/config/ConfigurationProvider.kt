package me.shouheng.icamera.config

import android.annotation.TargetApi
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.RequiresApi
import android.util.SparseArray
import android.util.SparseIntArray
import me.shouheng.icamera.config.calculator.CameraSizeCalculator
import me.shouheng.icamera.config.calculator.impl.CameraSizeCalculatorImpl
import me.shouheng.icamera.config.convert.ImageRawDataConverter
import me.shouheng.icamera.config.convert.ImageRawDataConverterImpl
import me.shouheng.icamera.config.creator.CameraManagerCreator
import me.shouheng.icamera.config.creator.CameraPreviewCreator
import me.shouheng.icamera.config.creator.impl.CameraManagerCreatorImpl
import me.shouheng.icamera.config.creator.impl.CameraPreviewCreatorImpl
import me.shouheng.icamera.config.size.AspectRatio
import me.shouheng.icamera.config.size.AspectRatio.Companion.of
import me.shouheng.icamera.config.size.Size
import me.shouheng.icamera.enums.*
import me.shouheng.icamera.util.LogWatcher
import me.shouheng.icamera.util.XLog
import me.shouheng.icamera.util.XLog.d
import me.shouheng.icamera.util.XLog.e
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Global configuration provider for camera library. Singleton, also used to
 * cache some values of the camera to avoid multiple calculations.
 * You can call methods of this instance to make set some default values of
 * camera before it finally launched, for example [.setDefaultFlashMode],
 * [.setDefaultAspectRatio] etc.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:44
 */
class ConfigurationProvider private constructor() {

    /** The creator for [me.shouheng.icamera.manager.CameraManager].  */
    var cameraManagerCreator: CameraManagerCreator =  CameraManagerCreatorImpl()

    /** The creator for [me.shouheng.icamera.preview.CameraPreview].  */
    var cameraPreviewCreator: CameraPreviewCreator = CameraPreviewCreatorImpl()

    /** The calculator for camera size.  */
    var cameraSizeCalculator: CameraSizeCalculator = CameraSizeCalculatorImpl()

    /** The image raw data converter.  */
    var imageRawDataConverter: ImageRawDataConverter = ImageRawDataConverterImpl()

    /** Whether use memory cache in library. default is true.  */
    var isUseCacheValues = true

    /** If the rear camera failed to launch, use the front instead, otherwise, throw exception. */
    var useCameraFallback = true

    /**
     * The sizes map from a int value, which was calculated from:
     * hash = [CameraFace] | [CameraSizeFor] | [CameraType]  */
    private val sizeMap: SparseArray<List<Size>> = SparseArray()

    /**
     * The room ratios map from a int value, which was calculated from:
     * hash = [CameraFace] | [CameraType]  */
    private val ratioMap: SparseArray<List<Float>> = SparseArray()

    @get:CameraFace
    @CameraFace
    var defaultCameraFace = CameraFace.FACE_REAR

    @get:MediaType
    @MediaType
    var defaultMediaType = MediaType.TYPE_PICTURE

    @get:MediaQuality
    @MediaQuality
    var defaultMediaQuality = MediaQuality.QUALITY_HIGH

    /**
     * Set default camera aspect ratio
     *
     * @param defaultAspectRatio the camera aspect ratio
     */
    var defaultAspectRatio: AspectRatio = of(4, 3)
    var isVoiceEnable = true
    var isAutoFocus = true

    @get:FlashMode
    @FlashMode
    var defaultFlashMode = FlashMode.FLASH_AUTO
    var defaultVideoFileSize: Long = -1
    var defaultVideoDuration = -1

    /** Device default orientation  */
    @get:DeviceDefaultOrientation
    @DeviceDefaultOrientation
    var deviceDefaultOrientation = 0

    /** The sensor position  */
    @get:SensorPosition
    @SensorPosition
    var sensorPosition = 0
    var degrees = -1
    private var numberOfCameras = 0
    private val camera2Prepared = AtomicBoolean()
    private val cameraIdCamera2 = SparseArray<String>()
    private val cameraCharacteristics = SparseArray<CameraCharacteristics>()
    private val cameraOrientations = SparseIntArray()
    private val streamConfigurationMaps = SparseArray<StreamConfigurationMap>()

    var isDebug = false
        set(debug) {
            field = debug
            XLog.setDebug(debug)
        }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun prepareCamera2(context: Context): ConfigurationProvider {
        if (!camera2Prepared.get()) {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            try {
                val ids = cameraManager.cameraIdList
                numberOfCameras = ids.size
                for (id in ids) {
                    val characteristics = cameraManager.getCameraCharacteristics(id)
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                        cameraIdCamera2.put(CameraFace.FACE_FRONT, id)
                        val iFrontCameraOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
                        cameraOrientations.put(CameraFace.FACE_FRONT, iFrontCameraOrientation ?: 0)
                        cameraCharacteristics.put(CameraFace.FACE_FRONT, characteristics)
                        streamConfigurationMaps.put(CameraFace.FACE_FRONT,
                            characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP))
                    } else if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                        cameraIdCamera2.put(CameraFace.FACE_REAR, id)
                        val iRearCameraOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
                        cameraOrientations.put(CameraFace.FACE_REAR, iRearCameraOrientation ?: 0)
                        cameraCharacteristics.put(CameraFace.FACE_REAR, characteristics)
                        streamConfigurationMaps.put(CameraFace.FACE_REAR,
                            characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP))
                    }
                }
                camera2Prepared.set(true)
            } catch (e: Exception) {
                e("ConfigurationProvider", "initCameraInfo error $e")
            }
        }
        return this
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun getNumberOfCameras(context: Context): Int {
        return prepareCamera2(context).numberOfCameras
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun getCameraId(context: Context, @CameraFace cameraFace: Int): String {
        return prepareCamera2(context).cameraIdCamera2[cameraFace]
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun getCameraCharacteristics(context: Context, @CameraFace cameraFace: Int): CameraCharacteristics {
        return prepareCamera2(context).cameraCharacteristics[cameraFace]
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun getCameraOrientation(context: Context, @CameraFace cameraFace: Int): Int {
        return prepareCamera2(context).cameraOrientations[cameraFace]
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun getStreamConfigurationMap(context: Context, @CameraFace cameraFace: Int): StreamConfigurationMap {
        return prepareCamera2(context).streamConfigurationMaps[cameraFace]
    }

    /**
     * Get standard supported sizes for camera1 of given condition.
     *
     * @param camera     camera
     * @param cameraFace camera face
     * @param sizeFor    camera size for
     * @return           the size
     */
    fun getSizes(camera: Camera, @CameraFace cameraFace: Int, @CameraSizeFor sizeFor: Int): List<Size> {
        // calculate hash of map
        val hash = cameraFace or sizeFor or CameraType.TYPE_CAMERA1
        d("ConfigurationProvider", "getSizes hash : " + Integer.toHexString(hash))
        // try to get sizes from cache first.
        if (isUseCacheValues) {
            val sizes = sizeMap[hash]
            if (sizes != null) return sizes
        }
        // get sizes from parameters
        val parameters = camera.parameters
        val sizes: List<Size>
        sizes = when (sizeFor) {
            CameraSizeFor.SIZE_FOR_PICTURE -> Size.fromList(parameters.supportedPictureSizes)
            CameraSizeFor.SIZE_FOR_PREVIEW -> Size.fromList(parameters.supportedPreviewSizes)
            // fix 2021-05-01 the video sizes might be null if camera don't separate preview and video output
            CameraSizeFor.SIZE_FOR_VIDEO -> Size.fromList(parameters.supportedVideoSizes?: emptyList())
            else -> throw IllegalArgumentException("Unsupported size for $sizeFor")
        }
        // cache the sizes in memory
        if (isUseCacheValues) {
            sizeMap.put(hash, sizes)
        }
        return sizes
    }

    /**
     * Get standard supported room ratios for camera1.
     *
     * @param camera     camera
     * @param cameraFace camera face
     * @return           supported zoom ratios
     */
    fun getZoomRatios(camera: Camera, @CameraFace cameraFace: Int): List<Float> {
        // fix 2020-05-01 check is zoom supported before trying to get supported ratios
        if (!camera.parameters.isZoomSupported) {
            return emptyList()
        }
        // calculate hash of map
        val hash = cameraFace or CameraType.TYPE_CAMERA1
        d("ConfigurationProvider", "getZoomRatios hash : " + Integer.toHexString(hash))
        // try to get ratios from cache first.
        if (isUseCacheValues) {
            val zoomRatios = ratioMap[hash]
            if (zoomRatios != null) return zoomRatios
        }
        // calculate room ratios
        val ratios = camera.parameters.zoomRatios
        val result: MutableList<Float> = ArrayList(ratios.size)
        for (ratio in ratios) {
            result.add(ratio * 0.01f)
        }
        // cache room ratios
        if (isUseCacheValues) {
            ratioMap.put(hash, result)
        }
        return result
    }

    /**
     * Get standard support sizes for camera2 of given condition.
     *
     * @param map               the configuration map
     * @param cameraFace       the camera face
     * @param sizeFor          the camera size for
     * @return                 the supported sizes
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun getSizes(map: StreamConfigurationMap, @CameraFace cameraFace: Int, @CameraSizeFor sizeFor: Int): List<Size> {
        // calculate hash
        val hash = cameraFace or sizeFor or CameraType.TYPE_CAMERA2
        d("ConfigurationProvider", "getSizes hash : " + Integer.toHexString(hash))
        // try to get sizes from cache
        if (isUseCacheValues) {
            val sizes = sizeMap[hash]
            if (sizes != null) return sizes
        }
        // calculate camera sizes
        val sizes: List<Size> = when (sizeFor) {
            CameraSizeFor.SIZE_FOR_PICTURE -> Size.fromList(map.getOutputSizes(ImageFormat.JPEG))
            CameraSizeFor.SIZE_FOR_PREVIEW -> Size.fromList(map.getOutputSizes(SurfaceTexture::class.java))
            CameraSizeFor.SIZE_FOR_VIDEO -> Size.fromList(map.getOutputSizes(MediaRecorder::class.java))
            else -> throw IllegalArgumentException("Unsupported size for $sizeFor")
        }
        // cache sizes
        if (isUseCacheValues) {
            sizeMap.put(hash, sizes)
        }
        return sizes
    }

    /** Add the log watcher */
    fun addLogWatcher(watcher: LogWatcher) {
        XLog.addLogWatcher(watcher)
    }

    /** Remove the log watcher */
    fun removeLogWatcher(watcher: LogWatcher) {
        XLog.removeLogWatcher(watcher)
    }

    companion object {

        @Volatile
        private var configurationProvider: ConfigurationProvider? = null

        fun get(): ConfigurationProvider {
            if (configurationProvider == null) {
                synchronized(ConfigurationProvider::class.java) {
                    if (configurationProvider == null) {
                        configurationProvider = ConfigurationProvider()
                    }
                }
            }
            return configurationProvider!!
        }
    }
}