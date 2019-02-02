package me.shouheng.camerax.preview.creator;

import android.os.Build;
import android.support.annotation.NonNull;
import me.shouheng.camerax.preview.CameraPreview;
import me.shouheng.camerax.preview.impl.SurfaceViewPreview;
import me.shouheng.camerax.preview.impl.TextureViewPreview;

/**
 * Default implementation for {@link PreviewCreatorStrategy}, will create the {@link CameraPreview}
 * base on the system version.
 */
public class DefaultPreviewCreatorStrategy implements PreviewCreatorStrategy {

    @NonNull
    @Override
    public CameraPreview create() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return new TextureViewPreview();
        }
        return new SurfaceViewPreview();
    }
}
