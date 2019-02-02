package me.shouheng.camerax.configuration;

import android.media.CamcorderProfile;
import me.shouheng.camerax.utils.Size;

import java.util.List;

/**
 * The strategy interface used to calculate the size used in
 * {@link me.shouheng.camerax.manager.CameraManager}
 */
public interface SizeCalculateStrategy {

    /**
     * Calculate camcorder according to configuration and camera id.
     *
     * @param configuration the configuration for camera.
     * @param cameraId the camera id.
     * @return the camcorder profile instance.
     */
    CamcorderProfile calCamcorderProfile(Configuration configuration, int cameraId);

    /**
     * Calculate the output photo size.
     *
     * @param sizes the supported sizes for camera.
     * @param camcorderProfile camcorder profile.
     * @return the photo size.
     */
    Size calPhotoSize(List<Size> sizes, CamcorderProfile camcorderProfile);

    /**
     * Calculate the output video size.
     *
     * @param sizes the supported sizes for camera.
     * @param camcorderProfile camcorder profile
     * @return the video size.
     */
    Size calVideoSize(List<Size> sizes, CamcorderProfile camcorderProfile);

    /**
     * Calculate the preview photo size.
     *
     * @param sizes the supported sizes for camera.
     * @param camcorderProfile camcorder profile.
     * @return the preview photo size.
     */
    Size calPreviewSize(List<Size> sizes, CamcorderProfile camcorderProfile);
}
