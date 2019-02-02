package me.shouheng.camerax.manager.impl;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import me.shouheng.camerax.configuration.Configuration;
import me.shouheng.camerax.manager.CameraManager;
import me.shouheng.camerax.preview.CameraPreview;

/**
 * Abstract camera manager.
 */
abstract class AbstractCameraManager<CameraId> implements CameraManager<CameraId> {

    private static final String TAG = "AbstractCameraManager";

    private CameraId currentCameraId;
    private Configuration configuration;

    protected Callback callback;
    protected CameraPreview cameraPreview;

    HandlerThread backgroundThread;
    Handler backgroundHandler;
    Handler uiHandler = new Handler(Looper.getMainLooper());

    @Override
    public void initializeCameraManager(Configuration configuration) {
        this.configuration = configuration;
    }

    AbstractCameraManager(Callback callback, CameraPreview cameraPreview) {
        this.callback = callback;
        this.cameraPreview = cameraPreview;
        startBackgroundThread();
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }
}
