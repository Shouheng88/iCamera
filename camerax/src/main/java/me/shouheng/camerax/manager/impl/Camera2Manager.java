package me.shouheng.camerax.manager.impl;

import me.shouheng.camerax.preview.CameraPreview;

public class Camera2Manager extends AbstractCameraManager {

    public static Camera2Manager getInstance(Callback callback, CameraPreview cameraPreview) {
        return new Camera2Manager(callback, cameraPreview);
    }

    private Camera2Manager(Callback callback, CameraPreview cameraPreview) {
        super(callback, cameraPreview);
    }

    @Override
    public boolean start() {
        return false;
    }
}
