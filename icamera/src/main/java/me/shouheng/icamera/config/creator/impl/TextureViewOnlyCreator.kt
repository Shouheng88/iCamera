package me.shouheng.icamera.config.creator.impl

import android.content.Context
import android.view.ViewGroup
import me.shouheng.icamera.config.creator.CameraPreviewCreator
import me.shouheng.icamera.preview.CameraPreview
import me.shouheng.icamera.preview.impl.TexturePreview

/**
 * @author [WngShhng](mailto:shouheng2015@gmail.com)
 * @version 2020-08-31 10:49
 */
class TextureViewOnlyCreator : CameraPreviewCreator {
    override fun create(
        context: Context,
        parent: ViewGroup
    ): CameraPreview {
        return TexturePreview(context, parent)
    }
}