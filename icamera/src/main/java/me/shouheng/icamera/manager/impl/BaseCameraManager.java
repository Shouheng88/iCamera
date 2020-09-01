package me.shouheng.icamera.manager.impl;

import android.content.Context;
import android.media.CamcorderProfile;
import android.media.MediaActionSound;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import me.shouheng.icamera.config.ConfigurationProvider;
import me.shouheng.icamera.config.size.AspectRatio;
import me.shouheng.icamera.config.size.Size;
import me.shouheng.icamera.config.size.SizeMap;
import me.shouheng.icamera.enums.CameraFace;
import me.shouheng.icamera.enums.CameraSizeFor;
import me.shouheng.icamera.enums.FlashMode;
import me.shouheng.icamera.enums.MediaQuality;
import me.shouheng.icamera.enums.MediaType;
import me.shouheng.icamera.listener.CameraCloseListener;
import me.shouheng.icamera.listener.CameraOpenListener;
import me.shouheng.icamera.listener.CameraPhotoListener;
import me.shouheng.icamera.listener.CameraPreviewListener;
import me.shouheng.icamera.listener.CameraSizeListener;
import me.shouheng.icamera.listener.CameraVideoListener;
import me.shouheng.icamera.manager.CameraManager;
import me.shouheng.icamera.preview.CameraPreview;
import me.shouheng.icamera.util.XLog;

/**
 * Base camera manager, the template class for camera1 and camera2 implementation.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:50
 */
abstract class BaseCameraManager<CameraId> implements CameraManager, MediaRecorder.OnInfoListener {

    private static final String TAG = "BaseCameraManager";

    protected Context context;

    @MediaType int mediaType;
    @MediaQuality int mediaQuality;
    @CameraFace int cameraFace;
    int numberOfCameras;
    CameraId rearCameraId;
    CameraId frontCameraId;
    CameraId currentCameraId;
    int rearCameraOrientation;
    int frontCameraOrientation;
    List<Size> previewSizes;
    List<Size> pictureSizes;
    List<Size> videoSizes;
    AspectRatio expectAspectRatio;
    SizeMap previewSizeMap;
    SizeMap pictureSizeMap;
    SizeMap videoSizeMap;
    @Nullable Size expectSize;
    Size previewSize;
    Size pictureSize;
    Size videoSize;
    CamcorderProfile camcorderProfile;
    File pictureFile;
    File videoOutFile;
    MediaRecorder videoRecorder;
    MediaActionSound mediaActionSound;
    boolean voiceEnabled;
    boolean isAutoFocus;
    @FlashMode int flashMode;
    float zoom = 1.0f;
    float maxZoom;
    int displayOrientation;

    long videoFileSize;
    int videoDuration;

    CameraOpenListener cameraOpenListener;
    CameraCloseListener cameraCloseListener;
    CameraPreviewListener cameraPreviewListener;
    private CameraPhotoListener cameraPhotoListener;
    private CameraVideoListener cameraVideoListener;
    private List<CameraSizeListener> cameraSizeListeners;

    CameraPreview cameraPreview;
    volatile boolean takingPicture;
    volatile boolean videoRecording;

    private HandlerThread backgroundThread;
    Handler backgroundHandler;
    Handler uiHandler = new Handler(Looper.getMainLooper());

    BaseCameraManager(CameraPreview cameraPreview) {
        this.cameraPreview = cameraPreview;
        cameraFace = ConfigurationProvider.get().getDefaultCameraFace();
        expectAspectRatio = ConfigurationProvider.get().getDefaultAspectRatio();
        mediaType = ConfigurationProvider.get().getDefaultMediaType();
        mediaQuality = ConfigurationProvider.get().getDefaultMediaQuality();
        voiceEnabled = ConfigurationProvider.get().isVoiceEnable();
        isAutoFocus = ConfigurationProvider.get().isAutoFocus();
        flashMode = ConfigurationProvider.get().getDefaultFlashMode();
        cameraSizeListeners = new LinkedList<>();
        videoFileSize = ConfigurationProvider.get().getDefaultVideoFileSize();
        videoDuration = ConfigurationProvider.get().getDefaultVideoDuration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mediaActionSound = new MediaActionSound();
        }
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
    public int getCameraFace() {
        return cameraFace;
    }

    @Override
    public void switchCamera(int cameraFace) {
        if (cameraFace == this.cameraFace) {
            return;
        }
        this.cameraFace = cameraFace;
        currentCameraId = cameraFace == CameraFace.FACE_FRONT ? frontCameraId : rearCameraId;
    }

    @Override
    public void setExpectSize(@Nullable Size expectSize) {
        if (expectSize == null || expectSize.equals(this.expectSize)) {
            return;
        }
        this.expectSize = expectSize;
        this.expectAspectRatio = AspectRatio.of(this.expectSize);
        ConfigurationProvider.get().getCameraSizeCalculator().changeExpectSize(expectSize);
        ConfigurationProvider.get().getCameraSizeCalculator().changeExpectAspectRatio(expectAspectRatio);
    }

    @Override
    public void setExpectAspectRatio(AspectRatio expectAspectRatio) {
        if (this.expectAspectRatio.equals(expectAspectRatio)) {
            return;
        }
        this.expectAspectRatio = expectAspectRatio;
        ConfigurationProvider.get().getCameraSizeCalculator().changeExpectAspectRatio(expectAspectRatio);
    }

