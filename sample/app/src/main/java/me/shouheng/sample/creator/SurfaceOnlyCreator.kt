package me.shouheng.sample.creator

import android.content.Context
import android.view.ViewGroup
import me.shouheng.xcamera.config.creator.CameraPreviewCreator
import me.shouheng.xcamera.preview.CameraPreview
import me.shouheng.xcamera.preview.impl.SurfacePreview

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