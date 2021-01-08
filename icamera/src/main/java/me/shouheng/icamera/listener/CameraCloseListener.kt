package me.shouheng.icamera.listener

import me.shouheng.icamera.enums.CameraFace

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/17 22:47
 */
interface CameraCloseListener {
    fun onCameraClosed(@CameraFace cameraFace: Int)
}