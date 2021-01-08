package me.shouheng.icamera.config.creator

import android.content.Context
import me.shouheng.icamera.manager.CameraManager
import me.shouheng.icamera.preview.CameraPreview

/**
 * Creator for [CameraManager].
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:55
 */
interface CameraManagerCreator {
    /**
     * Method used to create [CameraManager].
     *
     * @param context the context
     * @param cameraPreview the [CameraPreview]
     * @return CameraManager object.
     */
    fun create(context: Context, cameraPreview: CameraPreview): CameraManager
}