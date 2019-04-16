package me.shouheng.camerax.config.calculator;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import me.shouheng.camerax.config.sizes.AspectRatio;
import me.shouheng.camerax.config.sizes.Size;

import java.util.List;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:58
 */
public interface CameraSizeCalculator {

    Size getPicturePreviewSize(@NonNull List<Size> previewSizes, @NonNull Size pictureSize);

    Size getVideoPreviewSize(@NonNull List<Size> previewSizes, @NonNull Size videoSize);

    Size getPictureSize(@NonNull List<Size> pictureSizes, @NonNull AspectRatio expectAspectRatio, @Nullable Size expectSize);

    Size getVideoSize(@NonNull List<Size> videoSizes, @NonNull AspectRatio expectAspectRatio, @Nullable Size expectSize);
}
