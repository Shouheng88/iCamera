package me.shouheng.icamera.config.calculator;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import me.shouheng.icamera.config.ConfigurationProvider;
import me.shouheng.icamera.config.size.AspectRatio;
import me.shouheng.icamera.config.size.Size;

/**
 * Calculator size for camera.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:58
 */
public interface CameraSizeCalculator {

    /**
     * Get picture size.
     *
     * @param pictureSizes      supported picture sizes
     * @param expectAspectRatio desired aspect ratio of picture,
     *                          default value is got from {@link ConfigurationProvider#getDefaultAspectRatio()}
     * @param expectSize        desired size of picture, null if the user didn't set
     * @return                  the picture size
     */
    Size getPictureSize(@NonNull List<Size> pictureSizes, @NonNull AspectRatio expectAspectRatio, @Nullable Size expectSize);

    /**
     * Get size for picture preview.
     *
     * @param previewSizes preview sizes supported
     * @param pictureSize  final output picture size,
     *                     got from {@link #getPictureSize(List, AspectRatio, Size)}
     * @return             final picture preview size
     */
    Size getPicturePreviewSize(@NonNull List<Size> previewSizes, @NonNull Size pictureSize);

    /**
     * Get video size.
     *
     * @param videoSizes        supported video sizes
     * @param expectAspectRatio desired aspect ratio of video
     *                          default value is got from {@link ConfigurationProvider#getDefaultAspectRatio()}
     * @param expectSize        desired size of video, null if the user didn't set
     * @return                  the video size
     */
    Size getVideoSize(@NonNull List<Size> videoSizes, @NonNull AspectRatio expectAspectRatio, @Nullable Size expectSize);

    /**
     * Get size for video preview.
     *
     * @param previewSizes preview sizes supported
     * @param videoSize    final output video size,
     *                     got from {@link #getVideoSize(List, AspectRatio, Size)}
     * @return             final video preview size
     */
    Size getVideoPreviewSize(@NonNull List<Size> previewSizes, @NonNull Size videoSize);

}
