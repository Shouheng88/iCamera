package me.shouheng.icamera.preview.impl

import me.shouheng.icamera.config.size.Size
import me.shouheng.icamera.preview.CameraPreview
import me.shouheng.icamera.preview.CameraPreviewCallback

/**
 * Base camera preview.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:53
 */
abstract class BaseCameraPreview protected constructor() : CameraPreview {

    private var width = 0
    private var height = 0
    private var cameraPreviewCallback: CameraPreviewCallback? = null

    override fun setCameraPreviewCallback(cameraPreviewCallback: CameraPreviewCallback?) {
        this.cameraPreviewCallback = cameraPreviewCallback
    }

    protected fun notifyPreviewAvailable() {
        cameraPreviewCallback?.onAvailable(this)
    }

    override val isAvailable: Boolean
        get() = width > 0 && height > 0

    override val size: Size
        get() = Size.of(width, height)

    protected fun setSize(width: Int, height: Int) {
        this.width = width
        this.height = height
    }
}