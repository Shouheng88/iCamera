package me.shouheng.shining.config.creator;

import android.content.Context;
import me.shouheng.shining.manager.CameraManager;
import me.shouheng.shining.preview.CameraPreview;

/**
 * Creator for {@link CameraManager}.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:55
 */
public interface CameraManagerCreator {

    /**
     * Method used to create {@link CameraManager}.
     *
     * @param context the context
     * @param cameraPreview the {@link CameraPreview}
     * @return CameraManager object.
     */
    CameraManager create(Context context, CameraPreview cameraPreview);
}
