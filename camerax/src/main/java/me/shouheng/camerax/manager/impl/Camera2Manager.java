package me.shouheng.camerax.manager.impl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.media.MediaRecorder;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.view.Surface;
import android.view.SurfaceHolder;
import me.shouheng.camerax.config.ConfigurationProvider;
import me.shouheng.camerax.config.calculator.CameraSizeCalculator;
import me.shouheng.camerax.config.sizes.AspectRatio;
import me.shouheng.camerax.config.sizes.Size;
import me.shouheng.camerax.config.sizes.SizeMap;
import me.shouheng.camerax.enums.Camera;
import me.shouheng.camerax.enums.Flash;
import me.shouheng.camerax.enums.Media;
import me.shouheng.camerax.enums.Preview;
import me.shouheng.camerax.listener.CameraCloseListener;
import me.shouheng.camerax.listener.CameraOpenListener;
import me.shouheng.camerax.listener.CameraPhotoListener;
import me.shouheng.camerax.listener.CameraVideoListener;
import me.shouheng.camerax.preview.CameraPreview;
import me.shouheng.camerax.preview.CameraPreviewCallback;
import me.shouheng.camerax.util.CameraHelper;
import me.shouheng.camerax.util.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
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

    private SurfaceHolder surfaceHolder;
    private SurfaceTexture surfaceTexture;
    private Surface workingSurface;

    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder previewRequestBuilder;
    private CaptureRequest previewRequest;

    private CaptureSessionCallback captureSessionCallback = new CaptureSessionCallback() {

        private void processCaptureResultInternal(@NonNull CaptureResult result, int cameraPreviewState) {
            switch (cameraPreviewState) {
                case STATE_PREVIEW:
                    break;
                case STATE_WAITING_LOCK: {
                    final Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
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
                }
                case STATE_WAITING_PRE_CAPTURE: {
                    final Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        setCameraPreviewState(STATE_WAITING_NON_PRE_CAPTURE);
                    }
                    break;
                }
                case STATE_WAITING_NON_PRE_CAPTURE: {
                    final Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        setCameraPreviewState(STATE_PICTURE_TAKEN);
                        captureStillPicture();
                    }
                    break;
                }
                case STATE_PICTURE_TAKEN:
                    break;
            }
        }

        @Override
        void processCaptureResult(@NonNull CaptureResult result, int cameraPreviewState) {
            processCaptureResultInternal(result, cameraPreviewState);
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
                prepareCameraOutputs();
                adjustCameraConfiguration(false);
                try {
                    cameraManager.openCamera(currentCameraId, new CameraDevice.StateCallback() {
                        @Override
                        public void onOpened(@NonNull CameraDevice camera) {
                            Logger.d(TAG, "Camera opened.");
                            cameraDevice = camera;
                            if (cameraPreview.isAvailable()) {
                                createPreviewSession();
                            }
                            notifyCameraOpened();
                        }

                        @Override
                        public void onDisconnected(@NonNull CameraDevice camera) {
                            Logger.e(TAG, "Camera disconnected.");
                            camera.close();
                            cameraDevice = null;
                            notifyCameraOpenError(new RuntimeException("Camera disconnected."));
                        }

                        @Override
                        public void onError(@NonNull CameraDevice camera, int error) {
                            Logger.e(TAG, "Camera open error : " + error);
                            camera.close();
                            cameraDevice = null;
                            notifyCameraOpenError(new RuntimeException("Camera error : " + error));
                        }
                    }, backgroundHandler);
                } catch (Exception ex) {
                    Logger.e(TAG, "error : " + ex);
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
            ConfigurationProvider.get().clearCachedValues();
            openCamera(cameraOpenListener);
        }
    }

    @Override
    public void setMediaType(int mediaType) {
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
                        Logger.e(TAG, "setMediaType : " + ex);
                    }
                }
            });
        }
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
                    Logger.e(TAG, "setAutoFocus error : " + e);
                }
            } else {
                Logger.i(TAG, "setAutoFocus captureSession is null.");
            }
        } else {
            Logger.i(TAG, "setAutoFocus camera not open or previewRequestBuilder is null");
        }
    }

    @Override
    public boolean isAutoFocus() {
        return isAutoFocus;
    }

    @Override
    public void setFlashMode(@Flash.FlashMode int flashMode) {
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
                        Logger.e(TAG, "setFlashMode error : " + e);
                    }
                } else {
                    Logger.i(TAG, "setFlashMode captureSession is null.");
                }
            } else {
                Logger.i(TAG, "setFlashMode failed.");
            }
        } else {
            Logger.i(TAG, "setFlashMode camera not open or previewRequestBuilder is null");
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
            boolean succeed = setZoomInternal();
            if (succeed) {
                previewRequest = previewRequestBuilder.build();
                if (captureSession != null) {
                    try {
                        captureSession.setRepeatingRequest(previewRequest, captureSessionCallback, backgroundHandler);
                    } catch (CameraAccessException e) {
                        Logger.e(TAG, "setZoom error : " + e);
                    }
                } else {
                    Logger.i(TAG, "setZoom captureSession is null.");
                }
            } else {
                Logger.i(TAG, "setZoom failed : setZoomInternal failed.");
            }
        } else {
            Logger.i(TAG, "setZoom failed : camera not open.");
        }
    }

    @Override
    public float getZoom() {
        return zoom;
    }

    @Override
    public float getMaxZoom() {
        if (maxZoom == 0) {
            CameraCharacteristics cameraCharacteristics = cameraFace == Camera.FACE_FRONT ?
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
    public void setExpectSize(Size expectSize) {
        super.setExpectSize(expectSize);
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                adjustCameraConfiguration(true);
                createPreviewSession();
            }
        });
    }

    @Override
    public void setExpectAspectRatio(AspectRatio expectAspectRatio) {
        super.setExpectAspectRatio(expectAspectRatio);
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                adjustCameraConfiguration(true);
                createPreviewSession();
            }
        });
    }

    @Override
    public Size getSize(@Camera.SizeFor int sizeFor) {
        switch (sizeFor) {
            case Camera.SIZE_FOR_PREVIEW:
                return previewSize;
            case Camera.SIZE_FOR_PICTURE:
                return pictureSize;
            case Camera.SIZE_FOR_VIDEO:
                return videoSize;
            default:
                return null;
        }
    }

    @Override
    public SizeMap getSizes(@Camera.SizeFor int sizeFor) {
        switch (sizeFor) {
            case Camera.SIZE_FOR_PREVIEW:
                if (previewSizeMap == null) {
                    previewSizeMap = CameraHelper.getSizeMapFromSizes(previewSizes);
                }
                return previewSizeMap;
            case Camera.SIZE_FOR_PICTURE:
                if (pictureSizeMap == null) {
                    pictureSizeMap = CameraHelper.getSizeMapFromSizes(pictureSizes);
                }
                return pictureSizeMap;
            case Camera.SIZE_FOR_VIDEO:
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
        // TODO the display orientation
    }

    @Override
    public void takePicture(CameraPhotoListener cameraPhotoListener) {
        super.takePicture(cameraPhotoListener);
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    lockFocus();
                    if (voiceEnabled) {
                        new MediaActionSound().play(MediaActionSound.SHUTTER_CLICK);
                    }
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
                new MediaActionSound().play(MediaActionSound.START_VIDEO_RECORDING);
            }
            backgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    closePreviewSession();
                    if (prepareVideoRecorder()) {
                        createRecordSession();
                    } else {
                        Logger.i(TAG, "startVideoRecord : failed when prepare video recorder.");
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
                        new MediaActionSound().play(MediaActionSound.STOP_VIDEO_RECORDING);
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
                notifyCameraPictureTaken(bytes);
            } catch (IllegalStateException e) {
                Logger.e(TAG, "onImageAvailable error" + e);
                notifyCameraCaptureFailed(e);
            }
        } catch (Exception e) {
            Logger.e(TAG, "onImageAvailable error " + e);
            notifyCameraCaptureFailed(e);
        } finally {
            unlockFocus();
        }
    }

    /*---------------------------------inner methods------------------------------------*/

    // TODO this method cost a lot of time to finish
    private void initCameraInfo(Context context) {
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        long start = System.currentTimeMillis();
        try {
            assert cameraManager != null;
            final String[] ids = cameraManager.getCameraIdList();
            numberOfCameras = ids.length;
            for (String id : ids) {
                final CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    frontCameraId = id;
                    Integer iFrontCameraOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                    frontCameraOrientation = iFrontCameraOrientation == null ? 0 : frontCameraOrientation;
                    frontCameraCharacteristics = characteristics;
                } else if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK){
                    rearCameraId = id;
                    Integer iRearCameraOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                    rearCameraOrientation = iRearCameraOrientation == null ? 0 : rearCameraOrientation;
                    rearCameraCharacteristics = characteristics;
                }
            }
        } catch (Exception e) {
            Logger.e(TAG, "initCameraInfo error " + e);
        }
        Logger.d(TAG, "initCameraInfo basic cost : " + (System.currentTimeMillis() - start) + " ms");

        currentCameraId = cameraFace == Camera.FACE_REAR ? rearCameraId : frontCameraId;
    }

    private void prepareCameraOutputs() {
        boolean isFrontCamera = cameraFace == Camera.FACE_REAR;
        long start = System.currentTimeMillis();
        try {
            final CameraCharacteristics characteristics = isFrontCamera ? frontCameraCharacteristics : rearCameraCharacteristics;
            if (isFrontCamera && frontStreamConfigurationMap == null) {
                frontStreamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            } else if (!isFrontCamera && rearStreamConfigurationMap == null) {
                rearStreamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            }
        } catch (Exception ex) {
            Logger.e(TAG, "initCameraInfo error " + ex);
            notifyCameraOpenError(new RuntimeException(ex));
        }
        Logger.d(TAG, "initCameraInfo get map cost : " + (System.currentTimeMillis() - start) + " ms");

        start = System.currentTimeMillis();
        try {
            final StreamConfigurationMap map = isFrontCamera ? frontStreamConfigurationMap : rearStreamConfigurationMap;
            previewSizes = ConfigurationProvider.get().getPreviewSizes(map);
            pictureSizes = ConfigurationProvider.get().getPictureSizes(map);
            videoSizes = ConfigurationProvider.get().getVideoSizes(map);
        } catch (Exception ex) {
            Logger.e(TAG, "error : " + ex);
            notifyCameraOpenError(new RuntimeException(ex));
        }
        Logger.d(TAG, "prepareCameraOutputs cost : " + (System.currentTimeMillis() - start) + " ms");
    }

    private void adjustCameraConfiguration(boolean forceCalculate) {
        Size oldPreviewSize = previewSize;
        CameraSizeCalculator cameraSizeCalculator = ConfigurationProvider.get().getCameraSizeCalculator();
        if (pictureSize == null || forceCalculate) {
            pictureSize = cameraSizeCalculator.getPictureSize(pictureSizes, expectAspectRatio, expectSize);
            previewSize = cameraSizeCalculator.getPicturePreviewSize(previewSizes, pictureSize);
            notifyPictureSizeUpdated(pictureSize);

            imageReader = ImageReader.newInstance(pictureSize.width, pictureSize.height, ImageFormat.JPEG, /*maxImages*/2);
            imageReader.setOnImageAvailableListener(this, backgroundHandler);
        }
        if (mediaType == Media.TYPE_VIDEO && (videoSize == null || forceCalculate)) {
            camcorderProfile = CameraHelper.getCamcorderProfile(mediaQuality, currentCameraId);
            videoSize = cameraSizeCalculator.getVideoSize(videoSizes, expectAspectRatio, expectSize);
            previewSize = cameraSizeCalculator.getVideoPreviewSize(previewSizes, videoSize);
            notifyVideoSizeUpdated(videoSize);
        }
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
                        cameraDevice.createCaptureSession(Arrays.asList(workingSurface, imageReader.getSurface()),
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
                                                Logger.e(TAG, "createPreviewSession error " + ex);
                                                notifyCameraOpenError(ex);
                                            } catch (IllegalStateException ex) {
                                                Logger.e(TAG, "createPreviewSession error " + ex);
                                                notifyCameraOpenError(ex);
                                            }
                                        }
                                    }

                                    @Override
                                    public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                                        Logger.d(TAG, "onConfigureFailed");
                                        notifyCameraOpenError(new Throwable("Camera capture session configure failed."));
                                    }
                                }, backgroundHandler);
                    } catch (Exception ex) {
                        Logger.e(TAG, "createPreviewSession error " + ex);
                        notifyCameraOpenError(ex);
                    }
                }
            };

            if (cameraPreview.getPreviewType() == Preview.TEXTURE_VIEW) {
                this.surfaceTexture = cameraPreview.getSurfaceTexture();
                assert surfaceTexture != null;
                surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height);
                workingSurface = cameraPreview.getSurface();
                sessionCreationTask.run();
            } else if (cameraPreview.getPreviewType() == Preview.SURFACE_VIEW) {
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
            Logger.e(TAG, "createPreviewSession error " + ex);
            notifyCameraOpenError(ex);
        }
    }

    private void captureStillPicture() {
        if (isCameraOpened()) {
            try {
                CameraCharacteristics cameraCharacteristics = cameraFace == Camera.FACE_FRONT ?
                        frontCameraCharacteristics : rearCameraCharacteristics;

                final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                captureBuilder.addTarget(imageReader.getSurface());

                captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                captureBuilder.set(CaptureRequest.JPEG_ORIENTATION,
                        CameraHelper.getJpegOrientation(cameraCharacteristics, displayOrientation));

                // TODO the zoomed result is invalid
                setZoomInternal();

                captureSession.stopRepeating();
                captureSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                                   @NonNull CaptureRequest request,
                                                   @NonNull TotalCaptureResult result) {
                        Logger.d(TAG, "onCaptureCompleted: ");
                    }
                }, null);
            } catch (CameraAccessException e) {
                Logger.e(TAG, "Error during capturing picture");
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
                                Logger.e(TAG, "videoRecorder.start(): " + e);
                                notifyVideoRecordError(e);
                            }

                            try {
                                videoRecorder.start();
                                videoRecording = true;
                                notifyVideoRecordStart();
                            } catch (Exception e) {
                                Logger.e(TAG, "videoRecorder.start(): " + e);
                                notifyVideoRecordError(e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Logger.i(TAG, "onConfigureFailed");
                            notifyVideoRecordError(new RuntimeException("Video record configure failed."));
                        }
                    }, backgroundHandler);
                } catch (Exception e) {
                    Logger.e(TAG, "startVideoRecord: " + e);
                    notifyVideoRecordError(e);
                }
            }
        };

        if (cameraPreview.getPreviewType() == Preview.TEXTURE_VIEW) {
            final SurfaceTexture texture = Camera2Manager.this.surfaceTexture;
            texture.setDefaultBufferSize(videoSize.width, videoSize.height);
            sessionCreationTask.run();
        } else if (cameraPreview.getPreviewType() == Preview.SURFACE_VIEW) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    SurfaceHolder surfaceHolder = Camera2Manager.this.surfaceHolder;
                    surfaceHolder.setFixedSize(previewSize.width, previewSize.height);
                    sessionCreationTask.run();
                }
            });
        }
    }

    private void runPreCaptureSequence() {
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            captureSessionCallback.setCameraPreviewState(CaptureSessionCallback.STATE_WAITING_PRE_CAPTURE);
            captureSession.capture(previewRequestBuilder.build(), captureSessionCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            Logger.e(TAG, "runPreCaptureSequence error " + e);
        }
    }

    private boolean setFlashModeInternal() {
        try {
            CameraCharacteristics cameraCharacteristics = cameraFace == Camera.FACE_FRONT ?
                    frontCameraCharacteristics : rearCameraCharacteristics;
            Boolean isFlashAvailable = cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            if (isFlashAvailable == null || !isFlashAvailable) {
                Logger.i(TAG, "Flash is not available.");
                return false;
            }
            switch (flashMode) {
                case Flash.FLASH_ON:
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                    previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_SINGLE);
                    break;
                case Flash.FLASH_OFF:
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                    previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
                    break;
                case Flash.FLASH_AUTO:
                default:
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_SINGLE);
                    break;
            }
            return true;
        } catch (Exception ex) {
            Logger.e(TAG, "setFlashMode error : " + ex);
        }
        return false;
    }

    private void setAutoFocusInternal() {
        if (isAutoFocus) {
            CameraCharacteristics cameraCharacteristics = cameraFace == Camera.FACE_FRONT ?
                    frontCameraCharacteristics : rearCameraCharacteristics;
            int[] modes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
            if (modes == null || modes.length == 0 ||
                    (modes.length == 1 && modes[0] == CameraCharacteristics.CONTROL_AF_MODE_OFF)) {
                isAutoFocus = false;
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
            } else if (mediaType == Media.TYPE_PICTURE) {
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            } else if (mediaType == Media.TYPE_VIDEO) {
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
            }
        } else {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
        }
    }

    private boolean setZoomInternal() {
        float maxZoom = getMaxZoom();
        if (maxZoom == 1.0f || previewRequestBuilder == null) {
            return false;
        }

        CameraCharacteristics cameraCharacteristics = cameraFace == Camera.FACE_FRONT ?
                frontCameraCharacteristics : rearCameraCharacteristics;
        Rect rect = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        if (rect == null) {
            return false;
        }

        zoom = zoom < 1.f ? 1.f : zoom;
        zoom = zoom > maxZoom ? maxZoom : zoom;

        int cropW = (rect.width() - (int) ((float) rect.width() / zoom)) / 2;
        int cropH = (rect.height() - (int) ((float) rect.height() / zoom)) / 2;

        Rect zoomRect = new Rect(cropW, cropH, rect.width() - cropW, rect.height() - cropH);
        previewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);
        return true;
    }

    private void lockFocus() {
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            captureSessionCallback.setCameraPreviewState(CaptureSessionCallback.STATE_WAITING_LOCK);
            captureSession.capture(previewRequestBuilder.build(), captureSessionCallback, backgroundHandler);
        } catch (Exception e) {
            Logger.e(TAG, "lockFocus : error during focus locking");
        }
    }

    private void unlockFocus() {
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            captureSession.capture(previewRequestBuilder.build(), captureSessionCallback, backgroundHandler);
            captureSessionCallback.setCameraPreviewState(CaptureSessionCallback.STATE_PREVIEW);
            captureSession.setRepeatingRequest(previewRequest, captureSessionCallback, backgroundHandler);
        } catch (Exception e) {
            Logger.e(TAG, "unlockFocus : error during focus unlocking");
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

            CameraCharacteristics cameraCharacteristics = cameraFace == Camera.FACE_FRONT ?
                    frontCameraCharacteristics : rearCameraCharacteristics;
            videoRecorder.setOrientationHint(CameraHelper.getJpegOrientation(cameraCharacteristics, displayOrientation));

            videoRecorder.setPreviewDisplay(cameraPreview.getSurface());
            videoRecorder.prepare();

            return true;
        } catch (IllegalStateException error) {
            Logger.e(TAG, "IllegalStateException preparing MediaRecorder: " + error.getMessage());
            notifyVideoRecordError(error);
        } catch (IOException error) {
            Logger.e(TAG, "IOException preparing MediaRecorder: " + error.getMessage());
            notifyVideoRecordError(error);
        } catch (Throwable error) {
            Logger.e(TAG, "Error during preparing MediaRecorder: " + error.getMessage());
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
                Logger.e(TAG, "closePreviewSession error : " + e);
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
    }

    private static abstract class CaptureSessionCallback extends CameraCaptureSession.CaptureCallback {

        /**
         * Camera state: Showing camera preview.
         */
        static final int STATE_PREVIEW = 0;

        /**
         * Camera state: Waiting for the focus to be locked.
         */
        static final int STATE_WAITING_LOCK = 1;

        /**
         * Camera state: Waiting for the exposure to be precapture state.
         */
        static final int STATE_WAITING_PRE_CAPTURE = 2;

        /**
         * Camera state: Waiting for the exposure state to be something other than precapture.
         */
        static final int STATE_WAITING_NON_PRE_CAPTURE = 3;

        /**
         * Camera state: Picture was taken.
         */
        static final int STATE_PICTURE_TAKEN = 4;

        @CameraState
        private int cameraPreviewState;

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

        @IntDef({STATE_PREVIEW, STATE_WAITING_LOCK, STATE_WAITING_PRE_CAPTURE, STATE_WAITING_NON_PRE_CAPTURE, STATE_PICTURE_TAKEN})
        @Retention(RetentionPolicy.SOURCE)
        @interface CameraState {
        }
    }

}
