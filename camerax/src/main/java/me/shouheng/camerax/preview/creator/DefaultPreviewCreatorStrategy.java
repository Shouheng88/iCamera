package me.shouheng.camerax.preview.creator;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.widget.FrameLayout;
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
    public CameraPreview create(Context context, FrameLayout parent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return new TextureViewPreview(context, parent);
        }
        return new SurfaceViewPreview(context, parent);
    }
}
