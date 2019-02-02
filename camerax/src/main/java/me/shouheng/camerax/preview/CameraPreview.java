package me.shouheng.camerax.preview;

public interface CameraPreview {

    void setCallback(Callback callback);

    boolean isReady();

    interface Callback {
        void onSurfaceChanged();
    }
}
