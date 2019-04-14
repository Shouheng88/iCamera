package me.shouheng.camerax.config.calculator;

import me.shouheng.camerax.config.sizes.AspectRatio;
import me.shouheng.camerax.config.sizes.Size;

import java.util.List;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:58
 */
public interface CameraSizeCalculator {

    Size getPicturePreviewSize(List<Size> previewSizes, Size pictureSize);

    Size getVideoPreviewSize(List<Size> previewSizes, Size videoSize);

    Size getPictureSize(List<Size> pictureSizes, AspectRatio expectAspectRatio, Size expectSize);

    Size getVideoSize(List<Size> videoSizes, AspectRatio expectAspectRatio, Size expectSize);
}
