package me.shouheng.sample.creator

import android.content.Context
import android.view.ViewGroup
import me.shouheng.camerax.config.creator.CameraPreviewCreator
import me.shouheng.camerax.preview.CameraPreview
import me.shouheng.camerax.preview.impl.TexturePreview

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/20 12:00
 */
class TextureOnlyCreator : CameraPreviewCreator {

    override fun create(context: Context?, parent: ViewGroup?): CameraPreview  = TexturePreview(context, parent)

}