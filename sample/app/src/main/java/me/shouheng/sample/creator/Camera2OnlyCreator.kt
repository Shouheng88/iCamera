package me.shouheng.sample.creator

import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import me.shouheng.shining.config.creator.CameraManagerCreator
import me.shouheng.shining.manager.CameraManager
import me.shouheng.shining.manager.impl.Camera2Manager
import me.shouheng.shining.preview.CameraPreview

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/20 11:57
 */
class Camera2OnlyCreator : CameraManagerCreator {

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun create(context: Context?, cameraPreview: CameraPreview?): CameraManager = Camera2Manager(cameraPreview)

}