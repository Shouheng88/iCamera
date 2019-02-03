package me.shouheng.camerax.preview.impl;

import android.view.SurfaceView;

/**
 * The {@link me.shouheng.camerax.preview.CameraPreview} based on {@link android.view.SurfaceView}.
 */
public class SurfaceViewPreview extends AbstractCameraPreview<SurfaceView> {



    @Override
    public boolean isReady() {
        // TODO
        return true;
    }

    @Override
    public Class<SurfaceView> getOutputClass() {
        return SurfaceView.class;
    }
}
