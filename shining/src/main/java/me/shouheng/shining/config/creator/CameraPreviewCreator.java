package me.shouheng.shining.config.creator;

import android.content.Context;
import android.view.ViewGroup;
import me.shouheng.shining.preview.CameraPreview;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:56
 */
public interface CameraPreviewCreator {

    /**
     * Method used to create {@link CameraPreview}.
     *
     * @param context the context to create the preview view.
     * @param parent the parent view of the preview.
     * @return CameraPreview object.
     */
    CameraPreview create(Context context, ViewGroup parent);
}
