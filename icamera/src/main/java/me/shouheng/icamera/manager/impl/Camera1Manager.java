package me.shouheng.icamera.manager.impl;

import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.media.MediaActionSound;
import android.media.MediaRecorder;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;

import me.shouheng.icamera.config.ConfigurationProvider;
import me.shouheng.icamera.config.calculator.CameraSizeCalculator;
import me.shouheng.icamera.config.size.AspectRatio;
import me.shouheng.icamera.config.size.Size;
import me.shouheng.icamera.config.size.SizeMap;
import me.shouheng.icamera.enums.CameraFace;
import me.shouheng.icamera.enums.CameraSizeFor;
import me.shouheng.icamera.enums.CameraType;
import me.shouheng.icamera.enums.FlashMode;
import me.shouheng.icamera.enums.MediaQuality;
import me.shouheng.icamera.enums.MediaType;
import me.shouheng.icamera.enums.PreviewViewType;
import me.shouheng.icamera.listener.CameraCloseListener;
import me.shouheng.icamera.listener.CameraOpenListener;
import me.shouheng.icamera.listener.CameraPhotoListener;
import me.shouheng.icamera.listener.CameraVideoListener;
import me.shouheng.icamera.preview.CameraPreview;
import me.shouheng.icamera.preview.CameraPreviewCallback;
import me.shouheng.icamera.util.CameraHelper;
import me.shouheng.icamera.util.XLog;

/**
 * Camera manager for camera1.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:51
 */
public class Camera1Manager extends BaseCameraManager<Integer> {

    private static final String TAG = "Camera1Manager";

    private Camera camera;
    private List<Float> zoomRatios;
    private volatile boolean showingPreview;
    private boolean canDisableShutterSound;

    public Camera1Manager(CameraPreview cameraPreview) {
        super(cameraPreview);
        cameraPreview.setCameraPreviewCallback(new CameraPreviewCallback() {
            @Override
            public void onAvailable(CameraPreview cameraPreview) {
                XLog.d(TAG, "onAvailable : " + cameraPreview.isAvailable());
                if (isCameraOpened()) {
                    setupPreview();
                }
            }
        });
    }

    @Override
    public void initialize(Context context) {
        super.initialize(context);
        initCameraInfo();
    }

