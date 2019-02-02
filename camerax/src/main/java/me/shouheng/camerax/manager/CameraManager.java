package me.shouheng.camerax.manager;

import me.shouheng.camerax.configuration.Configuration;
import me.shouheng.camerax.configuration.SizeCalculateStrategy;
import me.shouheng.camerax.enums.Camera;

public interface CameraManager<CameraId> {

    void initializeCameraManager(Configuration configuration, SizeCalculateStrategy sizeCalculateStrategy);

    boolean openCamera(@Camera.CameraFace int cameraFace);

    void closeCamera();

    void switchCamera(@Camera.CameraFace int cameraFace);

    void setFlashMode(@Camera.FlashMode int flashMode);

    void resumePreview();

    void takePhoto();

    void startVideoRecord();

    void stopVideoRecord();

    void release();

    CameraId getCurrentCameraId();

    CameraId getFaceFrontCameraId();

    CameraId getFaceBackCameraId();

    int getNumberOfCameras();

    int getFaceFrontCameraOrientation();

    int getFaceBackCameraOrientation();

    interface Callback {

        void onCameraOpened();

        void onCameraClosed();

        void onPictureTaken(byte[] data);

        void onPreviewFrame(byte[] data, int width, int height, int format);

        void notPermission();
    }
}
