package me.shouheng.icamera.preview.impl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import me.shouheng.icamera.enums.PreviewViewType;

/**
 * Camera preview implementation based on {@link TextureView}.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:54
 */
public class TexturePreview extends BaseCameraPreview {

    private SurfaceTexture surfaceTexture;
    private TextureView textureView;

    public TexturePreview(Context context, ViewGroup parent) {
        super(context, parent);
        textureView = new TextureView(context);
        textureView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        parent.addView(textureView);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            /**
             * Invoked when a {@link TextureView}'s SurfaceTexture is ready for use.
             *
             * @param surface The surface returned by
             *                {@link android.view.TextureView#getSurfaceTexture()}
             * @param width   The width of the surface
             * @param height  The height of the surface
             */
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                updateSurfaceTexture(surface, width, height);
                notifyPreviewAvailable();
            }

            /**
             * Invoked when the {@link SurfaceTexture}'s buffers size changed.
             *
             * @param surface The surface returned by
             *                {@link android.view.TextureView#getSurfaceTexture()}
             * @param width   The new width of the surface
             * @param height  The new height of the surface
             */
            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                updateSurfaceTexture(surface, width, height);
                notifyPreviewAvailable();
            }

            /**
             * Invoked when the specified {@link SurfaceTexture} is about to be destroyed.
             * If returns true, no rendering should happen inside the surface texture after this method
             * is invoked. If returns false, the client needs to call {@link SurfaceTexture#release()}.
             * Most applications should return true.
             *
             * @param surface The surface about to be destroyed
             */
            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                updateSurfaceTexture(surface, 0, 0);
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                /* noop */
            }
        });
    }

    @Override
    public Surface getSurface() {
        return new Surface(surfaceTexture);
    }

    @Override
    @PreviewViewType
    public int getPreviewType() {
        return PreviewViewType.TEXTURE_VIEW;
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

    @Override
    public View getView() {
        return textureView;
    }

    /*-----------------------------------------inner methods---------------------------------------------*/

    private void updateSurfaceTexture(SurfaceTexture surfaceTexture, int width, int height) {
        this.surfaceTexture = surfaceTexture;
        setSize(width, height);
    }
}
