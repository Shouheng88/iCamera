package me.shouheng.icamera.listener;

import java.io.File;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/14 19:49
 */
public interface CameraVideoListener {

    void onVideoRecordStart();

    void onVideoRecordStop(File file);

    void onVideoRecordError(Throwable throwable);
}
