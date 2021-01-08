package me.shouheng.icamera.config.creator

import android.content.Context
import android.view.ViewGroup
import me.shouheng.icamera.preview.CameraPreview

/**
 * The creator for [CameraPreview].
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:56
 */
interface CameraPreviewCreator {
    /**
     * Method used to create [CameraPreview].
     *
     * @param context the context to create the preview view.
     * @param parent the parent view of the preview.
     * @return CameraPreview object.
     */
    fun create(context: Context, parent: ViewGroup): CameraPreview
}