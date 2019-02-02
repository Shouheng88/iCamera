package me.shouheng.camerax.preview;

public interface CameraPreview {

    interface Callback {
        void onSurfaceChanged();
    }

    void setCallback(Callback callback);
}
