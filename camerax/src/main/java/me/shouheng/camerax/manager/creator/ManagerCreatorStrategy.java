package me.shouheng.camerax.manager.creator;

import android.support.annotation.NonNull;
import me.shouheng.camerax.manager.CameraManager;
import me.shouheng.camerax.preview.CameraPreview;

/**
 * The strategy interface used to create {@link CameraManager}
 */
public interface ManagerCreatorStrategy {

    /**
     * The method used to create camera manager.
     *
     * @param callback the inner callback.
     * @param cameraPreview the {@link CameraPreview}
     * @return the {@link CameraManager}
     */
    @NonNull
    CameraManager create(CameraManager.Callback callback, CameraPreview cameraPreview);
}
