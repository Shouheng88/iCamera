package me.shouheng.shining.config.calculator.impl;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import me.shouheng.shining.config.calculator.CameraSizeCalculator;
import me.shouheng.shining.config.sizes.AspectRatio;
import me.shouheng.shining.config.sizes.Size;
import me.shouheng.shining.util.CameraHelper;
import me.shouheng.shining.util.SLogger;

import java.util.List;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:58
 */
public class CameraSizeCalculatorImpl implements CameraSizeCalculator {

    private static final String TAG = "CameraSizeCalculatorImp";

    private Size lastPictureSizeForPreview;
    private Size lastVideoSizeForPreview;

    private Size lastPictureSize;
    private Size lastVideoSize;
    private Size picturePreviewSize;
    private Size videoPreviewSize;

    private SizeCalculatorMethod lastVideoSizeCalculatorMethod;
    private SizeCalculatorMethod lastPictureSizeCalculatorMethod;

    @Override
    public Size getPicturePreviewSize(@NonNull List<Size> previewSizes, @NonNull Size pictureSize) {
        if (picturePreviewSize == null || !lastPictureSizeForPreview.equals(pictureSize)) {
            lastPictureSizeForPreview = pictureSize;
            picturePreviewSize = CameraHelper.getSizeWithClosestRatio(previewSizes, pictureSize);
        }
        return picturePreviewSize;
    }

    @Override
    public Size getVideoPreviewSize(@NonNull List<Size> previewSizes, @NonNull Size videoSize) {
        if (videoPreviewSize == null || !lastVideoSizeForPreview.equals(videoSize)) {
            lastVideoSizeForPreview = videoSize;
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
        SLogger.d(TAG, "getVideoSize : " + lastPictureSize);
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
        SLogger.d(TAG, "getVideoSize : " + lastVideoSize);
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
