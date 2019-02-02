package me.shouheng.camerax.manager;

import me.shouheng.camerax.configuration.Configuration;

public interface CameraManager<CameraId> {

    void initializeCameraManager(Configuration configuration);

    boolean start();

    interface Callback {

        void onCameraOpened();

        void onCameraClosed();

        void onPictureTaken(byte[] data);

        void onPreviewFrame(byte[] data, int width, int height, int format);

        void notPermission();
    }
}
