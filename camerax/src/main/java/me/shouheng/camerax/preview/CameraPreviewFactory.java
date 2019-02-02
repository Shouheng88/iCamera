package me.shouheng.camerax.preview;

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
     * @return the {@link CameraPreview} instance.
     */
    public static CameraPreview getCameraPreview() {
        if (previewCreatorStrategy == null) {
            previewCreatorStrategy = new DefaultPreviewCreatorStrategy();
        }
        return previewCreatorStrategy.create();
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
