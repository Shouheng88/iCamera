package me.shouheng.icamera.config.calculator.impl

import android.util.SparseArray
import me.shouheng.icamera.config.ConfigurationProvider
import me.shouheng.icamera.config.calculator.CameraSizeCalculator
import me.shouheng.icamera.config.size.AspectRatio
import me.shouheng.icamera.config.size.Size
import me.shouheng.icamera.enums.MediaQuality
import me.shouheng.icamera.util.CameraHelper
import me.shouheng.icamera.util.CameraHelper.getSizeWithClosestRatio
import me.shouheng.icamera.util.CameraHelper.getSizeWithClosestRatioSizeAndQuality
import me.shouheng.icamera.util.XLog.d

/**
 * Default implementation for [CameraSizeCalculator].
 *
 * Sample calculation strategies:
 * @see CameraHelper.getSizeWithClosestRatio
 * @see CameraHelper.getSizeWithClosestRatioSizeAndQuality
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:58
 */
class CameraSizeCalculatorImpl : CameraSizeCalculator {
    private var previewSizes: List<Size> = emptyList()
    private var pictureSizes: List<Size> = emptyList()
    private var videoSizes: List<Size> = emptyList()
    private var expectAspectRatio: AspectRatio = ConfigurationProvider.get().defaultAspectRatio
    private var expectSize: Size? = null

    @MediaQuality
    private var mediaQuality = 0
    private val outPictureSizes = SparseArray<Size>()
    private val outVideoSizes = SparseArray<Size>()
    private val outPicturePreviewSizes = SparseArray<Size>()
    private val outVideoPreviewSizes = SparseArray<Size>()

    override fun init(
        expectAspectRatio: AspectRatio,
        expectSize: Size?,
        @MediaQuality mediaQuality: Int,
        previewSizes: List<Size>,
        pictureSizes: List<Size>,
        videoSizes: List<Size>
    ) {
        this.expectAspectRatio = expectAspectRatio
        this.expectSize = expectSize
        this.mediaQuality = mediaQuality
        this.previewSizes = previewSizes
        this.pictureSizes = pictureSizes
        this.videoSizes = videoSizes
    }

    override fun changeExpectAspectRatio(expectAspectRatio: AspectRatio) {
        d("CameraSizeCalculator", "changeExpectAspectRatio : cache cleared")
        this.expectAspectRatio = expectAspectRatio
        outPictureSizes.clear()
        outPicturePreviewSizes.clear()
        outVideoSizes.clear()
        outVideoPreviewSizes.clear()
    }

    override fun changeExpectSize(expectSize: Size) {
        d("CameraSizeCalculator", "changeExpectSize : cache cleared")
        this.expectSize = expectSize
        outPictureSizes.clear()
        outPicturePreviewSizes.clear()
        outVideoSizes.clear()
        outVideoPreviewSizes.clear()
    }

    override fun changeMediaQuality(mediaQuality: Int) {
        d("CameraSizeCalculator", "changeMediaQuality : cache cleared")
        this.mediaQuality = mediaQuality
        outPictureSizes.clear()
        outPicturePreviewSizes.clear()
        outVideoSizes.clear()
        outVideoPreviewSizes.clear()
    }

    override fun getPictureSize(cameraType: Int): Size? {
        var size = outPictureSizes[cameraType]
        if (size != null) return size
        size = getSizeWithClosestRatioSizeAndQuality(
            pictureSizes, expectAspectRatio, expectSize, mediaQuality)
        outPictureSizes.put(cameraType, size)
        d("CameraSizeCalculator", "getPictureSize : $size")
        return size
    }

    override fun getPicturePreviewSize(cameraType: Int): Size? {
        var size = outPicturePreviewSizes[cameraType]
        if (size != null) return size
        size = getSizeWithClosestRatio(previewSizes, getPictureSize(cameraType))
        outPicturePreviewSizes.put(cameraType, size)
        d("CameraSizeCalculator", "getPicturePreviewSize : $size")
        return size
    }

    override fun getVideoSize(cameraType: Int): Size? {
        var size = outVideoSizes[cameraType]
        if (size != null) return size
        size = getSizeWithClosestRatioSizeAndQuality(
            videoSizes, expectAspectRatio, expectSize, mediaQuality)
        outVideoSizes.put(cameraType, size)
        d("CameraSizeCalculator", "getVideoSize : $size")
        return null
    }

    override fun getVideoPreviewSize(cameraType: Int): Size? {
        var size = outVideoPreviewSizes[cameraType]
        if (size != null) return size
        size = getSizeWithClosestRatio(previewSizes, getVideoSize(cameraType))
        outVideoPreviewSizes.put(cameraType, size)
        d("CameraSizeCalculator", "getVideoPreviewSize : $size")
        return size
    }
}
