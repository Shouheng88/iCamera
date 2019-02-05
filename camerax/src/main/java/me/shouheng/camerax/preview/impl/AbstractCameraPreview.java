package me.shouheng.camerax.preview.impl;

import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import me.shouheng.camerax.preview.CameraPreview;

/**
 * Abstract camera preview.
 */
abstract class AbstractCameraPreview<Preview extends View> implements CameraPreview<Preview> {

    private Callback callback;
    private int width;
    private int height;

    @Override
    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @Override
    public Class<Preview> getOutputClass() {
        return null;
    }

    @Override
    public Surface getSurface() {
        return null;
    }

    @Override
    public SurfaceHolder getSurfaceHolder() {
        return null;
    }

    @Override
    public SurfaceTexture getSurfaceTexture() {
        return null;
    }

    @Override
    public Preview getView() {
        return null;
    }

    @Override
    public void setDisplayOrientation(int displayOrientation) {

    }

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    void dispatchSurfaceChanged() {
        callback.onSurfaceChanged();
    }
}
