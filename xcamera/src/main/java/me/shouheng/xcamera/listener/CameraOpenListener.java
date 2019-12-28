package me.shouheng.xcamera.listener;

import me.shouheng.xcamera.enums.CameraFace;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/14 10:40
 */
public interface CameraOpenListener {

    void onCameraOpened(@CameraFace int cameraFace);

    void onCameraOpenError(Throwable throwable);
}
