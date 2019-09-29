package me.shouheng.shining.config.calculator;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import me.shouheng.shining.config.sizes.AspectRatio;
import me.shouheng.shining.config.sizes.Size;

import java.util.List;

/**
 * Calculator size for camera.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:58
 */
public interface CameraSizeCalculator {

    /**
     * Get size for picture preview.
     *
     * @param previewSizes preview sizes supported
     * @param pictureSize  desired picture size
     * @return             the final picture preview size
     */
    Size getPicturePreviewSize(@NonNull List<Size> previewSizes, @NonNull Size pictureSize);

    /**
     * Get size for video preview.
     *
     * @param previewSizes preview sizes supported
     * @param videoSize    desired video size
     * @return             the final video preview size
     */
    Size getVideoPreviewSize(@NonNull List<Size> previewSizes, @NonNull Size videoSize);

    /**
     * Get picture size.
     *
     * @param pictureSizes      supported picture size
     * @param expectAspectRatio desired aspect ratio of picture
     * @param expectSize        desired size of picture
     * @return                  the picture size
     */
    Size getPictureSize(@NonNull List<Size> pictureSizes, @NonNull AspectRatio expectAspectRatio, @Nullable Size expectSize);

    /**
     * Get video size.
     *
     * @param videoSizes        supported video sizes
     * @param expectAspectRatio desired aspect ratio of video
     * @param expectSize        desired size of video
     * @return                  the video size
     */
    Size getVideoSize(@NonNull List<Size> videoSizes, @NonNull AspectRatio expectAspectRatio, @Nullable Size expectSize);
}
