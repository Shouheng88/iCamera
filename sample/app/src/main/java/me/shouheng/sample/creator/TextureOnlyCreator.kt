package me.shouheng.sample.creator

import android.content.Context
import android.view.ViewGroup
import me.shouheng.shining.config.creator.CameraPreviewCreator
import me.shouheng.shining.preview.CameraPreview
import me.shouheng.shining.preview.impl.TexturePreview

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/20 12:00
 */
class TextureOnlyCreator : CameraPreviewCreator {

    override fun create(context: Context?, parent: ViewGroup?): CameraPreview  = TexturePreview(context, parent)

}