package me.shouheng.camerax.preview.impl;

import android.view.View;
import me.shouheng.camerax.preview.CameraPreview;

/**
 * Abstract camera preview.
 */
abstract class AbstractCameraPreview<Preview extends View> implements CameraPreview<Preview> {

    private Callback callback;

    @Override
    public void setCallback(Callback callback) {
        this.callback = callback;
    }
}
