package me.shouheng.icamera.manager.impl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.media.MediaRecorder;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import me.shouheng.icamera.config.ConfigurationProvider;
import me.shouheng.icamera.config.calculator.CameraSizeCalculator;
import me.shouheng.icamera.config.size.AspectRatio;
import me.shouheng.icamera.config.size.Size;
import me.shouheng.icamera.config.size.SizeMap;
import me.shouheng.icamera.enums.CameraFace;
import me.shouheng.icamera.enums.CameraSizeFor;
import me.shouheng.icamera.enums.CameraType;
import me.shouheng.icamera.enums.FlashMode;
import me.shouheng.icamera.enums.MediaType;
import me.shouheng.icamera.enums.PreviewViewType;
import me.shouheng.icamera.listener.CameraCloseListener;
import me.shouheng.icamera.listener.CameraOpenListener;
import me.shouheng.icamera.listener.CameraPhotoListener;
import me.shouheng.icamera.listener.CameraVideoListener;
import me.shouheng.icamera.preview.CameraPreview;
import me.shouheng.icamera.preview.CameraPreviewCallback;
import me.shouheng.icamera.util.CameraHelper;
import me.shouheng.icamera.util.ImageHelper;
import me.shouheng.icamera.util.XLog;

import static me.shouheng.icamera.manager.impl.Camera2Manager.CaptureSessionCallback.CameraState.STATE_PICTURE_TAKEN;
import static me.shouheng.icamera.manager.impl.Camera2Manager.CaptureSessionCallback.CameraState.STATE_PREVIEW;
import static me.shouheng.icamera.manager.impl.Camera2Manager.CaptureSessionCallback.CameraState.STATE_WAITING_LOCK;
import static me.shouheng.icamera.manager.impl.Camera2Manager.CaptureSessionCallback.CameraState.STATE_WAITING_NON_PRE_CAPTURE;
import static me.shouheng.icamera.manager.impl.Camera2Manager.CaptureSessionCallback.CameraState.STATE_WAITING_PRE_CAPTURE;

