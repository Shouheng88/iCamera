package me.shouheng.camerax.preview;

import android.content.Context;
import android.widget.FrameLayout;
import me.shouheng.camerax.preview.creator.DefaultPreviewCreatorStrategy;
import me.shouheng.camerax.preview.creator.PreviewCreatorStrategy;

/**
 * The factory for {@link CameraPreview}.
 */
public class CameraPreviewFactory {

    private static PreviewCreatorStrategy previewCreatorStrategy;

    private CameraPreviewFactory() {
    }

    /**
     * Get the {@link CameraPreview} instance base on the {@link #previewCreatorStrategy}.
     *
     * @param context the context.
     * @param parent the parent the camera preview is attached to.
     * @return the {@link CameraPreview} instance.
     */
    public static CameraPreview getCameraPreview(Context context, FrameLayout parent) {
        if (previewCreatorStrategy == null) {
            previewCreatorStrategy = new DefaultPreviewCreatorStrategy();
        }
        return previewCreatorStrategy.create(context, parent);
    }

    /**
     * Set the camera preview creator strategy.
     *
     * @param previewCreatorStrategy the preview creator strategy.
     */
    public static void setPreviewCreatorStrategy(PreviewCreatorStrategy previewCreatorStrategy) {
        CameraPreviewFactory.previewCreatorStrategy = previewCreatorStrategy;
    }
}
