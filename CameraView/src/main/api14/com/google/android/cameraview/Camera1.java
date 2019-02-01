/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.cameraview;

import android.annotation.TargetApi;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.support.v4.util.SparseArrayCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicBoolean;


@SuppressWarnings("deprecation")
class Camera1 extends CameraViewImpl {

    private static final int INVALID_CAMERA_ID = -1;

    private static final SparseArrayCompat<String> FLASH_MODES = new SparseArrayCompat<>();

    static {
        FLASH_MODES.put(Constants.FLASH_OFF, Camera.Parameters.FLASH_MODE_OFF);
        FLASH_MODES.put(Constants.FLASH_ON, Camera.Parameters.FLASH_MODE_ON);
        FLASH_MODES.put(Constants.FLASH_TORCH, Camera.Parameters.FLASH_MODE_TORCH);
        FLASH_MODES.put(Constants.FLASH_AUTO, Camera.Parameters.FLASH_MODE_AUTO);
        FLASH_MODES.put(Constants.FLASH_RED_EYE, Camera.Parameters.FLASH_MODE_RED_EYE);
    }

    private int mCameraId;

    private final AtomicBoolean isPictureCaptureInProgress = new AtomicBoolean(false);

    Camera mCamera;

    private Camera.Parameters mCameraParameters;

    private final Camera.CameraInfo mCameraInfo = new Camera.CameraInfo();

    private final SizeMap mPreviewSizes = new SizeMap();

    private final SizeMap mPictureSizes = new SizeMap();

    private AspectRatio mAspectRatio;

    private boolean mShowingPreview;

    private boolean mAutoFocus;

    private int mFacing;

    private int mFlash;

    private float mZoom = 1.f;

    private int mDisplayOrientation;

    private boolean mOpenVoice;

    private Handler mHandler = new Handler();

    private Size mSettingPictureSize;

    private Size mSettingPreviewSize;
    private Camera.PreviewCallback mPreviewCallback;
    private int mPreviewFormat;

    private Camera.AutoFocusCallback mAutofocusCallback;

