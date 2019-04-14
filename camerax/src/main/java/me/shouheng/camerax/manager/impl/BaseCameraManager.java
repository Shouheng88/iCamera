package me.shouheng.camerax.manager.impl;

import android.content.Context;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.*;
import android.os.Process;
import me.shouheng.camerax.config.ConfigurationProvider;
import me.shouheng.camerax.config.sizes.AspectRatio;
import me.shouheng.camerax.config.sizes.Size;
import me.shouheng.camerax.enums.Camera;
import me.shouheng.camerax.enums.Media;
import me.shouheng.camerax.listener.CameraOpenListener;
import me.shouheng.camerax.listener.CameraPhotoListener;
import me.shouheng.camerax.listener.CameraVideoListener;
import me.shouheng.camerax.manager.CameraManager;
import me.shouheng.camerax.preview.CameraPreview;
import me.shouheng.camerax.util.Logger;

import java.io.File;
import java.util.List;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:50
 */
abstract class BaseCameraManager<CameraId> implements CameraManager {

    private static final String TAG = "BaseCameraManager";

    protected Context context;

    @Media.Type int mediaType;
    @Media.Quality int mediaQuality;
    @Camera.Face int cameraFace;
    int numberOfCameras;
    CameraId rearCameraId;
    CameraId frontCameraId;
    CameraId currentCameraId;
    int rearCameraOrientation;
    int frontCameraOrientation;
    List<Size> previewSizes;
    List<Size> pictureSizes;
    List<Size> videoSizes;
    AspectRatio aspectRatio;
    Size userSize;
    Size previewSize;
    Size pictureSize;
    Size videoSize;
    CamcorderProfile camcorderProfile;
    File videoOutFile;
    MediaRecorder videoRecorder;

    private CameraOpenListener cameraOpenListener;
    private CameraPhotoListener cameraPhotoListener;
    private CameraVideoListener cameraVideoListener;

    CameraPreview cameraPreview;
    volatile boolean showingPreview;
    volatile boolean takingPicture;
    volatile boolean videoRecording;

    private HandlerThread backgroundThread;
    Handler backgroundHandler;
    Handler uiHandler = new Handler(Looper.getMainLooper());

    BaseCameraManager(CameraPreview cameraPreview) {
        this.cameraPreview = cameraPreview;
        aspectRatio = ConfigurationProvider.get().getDefaultAspectRatio();
        mediaType = ConfigurationProvider.get().getDefaultMediaType();
        mediaQuality = ConfigurationProvider.get().getDefaultMediaQuality();
    }

    @Override
    public void initialize(Context context) {
        this.context = context;
        startBackgroundThread();
    }

    @Override
    public void openCamera(CameraOpenListener cameraOpenListener) {
        this.cameraOpenListener = cameraOpenListener;
    }

    @Override
    public void takePicture(CameraPhotoListener cameraPhotoListener) {
        this.cameraPhotoListener = cameraPhotoListener;
    }

    @Override
    public void startVideoRecord(File file, CameraVideoListener cameraVideoListener) {
        this.videoOutFile = file;
        this.cameraVideoListener = cameraVideoListener;
    }

    @Override
    public void releaseCamera() {
        stopBackgroundThread();
    }

    /*-----------------------------------protected methods-----------------------------------*/

    void notifyCameraOpened() {
        if (cameraOpenListener != null) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    cameraOpenListener.onCameraOpened();
                }
            });
        }
    }

    void notifyCameraOpenError(final Throwable throwable) {
        if (cameraOpenListener != null) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    cameraOpenListener.onCameraOpenError(throwable);
                }
            });
        }
    }

    void notifyCameraPictureTaken(final byte[] data) {
        if (cameraPhotoListener != null) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    cameraPhotoListener.onPictureTaken(data);
                }
            });
        }
    }

    void notifyCameraCaptureFailed(final Throwable throwable) {
        if (cameraPhotoListener != null) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    cameraPhotoListener.onCaptureFailed(throwable);
                }
            });
        }
    }

    void notifyVideoRecordStart() {
        if (cameraVideoListener != null) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    cameraVideoListener.onVideoRecordStart();
                }
            });
        }
    }

    void notifyVideoRecordStop(final File file) {
        if (cameraVideoListener != null) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    cameraVideoListener.onVideoRecordStop(file);
                }
            });
        }
    }

    void notifyVideoRecordError(final Throwable throwable) {
        if (cameraVideoListener != null) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    cameraVideoListener.onVideoRecordError(throwable);
                }
            });
        }
    }

    void releaseVideoRecorder() {
        try {
            if (videoRecorder != null) {
                videoRecorder.reset();
                videoRecorder.release();
            }
        } catch (Exception ex) {
            notifyVideoRecordError(new RuntimeException(ex));
        } finally {
            videoRecorder = null;
        }
    }

    /*-----------------------------------private methods-----------------------------------*/

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (Build.VERSION.SDK_INT > 17) {
            backgroundThread.quitSafely();
        } else {
            backgroundThread.quit();
        }

        try {
            backgroundThread.join();
        } catch (InterruptedException e) {
            Logger.e(TAG, "stopBackgroundThread: " + e);
        } finally {
            backgroundThread = null;
            backgroundHandler = null;
        }
    }

}
