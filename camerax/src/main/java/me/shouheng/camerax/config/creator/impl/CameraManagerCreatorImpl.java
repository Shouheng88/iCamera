package me.shouheng.camerax.config.creator.impl;

import me.shouheng.camerax.config.creator.CameraManagerCreator;
import me.shouheng.camerax.manager.CameraManager;
import me.shouheng.camerax.manager.impl.Camera1Manager;
import me.shouheng.camerax.preview.CameraPreview;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:56
 */
public class CameraManagerCreatorImpl implements CameraManagerCreator {

    /**
     * The default implementation for {@link CameraManager}.
     * If the app version >= 21, the {@link android.hardware.camera2.CameraDevice} will be used,
     * else the {@link android.hardware.Camera} will be used.
     *
     * @param cameraPreview the {@link CameraPreview}
     * @return CameraManager object.
     */
    @Override
    public CameraManager create(CameraPreview cameraPreview) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            return new Camera2Manager(cameraPreview);
//        }
        // TODO remove when camera2 is ready
        return new Camera1Manager(cameraPreview);
    }
}