    Camera1(final Callback callback, PreviewImpl preview) {
        super(callback, preview);
        mPreviewCallback = new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] bytes, Camera camera) {
                callback.onPreviewFrame(bytes,
                        mCameraParameters.getPreviewSize().width,
                        mCameraParameters.getPreviewSize().height,
                        mCameraParameters.getPreviewFormat());
            }
        };
        preview.setCallback(new PreviewImpl.Callback() {
            @Override
            public void onSurfaceChanged() {
                if (mCamera != null) {
                    setUpPreview();
                    adjustCameraParameters();
                    mCamera.setPreviewCallback(mPreviewCallback);
                }
            }
        });
    }

    @Override
    boolean start() {
        try {
            chooseCamera();
            mShowingPreview = true;
            openCamera();
            if (mPreview.isReady()) {
                setUpPreview();
            }
            mCamera.startPreview();
            return true;
        } catch (Exception e) {
            mCallback.notPermission();
            e.printStackTrace();
            return false;
        }
    }

    @Override
    void stop() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
        }

        mShowingPreview = false;
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        releaseCamera();
    }

    private void setUpPreview() {
        try {
            if (mCamera != null) {
                if (mPreview.getOutputClass() == SurfaceHolder.class) {
                    final boolean needsToStopPreview =
                            mShowingPreview && Build.VERSION.SDK_INT < 14;
                    if (needsToStopPreview) {
                        mCamera.stopPreview();
                    }
                    mCamera.setPreviewDisplay(mPreview.getSurfaceHolder());
                    if (needsToStopPreview) {
                        mCamera.startPreview();
                    }
                } else {
                    mCamera.setPreviewTexture((SurfaceTexture) mPreview.getSurfaceTexture());
                }
                mCamera.setPreviewCallback(mPreviewCallback);
            } else {
                Log.e("error", "error");
            }
        } catch (Exception e) {
            Log.e("error", "error");
        }
    }

    @Override
    boolean isCameraOpened() {
        return mCamera != null;
    }

    @Override
    void setFacing(int facing) {
        if (mFacing == facing) {
            return;
        }
        mFacing = facing;
        if (isCameraOpened()) {
            stop();
            start();
        }
    }

    @Override
    int getFacing() {
        return mFacing;
    }

    @Override
    Set<AspectRatio> getSupportedAspectRatios() {
        SizeMap idealAspectRatios = mPreviewSizes;
        for (AspectRatio aspectRatio : idealAspectRatios.ratios()) {
            if (mPictureSizes.sizes(aspectRatio) == null) {
                idealAspectRatios.remove(aspectRatio);
            }
        }
        return idealAspectRatios.ratios();
    }

    @Override
    boolean setAspectRatio(AspectRatio ratio) {
        if (mAspectRatio == null || !isCameraOpened()) {
            // Handle this later when camera is opened
            mAspectRatio = ratio;
            return true;
        } else if (!mAspectRatio.equals(ratio)) {
            final Set<Size> sizes = mPreviewSizes.sizes(ratio);
            if (sizes == null) {
                throw new UnsupportedOperationException(ratio + " is not supported");
            } else {
                mAspectRatio = ratio;
                adjustCameraParameters();
                return true;
            }
        }
        return false;
    }

    @Override
    AspectRatio getAspectRatio() {
        return mAspectRatio;
    }

    @Override
    void setAutoFocus(boolean autoFocus) {
        if (mAutoFocus == autoFocus) {
            return;
        }
        if (setAutoFocusInternal(autoFocus)) {
            mCamera.setParameters(mCameraParameters);
        }
    }

    @Override
    boolean getAutoFocus() {
        if (!isCameraOpened()) {
            return mAutoFocus;
        }
        String focusMode = mCameraParameters.getFocusMode();
        return focusMode != null && focusMode.contains("continuous");
    }

    @Override
    void setFlash(int flash) {
        if (flash == mFlash) {
            return;
        }
        if (setFlashInternal(flash)) {
            mCamera.setParameters(mCameraParameters);
        }
    }

    @Override
    int getFlash() {
        return mFlash;
    }

    @Override
    void takePicture() {
        takePicture(true);
    }

    @Override
    void takePicture(boolean needFocus) {
        if (getAutoFocus()) {
            if (mCamera == null)
                return;
            mCamera.cancelAutoFocus();
            try {
                if (needFocus) {
                    mCamera.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
                            takePictureInternal();
                        }
                    });
                } else {
                    takePictureInternal();
                }
            } catch (Exception e) {
                e.printStackTrace();
                takePictureInternal();
            }
        } else {
            takePictureInternal();
        }
    }


    @Override
    void autoFocus() {
        Log.e("camera1", "autoFocus");
        if (mCamera == null)
            return;
        try {
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    resetFocus(success, camera);
                    Log.e("camera1", "success focus");
                }
            });
        } catch (Exception error) {
            //ignore this exception
            error.printStackTrace();
        }
    }

    @Override
    void openVoice(boolean open) {
        mOpenVoice = open;
    }

    private Camera.ShutterCallback shuttleCallback = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {

        }
    };

    void takePictureInternal() {
        Log.e("camera1", isPictureCaptureInProgress.get() + " ");

        if (!isPictureCaptureInProgress.getAndSet(true)) {
            Log.e("camera1", (mCamera == null) + " ");
            if (mCamera == null)
                return;

            mCamera.takePicture(mOpenVoice ? shuttleCallback : null, null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    Log.e("camera1", "onPictureTaken");
                    isPictureCaptureInProgress.set(false);
                    mCallback.onPictureTaken(data);
                    camera.cancelAutoFocus();
                }
            });
        }
    }

    @Override
    void setDisplayOrientation(int displayOrientation) {
        if (mDisplayOrientation == displayOrientation) {
            return;
        }
        mDisplayOrientation = displayOrientation;
        if (isCameraOpened()) {
            mCameraParameters.setRotation(calcCameraRotation(displayOrientation));
            mCamera.setParameters(mCameraParameters);
            final boolean needsToStopPreview = mShowingPreview && Build.VERSION.SDK_INT < 14;
            if (needsToStopPreview) {
                mCamera.stopPreview();
            }
//            mCamera.setDisplayOrientation(calcDisplayOrientation(displayOrientation));
            mCamera.setDisplayOrientation(90);
            if (needsToStopPreview) {
                mCamera.startPreview();
            }
        }
    }

    @Override
    void setPreferredPreviewFormat(int imageFormat) {
        if (mPreviewFormat == imageFormat) {
            return;
        }
        mPreviewFormat = imageFormat;
        if (isCameraOpened()) {
            stop();
            start();
        }
    }

    @Override
    int getPreferredPreviewFormat() {
        return mPreviewFormat;
    }

    @Override
    void setPictureSize(int width, int height) {
        mSettingPictureSize = new Size(width, height);
    }

    @Override
    void setZoom(float zoom) {
        if (zoom == mZoom) {
            return;
        }
        if (setZoomInternal(zoom)) {
            mCamera.setParameters(mCameraParameters);
        }
    }

    @Override
    float getZoom() {
        return mZoom;
    }

    @Override
    float getMaxZoom() {
        if (mCameraParameters == null) return 1.f;
        List<Integer> zoomRatios = mCameraParameters.getZoomRatios();
        if (zoomRatios.isEmpty()) return 1.f;
        return zoomRatios.get(zoomRatios.size() - 1) / 100.f;
    }

    @Override
    void resumePreview() {
        if (isCameraOpened()) {
            mCamera.startPreview();
        }
    }

    private int getZoomIdxForZoomFactor(float zoom) {
        List<Integer> zoomRatios = mCameraParameters.getZoomRatios();

        int zoomRatioFormat = (int) (zoom * 100);

        int len = zoomRatios.size();
        int possibleIdx = 0;
        int minDiff = Integer.MAX_VALUE;
        int tmp;
        for (int i = 0; i < len; ++i) {
            tmp = Math.abs(zoomRatioFormat - zoomRatios.get(i));
            if (tmp < minDiff) {
                minDiff = tmp;
                possibleIdx = i;
            }
        }
        return possibleIdx;
    }

    /**
     * @return {@code true} if {@link #mCameraParameters} was modified.
     */
    private boolean setZoomInternal(float zoom) {
        if (isCameraOpened()) {
            if (!mCameraParameters.isZoomSupported()) return false;
            int camera1Zoom = getZoomIdxForZoomFactor(zoom);
            mCameraParameters.setZoom(camera1Zoom);
            mZoom = zoom;
            return true;
        } else {
            mZoom = zoom;
            return false;
        }
    }

    /**
     * This rewrites {@link #mCameraId} and {@link #mCameraInfo}.
     */
    private void chooseCamera() {
        for (int i = 0, count = Camera.getNumberOfCameras(); i < count; i++) {
            Camera.getCameraInfo(i, mCameraInfo);
            if (mCameraInfo.facing == mFacing) {
                mCameraId = i;
                return;
            }
        }
        mCameraId = INVALID_CAMERA_ID;
    }

    private void openCamera() {
        if (mCamera != null) {
            releaseCamera();
        }
        mCamera = Camera.open(mCameraId);
        mZoom = 1.0f;
        mCameraParameters = mCamera.getParameters();
        // Supported preview sizes
        mPreviewSizes.clear();
        for (Camera.Size size : mCameraParameters.getSupportedPreviewSizes()) {
            mPreviewSizes.add(new Size(size.width, size.height));
        }
        // Supported picture sizes;
        mPictureSizes.clear();
        for (Camera.Size size : mCameraParameters.getSupportedPictureSizes()) {
            mPictureSizes.add(new Size(size.width, size.height));
        }
        // AspectRatio
        if (mAspectRatio == null) {
            mAspectRatio = Constants.DEFAULT_ASPECT_RATIO;
        }
        adjustCameraParameters();
//        mCamera.setDisplayOrientation(calcDisplayOrientation(mDisplayOrientation));
        mCamera.setDisplayOrientation(90);
        mCallback.onCameraOpened();
    }

    private AspectRatio chooseAspectRatio() {
        AspectRatio r = null;
        for (AspectRatio ratio : mPreviewSizes.ratios()) {
            r = ratio;
            if (ratio.equals(Constants.DEFAULT_ASPECT_RATIO)) {
                return ratio;
            }
        }
        return r;
    }

    private class SizeWHRateComparator implements Comparator<Camera.Size> {

        private final int bestFitWidth;
        private final int bestFitHeight;

        SizeWHRateComparator(int width, int height) {
            bestFitWidth = width;
            bestFitHeight = height;
        }

        @Override
        public int compare(Camera.Size size1, Camera.Size size2) {
            int factor1 = Math.max(size1.width, size1.height) * Math.min(size2.width, size2.height) * Math.min(bestFitWidth, bestFitHeight);
            int factor = Math.max(bestFitWidth, bestFitHeight) * Math.min(size1.width, size1.height) * Math.min(size2.width, size2.height);
            int factor2 = Math.max(size2.width, size2.height) * Math.min(size1.width, size1.height) * Math.min(bestFitWidth, bestFitHeight);
            int compare = Math.abs(factor1 - factor) - Math.abs(factor2 - factor);

            return compare == 0 ? size1.width - size2.width : compare;
        }
    }

    void adjustCameraParameters() {
        SortedSet<Size> sizes = mPreviewSizes.sizes(mAspectRatio);
        if (sizes == null) { // Not supported
            mAspectRatio = chooseAspectRatio();
            sizes = mPreviewSizes.sizes(mAspectRatio);
        }
        Size size = chooseOptimalSize(sizes);


        // Always re-apply camera parameters
        // Largest picture size in this ratio
        final Camera.Size currentSize = mCameraParameters.getPictureSize();
        if (currentSize.width != size.getWidth() || currentSize.height != size.getHeight()) {
            Size pictureSize = null;
            SortedSet<Size> pictureSizeS = mPictureSizes.sizes(mAspectRatio);
            if (mSettingPictureSize == null) {
                pictureSize = pictureSizeS.last(); //选择最大的Size
            } else {
                pictureSize = chooseSuitableSize(pictureSizeS, mSettingPictureSize);
                size = chooseSuitableSize(sizes, mSettingPictureSize);
            }
            if (mShowingPreview) {
                mCamera.stopPreview();
            }
            if (mCameraParameters.getSupportedPreviewFormats().contains(getPreferredPreviewFormat())) {
                mCameraParameters.setPreviewFormat(getPreferredPreviewFormat());
            }

            List<Camera.Size> previewSizes = mCameraParameters.getSupportedPreviewSizes();
            Collections.sort(previewSizes, new SizeWHRateComparator(pictureSize.getWidth(), pictureSize.getHeight()));
            for (Camera.Size previewSize : previewSizes) {
                if (Math.min(previewSize.width, previewSize.height) < 750) {
                    continue;
                }
                mCameraParameters.setPreviewSize(previewSize.width, previewSize.height);
                break;
            }
            mCameraParameters.setPictureSize(pictureSize.getWidth(), pictureSize.getHeight());
            mSettingPictureSize = size;
            setAutoFocusInternal(mAutoFocus);
            setFlashInternal(mFlash);
            setZoomInternal(mZoom);
            try {
                mCamera.setParameters(mCameraParameters);
                if (mShowingPreview) {
                    mCamera.startPreview();
                }
            } catch (Exception e) {
            }
        }
    }

    /**
     * 选择合适的pictureSize
     */
    private Size chooseSuitableSize(SortedSet<Size> sizes, Size desiredSize) {
        Size result = null;
        for (Size size : sizes) { // Iterate from small to large
            if (Math.min(size.getWidth(), size.getHeight()) < Math.min(desiredSize.getWidth(), desiredSize.getHeight())) {
                continue;
            }
            result = size;
            break;
        }
        return result;
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private Size chooseOptimalSize(SortedSet<Size> sizes) {
        if (!mPreview.isReady()) { // Not yet laid out
            return sizes.first(); // Return the smallest size
        }
        int desiredWidth;
        int desiredHeight;
        final int surfaceWidth = mPreview.getWidth();
        final int surfaceHeight = mPreview.getHeight();
        if (isLandscape(mDisplayOrientation)) {
            desiredWidth = surfaceHeight;
            desiredHeight = surfaceWidth;
        } else {
            desiredWidth = surfaceWidth;
            desiredHeight = surfaceHeight;
        }
        Size result = null;
        for (Size size : sizes) { // Iterate from small to large
            if (desiredWidth <= size.getWidth() && desiredHeight <= size.getHeight()) {
                return size;

            }
            result = size;
        }
        return result;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
            mCameraParameters = null;
            mCallback.onCameraClosed();
        }
    }

    /**
     * Calculate display orientation
     * https://developer.android.com/reference/android/hardware/Camera.html#setDisplayOrientation
     * (int)
     * <p>
     * This calculation is used for orienting the preview
     * <p>
     * Note: This is not the same calculation as the camera rotation
     *
     * @param screenOrientationDegrees Screen orientation in degrees
     * @return Number of degrees required to rotate preview
     */
    private int calcDisplayOrientation(int screenOrientationDegrees) {
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return (360 - (mCameraInfo.orientation + screenOrientationDegrees) % 360) % 360;
        } else {  // back-facing
            return (mCameraInfo.orientation - screenOrientationDegrees + 360) % 360;
        }
    }

    /**
     * Calculate camera rotation
     * <p>
     * This calculation is applied to the output JPEG either via Exif Orientation tag
     * or by actually transforming the bitmap. (Determined by vendor camera API implementation)
     * <p>
     * Note: This is not the same calculation as the display orientation
     *
     * @param screenOrientationDegrees Screen orientation in degrees
     * @return Number of degrees to rotate image in order for it to view correctly.
     */
    private int calcCameraRotation(int screenOrientationDegrees) {
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return (mCameraInfo.orientation + screenOrientationDegrees) % 360;
        } else {  // back-facing
            final int landscapeFlip = isLandscape(screenOrientationDegrees) ? 180 : 0;
            return (mCameraInfo.orientation + screenOrientationDegrees + landscapeFlip) % 360;
        }
    }

    /**
     * Test if the supplied orientation is in landscape.
     *
     * @param orientationDegrees Orientation in degrees (0,90,180,270)
     * @return True if in landscape, false if portrait
     */
    private boolean isLandscape(int orientationDegrees) {
        return (orientationDegrees == Constants.LANDSCAPE_90 ||
                orientationDegrees == Constants.LANDSCAPE_270);
    }

    /**
     * @return {@code true} if {@link #mCameraParameters} was modified.
     */
    private boolean setAutoFocusInternal(boolean autoFocus) {
        mAutoFocus = autoFocus;
        if (isCameraOpened()) {
            final List<String> modes = mCameraParameters.getSupportedFocusModes();
            if (autoFocus && modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                attachFocusTapListener();
                mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } else if (modes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
                detachFocusTapListener();
                mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
            } else if (modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
                detachFocusTapListener();
                mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
            } else {
                detachFocusTapListener();
                mCameraParameters.setFocusMode(modes.get(0));
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return {@code true} if {@link #mCameraParameters} was modified.
     */
    private boolean setFlashInternal(int flash) {
        if (isCameraOpened()) {
            List<String> modes = mCameraParameters.getSupportedFlashModes();
            String mode = FLASH_MODES.get(flash);
            if (modes != null && modes.contains(mode)) {
                mCameraParameters.setFlashMode(mode);
                mFlash = flash;
                return true;
            }
            String currentMode = FLASH_MODES.get(mFlash);
            if (modes == null || !modes.contains(currentMode)) {
                mCameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                mFlash = Constants.FLASH_OFF;
                return true;
            }
            return false;
        } else {
            mFlash = flash;
            return false;
        }
    }

    float finger_spacing;

    @TargetApi(14)
    private void attachFocusTapListener() {
        if (mCamera == null)
            return;
        mPreview.getView().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getPointerCount() > 1) {
                    float current_finger_spacing = getFingerSpacing(event);
                    if (finger_spacing != 0) {
                        try {
                            if (mCamera != null) {
                                Camera.Parameters params = mCamera.getParameters();
                                if (params.isZoomSupported()) {
                                    int maxZoom = params.getMaxZoom();
                                    int zoom = params.getZoom();
                                    if (finger_spacing < current_finger_spacing && zoom < maxZoom) {
                                        zoom++;
                                    } else if (current_finger_spacing < finger_spacing && zoom > 0) {
                                        zoom--;
                                    }
                                    mZoom = zoom;
                                    params.setZoom(zoom);
                                    mCamera.setParameters(params);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    finger_spacing = current_finger_spacing;
                } else {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        try {
                            if (mCamera != null) {
                                Camera.Parameters parameters = mCamera.getParameters();
                                String focusMode = parameters.getFocusMode();
                                Rect rect = calculateFocusArea(event.getX(), event.getY());
                                List<Camera.Area> meteringAreas = new ArrayList<>();
                                meteringAreas.add(new Camera.Area(rect, getFocusMeteringAreaWeight()));
                                if (parameters.getMaxNumFocusAreas() != 0 && focusMode != null &&
                                        (focusMode.equals(Camera.Parameters.FOCUS_MODE_AUTO) ||
                                                focusMode.equals(Camera.Parameters.FOCUS_MODE_MACRO) ||
                                                focusMode.equals(
                                                        Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) ||
                                                focusMode.equals(
                                                        Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
                                        ) {
                                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                                    parameters.setFocusAreas(meteringAreas);
                                    if (parameters.getMaxNumMeteringAreas() > 0) {
                                        parameters.setMeteringAreas(meteringAreas);
                                    }
                                    if (!parameters.getSupportedFocusModes().contains(
                                            Camera.Parameters.FOCUS_MODE_AUTO)) {
                                        return false; //cannot autoFocus
                                    }

                                    try {
                                        mCamera.setParameters(parameters);
                                        mCamera.autoFocus(new Camera.AutoFocusCallback() {
                                            @Override
                                            public void onAutoFocus(boolean success, Camera camera) {
                                                resetFocus(success, camera);
                                            }
                                        });
                                    } catch (Exception error) {
                                        //ignore this exception
                                        error.printStackTrace();
                                    }
                                } else if (parameters.getMaxNumMeteringAreas() > 0) {
                                    if (!parameters.getSupportedFocusModes().contains(
                                            Camera.Parameters.FOCUS_MODE_AUTO)) {
                                        return false; //cannot autoFocus
                                    }
                                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                                    parameters.setFocusAreas(meteringAreas);
                                    parameters.setMeteringAreas(meteringAreas);

                                    mCamera.setParameters(parameters);
                                    mCamera.autoFocus(new Camera.AutoFocusCallback() {
                                        @Override
                                        public void onAutoFocus(boolean success, Camera camera) {
                                            resetFocus(success, camera);
                                        }
                                    });
                                } else {
                                    mCamera.autoFocus(new Camera.AutoFocusCallback() {
                                        @Override
                                        public void onAutoFocus(boolean success, Camera camera) {
                                            if (mAutofocusCallback != null) {
                                                mAutofocusCallback.onAutoFocus(success, camera);
                                            }
                                        }
                                    });
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                return true;
            }
        });
    }

    private float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    @TargetApi(14)
    private void resetFocus(final boolean success, final Camera camera) {
        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (camera != null) {
                    camera.cancelAutoFocus();
                    try {
                        Camera.Parameters params = camera.getParameters();//数据上报中红米Note3在这里可能crash
                        if (params != null && !params.getFocusMode().equalsIgnoreCase(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                            //之前这里并没有考虑相机是否支持FOCUS_MODE_CONTINUOUS_PICTURE，可能是因为这个原因导致部分三星机型上调用后面的setParameters失败
                            if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                                params.setFocusAreas(null);
                                params.setMeteringAreas(null);
                                camera.setParameters(params);//数据上报中三星低端机型在这里可能crash
                            }
                        }
                    } catch (Exception error) {
                        //ignore this exception
                    }
                    if (mAutofocusCallback != null) {
                        mAutofocusCallback.onAutoFocus(success, camera);
                    }
                }
            }
        }, DELAY_MILLIS_BEFORE_RESETTING_FOCUS);
    }

    Rect calculateFocusArea(float x, float y) {
        int buffer = getFocusAreaSize() / 2;
        int centerX = calculateCenter(x, mPreview.getView().getWidth(), buffer);
        int centerY = calculateCenter(y, mPreview.getView().getHeight(), buffer);
        return new Rect(
                centerX - buffer,
                centerY - buffer,
                centerX + buffer,
                centerY + buffer
        );
    }

    static int calculateCenter(float coord, int dimen, int buffer) {
        int normalized = (int) ((coord / dimen) * 2000 - 1000);
        if (Math.abs(normalized) + buffer > 1000) {
            if (normalized > 0) {
                return 1000 - buffer;
            } else {
                return -1000 + buffer;
            }
        } else {
            return normalized;
        }
    }

}
