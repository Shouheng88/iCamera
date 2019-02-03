package me.shouheng.camerax.configuration.impl;

import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.util.Log;
import me.shouheng.camerax.configuration.Configuration;
import me.shouheng.camerax.configuration.SizeCalculateStrategy;
import me.shouheng.camerax.enums.Media;
import me.shouheng.camerax.manager.CameraManager;
import me.shouheng.camerax.utils.CameraHelper;
import me.shouheng.camerax.utils.Size;

import java.util.List;

/**
 * Default implementation for camera output size strategy.
 */
public class SizeCalculateStrategyImpl implements SizeCalculateStrategy {

    private static final String TAG = "DefaultSizeCalculateStr";

    @Override
    public Result calculate(Camera.Parameters cameraParameters, Configuration configuration, CameraManager cameraManager) {
        Object cameraId = cameraManager.getCurrentCameraId();
        if (!(cameraId instanceof String) && !(cameraId instanceof Integer)) {
            throw new IllegalStateException("The camera id should be integer or string.");
        }

        List<Size> previewSizes = Size.fromList(cameraParameters.getSupportedPreviewSizes());
        List<Size> videoSizes = Size.fromList(cameraParameters.getSupportedVideoSizes());
        List<Size> photoSizes = Size.fromList(cameraParameters.getSupportedPictureSizes());

        // 1. Get camcorder profile.
        CamcorderProfile camcorderProfile;
        if (configuration.getMediaQuality() == Media.MEDIA_QUALITY_AUTO) {
            if (cameraId instanceof String) {
                camcorderProfile = CameraHelper.getCamcorderProfile(
                        (String) cameraId, configuration.getVideoFileSize(), configuration.getMinimumVideoDuration());
            } else {
                camcorderProfile = CameraHelper.getCamcorderProfile(
                        (Integer) cameraId, configuration.getVideoFileSize(), configuration.getMinimumVideoDuration());
            }
        } else {
            if (cameraId instanceof String) {
                camcorderProfile = CameraHelper.getCamcorderProfile(configuration.getMediaQuality(), (String) cameraId);
            } else {
                camcorderProfile = CameraHelper.getCamcorderProfile(configuration.getMediaQuality(), (Integer) cameraId);
            }
        }

        // 2. Get video size.
        Size videoSize = CameraHelper.getSizeWithClosestRatio(
                (videoSizes == null || videoSizes.isEmpty()) ? previewSizes : videoSizes,
                camcorderProfile.videoFrameWidth, camcorderProfile.videoFrameHeight);

        // 3. Get photo size.
        Size photoSize = CameraHelper.getPictureSize(
                (photoSizes == null || photoSizes.isEmpty()) ? previewSizes : photoSizes,
                configuration.getMediaQuality() == Media.MEDIA_QUALITY_AUTO
                        ? Media.MEDIA_QUALITY_HIGHEST : configuration.getMediaQuality());

        // 4. Get preview size.
        Size previewSize;
        if (configuration.getMediaAction() == Media.MEDIA_ACTION_PHOTO
                || configuration.getMediaAction() == Media.MEDIA_ACTION_UNSPECIFIED) {
            previewSize = CameraHelper.getSizeWithClosestRatio(previewSizes, photoSize.getWidth(), photoSize.getHeight());
        } else {
            previewSize = CameraHelper.getSizeWithClosestRatio(previewSizes, videoSize.getWidth(), videoSize.getHeight());
        }

        return new Result(camcorderProfile, photoSize, videoSize, previewSize);
    }
}
