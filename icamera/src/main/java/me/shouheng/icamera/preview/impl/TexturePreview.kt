package me.shouheng.icamera.preview.impl

import android.content.Context
import android.graphics.SurfaceTexture
import android.view.*
import android.view.TextureView.SurfaceTextureListener
import me.shouheng.icamera.enums.PreviewViewType

/**
 * Camera preview implementation based on [TextureView].
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:54
 */
class TexturePreview(context: Context, parent: ViewGroup) : BaseCameraPreview() {

    override var surfaceTexture: SurfaceTexture? = null
        private set

    private val textureView: TextureView = TextureView(context)

    override val surface: Surface
        get() = Surface(surfaceTexture)

    @get:PreviewViewType
    override val previewType: Int
        get() = PreviewViewType.TEXTURE_VIEW

    override var surfaceHolder: SurfaceHolder? = null

    override val view: View
        get() = textureView

    /*-----------------------------------------inner methods---------------------------------------------*/
    private fun updateSurfaceTexture(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        this.surfaceTexture = surfaceTexture
        setSize(width, height)
    }

    init {
        textureView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )
        parent.addView(textureView)
        textureView.surfaceTextureListener = object : SurfaceTextureListener {
            /**
             * Invoked when a [TextureView]'s SurfaceTexture is ready for use.
             *
             * @param surface The surface returned by
             * [android.view.TextureView.getSurfaceTexture]
             * @param width   The width of the surface
             * @param height  The height of the surface
             */
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                updateSurfaceTexture(surface, width, height)
                notifyPreviewAvailable()
            }

            /**
             * Invoked when the [SurfaceTexture]'s buffers size changed.
             *
             * @param surface The surface returned by
             * [android.view.TextureView.getSurfaceTexture]
             * @param width   The new width of the surface
             * @param height  The new height of the surface
             */
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                updateSurfaceTexture(surface, width, height)
                notifyPreviewAvailable()
            }

            /**
             * Invoked when the specified [SurfaceTexture] is about to be destroyed.
             * If returns true, no rendering should happen inside the surface texture after this method
             * is invoked. If returns false, the client needs to call [SurfaceTexture.release].
             * Most applications should return true.
             *
             * @param surface The surface about to be destroyed
             */
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                updateSurfaceTexture(surface, 0, 0)
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {/* noop */ }
        }
    }
}