package me.shouheng.icamera.config.creator.impl;

import android.content.Context;
import android.view.ViewGroup;

import me.shouheng.icamera.config.creator.CameraPreviewCreator;
import me.shouheng.icamera.preview.CameraPreview;
import me.shouheng.icamera.preview.impl.SurfacePreview;

/**
 * @author <a href="mailto:shouheng2015@gmail.com">WngShhng</a>
 * @version 2020-08-31 10:49
 */
public class SurfaceViewOnlyCreator implements CameraPreviewCreator {

    @Override
    public CameraPreview create(Context context, ViewGroup parent) {
        return new SurfacePreview(context, parent);
    }
}
