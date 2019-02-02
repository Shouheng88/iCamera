package me.shouheng.camerax.manager.impl;

import android.hardware.Camera;
import me.shouheng.camerax.preview.CameraPreview;

public class Camera1Manager extends AbstractCameraManager<Integer> {

    private Camera.PreviewCallback previewCallback;
    private Camera.Parameters cameraParameters;

    public static Camera1Manager getInstance(Callback callback, CameraPreview cameraPreview) {
        return new Camera1Manager(callback, cameraPreview);
    }

    private Camera1Manager(final Callback callback, CameraPreview cameraPreview) {
        super(callback, cameraPreview);
        previewCallback = new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                callback.onPreviewFrame(data,
                        cameraParameters.getPreviewSize().width,
                        cameraParameters.getPreviewSize().height,
                        cameraParameters.getPreviewFormat());
            }
        };
        cameraPreview.setCallback(new CameraPreview.Callback() {
            @Override
            public void onSurfaceChanged() {

            }
        });
    }

    @Override
    public boolean start() {

        return false;
    }
}
