package me.shouheng.icamera.config.creator.impl

import android.content.Context
import android.view.ViewGroup
import me.shouheng.icamera.config.creator.CameraPreviewCreator
import me.shouheng.icamera.preview.CameraPreview
import me.shouheng.icamera.preview.impl.SurfacePreview

/**
 * @author [WngShhng](mailto:shouheng2015@gmail.com)
 * @version 2020-08-31 10:49
 */
class SurfaceViewOnlyCreator : CameraPreviewCreator {
    override fun create(
        context: Context,
        parent: ViewGroup
    ): CameraPreview {
        return SurfacePreview(context, parent)
    }
}