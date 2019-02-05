package me.shouheng.camerax.preview.creator;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.FrameLayout;
import me.shouheng.camerax.preview.CameraPreview;

/**
 * The creator strategy for {@link CameraPreview}, will be used in {@link me.shouheng.camerax.preview.CameraPreviewFactory}.
 */
public interface PreviewCreatorStrategy {

    /**
     * The implementation method to create {@link CameraPreview}.
     *
     * @param context the context used to get the view.
     * @param parent the parent the camera preview is attached to.
     * @return the {@link CameraPreview}.
     */
    @NonNull
    CameraPreview create(Context context, FrameLayout parent);
}
