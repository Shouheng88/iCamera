package me.shouheng.camerax.manager.impl;

import android.media.CamcorderProfile;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import me.shouheng.camerax.configuration.Configuration;
import me.shouheng.camerax.configuration.impl.SizeCalculateStrategyImpl;
import me.shouheng.camerax.configuration.SizeCalculateStrategy;
import me.shouheng.camerax.enums.Camera;
import me.shouheng.camerax.manager.CameraManager;
import me.shouheng.camerax.preview.CameraPreview;
import me.shouheng.camerax.utils.Size;

/**
 * Abstract camera manager.
 */
abstract class AbstractCameraManager<CameraId> implements CameraManager<CameraId> {

    private static final String TAG = "AbstractCameraManager";

    CameraId currentCameraId = null;
    CameraId faceFrontCameraId = null;
    CameraId faceBackCameraId = null;

    Configuration configuration;

    Callback callback;
    CameraPreview cameraPreview;

    /**
     * Background thread, used to handle camera methods in background thread.
     */
    private HandlerThread backgroundThread;
    Handler backgroundHandler;
    Handler uiHandler = new Handler(Looper.getMainLooper());

    /**
     * The output file information.
     */
    CamcorderProfile camcorderProfile;
    Size photoSize;
    Size videoSize;
    Size previewSize;
    Size windowSize;
    SizeCalculateStrategy sizeCalculateStrategy;

    /**
     * Do open camera with given camera id.
     *
     * @param cameraId camera id.
     * @return true if the camera is opened success.
     */
    abstract boolean openCamera(final CameraId cameraId);

    AbstractCameraManager(Callback callback, CameraPreview cameraPreview) {
        this.callback = callback;
        this.cameraPreview = cameraPreview;
        startBackgroundThread();
    }

    @Override
    public void initializeCameraManager(Configuration configuration, SizeCalculateStrategy sizeCalculateStrategy) {
        this.configuration = configuration;
        if (sizeCalculateStrategy == null) {
            sizeCalculateStrategy = new SizeCalculateStrategyImpl();
        }
        this.sizeCalculateStrategy = sizeCalculateStrategy;
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    @Override
    public final boolean openCamera(int cameraFace) {
        return openCamera(getCameraId(cameraFace));
    }

    @Override
    public void closeCamera() {

    }

    @Override
    public void switchCamera(int cameraFace) {

    }

    @Override
    public void setFlashMode(int flashMode) {

    }

    @Override
    public void resumePreview() {

    }

    @Override
    public void takePhoto() {

    }

    @Override
    public void startVideoRecord() {

    }

    @Override
    public void stopVideoRecord() {

    }

    @Override
    public void release() {

    }

    @Override
    public CameraId getCurrentCameraId() {
        return currentCameraId;
    }

    @Override
    public CameraId getFaceFrontCameraId() {
        return faceFrontCameraId;
    }

    @Override
    public CameraId getFaceBackCameraId() {
        return faceBackCameraId;
    }

    /**
     * TODO check the camera id null.
     *
     * @param cameraFace the camera face
     * @return the camera id associated.
     */
    private CameraId getCameraId(@Camera.CameraFace int cameraFace) {
        return cameraFace == Camera.CAMERA_FACE_FRONT ? faceFrontCameraId : faceBackCameraId;
    }
}
