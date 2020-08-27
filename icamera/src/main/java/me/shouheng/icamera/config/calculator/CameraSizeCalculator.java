package me.shouheng.icamera.config.calculator;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import me.shouheng.icamera.config.size.AspectRatio;
import me.shouheng.icamera.config.size.Size;
import me.shouheng.icamera.enums.CameraType;
import me.shouheng.icamera.enums.MediaQuality;

/**
 * Calculator size for camera. You may implement this interface to add your own camera
 * size calculation logic.
 *
 * @see me.shouheng.icamera.config.calculator.impl.CameraSizeCalculatorImpl as an example
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:58
 */
public interface CameraSizeCalculator {

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
    void init(@NonNull AspectRatio expectAspectRatio,
              @Nullable Size expectSize,
              @MediaQuality int mediaQuality,
              @NonNull List<Size> previewSizes,
              @NonNull List<Size> pictureSizes,
              @NonNull List<Size> videoSizes);

    /**
     * Change expect aspect ratio. You can implement this method to get the new desired
     * aspect ratio and clear the calculated values cache. Anyway this is the method
     * we used to notify you the camera state changed.
     *
     * See also,
     * @see #changeExpectSize(Size)
     * @see #changeMediaQuality(int)
     *
     * @param expectAspectRatio the new expect aspect ratio
     */
    void changeExpectAspectRatio(@NonNull AspectRatio expectAspectRatio);

    /**
     * Change expect size
     *
     * @param expectSize the new expect size
     */
    void changeExpectSize(@Nullable Size expectSize);

    /**
     * Change expect media quality
     *
     * @param mediaQuality the new expect media quality
     */
    void changeMediaQuality(@MediaQuality int mediaQuality);

    /**
     * Get calculated picture size
     *
     * @param cameraType camera type, aka, camera1 or camera2
     * @return           the picture size
     */
    Size getPictureSize(@CameraType int cameraType);

    /**
     * Get calculated picture preview size
     *
     * @param cameraType camera type, aka, camera1 or camera2
     * @return           the picture preview size
     */
    Size getPicturePreviewSize(@CameraType int cameraType);

    /**
     * Get calculated video size
     *
     * @param cameraType camera type, aka, camera1 or camera2
     * @return           the video size
     */
    Size getVideoSize(@CameraType int cameraType);

    /**
     * Get calculated video preview size
     *
     * @param cameraType camera type, aka, camera1 or camera2
     * @return           the video preview size
     */
    Size getVideoPreviewSize(@CameraType int cameraType);

}
