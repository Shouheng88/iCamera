package me.shouheng.camerax.configuration;

import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.support.annotation.NonNull;
import me.shouheng.camerax.manager.CameraManager;
import me.shouheng.camerax.utils.Size;

/**
 * The strategy interface used to calculate the size used in
 * {@link me.shouheng.camerax.manager.CameraManager}
 */
public interface SizeCalculateStrategy {

    /**
     * Method to calculate size for output photo and video.
     *
     * @param cameraParameters camera parameters
     * @param configuration configuration
     * @param cameraManager camera manager
     * @return result wrapper
     */
    Result calculate(Camera.Parameters cameraParameters, Configuration configuration, CameraManager cameraManager);

    /**
     * Wrapper class for calculate result.
     */
    class Result {
        public final CamcorderProfile camcorderProfile;
        public final Size photoSize;
        public final Size videoSize;
        public final Size previewSize;

        public Result(CamcorderProfile camcorderProfile, Size photoSize, Size videoSize, Size previewSize) {
            this.camcorderProfile = camcorderProfile;
            this.photoSize = photoSize;
            this.videoSize = videoSize;
            this.previewSize = previewSize;
        }

        @NonNull
        @Override
        public String toString() {
            return "Result{" +
                    "camcorderProfile=" + camcorderProfile +
                    ", photoSize=" + photoSize +
                    ", videoSize=" + videoSize +
                    ", previewSize=" + previewSize +
                    '}';
        }
    }
}
