package me.shouheng.camerax.config.creator.impl;

import android.content.Context;
import android.os.Build;
import me.shouheng.camerax.config.creator.CameraManagerCreator;
import me.shouheng.camerax.manager.CameraManager;
import me.shouheng.camerax.manager.impl.Camera1Manager;
import me.shouheng.camerax.manager.impl.Camera2Manager;
import me.shouheng.camerax.preview.CameraPreview;
import me.shouheng.camerax.util.CameraHelper;

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
     * @param context context
     * @param cameraPreview the {@link CameraPreview}
     * @return CameraManager object.
     */
    @Override
    public CameraManager create(Context context, CameraPreview cameraPreview) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && CameraHelper.hasCamera2(context)) {
            return new Camera2Manager(cameraPreview);
        }
        return new Camera1Manager(cameraPreview);
    }
}