    @Override
    public void setMediaQuality(int mediaQuality) {
        if (this.mediaQuality == mediaQuality) {
            return;
        }
        this.mediaQuality = mediaQuality;
        ConfigurationProvider.get().getCameraSizeCalculator().changeMediaQuality(mediaQuality);
    }

    @Override
    public AspectRatio getAspectRatio(@CameraSizeFor int sizeFor) {
        switch (sizeFor) {
            case CameraSizeFor.SIZE_FOR_PICTURE:
                return pictureSize == null ? null : AspectRatio.of(pictureSize);
            case CameraSizeFor.SIZE_FOR_VIDEO:
                return videoSize == null ? null : AspectRatio.of(videoSize);
            case CameraSizeFor.SIZE_FOR_PREVIEW:
            default:
                return previewSize == null ? null : AspectRatio.of(previewSize);
        }
    }

    @Override
    public void addCameraSizeListener(CameraSizeListener cameraSizeListener) {
        this.cameraSizeListeners.add(cameraSizeListener);
    }

    @Override
    public void setCameraPreviewListener(CameraPreviewListener cameraPreviewListener) {
        this.cameraPreviewListener = cameraPreviewListener;
    }

    @Override
    public void takePicture(File fileToSave, CameraPhotoListener cameraPhotoListener) {
        this.pictureFile = fileToSave;
        this.cameraPhotoListener = cameraPhotoListener;
    }

    @Override
    public void setVideoFileSize(long videoFileSize) {
        this.videoFileSize = videoFileSize;
    }

    @Override
    public void setVideoDuration(int videoDuration) {
        this.videoDuration = videoDuration;
    }

    @Override
    public void startVideoRecord(File file, CameraVideoListener cameraVideoListener) {
        this.videoOutFile = file;
        this.cameraVideoListener = cameraVideoListener;
    }

    @Override
    public void releaseCamera() {
        stopBackgroundThread();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && mediaActionSound != null) {
            mediaActionSound.release();
        }
    }

    @Override
    public void closeCamera(CameraCloseListener cameraCloseListener) {
        this.cameraCloseListener = cameraCloseListener;
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        if (MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED == what) {
            onMaxDurationReached();
        } else if (MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED == what) {
            onMaxFileSizeReached();
        }
    }

    /*----------------------------------- Protected Methods Region -----------------------------------*/

    void handlePictureTakenResult(byte[] bytes) {
        if (pictureFile == null) {
            notifyCameraCaptureFailed(new RuntimeException("Error creating media file, check storage permissions."));
            XLog.d(TAG, "Error creating media file, check storage permissions.");
            return;
        }
        // do write
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(pictureFile);
            fileOutputStream.write(bytes);
            fileOutputStream.close();
        } catch (FileNotFoundException error) {
            XLog.e(TAG, "File not found: " + error.getMessage());
            notifyCameraCaptureFailed(new RuntimeException("File not found: " + error.getMessage()));
        } catch (IOException error) {
            XLog.e(TAG, "Error accessing file: " + error.getMessage());
            notifyCameraCaptureFailed(new RuntimeException("Error accessing file: " + error.getMessage()));
        } catch (Throwable error) {
            XLog.e(TAG, "Error saving file: " + error.getMessage());
            notifyCameraCaptureFailed(new RuntimeException("Error saving file: " + error.getMessage()));
        }
    }

    void notifyCameraOpened() {
        if (cameraOpenListener != null) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    cameraOpenListener.onCameraOpened(cameraFace);
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
                    cameraPhotoListener.onPictureTaken(data, pictureFile);
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

    void safeStopVideoRecorder() {
        try {
            if (videoRecorder != null) {
                videoRecorder.stop();
            }
        } catch (Exception ex) {
            notifyVideoRecordError(new RuntimeException(ex));
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

    void notifyPreviewSizeUpdated(final Size previewSize) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (CameraSizeListener cameraSizeListener : cameraSizeListeners) {
                    cameraSizeListener.onPreviewSizeUpdated(previewSize);
                }
            }
        });
    }

    void notifyPictureSizeUpdated(final Size pictureSize) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (CameraSizeListener cameraSizeListener : cameraSizeListeners) {
                    cameraSizeListener.onPictureSizeUpdated(pictureSize);
                }
            }
        });
    }

    void notifyVideoSizeUpdated(final Size videoSize) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (CameraSizeListener cameraSizeListener : cameraSizeListeners) {
                    cameraSizeListener.onVideoSizeUpdated(videoSize);
                }
            }
        });
    }

    void notifyPreviewFrameChanged(final byte[] data, final Size size, final int format) {
        if (cameraPreviewListener != null) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (cameraPreviewListener != null) {
                        cameraPreviewListener.onPreviewFrame(data, size, format);
                    }
                }
            });
        }
    }

    void notifyCameraClosed() {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                if (cameraCloseListener != null) {
                    cameraCloseListener.onCameraClosed(cameraFace);
                }
            }
        });
    }

    /*----------------------------------- Private Methods Region -----------------------------------*/

    private void onMaxDurationReached() {
        stopVideoRecord();
    }

    private void onMaxFileSizeReached() {
        stopVideoRecord();
    }

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
            XLog.e(TAG, "stopBackgroundThread: " + e);
        } finally {
            backgroundThread = null;
            backgroundHandler = null;
        }
    }

}
