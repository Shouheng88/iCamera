package me.shouheng.icamera.config.calculator.impl;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import java.util.List;

import me.shouheng.icamera.config.calculator.CameraSizeCalculator;
import me.shouheng.icamera.config.size.AspectRatio;
import me.shouheng.icamera.config.size.Size;
import me.shouheng.icamera.enums.MediaQuality;
import me.shouheng.icamera.util.CameraHelper;
import me.shouheng.icamera.util.XLog;

/**
 * Default implementation for {@link CameraSizeCalculator}.
 *
 * Sample calculation strategies:
 * @see CameraHelper#getSizeWithClosestRatio(List, Size)
 * @see CameraHelper#getSizeWithClosestRatioSizeAndQuality(List, AspectRatio, Size, int)
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:58
 */
public class CameraSizeCalculatorImpl implements CameraSizeCalculator {

    private static final String TAG = "CameraSizeCalculator";

    private List<Size> previewSizes;
    private List<Size> pictureSizes;
    private List<Size> videoSizes;
    private AspectRatio expectAspectRatio;
    @Nullable private Size expectSize;
    @MediaQuality private int mediaQuality;

    private SparseArray<Size> outPictureSizes = new SparseArray<>();
    private SparseArray<Size> outVideoSizes = new SparseArray<>();
    private SparseArray<Size> outPicturePreviewSizes = new SparseArray<>();
    private SparseArray<Size> outVideoPreviewSizes = new SparseArray<>();

    @Override
    public void init(@NonNull AspectRatio expectAspectRatio,
                     @Nullable Size expectSize,
                     @MediaQuality int mediaQuality,
                     @NonNull List<Size> previewSizes,
                     @NonNull List<Size> pictureSizes,
                     @NonNull List<Size> videoSizes) {
        this.expectAspectRatio = expectAspectRatio;
        this.expectSize = expectSize;
        this.mediaQuality = mediaQuality;
        this.previewSizes = previewSizes;
        this.pictureSizes = pictureSizes;
        this.videoSizes = videoSizes;
    }

    @Override
    public void changeExpectAspectRatio(@NonNull AspectRatio expectAspectRatio) {
        XLog.d(TAG, "changeExpectAspectRatio : cache cleared");
        this.expectAspectRatio = expectAspectRatio;
        outPictureSizes.clear();
        outPicturePreviewSizes.clear();
        outVideoSizes.clear();
        outVideoPreviewSizes.clear();
    }

    @Override
    public void changeExpectSize(@Nullable Size expectSize) {
        XLog.d(TAG, "changeExpectSize : cache cleared");
        this.expectSize = expectSize;
        outPictureSizes.clear();
        outPicturePreviewSizes.clear();
        outVideoSizes.clear();
        outVideoPreviewSizes.clear();
    }

    @Override
    public void changeMediaQuality(int mediaQuality) {
        XLog.d(TAG, "changeMediaQuality : cache cleared");
        this.mediaQuality = mediaQuality;
        outPictureSizes.clear();
        outPicturePreviewSizes.clear();
        outVideoSizes.clear();
        outVideoPreviewSizes.clear();
    }

    @Override
    public Size getPictureSize(int cameraType) {
        Size size = outPictureSizes.get(cameraType);
        if (size != null) {
            return size;
        }
        size = CameraHelper.getSizeWithClosestRatioSizeAndQuality(
                pictureSizes, expectAspectRatio, expectSize, mediaQuality);
        outPictureSizes.put(cameraType, size);
        XLog.d(TAG, "getPictureSize : " + size);
        return size;
    }

    @Override
    public Size getPicturePreviewSize(int cameraType) {
        Size size = outPicturePreviewSizes.get(cameraType);
        if (size != null) {
            return size;
        }
        size = CameraHelper.getSizeWithClosestRatio(previewSizes, getPictureSize(cameraType));
        outPicturePreviewSizes.put(cameraType, size);
        XLog.d(TAG, "getPicturePreviewSize : " + size);
        return size;
    }

    @Override
    public Size getVideoSize(int cameraType) {
        Size size = outVideoSizes.get(cameraType);
        if (size != null) {
            return size;
        }
        size = CameraHelper.getSizeWithClosestRatioSizeAndQuality(
                videoSizes, expectAspectRatio, expectSize, mediaQuality);
        outVideoSizes.put(cameraType, size);
        XLog.d(TAG, "getVideoSize : " + size);
        return null;
    }

    @Override
    public Size getVideoPreviewSize(int cameraType) {
        Size size = outVideoPreviewSizes.get(cameraType);
        if (size != null) {
            return size;
        }
        size = CameraHelper.getSizeWithClosestRatio(previewSizes, getVideoSize(cameraType));
        outVideoPreviewSizes.put(cameraType, size);
        XLog.d(TAG, "getVideoPreviewSize : " + size);
        return size;
    }
}
