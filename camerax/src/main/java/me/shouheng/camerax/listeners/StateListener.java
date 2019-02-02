package me.shouheng.camerax.listeners;

import me.shouheng.camerax.CameraView;

/**
 * The lifecycle event state for camera.
 */
public interface StateListener {

    /**
     * Called when camera is opened.
     *
     * @param cameraView the associated {@link CameraView}
     */
    void onCameraOpened(CameraView cameraView);

    /**
     * Called when camera is closed.
     *
     * @param cameraView the associated {@link CameraView}
     */
    void onCameraClosed(CameraView cameraView);

    /**
     * Called the camera permissions is denied.
     *
     * @param cameraView the associated {@link CameraView}
     * @param permissions the permissions required
     */
    void onPermissionDenied(CameraView cameraView, String[] permissions);

    /**
     * Called the a picture is token.
     *
     * @param cameraView the associated {@link CameraView}
     * @param data the picture data in byte
     */
    void onPictureTaken(CameraView cameraView, byte[] data);

    /**
     * TODO check the params
     *
     * @param cameraView
     * @param data
     * @param width
     * @param height
     * @param format
     */
    void onPreviewFrame(CameraView cameraView, byte[] data, int width, int height, int format);

    /**
     * TODO design this logic
     */
    void onVideoToken();

    /**
     * TODO touch event happened!!
     */
    void onTouch();
}