/**
 * Camera manager for camera2.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:52
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
@SuppressLint("MissingPermission")
public class Camera2Manager extends BaseCameraManager<String> implements ImageReader.OnImageAvailableListener {

    private static final String TAG = "Camera2Manager";

    private CameraManager cameraManager;
    private CameraDevice cameraDevice;

    private CameraCharacteristics frontCameraCharacteristics;
    private CameraCharacteristics rearCameraCharacteristics;
    private StreamConfigurationMap frontStreamConfigurationMap;
    private StreamConfigurationMap rearStreamConfigurationMap;

    private ImageReader imageReader;
    private ImageReader previewReader;

    private SurfaceHolder surfaceHolder;
    private SurfaceTexture surfaceTexture;
    private Surface workingSurface;

    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder previewRequestBuilder;
    private CaptureRequest previewRequest;

    private CaptureSessionCallback captureSessionCallback = new CaptureSessionCallback() {

        private void processCaptureResultInternal(@NonNull CaptureResult result, @CameraState int cameraPreviewState) {
            switch (cameraPreviewState) {
                case STATE_PREVIEW:
                    break;
                case STATE_WAITING_LOCK:
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState
                            || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState
                            || CaptureResult.CONTROL_AF_STATE_INACTIVE == afState
                            || CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN == afState) {
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            setCameraPreviewState(STATE_PICTURE_TAKEN);
                            captureStillPicture();
                        } else {
                            runPreCaptureSequence();
                        }
                    }
                    break;
                case STATE_WAITING_PRE_CAPTURE:
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        setCameraPreviewState(STATE_WAITING_NON_PRE_CAPTURE);
                    }
                    break;
                case STATE_WAITING_NON_PRE_CAPTURE:
                    aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        setCameraPreviewState(STATE_PICTURE_TAKEN);
                        captureStillPicture();
                    }
                    break;
                case STATE_PICTURE_TAKEN:
                    // noop
                    break;
                default:
                    // noop
            }
        }

        @Override
        void processCaptureResult(@NonNull CaptureResult result, @CameraState int cameraPreviewState) {
            processCaptureResultInternal(result, cameraPreviewState);
        }
    };

    private ImageReader.OnImageAvailableListener onPreviewImageAvailableListener = new ImageReader.OnImageAvailableListener() {

        private ReentrantLock lock = new ReentrantLock();

        @Override
        public void onImageAvailable(ImageReader reader) {
            try (Image image = reader.acquireNextImage()) {
                // Y:U:V == 4:2:2
                if (cameraPreviewListener != null && image.getFormat() == ImageFormat.YUV_420_888) {
                    // lock to ensure that all data from same Image object
                    lock.lock();
                    notifyPreviewFrameChanged(ImageHelper.convertYUV_420_888toNV21(image), previewSize, ImageFormat.NV21);
                    lock.unlock();
                }
            } catch (Exception ex) {
                XLog.e(TAG, "error for image preview : " + ex);
            }
        }
    };

    public Camera2Manager(CameraPreview cameraPreview) {
        super(cameraPreview);
        cameraPreview.setCameraPreviewCallback(new CameraPreviewCallback() {
            @Override
            public void onAvailable(CameraPreview cameraPreview) {
                if (isCameraOpened()) {
                    createPreviewSession();
                }
            }
        });
    }

    @Override
    public void initialize(Context context) {
        super.initialize(context);
        initCameraInfo(context);
    }

    @Override
    public void openCamera(CameraOpenListener cameraOpenListener) {
        super.openCamera(cameraOpenListener);
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                final long start0 = System.currentTimeMillis();
                prepareCameraOutputs();
                adjustCameraConfiguration(false);
                try {
                    final long start1 = System.currentTimeMillis();
                    cameraManager.openCamera(currentCameraId, new CameraDevice.StateCallback() {
                        @Override
                        public void onOpened(@NonNull CameraDevice camera) {
                            final long start2 = System.currentTimeMillis();
                            cameraDevice = camera;
                            if (cameraPreview.isAvailable()) {
                                createPreviewSession();
                            }
                            notifyCameraOpened();
                            XLog.d(TAG, "Camera opened cost : "
                                    + (System.currentTimeMillis() - start2) + "ms "
                                    + (System.currentTimeMillis() - start1) + "ms "
                                    + (System.currentTimeMillis() - start0) + "ms.");
                        }

                        @Override
                        public void onDisconnected(@NonNull CameraDevice camera) {
                            XLog.e(TAG, "Camera disconnected.");
                            camera.close();
                            cameraDevice = null;
                            notifyCameraOpenError(new RuntimeException("Camera disconnected."));
                        }

                        @Override
                        public void onError(@NonNull CameraDevice camera, int error) {
                            XLog.e(TAG, "Camera open error : " + error);
                            camera.close();
                            cameraDevice = null;
                            notifyCameraOpenError(new RuntimeException("Camera error : " + error));
                        }
                    }, backgroundHandler);
                } catch (Exception ex) {
                    XLog.e(TAG, "error : " + ex);
                    notifyCameraOpenError(ex);
                }
            }
        });
    }

    @Override
    public boolean isCameraOpened() {
        return cameraDevice != null;
    }

    @Override
    public void switchCamera(int cameraFace) {
        super.switchCamera(cameraFace);
        if (isCameraOpened()) {
            closeCamera(cameraCloseListener);
            openCamera(cameraOpenListener);
        }
    }

    @Override
    public void setMediaType(int mediaType) {
        XLog.d(TAG, "setMediaType : " + mediaType + " with mediaType " + this.mediaType);
        if (this.mediaType == mediaType) {
            return;
        }
        this.mediaType = mediaType;
        if (isCameraOpened()) {
            backgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        adjustCameraConfiguration(false);
                    } catch (Exception ex) {
                        XLog.e(TAG, "setMediaType : " + ex);
                    }
                }
            });
        }
    }

    @Override
    public int getMediaType() {
        return mediaType;
    }

    @Override
    public void setVoiceEnable(boolean voiceEnable) {
        if (this.voiceEnabled == voiceEnable) {
            return;
        }
        this.voiceEnabled = voiceEnable;
    }

    @Override
    public boolean isVoiceEnable() {
        return voiceEnabled;
    }

    @Override
    public void setAutoFocus(boolean autoFocus) {
        if (this.isAutoFocus == autoFocus) {
            return;
        }
        this.isAutoFocus = autoFocus;
        if (isCameraOpened() && previewRequestBuilder != null) {
            setAutoFocusInternal();
            previewRequest = previewRequestBuilder.build();
            if (captureSession != null) {
                try {
                    captureSession.setRepeatingRequest(previewRequest, captureSessionCallback, backgroundHandler);
                } catch (CameraAccessException e) {
                    XLog.e(TAG, "setAutoFocus error : " + e);
                }
            } else {
                XLog.i(TAG, "setAutoFocus captureSession is null.");
            }
        } else {
            XLog.i(TAG, "setAutoFocus camera not open or previewRequestBuilder is null");
        }
    }

    @Override
    public boolean isAutoFocus() {
        return isAutoFocus;
    }

    @Override
    public void setFlashMode(@FlashMode int flashMode) {
        if (this.flashMode == flashMode) {
            return;
        }
        this.flashMode = flashMode;
        if (isCameraOpened() && previewRequestBuilder != null) {
            boolean succeed = setFlashModeInternal();
            if (succeed) {
                previewRequest = previewRequestBuilder.build();
                if (captureSession != null) {
                    try {
                        captureSession.setRepeatingRequest(previewRequest, captureSessionCallback, backgroundHandler);
                    } catch (CameraAccessException e) {
                        XLog.e(TAG, "setFlashMode error : " + e);
                    }
                } else {
                    XLog.i(TAG, "setFlashMode captureSession is null.");
                }
            } else {
                XLog.i(TAG, "setFlashMode failed.");
            }
        } else {
            XLog.i(TAG, "setFlashMode camera not open or previewRequestBuilder is null");
        }
    }

    @Override
    public int getFlashMode() {
        return flashMode;
    }

    @Override
    public void setZoom(float zoom) {
        if (zoom == this.zoom || zoom > getMaxZoom() || zoom < 1.f) {
            return;
        }
        this.zoom = zoom;
        if (isCameraOpened()) {
            boolean succeed = setZoomInternal(previewRequestBuilder);
            if (succeed) {
                previewRequest = previewRequestBuilder.build();
                if (captureSession != null) {
                    try {
                        captureSession.setRepeatingRequest(previewRequest, captureSessionCallback, backgroundHandler);
                    } catch (CameraAccessException e) {
                        XLog.e(TAG, "setZoom error : " + e);
                    }
                } else {
                    XLog.i(TAG, "setZoom captureSession is null.");
                }
            } else {
                XLog.i(TAG, "setZoom failed : setZoomInternal failed.");
            }
        } else {
            XLog.i(TAG, "setZoom failed : camera not open.");
        }
    }

    @Override
    public float getZoom() {
        return zoom;
    }

    @Override
    public float getMaxZoom() {
        if (maxZoom == 0) {
            CameraCharacteristics cameraCharacteristics = cameraFace == CameraFace.FACE_FRONT ?
                    frontCameraCharacteristics : rearCameraCharacteristics;
            Float fMaxZoom = cameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
            if (fMaxZoom == null) {
                maxZoom = 1.0f;
            } else {
                maxZoom = fMaxZoom;
            }
        }
        return maxZoom;
    }

    @Override
    public void setExpectSize(@Nullable Size expectSize) {
        super.setExpectSize(expectSize);
        if (isCameraOpened()) {
            backgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    adjustCameraConfiguration(true);
                    createPreviewSession();
                }
            });
        }
    }

    @Override
    public void setExpectAspectRatio(AspectRatio expectAspectRatio) {
        super.setExpectAspectRatio(expectAspectRatio);
        if (isCameraOpened()) {
            backgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    adjustCameraConfiguration(true);
                    createPreviewSession();
                }
            });
        }
    }

    @Override
    public Size getSize(@CameraSizeFor int sizeFor) {
        switch (sizeFor) {
            case CameraSizeFor.SIZE_FOR_PREVIEW:
                return previewSize;
            case CameraSizeFor.SIZE_FOR_PICTURE:
                return pictureSize;
            case CameraSizeFor.SIZE_FOR_VIDEO:
                return videoSize;
            default:
                return null;
        }
    }

    @Override
    public SizeMap getSizes(@CameraSizeFor int sizeFor) {
        switch (sizeFor) {
            case CameraSizeFor.SIZE_FOR_PREVIEW:
                if (previewSizeMap == null) {
                    previewSizeMap = CameraHelper.getSizeMapFromSizes(previewSizes);
                }
                return previewSizeMap;
            case CameraSizeFor.SIZE_FOR_PICTURE:
                if (pictureSizeMap == null) {
                    pictureSizeMap = CameraHelper.getSizeMapFromSizes(pictureSizes);
                }
                return pictureSizeMap;
            case CameraSizeFor.SIZE_FOR_VIDEO:
                if (videoSizeMap == null) {
                    videoSizeMap = CameraHelper.getSizeMapFromSizes(videoSizes);
                }
                return videoSizeMap;
            default:
                return null;
        }
    }

    @Override
    public void setDisplayOrientation(int displayOrientation) {
        if (this.displayOrientation == displayOrientation) {
            return;
        }
        this.displayOrientation = displayOrientation;
    }

    @Override
    public void takePicture(File fileToSave, CameraPhotoListener cameraPhotoListener) {
        super.takePicture(fileToSave, cameraPhotoListener);
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (voiceEnabled) {
                        mediaActionSound.play(MediaActionSound.SHUTTER_CLICK);
                    }
                    lockFocus();
                } catch (Exception ex) {
                    notifyCameraCaptureFailed(ex);
                }
            }
        });
    }

    @Override
    public void startVideoRecord(File file, CameraVideoListener cameraVideoListener) {
        super.startVideoRecord(file, cameraVideoListener);
        if (videoRecording) {
            return;
        }
        if (isCameraOpened()) {
            if (voiceEnabled) {
                mediaActionSound.play(MediaActionSound.START_VIDEO_RECORDING);
            }
            backgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    closePreviewSession();
                    if (prepareVideoRecorder()) {
                        createRecordSession();
                    } else {
                        XLog.i(TAG, "startVideoRecord : failed when prepare video recorder.");
                        notifyVideoRecordError(new RuntimeException("Failed when prepare video recorder."));
                    }
                }
            });
        }
    }

    @Override
    public void stopVideoRecord() {
        if (videoRecording && isCameraOpened()) {
            backgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    closePreviewSession();
                    safeStopVideoRecorder();
                    releaseVideoRecorder();
                    videoRecording = false;
                    notifyVideoRecordStop(videoOutFile);
                    if (voiceEnabled) {
                        mediaActionSound.play(MediaActionSound.STOP_VIDEO_RECORDING);
                    }
                    createPreviewSession();
                }
            });
        }
    }

    @Override
    public void resumePreview() {
        if (isCameraOpened()) {
            unlockFocus();
        }
    }

    @Override
    public void closeCamera(CameraCloseListener cameraCloseListener) {
        super.closeCamera(cameraCloseListener);
        if (isCameraOpened()) {
            cameraDevice.close();
            cameraDevice = null;
        }
        closePreviewSession();
//        releaseTexture();
        closeImageReader();
        releaseVideoRecorder();
        releaseCameraInternal();
        if (uiHandler != null) {
            uiHandler.removeCallbacksAndMessages(null);
        }
        if (backgroundHandler != null) {
            backgroundHandler.removeCallbacksAndMessages(null);
        }
        notifyCameraClosed();
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        try {
            try (Image image = reader.acquireNextImage()) {
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                handlePictureTakenResult(bytes);
                notifyCameraPictureTaken(bytes);
            } catch (IllegalStateException e) {
                XLog.e(TAG, "onImageAvailable error" + e);
                notifyCameraCaptureFailed(e);
            }
        } catch (Exception e) {
            XLog.e(TAG, "onImageAvailable error " + e);
            notifyCameraCaptureFailed(e);
        } finally {
            unlockFocus();
        }
    }

    /*---------------------------------inner methods------------------------------------*/

    /**
     * This method cost more but little time (10ms) to finish, we could get the params here
     * before initialize camera information here to accelerate the camera launch. For
     * example get them from {@link ConfigurationProvider#get()} and then use theme here.
     *
     * @param context context
     */
    private void initCameraInfo(Context context) {
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        long start = System.currentTimeMillis();
        try {
            numberOfCameras = ConfigurationProvider.get().getNumberOfCameras(context);
            frontCameraId = ConfigurationProvider.get().getCameraId(context, CameraFace.FACE_FRONT);
            rearCameraId = ConfigurationProvider.get().getCameraId(context, CameraFace.FACE_REAR);
            frontCameraOrientation = ConfigurationProvider.get().getCameraOrientation(context, CameraFace.FACE_FRONT);
            rearCameraOrientation = ConfigurationProvider.get().getCameraOrientation(context, CameraFace.FACE_REAR);
            frontCameraCharacteristics = ConfigurationProvider.get().getCameraCharacteristics(context, CameraFace.FACE_FRONT);
            rearCameraCharacteristics = ConfigurationProvider.get().getCameraCharacteristics(context, CameraFace.FACE_REAR);
        } catch (Exception e) {
            XLog.e(TAG, "initCameraInfo error " + e);
        }
        XLog.d(TAG, "initCameraInfo basic cost : " + (System.currentTimeMillis() - start) + " ms");

        currentCameraId = cameraFace == CameraFace.FACE_REAR ? rearCameraId : frontCameraId;
    }

    private void prepareCameraOutputs() {
        boolean isFrontCamera = cameraFace == CameraFace.FACE_REAR;
        long start = System.currentTimeMillis();
        try {
            if (isFrontCamera && frontStreamConfigurationMap == null) {
                frontStreamConfigurationMap = ConfigurationProvider.get().getStreamConfigurationMap(context, CameraFace.FACE_REAR);
            } else if (!isFrontCamera && rearStreamConfigurationMap == null) {
                rearStreamConfigurationMap = ConfigurationProvider.get().getStreamConfigurationMap(context, CameraFace.FACE_FRONT);
            }
        } catch (Exception ex) {
            XLog.e(TAG, "initCameraInfo error " + ex);
            notifyCameraOpenError(new RuntimeException(ex));
        }
        XLog.d(TAG, "initCameraInfo get map cost : " + (System.currentTimeMillis() - start) + " ms");

        start = System.currentTimeMillis();
        try {
            final StreamConfigurationMap map = isFrontCamera ? frontStreamConfigurationMap : rearStreamConfigurationMap;
            previewSizes = ConfigurationProvider.get().getSizes(map, cameraFace, CameraSizeFor.SIZE_FOR_PREVIEW);
            pictureSizes = ConfigurationProvider.get().getSizes(map, cameraFace, CameraSizeFor.SIZE_FOR_PICTURE);
            videoSizes = ConfigurationProvider.get().getSizes(map, cameraFace, CameraSizeFor.SIZE_FOR_VIDEO);
            ConfigurationProvider.get().getCameraSizeCalculator().init(expectAspectRatio, expectSize, mediaQuality, previewSizes, pictureSizes, videoSizes);
        } catch (Exception ex) {
            XLog.e(TAG, "error : " + ex);
            notifyCameraOpenError(new RuntimeException(ex));
        }
        XLog.d(TAG, "prepareCameraOutputs cost : " + (System.currentTimeMillis() - start) + " ms");
    }

    private void adjustCameraConfiguration(boolean forceCalculate) {
        Size oldPreviewSize = previewSize;
        CameraSizeCalculator cameraSizeCalculator = ConfigurationProvider.get().getCameraSizeCalculator();
        if (pictureSize == null || forceCalculate) {
            pictureSize = cameraSizeCalculator.getPictureSize(CameraType.TYPE_CAMERA2);
            previewSize = cameraSizeCalculator.getPicturePreviewSize(CameraType.TYPE_CAMERA2);
            notifyPictureSizeUpdated(pictureSize);

            // fix: CaptureRequest contains un-configured Input/Output Surface!
            imageReader = ImageReader.newInstance(pictureSize.width, pictureSize.height, ImageFormat.JPEG, /*maxImages*/2);
            imageReader.setOnImageAvailableListener(this, backgroundHandler);

            previewReader = ImageReader.newInstance(previewSize.width, previewSize.height, ImageFormat.YUV_420_888, 2);
            previewReader.setOnImageAvailableListener(onPreviewImageAvailableListener, backgroundHandler);
        }
        // fixed 2020-08-29 : the video size might be null if quickly switched
        // from media types while first time launch the camera.
        if (videoSize == null || forceCalculate) {
            camcorderProfile = CameraHelper.getCamcorderProfile(mediaQuality, currentCameraId);
            videoSize = cameraSizeCalculator.getVideoSize(CameraType.TYPE_CAMERA2);
            previewSize = cameraSizeCalculator.getVideoPreviewSize(CameraType.TYPE_CAMERA2);
            notifyVideoSizeUpdated(videoSize);
        }
        XLog.d(TAG, "previewSize: " + previewSize + " oldPreviewSize:" + oldPreviewSize);
        if (!previewSize.equals(oldPreviewSize)) {
            notifyPreviewSizeUpdated(previewSize);
        }
    }

    private void createPreviewSession() {
        try {
            final Runnable sessionCreationTask = new Runnable() {
                @Override
                public void run() {
                    try {
                        previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        previewRequestBuilder.addTarget(workingSurface);
                        previewRequestBuilder.addTarget(previewReader.getSurface());
                        cameraDevice.createCaptureSession(Arrays.asList(workingSurface, imageReader.getSurface(), previewReader.getSurface()),
                                new CameraCaptureSession.StateCallback() {
                                    @Override
                                    public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                                        if (isCameraOpened()) {
                                            captureSession = cameraCaptureSession;
                                            setFlashModeInternal();
                                            previewRequest = previewRequestBuilder.build();
                                            try {
                                                captureSession.setRepeatingRequest(previewRequest, captureSessionCallback, backgroundHandler);
                                            } catch (CameraAccessException ex) {
                                                XLog.e(TAG, "create preview session error " + ex);
                                                notifyCameraOpenError(ex);
                                            } catch (IllegalStateException ex) {
                                                XLog.e(TAG, "create preview session error " + ex);
                                                notifyCameraOpenError(ex);
                                            }
                                        }
                                    }

                                    @Override
                                    public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                                        XLog.d(TAG, "onConfigureFailed");
                                        notifyCameraOpenError(new Throwable("Camera capture session configure failed."));
                                    }
                                }, backgroundHandler);
                    } catch (Exception ex) {
                        XLog.e(TAG, "createPreviewSession error " + ex);
                        notifyCameraOpenError(ex);
                    }
                }
            };

            if (cameraPreview.getPreviewType() == PreviewViewType.TEXTURE_VIEW) {
                this.surfaceTexture = cameraPreview.getSurfaceTexture();
                assert surfaceTexture != null;
                surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height);
                workingSurface = cameraPreview.getSurface();
                sessionCreationTask.run();
            } else if (cameraPreview.getPreviewType() == PreviewViewType.SURFACE_VIEW) {
                // only ui thread can touch surface view
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        surfaceHolder = cameraPreview.getSurfaceHolder();
                        assert surfaceHolder != null;
                        surfaceHolder.setFixedSize(previewSize.width, previewSize.height);
                        workingSurface = cameraPreview.getSurface();
                        // keep the initialization procedure
                        sessionCreationTask.run();
                    }
                });
            }
        } catch (Exception ex) {
            XLog.e(TAG, "createPreviewSession error " + ex);
            notifyCameraOpenError(ex);
        }
    }

    private void captureStillPicture() {
        if (isCameraOpened()) {
            try {
                CameraCharacteristics cameraCharacteristics = cameraFace == CameraFace.FACE_FRONT ?
                        frontCameraCharacteristics : rearCameraCharacteristics;

                final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                captureBuilder.addTarget(imageReader.getSurface());

                captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                captureBuilder.set(CaptureRequest.JPEG_ORIENTATION,
                        CameraHelper.getJpegOrientation(cameraCharacteristics, displayOrientation));

                // calculate zoom for capture request
                setZoomInternal(captureBuilder);

                captureSession.stopRepeating();
                captureSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                                   @NonNull CaptureRequest request,
                                                   @NonNull TotalCaptureResult result) {
                        XLog.d(TAG, "onCaptureCompleted: ");
                    }
                }, null);
            } catch (CameraAccessException e) {
                XLog.e(TAG, "Error during capturing picture");
                notifyCameraCaptureFailed(e);
            }
        } else {
            notifyCameraCaptureFailed(new RuntimeException("Camera not open."));
        }
    }

    private void createRecordSession() {
        final Runnable sessionCreationTask = new Runnable() {
            @Override
            public void run() {
                try {
                    final List<Surface> surfaces = new ArrayList<>();

                    previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);

                    final Surface previewSurface = workingSurface;
                    surfaces.add(previewSurface);
                    previewRequestBuilder.addTarget(previewSurface);

                    workingSurface = videoRecorder.getSurface();
                    surfaces.add(workingSurface);
                    previewRequestBuilder.addTarget(workingSurface);

                    cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            captureSession = cameraCaptureSession;

                            previewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                            try {
                                captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler);
                            } catch (Exception e) {
                                XLog.e(TAG, "videoRecorder.start(): " + e);
                                notifyVideoRecordError(e);
                            }

                            try {
                                videoRecorder.start();
                                videoRecording = true;
                                notifyVideoRecordStart();
                            } catch (Exception e) {
                                XLog.e(TAG, "videoRecorder.start(): " + e);
                                notifyVideoRecordError(e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            XLog.i(TAG, "onConfigureFailed");
                            notifyVideoRecordError(new RuntimeException("Video record configure failed."));
                        }
                    }, backgroundHandler);
                } catch (Exception e) {
                    XLog.e(TAG, "startVideoRecord: " + e);
                    notifyVideoRecordError(e);
                }
            }
        };

        if (cameraPreview.getPreviewType() == PreviewViewType.TEXTURE_VIEW) {
            final SurfaceTexture texture = Camera2Manager.this.surfaceTexture;
            texture.setDefaultBufferSize(videoSize.width, videoSize.height);
            sessionCreationTask.run();
        } else if (cameraPreview.getPreviewType() == PreviewViewType.SURFACE_VIEW) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    SurfaceHolder holder = Camera2Manager.this.surfaceHolder;
                    holder.setFixedSize(previewSize.width, previewSize.height);
                    sessionCreationTask.run();
                }
            });
        }
    }

    private void runPreCaptureSequence() {
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            captureSessionCallback.setCameraPreviewState(CaptureSessionCallback.CameraState.STATE_WAITING_PRE_CAPTURE);
            captureSession.capture(previewRequestBuilder.build(), captureSessionCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            XLog.e(TAG, "runPreCaptureSequence error " + e);
        }
    }

    private boolean setFlashModeInternal() {
        try {
            CameraCharacteristics cameraCharacteristics = cameraFace == CameraFace.FACE_FRONT ?
                    frontCameraCharacteristics : rearCameraCharacteristics;
            Boolean isFlashAvailable = cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            if (isFlashAvailable == null || !isFlashAvailable) {
                XLog.i(TAG, "Flash is not available.");
                return false;
            }
            switch (flashMode) {
                case FlashMode.FLASH_ON:
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                    previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_SINGLE);
                    break;
                case FlashMode.FLASH_OFF:
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                    previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
                    break;
                case FlashMode.FLASH_AUTO:
                default:
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_SINGLE);
                    break;
            }
            return true;
        } catch (Exception ex) {
            XLog.e(TAG, "setFlashMode error : " + ex);
        }
        return false;
    }

    private void setAutoFocusInternal() {
        if (isAutoFocus) {
            CameraCharacteristics cameraCharacteristics = cameraFace == CameraFace.FACE_FRONT ?
                    frontCameraCharacteristics : rearCameraCharacteristics;
            int[] modes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
            if (modes == null || modes.length == 0 ||
                    (modes.length == 1 && modes[0] == CameraCharacteristics.CONTROL_AF_MODE_OFF)) {
                isAutoFocus = false;
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
            } else if (mediaType == MediaType.TYPE_PICTURE) {
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            } else if (mediaType == MediaType.TYPE_VIDEO) {
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
            }
        } else {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
        }
    }

    private boolean setZoomInternal(CaptureRequest.Builder builder) {
        float maxZoom = getMaxZoom();
        if (maxZoom == 1.0f || builder == null) {
            return false;
        }

        CameraCharacteristics cameraCharacteristics = cameraFace == CameraFace.FACE_FRONT ?
                frontCameraCharacteristics : rearCameraCharacteristics;
        Rect rect = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        if (rect == null) {
            return false;
        }

        zoom = Math.min(Math.max(zoom, 1.f), maxZoom);

        int cropW = (rect.width() - (int) ((float) rect.width() / zoom)) / 2;
        int cropH = (rect.height() - (int) ((float) rect.height() / zoom)) / 2;

        Rect zoomRect = new Rect(cropW, cropH, rect.width() - cropW, rect.height() - cropH);
        builder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);
        return true;
    }

    private void lockFocus() {
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            captureSessionCallback.setCameraPreviewState(CaptureSessionCallback.CameraState.STATE_WAITING_LOCK);
            captureSession.capture(previewRequestBuilder.build(), captureSessionCallback, backgroundHandler);
        } catch (Exception e) {
            XLog.e(TAG, "lockFocus : error during focus locking");
        }
    }

    private void unlockFocus() {
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            captureSession.capture(previewRequestBuilder.build(), captureSessionCallback, backgroundHandler);
            captureSessionCallback.setCameraPreviewState(CaptureSessionCallback.CameraState.STATE_PREVIEW);
            captureSession.setRepeatingRequest(previewRequest, captureSessionCallback, backgroundHandler);
        } catch (Exception e) {
            XLog.e(TAG, "unlockFocus : error during focus unlocking");
        }
    }

    private boolean prepareVideoRecorder() {
        videoRecorder = new MediaRecorder();
        try {
            videoRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            videoRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);

            videoRecorder.setOutputFormat(camcorderProfile.fileFormat);
            videoRecorder.setVideoFrameRate(camcorderProfile.videoFrameRate);
            videoRecorder.setVideoSize(videoSize.width, videoSize.height);
            videoRecorder.setVideoEncodingBitRate(camcorderProfile.videoBitRate);
            videoRecorder.setVideoEncoder(camcorderProfile.videoCodec);

            videoRecorder.setAudioEncodingBitRate(camcorderProfile.audioBitRate);
            videoRecorder.setAudioChannels(camcorderProfile.audioChannels);
            videoRecorder.setAudioSamplingRate(camcorderProfile.audioSampleRate);
            videoRecorder.setAudioEncoder(camcorderProfile.audioCodec);

            videoRecorder.setOutputFile(videoOutFile.toString());

            if (videoFileSize > 0) {
                videoRecorder.setMaxFileSize(videoFileSize);
                videoRecorder.setOnInfoListener(this);
            }
            if (videoDuration > 0) {
                videoRecorder.setMaxDuration(videoDuration);
                videoRecorder.setOnInfoListener(this);
            }

            CameraCharacteristics cameraCharacteristics = cameraFace == CameraFace.FACE_FRONT ?
                    frontCameraCharacteristics : rearCameraCharacteristics;
            videoRecorder.setOrientationHint(CameraHelper.getJpegOrientation(cameraCharacteristics, displayOrientation));

            videoRecorder.setPreviewDisplay(cameraPreview.getSurface());
            videoRecorder.prepare();

            return true;
        } catch (IllegalStateException error) {
            XLog.e(TAG, "IllegalStateException preparing MediaRecorder: " + error.getMessage());
            notifyVideoRecordError(error);
        } catch (IOException error) {
            XLog.e(TAG, "IOException preparing MediaRecorder: " + error.getMessage());
            notifyVideoRecordError(error);
        } catch (Exception error) {
            XLog.e(TAG, "Error during preparing MediaRecorder: " + error.getMessage());
            notifyVideoRecordError(error);
        }

        releaseVideoRecorder();
        return false;
    }

    private void closePreviewSession() {
        if (captureSession != null) {
            captureSession.close();
            try {
                captureSession.abortCaptures();
            } catch (Exception e) {
                XLog.e(TAG, "closePreviewSession error : " + e);
            } finally {
                captureSession = null;
            }
        }
    }

    private void releaseCameraInternal() {
        previewSize = null;
        pictureSize = null;
        videoSize = null;
        maxZoom = 0;
    }

    private void releaseTexture() {
        if (null != surfaceTexture) {
            surfaceTexture.release();
            surfaceTexture = null;
        }
    }

    private void closeImageReader() {
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
        if (previewReader != null) {
            previewReader.close();
            previewReader = null;
        }
    }

    abstract static class CaptureSessionCallback extends CameraCaptureSession.CaptureCallback {

        @CameraState private int cameraPreviewState;

        void setCameraPreviewState(@CameraState int cameraPreviewState) {
            this.cameraPreviewState = cameraPreviewState;
        }

        abstract void processCaptureResult(@NonNull CaptureResult result, @CameraState int cameraPreviewState);

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            processCaptureResult(partialResult, cameraPreviewState);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            processCaptureResult(result, cameraPreviewState);
        }

        @IntDef({STATE_PREVIEW, STATE_WAITING_LOCK, STATE_WAITING_PRE_CAPTURE,
                STATE_WAITING_NON_PRE_CAPTURE, STATE_PICTURE_TAKEN})
        @Retention(RetentionPolicy.SOURCE)
        @interface CameraState {
            /** Camera state: Showing camera preview. */
            int STATE_PREVIEW = 0;

            /** Camera state: Waiting for the focus to be locked. */
            int STATE_WAITING_LOCK = 1;

            /** Camera state: Waiting for the exposure to be precapture state. */
            int STATE_WAITING_PRE_CAPTURE = 2;

            /** Camera state: Waiting for the exposure state to be something other than precapture. */
            int STATE_WAITING_NON_PRE_CAPTURE = 3;

            /** Camera state: Picture was taken. */
            int STATE_PICTURE_TAKEN = 4;
        }
    }

}
