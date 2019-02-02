package me.shouheng.camerax.preview.creator;

import android.support.annotation.NonNull;
import me.shouheng.camerax.preview.CameraPreview;

/**
 * The creator strategy for {@link CameraPreview}, will be used in {@link me.shouheng.camerax.preview.CameraPreviewFactory}.
 */
public interface PreviewCreatorStrategy {

    /**
     * The implementation method to create {@link CameraPreview}.
     *
     * @return the {@link CameraPreview}.
     */
    @NonNull
    CameraPreview create();
}
