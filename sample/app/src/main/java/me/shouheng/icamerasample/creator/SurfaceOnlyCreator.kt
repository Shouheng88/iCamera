package me.shouheng.icamerasample.creator

import android.content.Context
import android.view.ViewGroup
import me.shouheng.icamera.config.creator.CameraPreviewCreator
import me.shouheng.icamera.preview.CameraPreview
import me.shouheng.icamera.preview.impl.SurfacePreview

/**
 * The camera preview creator only for surface view.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/20 12:00
 */
class SurfaceOnlyCreator : CameraPreviewCreator {

    override fun create(context: Context?, parent: ViewGroup?)
            : CameraPreview = SurfacePreview(context, parent)

}