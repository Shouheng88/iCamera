package me.shouheng.icamera.preview

import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import me.shouheng.icamera.config.size.Size
import me.shouheng.icamera.enums.PreviewViewType

/**
 * The camera preview interface.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:53
 */
interface CameraPreview {

    /**
     * Add callback to the camera preview.
     *
     * @param cameraPreviewCallback the callback interface.
     */
    fun setCameraPreviewCallback(cameraPreviewCallback: CameraPreviewCallback?)

    /**
     * Get the surface from this preview that will be used for
     * [android.hardware.camera2.CameraDevice]
     *
     * @return surface
     */
    val surface: Surface?

    /**
     * Get the preview type the camera preview is based on.
     * This will be used to decide which method of [.getSurfaceHolder]
     * and [.getSurfaceTexture] will be called.
     *
     * @return the preview type
     * @see PreviewViewType
     */
    @get:PreviewViewType
    val previewType: Int

    /**
     * Get [SurfaceHolder] from [android.view.SurfaceView].
     *
     * @return SurfaceHolder object, Might be null if the preview view is
     * [android.view.TextureView].
     */
    val surfaceHolder: SurfaceHolder?

    /**
     * Get [SurfaceTexture] from [android.view.TextureView].
     *
     * @return SurfaceTexture object. Might be null if the preview view is
     * [android.view.SurfaceView].
     */
    val surfaceTexture: SurfaceTexture?

    /**
     * Is the camera preview available now.
     *
     * @return true if available
     */
    val isAvailable: Boolean

    /**
     * Get the size of this camera preview.
     *
     * @return the size
     */
    val size: Size?

    /**
     * Get the real view that is displaying the camera data.
     *
     * @return the view
     */
    val view: View?
}