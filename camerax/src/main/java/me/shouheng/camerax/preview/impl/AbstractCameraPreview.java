package me.shouheng.camerax.preview.impl;

import me.shouheng.camerax.preview.CameraPreview;

/**
 * Abstract camera preview.
 */
abstract class AbstractCameraPreview implements CameraPreview {

    private Callback callback;

    @Override
    public void setCallback(Callback callback) {
        this.callback = callback;
    }
}
