package me.shouheng.camerax.preview.impl;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

/**
 * The {@link me.shouheng.camerax.preview.CameraPreview} based on {@link android.view.SurfaceView}.
 */
public class SurfaceViewPreview extends AbstractCameraPreview<SurfaceView> {

    private final SurfaceView surfaceView;

    public SurfaceViewPreview(Context context, ViewGroup parent) {
        surfaceView = new SurfaceView(context);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        parent.addView(surfaceView, params);
        final SurfaceHolder holder = surfaceView.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                // empty
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                setSize(width, height);
                if (!ViewCompat.isInLayout(surfaceView)) {
                    dispatchSurfaceChanged();
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                setSize(0, 0);
            }
        });
    }

    @Override
    public SurfaceView getView() {
        return surfaceView;
    }

    @Override
    public Surface getSurface() {
        return surfaceView.getHolder().getSurface();
    }

    @Override
    public SurfaceHolder getSurfaceHolder() {
        return surfaceView.getHolder();
    }

    @Override
    public boolean isReady() {
        return getWidth() != 0 && getHeight() != 0;
    }

    @Override
    public Class<SurfaceView> getOutputClass() {
        return SurfaceView.class;
    }
}
