package me.shouheng.icamera.config.creator.impl

import android.content.Context
import android.os.Build
import android.view.ViewGroup
import me.shouheng.icamera.config.creator.CameraPreviewCreator
import me.shouheng.icamera.preview.CameraPreview
import me.shouheng.icamera.preview.impl.SurfacePreview
import me.shouheng.icamera.preview.impl.TexturePreview

/**
 * Default creator for [CameraPreview], will decide witch to use
 * according to build version.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:56
 */
class CameraPreviewCreatorImpl : CameraPreviewCreator {
    /**
     * The default implementation for [CameraPreview].
     * If the app version >= 14, [android.view.TextureView] will be used,
     * else [android.view.SurfaceView] will be used.
     *
     * @param context the context to create the preview view.
     * @param parent  the parent view of the preview.
     * @return        CameraPreview object.
     */
    override fun create(
        context: Context,
        parent: ViewGroup
    ): CameraPreview {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            TexturePreview(context, parent)
        } else SurfacePreview(context, parent)
    }
}