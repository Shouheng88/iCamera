package me.shouheng.camerax.configuration.impl;

import android.media.CamcorderProfile;
import me.shouheng.camerax.configuration.Configuration;
import me.shouheng.camerax.configuration.SizeCalculateStrategy;
import me.shouheng.camerax.enums.Media;
import me.shouheng.camerax.utils.CameraHelper;
import me.shouheng.camerax.utils.Size;

import java.util.List;

/**
 * Default implementation for camera output size strategy.
 */
public class DefaultSizeCalculateStrategy implements SizeCalculateStrategy {

    @Override
    public CamcorderProfile calCamcorderProfile(Configuration configuration, int cameraId) {
        if (configuration.getMediaQuality() == Media.MEDIA_QUALITY_AUTO) {
            return CameraHelper.getCamcorderProfile(cameraId, configuration.getVideoFileSize(), configuration.getMinimumVideoDuration());
        }
        return CameraHelper.getCamcorderProfile(configuration.getMediaQuality(), cameraId);
    }

    @Override
    public Size calPhotoSize(List<Size> sizes, CamcorderProfile camcorderProfile) {
        return null;
    }

    @Override
    public Size calVideoSize(List<Size> sizes, CamcorderProfile camcorderProfile) {
        return null;
    }

    @Override
    public Size calPreviewSize(List<Size> sizes, CamcorderProfile camcorderProfile) {
        return null;
    }

}
