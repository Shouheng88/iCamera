package me.shouheng.camerax.config.calculator.impl;

import me.shouheng.camerax.config.calculator.CameraSizeCalculator;
import me.shouheng.camerax.config.sizes.AspectRatio;
import me.shouheng.camerax.config.sizes.Size;
import me.shouheng.camerax.util.CameraHelper;

import java.util.List;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:58
 */
public class CameraSizeCalculatorImpl implements CameraSizeCalculator {

    @Override
    public Size getPicturePreviewSize(List<Size> previewSizes, Size pictureSize) {
        return CameraHelper.getSizeWithClosestRatio(previewSizes, pictureSize);
    }

    @Override
    public Size getVideoPreviewSize(List<Size> previewSizes, Size videoSize) {
        return CameraHelper.getSizeWithClosestRatio(previewSizes, videoSize);
    }

    @Override
    public Size getPictureSize(List<Size> sizes, AspectRatio expectAspectRatio, Size expectSize) {
        return sizes.get(0);
    }

    @Override
    public Size getVideoSize(List<Size> sizes, AspectRatio expectAspectRatio, Size expectSize) {
        return sizes.get(0);
    }
}
