package me.shouheng.icamera.preview.impl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import me.shouheng.icamera.enums.PreviewViewType;

/**
 * Camera preview implementation based on {@link SurfaceView}.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:53
 */
public class SurfacePreview extends BaseCameraPreview {

    private SurfaceHolder surfaceHolder;
    private SurfaceView surfaceView;

    public SurfacePreview(Context context, ViewGroup parent) {
        super(context, parent);
        surfaceView = new SurfaceView(context);
        surfaceView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        parent.addView(surfaceView);
        surfaceHolder = surfaceView.getHolder();
        // Enable or disable option to keep the screen turned on while this
        // surface is displayed.  The default is false, allowing it to turn off.
        // This is safe to call from any thread.
        surfaceHolder.setKeepScreenOn(true);
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            /**
             * This is called immediately after the surface is first created.
             * Implementations of this should start up whatever rendering code
             * they desire.  Note that only one thread can ever draw into
             * a {@link Surface}, so you should not draw into the Surface here
             * if your normal rendering will be in another thread.
             *
             * @param holder The SurfaceHolder whose surface is being created.
             */
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                /* noop */
            }

            /**
             * This is called immediately after any structural changes (format or
             * size) have been made to the surface.  You should at this point update
             * the imagery in the surface.  This method is always called at least
             * once, after {@link #surfaceCreated}.
             *
             * @param holder The SurfaceHolder whose surface has changed.
             * @param format The new PixelFormat of the surface.
             * @param width The new width of the surface.
             * @param height The new height of the surface.
             */
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                updateSurfaceTexture(holder, width, height);
                notifyPreviewAvailable();
            }

            /**
             * This is called immediately before a surface is being destroyed. After
             * returning from this call, you should no longer try to access this
             * surface.  If you have a rendering thread that directly accesses
             * the surface, you must ensure that thread is no longer touching the
             * Surface before returning from this function.
             *
             * @param holder The SurfaceHolder whose surface is being destroyed.
             */
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                updateSurfaceTexture(holder, 0, 0);
            }
        });
    }

    @Override
    public Surface getSurface() {
        return surfaceHolder.getSurface();
    }

    @Override
    @PreviewViewType
    public int getPreviewType() {
        return PreviewViewType.SURFACE_VIEW;
    }

    @Nullable
    @Override
    public SurfaceHolder getSurfaceHolder() {
        return surfaceHolder;
    }

    @Nullable
    @Override
    public SurfaceTexture getSurfaceTexture() {
        return null;
    }

    @Override
    public View getView() {
        return surfaceView;
    }

    /*-----------------------------------------inner methods---------------------------------------------*/

    private void updateSurfaceTexture(SurfaceHolder surfaceHolder, int width, int height) {
        this.surfaceHolder = surfaceHolder;
        setSize(width, height);
    }
}
