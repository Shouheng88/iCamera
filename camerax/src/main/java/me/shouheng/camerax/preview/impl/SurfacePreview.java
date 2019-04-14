package me.shouheng.camerax.preview.impl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.ViewGroup;
import me.shouheng.camerax.enums.Preview;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:53
 */
public class SurfacePreview extends BaseCameraPreview {

    public SurfacePreview(Context context, ViewGroup parent) {
        super(context, parent);
    }

    @Override
    public Surface getSurface() {
        return null;
    }

    @Override
    @Preview.Type
    public int getPreviewType() {
        return Preview.SURFACE_VIEW;
    }

    @Nullable
    @Override
    public SurfaceHolder getSurfaceHolder() {
        return null;
    }

    @Nullable
    @Override
    public SurfaceTexture getSurfaceTexture() {
        return null;
    }
}
