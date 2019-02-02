package me.shouheng.camerax.manager;

import android.content.Context;
import me.shouheng.camerax.manager.creator.DefaultManagerCreatorStrategy;
import me.shouheng.camerax.manager.creator.ManagerCreatorStrategy;
import me.shouheng.camerax.preview.CameraPreview;

/**
 * The {@link CameraManager} factory. You can call the {@link #setManagerCreatorStrategy(ManagerCreatorStrategy)}
 * to set the custom creator strategy.
 */
public class CameraManagerFactory {

    private static ManagerCreatorStrategy managerCreatorStrategy;

    private CameraManagerFactory() {
    }

    /**
     * Get the {@link CameraManager} instance base on the {@link #managerCreatorStrategy}.
     *
     * @param context the context.
     * @param callback the inner callback.
     * @param cameraPreview the camera preview.
     * @return the {@link CameraManager} instance.
     */
    public static CameraManager getCameraManager(Context context, CameraManager.Callback callback, CameraPreview cameraPreview) {
        if (managerCreatorStrategy == null) {
            managerCreatorStrategy = new DefaultManagerCreatorStrategy(context);
        }
        return managerCreatorStrategy.create(callback, cameraPreview);
    }

    /**
     * Set the camera manager creator strategy.
     *
     * @param managerCreatorStrategy the camera manager strategy.
     */
    public static void setManagerCreatorStrategy(ManagerCreatorStrategy managerCreatorStrategy) {
        CameraManagerFactory.managerCreatorStrategy = managerCreatorStrategy;
    }
}
