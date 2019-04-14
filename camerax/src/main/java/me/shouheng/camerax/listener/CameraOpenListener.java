package me.shouheng.camerax.listener;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/14 10:40
 */
public interface CameraOpenListener {

    void onCameraOpened();

    void onCameraOpenError(Throwable throwable);
}
