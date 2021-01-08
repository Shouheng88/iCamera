package me.shouheng.icamera.config.calculator

import me.shouheng.icamera.config.size.AspectRatio
import me.shouheng.icamera.config.size.Size
import me.shouheng.icamera.enums.CameraType
import me.shouheng.icamera.enums.MediaQuality

/**
 * Calculator size for camera. You may implement this interface to add your own camera
 * size calculation logic.
 *
 * @see me.shouheng.icamera.config.calculator.impl.CameraSizeCalculatorImpl as an example
 *
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:58
 */
interface CameraSizeCalculator {
    /**
     * Initialize values of calculator.
     *
     * @param expectAspectRatio expect aspect ratio
     * @param expectSize        expect size
     * @param mediaQuality      expect media quality
     * @param previewSizes      support preview sizes
     * @param pictureSizes      support picture sizes
     * @param videoSizes        support video sizes
     */
    fun init(
        expectAspectRatio: AspectRatio,
        expectSize: Size?,
        @MediaQuality mediaQuality: Int,
        previewSizes: List<Size>,
        pictureSizes: List<Size>,
        videoSizes: List<Size>
    )

    /**
     * Change expect aspect ratio. You can implement this method to get the new desired
     * aspect ratio and clear the calculated values cache. Anyway this is the method
     * we used to notify you the camera state changed.
     *
     * See also,
     * @see .changeExpectSize
     * @see .changeMediaQuality
     * @param expectAspectRatio the new expect aspect ratio
     */
    fun changeExpectAspectRatio(expectAspectRatio: AspectRatio)

    /**
     * Change expect size
     *
     * @param expectSize the new expect size
     */
    fun changeExpectSize(expectSize: Size)

    /**
     * Change expect media quality
     *
     * @param mediaQuality the new expect media quality
     */
    fun changeMediaQuality(@MediaQuality mediaQuality: Int)

    /**
     * Get calculated picture size
     *
     * @param cameraType camera type, aka, camera1 or camera2
     * @return           the picture size
     */
    fun getPictureSize(@CameraType cameraType: Int): Size?

    /**
     * Get calculated picture preview size
     *
     * @param cameraType camera type, aka, camera1 or camera2
     * @return           the picture preview size
     */
    fun getPicturePreviewSize(@CameraType cameraType: Int): Size?

    /**
     * Get calculated video size
     *
     * @param cameraType camera type, aka, camera1 or camera2
     * @return           the video size
     */
    fun getVideoSize(@CameraType cameraType: Int): Size?

    /**
     * Get calculated video preview size
     *
     * @param cameraType camera type, aka, camera1 or camera2
     * @return           the video preview size
     */
    fun getVideoPreviewSize(@CameraType cameraType: Int): Size?
}