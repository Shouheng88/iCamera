package me.shouheng.camerax.preview.impl;

import android.view.TextureView;

/**
 * The {@link me.shouheng.camerax.preview.CameraPreview} based on {@link android.view.TextureView}.
 */
public class TextureViewPreview extends AbstractCameraPreview<TextureView> {
    @Override
    public boolean isReady() {
        // TODO
        return true;
    }

    @Override
    public Class<TextureView> getOutputClass() {
        return TextureView.class;
    }
}
