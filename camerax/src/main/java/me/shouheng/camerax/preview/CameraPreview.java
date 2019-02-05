package me.shouheng.camerax.preview;

import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;

public interface CameraPreview<PreviewView extends View> {

    void setCallback(Callback callback);

    Class<PreviewView> getOutputClass();

    Surface getSurface();

    SurfaceHolder getSurfaceHolder();

    SurfaceTexture getSurfaceTexture();

    PreviewView getView();

    void setDisplayOrientation(int displayOrientation);

    boolean isReady();

    void setSize(int width, int height);

    interface Callback {
        void onSurfaceChanged();
    }
}
