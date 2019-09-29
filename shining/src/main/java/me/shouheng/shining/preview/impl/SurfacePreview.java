package me.shouheng.shining.preview.impl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.support.annotation.Nullable;
import android.view.*;
import me.shouheng.shining.enums.Preview;

/**
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
        surfaceHolder.setKeepScreenOn(true);
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                updateSurfaceTexture(holder, width, height);
                notifyPreviewAvailable();
            }

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
    @Preview.Type
    public int getPreviewType() {
        return Preview.SURFACE_VIEW;
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
