package me.shouheng.icamera.listener;

import me.shouheng.icamera.config.size.Size;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/16 22:46
 */
public interface CameraSizeListener {

    void onPreviewSizeUpdated(Size previewSize);

    void onVideoSizeUpdated(Size videoSize);

    void onPictureSizeUpdated(Size pictureSize);
}
