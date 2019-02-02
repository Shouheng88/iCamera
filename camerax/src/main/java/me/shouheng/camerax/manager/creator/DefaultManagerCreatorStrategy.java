package me.shouheng.camerax.manager.creator;

import android.content.Context;
import android.support.annotation.NonNull;
import me.shouheng.camerax.manager.CameraManager;
import me.shouheng.camerax.manager.impl.Camera1Manager;
import me.shouheng.camerax.manager.impl.Camera2Manager;
import me.shouheng.camerax.preview.CameraPreview;
import me.shouheng.camerax.utils.CameraHelper;

/**
 * Default creator strategy for {@link CameraManager}. Choose {@link Camera1Manager} or {@link Camera2Manager}
 * according to {@link CameraHelper#hasCamera2(Context)}.
 */
public class DefaultManagerCreatorStrategy implements ManagerCreatorStrategy {

    private Context context;

    public DefaultManagerCreatorStrategy(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public CameraManager create(CameraManager.Callback callback, CameraPreview cameraPreview) {
        if (CameraHelper.hasCamera2(context)) {
            return Camera2Manager.getInstance(callback, cameraPreview);
        } else {
            return Camera1Manager.getInstance(callback, cameraPreview);
        }
    }
}
