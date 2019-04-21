package me.shouheng.camerax.preview.impl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.ViewGroup;
import me.shouheng.camerax.enums.Preview;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:54
 */
public class TexturePreview extends BaseCameraPreview {

    private SurfaceTexture surfaceTexture;
    private Surface surface;

    public TexturePreview(Context context, ViewGroup parent) {
        super(context, parent);
        TextureView textureView = new TextureView(context);
        textureView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        parent.addView(textureView);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                updateSurfaceTexture(surface, width, height);
                notifyPreviewAvailable();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                updateSurfaceTexture(surface, width, height);
                notifyPreviewAvailable();
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                updateSurfaceTexture(surface, 0, 0);
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
    }

    @Override
    public Surface getSurface() {
        if (surface == null) {
            surface = new Surface(surfaceTexture);
        }
        return surface;
    }

    @Override
    @Preview.Type
    public int getPreviewType() {
        return Preview.TEXTURE_VIEW;
    }

    @Nullable
    @Override
    public SurfaceHolder getSurfaceHolder() {
        return null;
    }

    @Nullable
    @Override
    public SurfaceTexture getSurfaceTexture() {
        return surfaceTexture;
    }

    /*-----------------------------------------inner methods---------------------------------------------*/

    private void updateSurfaceTexture(SurfaceTexture surfaceTexture, int width, int height) {
        this.surfaceTexture = surfaceTexture;
        setSize(width, height);
    }
}
