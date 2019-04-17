package me.shouheng.camerax.listener;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/14 10:40
 */
public interface CameraOpenListener {

    // TODO add camera id to this method
    void onCameraOpened();

    void onCameraOpenError(Throwable throwable);
}
