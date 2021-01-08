package me.shouheng.icamera.util

import android.annotation.TargetApi
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.CamcorderProfile
import android.os.Build
import android.text.TextUtils
import android.view.OrientationEventListener
import android.view.Surface
import android.view.WindowManager
import me.shouheng.icamera.config.size.AspectRatio
import me.shouheng.icamera.config.size.Size
import me.shouheng.icamera.config.size.SizeMap
import me.shouheng.icamera.enums.CameraFace
import me.shouheng.icamera.enums.MediaQuality
import java.util.*

/**
 * Camera helper
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/14 9:34
 */
object CameraHelper {

    fun hasCamera(context: Context): Boolean {
        val pm = context.packageManager
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) ||
                pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
    }

    /**
     * Try to get available cameras. This method will try to get camera info from
     * [PackageManager] first, and then detect if camera exists by getting camera
     * id from camera info provided based on platform versions.
     *
     * @return a list contains supported camera faces of [CameraFace].
     */
    fun getCameras(context: Context): List<Int> {
        val list = mutableListOf<Int>()
        val pmgr = context.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val mgr = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            try {
                for (id in  mgr.cameraIdList) {
                    val characteristics = mgr.getCameraCharacteristics(id)
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                        if (pmgr.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
                            list.add(CameraFace.FACE_FRONT)
                        }
                    } else if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                        if (pmgr.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                            list.add(CameraFace.FACE_REAR)
                        }
                    }
                }
            } catch (e: Exception) {
                XLog.e("ConfigurationProvider", "initCameraInfo error $e")
            }
        } else {
            for (i in 0 until Camera.getNumberOfCameras()) {
                val cameraInfo = CameraInfo()
                Camera.getCameraInfo(i, cameraInfo)
                if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
                    if (pmgr.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                        list.add(CameraFace.FACE_REAR)
                    }
                } else if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
                    if (pmgr.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
                        list.add(CameraFace.FACE_FRONT)
                    }
                }
            }
        }
        return list
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun hasCamera2(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) false else try {
            val manager = (context.getSystemService(Context.CAMERA_SERVICE) as CameraManager)
            val idList = manager.cameraIdList
            var notNull = true
            if (idList.isEmpty()) {
                notNull = false
            } else {
                for (str in idList) {
                    if (str == null || str.trim { it <= ' ' }.isEmpty()) {
                        notNull = false
                        break
                    }
                    val characteristics = manager.getCameraCharacteristics(str)
                    val iSupportLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                    if (iSupportLevel != null && iSupportLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                        notNull = false
                        break
                    }
                }
            }
            notNull
        } catch (ignore: Throwable) {
            false
        }
    }

    fun onOrientationChanged(cameraId: Int, orientation: Int, parameters: Camera.Parameters) {
        var orien = orientation
        if (orien == OrientationEventListener.ORIENTATION_UNKNOWN) return
        val info = CameraInfo()
        Camera.getCameraInfo(cameraId, info)
        orien = (orien + 45) / 90 * 90
        val rotation: Int
        rotation = if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            (info.orientation - orien + 360) % 360
        } else {  // back-facing camera
            (info.orientation + orien) % 360
        }
        parameters.setRotation(rotation)
    }

    fun calDisplayOrientation(context: Context, @CameraFace face: Int, orientation: Int): Int {
        val displayRotation: Int
        val manager = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
        val rotation = manager.defaultDisplay.rotation
        var degrees = 0
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }
        displayRotation = if (face == CameraFace.FACE_FRONT) {
            (360 - (orientation + degrees) % 360) % 360 // compensate
        } else {
            (orientation - degrees + 360) % 360
        }
        return displayRotation
    }

    fun getDeviceDefaultOrientation(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val config = context.resources.configuration
        val rotation = windowManager.defaultDisplay.rotation
        return if ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) &&
            config.orientation == Configuration.ORIENTATION_LANDSCAPE
            || (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) &&
            config.orientation == Configuration.ORIENTATION_PORTRAIT
        ) {
            Configuration.ORIENTATION_LANDSCAPE
        } else {
            Configuration.ORIENTATION_PORTRAIT
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun getJpegOrientation(c: CameraCharacteristics, deviceOrientation: Int): Int {
        var orientation = deviceOrientation
        if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) return 0
        val sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION)!!

        // Round device orientation to a multiple of 90
        orientation = (orientation + 45) / 90 * 90

        // Reverse device orientation for front-facing cameras
        val lensFacing = c.get(CameraCharacteristics.LENS_FACING)
        val facingFront = lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_FRONT
        if (facingFront) orientation = -orientation

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        return (sensorOrientation + orientation + 360) % 360
    }

    /**
     * Get a map from aspect to sizes.
     *
     * @param sizes sizes to get from
     * @return      the map
     */
    fun getSizeMapFromSizes(sizes: List<Size>): SizeMap {
        val sizeMap = SizeMap()
        for (size in sizes) {
            val aspectRatio = AspectRatio.of(size)
            var list = sizeMap[aspectRatio]
            if (list == null) {
                list = LinkedList()
                list.add(size)
                sizeMap[aspectRatio] = list
            } else {
                list.add(size)
            }
        }
        return sizeMap
    }

    /**
     * Ratio first, we will find out the minimum ratio diff and then get the closet
     * size of the same ratio from sizes.
     *
     * @param sizes      sizes to get from
     * @param expectSize expect size
     * @return           final size, null if the sizes is empty
     */
    fun getSizeWithClosestRatio(sizes: List<Size>, expectSize: Size?): Size? {
        if (sizes.isEmpty()) return null
        if (expectSize == null) return null
        var optimalSize: Size? = null
        val targetRatio = expectSize.ratio()
        var minRatioDiff = Double.MAX_VALUE
        var closetRatio = targetRatio
        for (size in sizes) {
            // ratio first
            if (size == expectSize) {
                return size
            }
            // get size with minimum ratio diff
            val ratioDiff = Math.abs(size.ratio() - targetRatio)
            if (ratioDiff < minRatioDiff) {
                optimalSize = size
                minRatioDiff = ratioDiff
                closetRatio = size.ratio()
            }
        }
        var minHeightDiff = Int.MAX_VALUE
        val targetHeight = expectSize.height
        for (size in sizes) {
            if (size.ratio() == closetRatio) {
                // get size of same ratio, but with minimum height diff
                val heightDiff = Math.abs(size.height - targetHeight)
                if (heightDiff <= minHeightDiff) {
                    minHeightDiff = heightDiff
                    optimalSize = size
                }
            }
        }
        XLog.d(javaClass.simpleName, "getSizeWithClosestRatio : expected $expectSize, result $optimalSize")
        return optimalSize
    }

    /**
     * Aspect first, then size, the quality.
     *
     * @param sizes        sizes to get from
     * @param aspectRatio  expect aspect ratio
     * @param expectSize   expect size
     * @param mediaQuality expect media quality
     * @return             the final output size
     */
    fun getSizeWithClosestRatioSizeAndQuality(
        sizes: List<Size>,
        aspectRatio: AspectRatio?,
        expectSize: Size?,
        @MediaQuality mediaQuality: Int
    ): Size? {
        if (aspectRatio == null) return null
        if (aspectRatio.ratio() != expectSize?.ratio()) {
            XLog.w(javaClass.simpleName, "The expected ratio differs from ratio of expected size.")
        }
        var optimalSize: Size? = null
        val targetRatio = aspectRatio.ratio()
        var minRatioDiff = Double.MAX_VALUE
        var closetRatio = targetRatio

        // 1. find closet ratio first
        for (size in sizes) {
            // ratio first
            if (size == expectSize) {
                // bingo!!
                return size
            }
            // get size with minimum ratio diff
            val ratioDiff = Math.abs(size.ratio() - targetRatio)
            if (ratioDiff < minRatioDiff) {
                optimalSize = size
                minRatioDiff = ratioDiff
                closetRatio = size.ratio()
            }
        }

        // 2. find closet area
        if (expectSize != null) {
            var minAreaDiff = Int.MAX_VALUE
            for (size in sizes) {
                if (size.ratio() == closetRatio) {
                    if (size.area() == expectSize.area()) {
                        // bingo!!
                        return size
                    }
                    val areaDiff = Math.abs(size.area() - expectSize.area())
                    if (areaDiff <= minAreaDiff) {
                        minAreaDiff = areaDiff
                        optimalSize = size
                    }
                }
            }
            return optimalSize
        }

        // 3. find closet media quality (area)
        val sameSizes: MutableList<Size> = LinkedList()
        for (size in sizes) {
            if (size.ratio() == closetRatio) {
                sameSizes.add(size)
            }
        }
        if (sameSizes.isEmpty()) {
            return optimalSize
        }
        sameSizes.sortWith(Comparator { o1, o2 -> o1.area() - o2.area() })
        XLog.d(javaClass.simpleName, "sorted sizes : $sameSizes")
        val size = sameSizes.size
        val index: Int
        index = when (mediaQuality) {
            MediaQuality.QUALITY_LOWEST -> 0
            MediaQuality.QUALITY_LOW -> size / 4
            MediaQuality.QUALITY_MEDIUM -> size * 2 / 4
            MediaQuality.QUALITY_HIGH -> size * 3 / 4
            MediaQuality.QUALITY_HIGHEST, MediaQuality.QUALITY_AUTO -> size - 1
            else -> size - 1
        }
        return sameSizes[index]
    }

    private fun calculateApproximateVideoSize(profile: CamcorderProfile, seconds: Int): Double {
        return ((profile.videoBitRate / 1.toFloat() + profile.audioBitRate / 1.toFloat()) * seconds / 8.toFloat()).toDouble()
    }

    fun calculateApproximateVideoDuration(profile: CamcorderProfile, maxSize: Long): Double {
        return (8 * maxSize / (profile.videoBitRate + profile.audioBitRate)).toDouble()
    }

    private fun calculateMinimumRequiredBitRate(profile: CamcorderProfile, maxSize: Long, seconds: Int): Long {
        return 8 * maxSize / seconds - profile.audioBitRate
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun getCamcorderProfile(
        cameraId: String,
        maximumFileSize: Long,
        minimumDurationInSeconds: Int
    ): CamcorderProfile? {
        if (TextUtils.isEmpty(cameraId)) {
            return null
        }
        val cameraIdInt = cameraId.toInt()
        return getCamcorderProfile(
            cameraIdInt,
            maximumFileSize,
            minimumDurationInSeconds
        )
    }

    private fun getCamcorderProfile(
        currentCameraId: Int,
        maximumFileSize: Long,
        minimumDurationInSeconds: Int
    ): CamcorderProfile {
        if (maximumFileSize <= 0) return CamcorderProfile.get(currentCameraId, MediaQuality.QUALITY_HIGHEST)
        val qualities = intArrayOf(
            MediaQuality.QUALITY_HIGHEST,
            MediaQuality.QUALITY_HIGH,
            MediaQuality.QUALITY_MEDIUM,
            MediaQuality.QUALITY_LOW,
            MediaQuality.QUALITY_LOWEST
        )
        var camcorderProfile: CamcorderProfile
        for (quality in qualities) {
            camcorderProfile = getCamcorderProfile(quality, currentCameraId)
            val fileSize = calculateApproximateVideoSize(camcorderProfile, minimumDurationInSeconds)
            if (fileSize > maximumFileSize) {
                val minimumRequiredBitRate = calculateMinimumRequiredBitRate(
                    camcorderProfile,
                    maximumFileSize,
                    minimumDurationInSeconds
                )
                if (minimumRequiredBitRate >= camcorderProfile.videoBitRate / 4
                    && minimumRequiredBitRate <= camcorderProfile.videoBitRate) {
                    camcorderProfile.videoBitRate = minimumRequiredBitRate.toInt()
                    return camcorderProfile
                }
            } else return camcorderProfile
        }
        return getCamcorderProfile(MediaQuality.QUALITY_LOWEST, currentCameraId)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun getCamcorderProfile(@MediaQuality quality: Int, cameraId: String): CamcorderProfile? {
        if (TextUtils.isEmpty(cameraId)) return null
        val cameraIdInt = cameraId.toInt()
        return getCamcorderProfile(quality, cameraIdInt)
    }

    fun getCamcorderProfile(@MediaQuality mediaQuality: Int, cameraId: Int): CamcorderProfile {
        return if (mediaQuality == MediaQuality.QUALITY_HIGHEST) {
            CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH)
        } else if (mediaQuality == MediaQuality.QUALITY_HIGH) {
            when {
                CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_1080P) -> {
                    CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_1080P)
                }
                CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_720P) -> {
                    CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_720P)
                }
                else -> {
                    CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH)
                }
            }
        } else if (mediaQuality == MediaQuality.QUALITY_MEDIUM) {
            when {
                CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_720P) -> {
                    CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_720P)
                }
                CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_480P) -> {
                    CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_480P)
                }
                else -> {
                    CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW)
                }
            }
        } else if (mediaQuality == MediaQuality.QUALITY_LOW) {
            if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_480P)) {
                CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_480P)
            } else {
                CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW)
            }
        } else if (mediaQuality == MediaQuality.QUALITY_LOWEST) {
            CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW)
        } else {
            CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH)
        }
    }

    fun getZoomIdxForZoomFactor(zoomRatios: List<Int>, zoom: Float): Int {
        val zoomRatioFormat = (zoom * 100).toInt()
        val len = zoomRatios.size
        var possibleIdx = 0
        var minDiff = Int.MAX_VALUE
        var tmp: Int
        for (i in 0 until len) {
            tmp = Math.abs(zoomRatioFormat - zoomRatios[i])
            if (tmp < minDiff) {
                minDiff = tmp
                possibleIdx = i
            }
        }
        return possibleIdx
    }
}