package me.shouheng.icamera.preview.impl

import android.content.Context
import android.graphics.SurfaceTexture
import android.view.*
import me.shouheng.icamera.enums.PreviewViewType

/**
 * Camera preview implementation based on [SurfaceView].
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:53
 */
class SurfacePreview(context: Context, parent: ViewGroup) : BaseCameraPreview() {

    private val surfaceView: SurfaceView = SurfaceView(context)

    override val surface: Surface?
        get() = surfaceHolder?.surface

    @get:PreviewViewType
    override val previewType: Int
        get() = PreviewViewType.SURFACE_VIEW

    override var surfaceHolder: SurfaceHolder? = null

    override val surfaceTexture: SurfaceTexture? get() = null

    override val view: View get() = surfaceView

    /*-----------------------------------------inner methods---------------------------------------------*/
    private fun updateSurfaceTexture(surfaceHolder: SurfaceHolder, width: Int, height: Int) {
        this.surfaceHolder = surfaceHolder
        setSize(width, height)
    }

    init {
        surfaceView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )
        parent.addView(surfaceView)
        surfaceHolder = surfaceView.holder
        // Enable or disable option to keep the screen turned on while this
        // surface is displayed.  The default is false, allowing it to turn off.
        // This is safe to call from any thread.
        surfaceHolder?.setKeepScreenOn(true)
        surfaceHolder?.addCallback(object : SurfaceHolder.Callback {
            /**
             * This is called immediately after the surface is first created.
             * Implementations of this should start up whatever rendering code
             * they desire.  Note that only one thread can ever draw into
             * a [Surface], so you should not draw into the Surface here
             * if your normal rendering will be in another thread.
             *
             * @param holder The SurfaceHolder whose surface is being created.
             */
            override fun surfaceCreated(holder: SurfaceHolder) { /* noop */ }

            /**
             * This is called immediately after any structural changes (format or
             * size) have been made to the surface.  You should at this point update
             * the imagery in the surface.  This method is always called at least
             * once, after [.surfaceCreated].
             *
             * @param holder The SurfaceHolder whose surface has changed.
             * @param format The new PixelFormat of the surface.
             * @param width The new width of the surface.
             * @param height The new height of the surface.
             */
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                updateSurfaceTexture(holder, width, height)
                notifyPreviewAvailable()
            }

            /**
             * This is called immediately before a surface is being destroyed. After
             * returning from this call, you should no longer try to access this
             * surface.  If you have a rendering thread that directly accesses
             * the surface, you must ensure that thread is no longer touching the
             * Surface before returning from this function.
             *
             * @param holder The SurfaceHolder whose surface is being destroyed.
             */
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                updateSurfaceTexture(holder, 0, 0)
            }
        })
    }
}