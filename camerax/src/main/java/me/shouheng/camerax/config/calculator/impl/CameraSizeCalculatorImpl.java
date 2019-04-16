package me.shouheng.camerax.config.calculator.impl;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import me.shouheng.camerax.config.calculator.CameraSizeCalculator;
import me.shouheng.camerax.config.sizes.AspectRatio;
import me.shouheng.camerax.config.sizes.Size;
import me.shouheng.camerax.util.CameraHelper;
import me.shouheng.camerax.util.Logger;

import java.util.List;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:58
 */
public class CameraSizeCalculatorImpl implements CameraSizeCalculator {

    private static final String TAG = "CameraSizeCalculatorImp";

    private Size lastPictureSize;
    private Size lastVideoSize;
    private Size picturePreviewSize;
    private Size videoPreviewSize;

    private SizeCalculatorMethod lastVideoSizeCalculatorMethod;
    private SizeCalculatorMethod lastPictureSizeCalculatorMethod;

    @Override
    public Size getPicturePreviewSize(@NonNull List<Size> previewSizes, @NonNull Size pictureSize) {
        if (picturePreviewSize == null || !lastPictureSize.equals(pictureSize)) {
            lastPictureSize = pictureSize;
            picturePreviewSize = CameraHelper.getSizeWithClosestRatio(previewSizes, pictureSize);
        }
        return picturePreviewSize;
    }

    @Override
    public Size getVideoPreviewSize(@NonNull List<Size> previewSizes, @NonNull Size videoSize) {
        if (videoPreviewSize == null || !lastVideoSize.equals(videoSize)) {
            lastVideoSize = videoSize;
            videoPreviewSize = CameraHelper.getSizeWithClosestRatio(previewSizes, videoSize);
        }
        return videoPreviewSize;
    }

    @Override
    public Size getPictureSize(@NonNull List<Size> pictureSizes, @NonNull AspectRatio expectAspectRatio, @Nullable Size expectSize) {
        SizeCalculatorMethod sizeCalculatorMethod = new SizeCalculatorMethod(expectAspectRatio, expectSize);
        if (lastPictureSize == null || !sizeCalculatorMethod.equals(lastPictureSizeCalculatorMethod)) {
            lastPictureSizeCalculatorMethod = sizeCalculatorMethod;
            if (expectSize == null) {
                expectSize = Size.of((int) (4000 * expectAspectRatio.ratio()), 4000 );
            }
            lastPictureSize = CameraHelper.getSizeWithClosestRatio(pictureSizes, expectSize);
        }
        Logger.d(TAG, "getVideoSize : " + lastPictureSize);
        return lastPictureSize;
    }

    @Override
    public Size getVideoSize(@NonNull List<Size> videoSizes, @NonNull AspectRatio expectAspectRatio, @Nullable Size expectSize) {
        SizeCalculatorMethod sizeCalculatorMethod = new SizeCalculatorMethod(expectAspectRatio, expectSize);
        if (lastVideoSize == null || !sizeCalculatorMethod.equals(lastVideoSizeCalculatorMethod)) {
            lastVideoSizeCalculatorMethod = sizeCalculatorMethod;
            if (expectSize == null) {
                expectSize = Size.of((int) (4000 * expectAspectRatio.ratio()), 4000 );
            }
            lastVideoSize = CameraHelper.getSizeWithClosestRatio(videoSizes, expectSize);
        }
        Logger.d(TAG, "getVideoSize : " + lastVideoSize);
        return lastVideoSize;
    }

    private static class SizeCalculatorMethod {
        @NonNull
        private AspectRatio expectAspectRatio;
        @Nullable
        private Size expectSize;

        SizeCalculatorMethod(@NonNull AspectRatio expectAspectRatio, @Nullable Size expectSize) {
            this.expectAspectRatio = expectAspectRatio;
            this.expectSize = expectSize;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SizeCalculatorMethod that = (SizeCalculatorMethod) o;

            if (!expectAspectRatio.equals(that.expectAspectRatio)) return false;
            return expectSize != null ? expectSize.equals(that.expectSize) : that.expectSize == null;
        }

        @Override
        public int hashCode() {
            int result = expectAspectRatio.hashCode();
            result = 31 * result + (expectSize != null ? expectSize.hashCode() : 0);
            return result;
        }
    }
}