    @Override
    public void openCamera(final CameraOpenListener cameraOpenListener) {
        super.openCamera(cameraOpenListener);
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                XLog.d(TAG, "openCamera");
                try {
                    camera = Camera.open(currentCameraId);
                    camera.setPreviewCallback(new Camera.PreviewCallback() {
                        @Override
                        public void onPreviewFrame(byte[] bytes, Camera camera) {
                            notifyPreviewFrameChanged(bytes, previewSize, camera.getParameters().getPreviewFormat());
                        }
                    });
                    prepareCameraOutputs();
                    adjustCameraParameters(false, true, true);
                    if (cameraPreview.isAvailable()) {
                        setupPreview();
                    }
                    camera.startPreview();
                    showingPreview = true;
                    notifyCameraOpened();
                } catch (final Exception ex) {
                    XLog.e(TAG, "error : " + ex);
                    notifyCameraOpenError(ex);
                }
            }
        });
    }

    @Override
    public boolean isCameraOpened() {
        return camera != null;
    }

    @Override
    public void switchCamera(int cameraFace) {
        XLog.d(TAG, "switchCamera : " + cameraFace);
        super.switchCamera(cameraFace);
        if (isCameraOpened()) {
            closeCamera(cameraCloseListener);
            openCamera(cameraOpenListener);
        }
    }

    @Override
    public void setMediaType(@MediaType int mediaType) {
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
                        adjustCameraParameters(true, false, false);
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
        if (voiceEnabled == voiceEnable) {
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
        if (isCameraOpened()) {
            backgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    setFocusModeInternal(null);
                }
            });
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
        if (isCameraOpened()) {
            backgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    setFlashModeInternal(null);
                }
            });
        }
    }

    @FlashMode
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
            backgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    setZoomInternal(null);
                }
            });
        }
    }

    @Override
    public float getZoom() {
        return zoom;
    }

    @Override
    public float getMaxZoom() {
        if (zoomRatios == null) {
            XLog.w(TAG, "Try to get max zoom while it's not ready.");
            return 0;
        }
        if (maxZoom == 0) {
            maxZoom = zoomRatios.get(zoomRatios.size() - 1);
        }
        return maxZoom;
    }

    @Override
    public void setExpectSize(@Nullable Size expectSize) {
        super.setExpectSize(expectSize);
        if (isCameraOpened()) {
            adjustCameraParameters(true, false, false);
        }
    }

    @Override
    public void setExpectAspectRatio(AspectRatio expectAspectRatio) {
        super.setExpectAspectRatio(expectAspectRatio);
        if (isCameraOpened()) {
            adjustCameraParameters(true, false, false);
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
        XLog.d(TAG, "displayOrientation : " + displayOrientation);
        if (this.displayOrientation == displayOrientation) {
            return;
        }
        this.displayOrientation = displayOrientation;
        if (isCameraOpened()) {
            Camera.Parameters parameters = camera.getParameters();
            CameraHelper.onOrientationChanged(currentCameraId, displayOrientation, parameters);
            camera.setParameters(parameters);
            if (showingPreview) {
                camera.stopPreview();
                showingPreview = false;
            }
            camera.setDisplayOrientation(CameraHelper.calDisplayOrientation(context, cameraFace,
                    cameraFace == CameraFace.FACE_FRONT ? frontCameraOrientation : rearCameraOrientation));
            if (!showingPreview) {
                camera.startPreview();
                showingPreview = true;
            }
        }
    }

    @Override
    public void takePicture(File fileToSave, CameraPhotoListener cameraPhotoListener) {
        super.takePicture(fileToSave, cameraPhotoListener);
        if (!isCameraOpened()) {
            notifyCameraCaptureFailed(new RuntimeException("Camera not open yet!"));
            return;
        }
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!takingPicture) {
                        takingPicture = true;
                        setCameraPhotoQualityInternal(camera);
                        // Disable shutter default and play it by custom MediaActionSound.
                        // Make it consistent with camera 2. The shutter of camera1
                        // was not based on the phone mode, so even when the phone is in silent mode
                        // it makes sound still.
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && canDisableShutterSound) {
                            camera.enableShutterSound(false);
                            if (voiceEnabled && mediaActionSound != null) {
                                mediaActionSound.play(MediaActionSound.SHUTTER_CLICK);
                            }
                        } // else, let it go ... there is nothing we could do.
                        camera.takePicture(null, null, new Camera.PictureCallback() {
                            @Override
                            public void onPictureTaken(byte[] bytes, Camera camera) {
                                onPictureTakenInternal(bytes);
                                takingPicture = false;
                            }
                        });
                    } else {
                        XLog.i(TAG, "takePicture : taking picture");
                    }
                } catch (Exception ex) {
                    takingPicture = false;
                    XLog.e(TAG, "takePicture error : " + ex);
                    notifyCameraCaptureFailed(new RuntimeException(ex));
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
            backgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (prepareVideoRecorder()) {
                        videoRecorder.start();
                        videoRecording = true;
                        notifyVideoRecordStart();
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
                    safeStopVideoRecorder();
                    releaseVideoRecorder();
                    videoRecording = false;
                    notifyVideoRecordStop(videoOutFile);
                }
            });
        }
    }

    @Override
    public void resumePreview() {
        if (isCameraOpened()) {
            camera.startPreview();
        }
    }

    @Override
    public void closeCamera(CameraCloseListener cameraCloseListener) {
        if (isCameraOpened()) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
        }
        showingPreview = false;
        if (uiHandler != null) {
            uiHandler.removeCallbacksAndMessages(null);
        }
        if (backgroundHandler != null) {
            backgroundHandler.removeCallbacksAndMessages(null);
        }
        releaseCameraInternal();
        notifyCameraClosed();
    }

    /*-------------------------------------- Inner Methods Region -----------------------------------------*/

    private void initCameraInfo() {
        numberOfCameras = Camera.getNumberOfCameras();
        for (int i=0; i<numberOfCameras; i++) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                rearCameraId = i;
                rearCameraOrientation = cameraInfo.orientation;
            } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                frontCameraId = i;
                frontCameraOrientation = cameraInfo.orientation;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                canDisableShutterSound = cameraInfo.canDisableShutterSound;
            }
        }

        currentCameraId = cameraFace == CameraFace.FACE_REAR ? rearCameraId : frontCameraId;
    }

    private void prepareCameraOutputs() {
        try {
            long start = System.currentTimeMillis();
            previewSizes = ConfigurationProvider.get().getSizes(camera, cameraFace, CameraSizeFor.SIZE_FOR_PREVIEW);
            pictureSizes = ConfigurationProvider.get().getSizes(camera, cameraFace, CameraSizeFor.SIZE_FOR_PICTURE);
            videoSizes = ConfigurationProvider.get().getSizes(camera, cameraFace, CameraSizeFor.SIZE_FOR_VIDEO);
            zoomRatios = ConfigurationProvider.get().getZoomRatios(camera, cameraFace);
            ConfigurationProvider.get().getCameraSizeCalculator().init(
                    expectAspectRatio, expectSize, mediaQuality, previewSizes, pictureSizes, videoSizes);
            XLog.d(TAG, "prepareCameraOutputs cost : " + (System.currentTimeMillis() - start) + " ms");
        } catch (Exception ex) {
            XLog.e(TAG, "error : " + ex);
            notifyCameraOpenError(new RuntimeException(ex));
        }
    }

    private void adjustCameraParameters(boolean forceCalculateSizes, boolean changeFocusMode, boolean changeFlashMode) {
        Size oldPreview = previewSize;
        long start = System.currentTimeMillis();
        CameraSizeCalculator cameraSizeCalculator = ConfigurationProvider.get().getCameraSizeCalculator();
        Camera.Parameters parameters = camera.getParameters();
        if (mediaType == MediaType.TYPE_PICTURE && (pictureSize == null || forceCalculateSizes)) {
            pictureSize = cameraSizeCalculator.getPictureSize(CameraType.TYPE_CAMERA1);
            previewSize = cameraSizeCalculator.getPicturePreviewSize(CameraType.TYPE_CAMERA1);
            parameters.setPictureSize(pictureSize.width, pictureSize.height);
            notifyPictureSizeUpdated(pictureSize);
        }
        // fixed 2020-08-29 : the video size might be null if quickly switched
        // from media types while first time launch the camera.
        if (camcorderProfile == null || forceCalculateSizes) {
            camcorderProfile = CameraHelper.getCamcorderProfile(mediaQuality, currentCameraId);
        }
        if (videoSize == null || forceCalculateSizes) {
            videoSize = cameraSizeCalculator.getVideoSize(CameraType.TYPE_CAMERA1);
            previewSize = cameraSizeCalculator.getVideoPreviewSize(CameraType.TYPE_CAMERA1);
            notifyVideoSizeUpdated(videoSize);
        }
        if (!previewSize.equals(oldPreview)) {
            parameters.setPreviewSize(previewSize.width, previewSize.height);
            notifyPreviewSizeUpdated(previewSize);
        }
        XLog.d(TAG, "adjustCameraParameters size cost : " + (System.currentTimeMillis() - start) + " ms");

        start = System.currentTimeMillis();
        if (changeFocusMode) {
            setFocusModeInternal(parameters);
        }
        XLog.d(TAG, "adjustCameraParameters focus cost : " + (System.currentTimeMillis() - start) + " ms");

        start = System.currentTimeMillis();
        if (changeFlashMode) {
            setFlashModeInternal(parameters);
        }
        XLog.d(TAG, "adjustCameraParameters flash cost : " + (System.currentTimeMillis() - start) + " ms");

        start = System.currentTimeMillis();
        setZoomInternal(parameters);
        XLog.d(TAG, "adjustCameraParameters zoom cost : " + (System.currentTimeMillis() - start) + " ms");

        start = System.currentTimeMillis();
        if (showingPreview) {
            showingPreview = false;
            camera.stopPreview();
        }
        camera.setParameters(parameters);
        if (!showingPreview) {
            showingPreview = true;
            camera.startPreview();
        }
        XLog.d(TAG, "adjustCameraParameters restart preview cost : " + (System.currentTimeMillis() - start) + " ms");
    }

    private void setCameraPhotoQualityInternal(Camera camera) {
        final Camera.Parameters parameters = camera.getParameters();

        parameters.setPictureFormat(PixelFormat.JPEG);

        if (mediaQuality == MediaQuality.QUALITY_LOWEST) {
            parameters.setJpegQuality(25);
        } else if (mediaQuality == MediaQuality.QUALITY_LOW) {
            parameters.setJpegQuality(50);
        } else if (mediaQuality == MediaQuality.QUALITY_MEDIUM) {
            parameters.setJpegQuality(75);
        } else if (mediaQuality == MediaQuality.QUALITY_HIGH) {
            parameters.setJpegQuality(100);
        } else if (mediaQuality == MediaQuality.QUALITY_HIGHEST) {
            parameters.setJpegQuality(100);
        }

        camera.setParameters(parameters);
    }

    private int getPhotoOrientationInternal() {
        final int rotate;
        if (cameraFace == CameraFace.FACE_FRONT) {
            rotate = (360 + frontCameraOrientation + ConfigurationProvider.get().getDegrees()) % 360;
        } else {
            rotate = (360 + rearCameraOrientation - ConfigurationProvider.get().getDegrees()) % 360;
        }

        int orientation = ExifInterface.ORIENTATION_NORMAL;
        if (rotate == 90) {
            orientation = ExifInterface.ORIENTATION_ROTATE_90;
        } else if (rotate == 180) {
            orientation = ExifInterface.ORIENTATION_ROTATE_180;
        } else if (rotate == 270) {
            orientation = ExifInterface.ORIENTATION_ROTATE_270;
        }

        return orientation;
    }

    private int getVideoOrientationInternal() {
        final int rotate;
        if (cameraFace == CameraFace.FACE_FRONT) {
            rotate = (360 + frontCameraOrientation + ConfigurationProvider.get().getDegrees()) % 360;
        } else {
            rotate = (360 + rearCameraOrientation - ConfigurationProvider.get().getDegrees()) % 360;
        }
        return rotate;
    }

    private void onPictureTakenInternal(byte[] bytes) {
        handlePictureTakenResult(bytes);
        // rotate
        try {
            final ExifInterface exif = new ExifInterface(pictureFile.getAbsolutePath());
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, "" + getPhotoOrientationInternal());
            exif.saveAttributes();
            notifyCameraPictureTaken(bytes);
        } catch (Throwable error) {
            XLog.e(TAG, "Can't save exif info: " + error.getMessage());
        }
    }

    private void setupPreview() {
        try {
            if (cameraPreview.getPreviewType() == PreviewViewType.SURFACE_VIEW) {
                if (showingPreview) {
                    camera.stopPreview();
                    showingPreview = false;
                }
                camera.setPreviewDisplay(cameraPreview.getSurfaceHolder());
                if (!showingPreview) {
                    camera.startPreview();
                    showingPreview = true;
                }
            } else {
                camera.setPreviewTexture(cameraPreview.getSurfaceTexture());
            }

            camera.setDisplayOrientation(CameraHelper.calDisplayOrientation(context, cameraFace,
                    cameraFace == CameraFace.FACE_FRONT ? frontCameraOrientation : rearCameraOrientation));
        } catch (IOException e) {
            notifyCameraOpenError(new RuntimeException(e));
        }
    }

    private boolean prepareVideoRecorder() {
        videoRecorder = new MediaRecorder();
        try {
            camera.lock();
            camera.unlock();
            videoRecorder.setCamera(camera);

            videoRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            videoRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);

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

            videoRecorder.setPreviewDisplay(cameraPreview.getSurface());
            videoRecorder.setOrientationHint(getVideoOrientationInternal());
            videoRecorder.prepare();

            return true;
        } catch (IllegalStateException error) {
            XLog.e(TAG, "IllegalStateException preparing MediaRecorder: " + error.getMessage());
            notifyVideoRecordError(error);
        } catch (IOException error) {
            XLog.e(TAG, "IOException preparing MediaRecorder: " + error.getMessage());
            notifyVideoRecordError(error);
        } catch (Throwable error) {
            XLog.e(TAG, "Error during preparing MediaRecorder: " + error.getMessage());
            notifyVideoRecordError(error);
        }

        releaseVideoRecorder();
        return false;
    }

    private void setFocusModeInternal(@Nullable Camera.Parameters parameters) {
        boolean nullParameters = parameters == null;
        parameters = nullParameters ? camera.getParameters() : parameters;
        if (mediaType == MediaType.TYPE_VIDEO) {
            if (!turnVideoCameraFeaturesOn(parameters)) {
                setAutoFocusInternal(parameters);
            }
        } else if (mediaType == MediaType.TYPE_PICTURE) {
            if (!turnPhotoCameraFeaturesOn(parameters)) {
                setAutoFocusInternal(parameters);
            }
        }
        if (nullParameters) {
            camera.setParameters(parameters);
        }
    }

    private boolean turnPhotoCameraFeaturesOn(@NonNull Camera.Parameters parameters) {
        if (parameters.getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            return true;
        }
        return false;
    }

    private boolean turnVideoCameraFeaturesOn(@NonNull Camera.Parameters parameters) {
        if (parameters.getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            return true;
        }
        return false;
    }

    private void setAutoFocusInternal(@NonNull Camera.Parameters parameters) {
        try {
            final List<String> modes = parameters.getSupportedFocusModes();
            if (isAutoFocus && modes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            } else if (modes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
            } else if (modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
            } else {
                parameters.setFocusMode(modes.get(0));
            }
        } catch (Exception ex) {
            XLog.e(TAG, "setAutoFocusInternal " + ex);
        }
    }

    private void setFlashModeInternal(@Nullable Camera.Parameters parameters) {
        boolean nullParameters = parameters == null;
        parameters = nullParameters ? camera.getParameters() : parameters;
        List<String> modes = parameters.getSupportedFlashModes();
        try {
            switch (flashMode) {
                case FlashMode.FLASH_ON:
                    setFlashModeOrAuto(parameters, modes, Camera.Parameters.FLASH_MODE_ON);
                    break;
                case FlashMode.FLASH_OFF:
                    setFlashModeOrAuto(parameters, modes, Camera.Parameters.FLASH_MODE_OFF);
                    break;
                case FlashMode.FLASH_AUTO:
                default:
                    if (modes.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                    }
                    break;
            }
            if (nullParameters) {
                camera.setParameters(parameters);
            }
        } catch (Exception ex) {
            XLog.e(TAG, "setFlashModeInternal : " + ex);
        }
    }

    private void setFlashModeOrAuto(Camera.Parameters parameters, List<String> supportModes, String mode) {
        if (supportModes.contains(mode)) {
            parameters.setFlashMode(mode);
        } else {
            if (supportModes.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
            }
        }
    }

    private void setZoomInternal(@Nullable Camera.Parameters parameters) {
        boolean nullParameters = parameters == null;
        parameters = nullParameters ? camera.getParameters() : parameters;
        if (parameters.isZoomSupported()) {
            parameters.setZoom(CameraHelper.getZoomIdxForZoomFactor(parameters.getZoomRatios(), zoom));
            if (nullParameters) {
                camera.setParameters(parameters);
            }
        }
    }

    private void releaseCameraInternal() {
        if (camera != null) {
            camera.release();
            camera = null;
            previewSize = null;
            pictureSize = null;
            videoSize = null;
            maxZoom = 0;
        }
    }

}
