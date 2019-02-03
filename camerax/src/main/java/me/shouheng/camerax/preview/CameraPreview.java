package me.shouheng.camerax.preview;

import android.view.View;

public interface CameraPreview<PreviewView extends View> {

    void setCallback(Callback callback);

    boolean isReady();

    Class<PreviewView> getOutputClass();

    interface Callback {
        void onSurfaceChanged();
    }
}
