package me.shouheng.icamera.listener;

import java.io.File;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/14 14:30
 */
public interface CameraPhotoListener {

    void onPictureTaken(byte[] data, File picture);

    void onCaptureFailed(Throwable throwable);
}
