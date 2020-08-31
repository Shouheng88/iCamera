package me.shouheng.icamera.config.creator.impl;

import android.content.Context;

import me.shouheng.icamera.config.creator.CameraManagerCreator;
import me.shouheng.icamera.manager.CameraManager;
import me.shouheng.icamera.manager.impl.Camera1Manager;
import me.shouheng.icamera.preview.CameraPreview;

/**
 * @author <a href="mailto:shouheng2015@gmail.com">WngShhng</a>
 * @version 2020-08-31 10:47
 */
public class Camera1OnlyCreator implements CameraManagerCreator {

    @Override
    public CameraManager create(Context context, CameraPreview cameraPreview) {
        return new Camera1Manager(cameraPreview);
    }
}
