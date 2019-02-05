package me.shouheng.camerax.manager.impl;

import me.shouheng.camerax.preview.CameraPreview;

public class Camera2Manager extends AbstractCameraManager<String> {

    public static Camera2Manager getInstance(Callback callback, CameraPreview cameraPreview) {
        return new Camera2Manager(callback, cameraPreview);
    }

    private Camera2Manager(Callback callback, CameraPreview cameraPreview) {
        super(callback, cameraPreview);
    }

    @Override
    public boolean openCamera(final String cameraId) {
        return false;
    }

    @Override
    public boolean isCameraOpened() {
        return false;
    }

    @Override
    public int getNumberOfCameras() {
        return 0;
    }

    @Override
    public int getFaceFrontCameraOrientation() {
        return 0;
    }

    @Override
    public int getFaceBackCameraOrientation() {
        return 0;
    }
}
