package me.shouheng.camerax.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.CamcorderProfile;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.Surface;
import android.view.WindowManager;
import me.shouheng.camerax.config.sizes.AspectRatio;
import me.shouheng.camerax.config.sizes.Size;
import me.shouheng.camerax.config.sizes.SizeMap;
import me.shouheng.camerax.enums.Camera;
import me.shouheng.camerax.enums.Media;

import java.util.LinkedList;
import java.util.List;

import static android.view.OrientationEventListener.ORIENTATION_UNKNOWN;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/14 9:34
 */
public final class CameraHelper {

    private static final String TAG = "CameraHelper";

    private CameraHelper() {
        throw new UnsupportedOperationException("U can't initialize me!");
    }

    public static boolean hasCamera(Context context) {
        PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) ||
                pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static boolean hasCamera2(Context context) {
        if (context == null) return false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return false;
        try {
            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            assert manager != null;
            String[] idList = manager.getCameraIdList();
            boolean notNull = true;
            if (idList.length == 0) {
                notNull = false;
            } else {
                for (final String str : idList) {
                    if (str == null || str.trim().isEmpty()) {
                        notNull = false;
                        break;
                    }
                    final CameraCharacteristics characteristics = manager.getCameraCharacteristics(str);

                    Integer iSupportLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                    if (iSupportLevel != null && iSupportLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                        notNull = false;
                        break;
                    }
                }
            }
            return notNull;
        } catch (Throwable ignore) {
            return false;
        }
    }

    public static void onOrientationChanged(int cameraId, int orientation, android.hardware.Camera.Parameters parameters) {
        if (orientation == ORIENTATION_UNKNOWN) return;
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        orientation = (orientation + 45) / 90 * 90;
        int rotation;
        if (info.facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT) {
            rotation = (info.orientation - orientation + 360) % 360;
        } else {  // back-facing camera
            rotation = (info.orientation + orientation) % 360;
        }
        parameters.setRotation(rotation);
    }

    public static int calDisplayOrientation(Context context, @Camera.Face int cameraFace, int cameraOrientation) {
        int displayRotation;

        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        assert manager != null;
        final int rotation = manager.getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break; // Natural orientation
            case Surface.ROTATION_90:
                degrees = 90;
                break; // Landscape left
            case Surface.ROTATION_180:
                degrees = 180;
                break;// Upside down
            case Surface.ROTATION_270:
                degrees = 270;
                break;// Landscape right
        }

        if (cameraFace == Camera.FACE_FRONT) {
            displayRotation = (360 - (cameraOrientation + degrees) % 360) % 360; // compensate
        } else {
            displayRotation = (cameraOrientation - degrees + 360) % 360;
        }

        return displayRotation;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static int getJpegOrientation(@NonNull CameraCharacteristics c, int deviceOrientation) {
        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN) return 0;
        Integer sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION);
        assert sensorOrientation != null;

        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90;

        // Reverse device orientation for front-facing cameras
        Integer lensFacing = c.get(CameraCharacteristics.LENS_FACING);
        boolean facingFront = lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_FRONT;
        if (facingFront) deviceOrientation = -deviceOrientation;

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    public static SizeMap getSizeMapFromSizes(@NonNull List<Size> sizes) {
        SizeMap sizeMap = new SizeMap();
        for (Size size : sizes) {
            AspectRatio aspectRatio = AspectRatio.of(size);
            List<Size> list = sizeMap.get(aspectRatio);
            if (list == null) {
                list = new LinkedList<>();
                list.add(size);
                sizeMap.put(aspectRatio, list);
            } else {
                list.add(size);
            }
        }
        return sizeMap;
    }

    public static Size getSizeWithClosestRatio(List<Size> sizes, Size expectSize) {
        if (sizes == null) return null;

        Size optimalSize = null;
        double targetRatio = expectSize.ratio();
        double minRatioDiff = Double.MAX_VALUE;

        for (Size size : sizes) {
            if (size.equals(expectSize)) {
                return size;
            }
            double ratioDiff = Math.abs(size.ratio() - targetRatio);
            if (ratioDiff < minRatioDiff) {
                optimalSize = size;
                minRatioDiff = ratioDiff;
            }
        }

        int minHeightDiff = Integer.MAX_VALUE;
        int targetHeight = expectSize.height;
        for (Size size : sizes) {
            if (size.ratio() == minHeightDiff) {
                int heightDiff = Math.abs(size.height - targetHeight);
                if (heightDiff <= minHeightDiff) {
                    minHeightDiff = heightDiff;
                    optimalSize = size;
                }
            }
        }

        Logger.d(TAG, "getSizeWithClosestRatio : expected " + expectSize + ", result " + optimalSize);
        return optimalSize;
    }

    private static double calculateApproximateVideoSize(CamcorderProfile camcorderProfile, int seconds) {
        return ((camcorderProfile.videoBitRate / (float) 1 + camcorderProfile.audioBitRate / (float) 1) * seconds) / (float) 8;
    }

    public static double calculateApproximateVideoDuration(CamcorderProfile camcorderProfile, long maxFileSize) {
        return 8 * maxFileSize / (camcorderProfile.videoBitRate + camcorderProfile.audioBitRate);
    }

    private static long calculateMinimumRequiredBitRate(CamcorderProfile camcorderProfile, long maxFileSize, int seconds) {
        return 8 * maxFileSize / seconds - camcorderProfile.audioBitRate;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static CamcorderProfile getCamcorderProfile(String cameraId, long maximumFileSize, int minimumDurationInSeconds) {
        if (TextUtils.isEmpty(cameraId)) {
            return null;
        }
        int cameraIdInt = Integer.parseInt(cameraId);
        return getCamcorderProfile(cameraIdInt, maximumFileSize, minimumDurationInSeconds);
    }

    public static CamcorderProfile getCamcorderProfile(int currentCameraId, long maximumFileSize, int minimumDurationInSeconds) {
        if (maximumFileSize <= 0)
            return CamcorderProfile.get(currentCameraId, Media.QUALITY_HIGHEST);

        int[] qualities = new int[]{Media.QUALITY_HIGHEST,
                Media.QUALITY_HIGH, Media.QUALITY_MEDIUM,
                Media.QUALITY_LOW, Media.QUALITY_LOWEST};

        CamcorderProfile camcorderProfile;
        for (int quality : qualities) {
            camcorderProfile = CameraHelper.getCamcorderProfile(quality, currentCameraId);
            double fileSize = CameraHelper.calculateApproximateVideoSize(camcorderProfile, minimumDurationInSeconds);

            if (fileSize > maximumFileSize) {
                long minimumRequiredBitRate = calculateMinimumRequiredBitRate(camcorderProfile, maximumFileSize, minimumDurationInSeconds);

                if (minimumRequiredBitRate >= camcorderProfile.videoBitRate / 4 && minimumRequiredBitRate <= camcorderProfile.videoBitRate) {
                    camcorderProfile.videoBitRate = (int) minimumRequiredBitRate;
                    return camcorderProfile;
                }
            } else return camcorderProfile;
        }
        return CameraHelper.getCamcorderProfile(Media.QUALITY_LOWEST, currentCameraId);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static CamcorderProfile getCamcorderProfile(@Media.Quality int mediaQuality, String cameraId) {
        if (TextUtils.isEmpty(cameraId)) {
            return null;
        }
        int cameraIdInt = Integer.parseInt(cameraId);
        return getCamcorderProfile(mediaQuality, cameraIdInt);
    }

    public static CamcorderProfile getCamcorderProfile(@Media.Quality int mediaQuality, int cameraId) {
        if (mediaQuality == Media.QUALITY_HIGHEST) {
            return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH);
        } else if (mediaQuality == Media.QUALITY_HIGH) {
            if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_1080P)) {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_1080P);
            } else if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_720P)) {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_720P);
            } else {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH);
            }
        } else if (mediaQuality == Media.QUALITY_MEDIUM) {
            if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_720P)) {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_720P);
            } else if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_480P)) {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_480P);
            } else {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW);
            }
        } else if (mediaQuality == Media.QUALITY_LOW) {
            if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_480P)) {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_480P);
            } else {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW);
            }
        } else if (mediaQuality == Media.QUALITY_LOWEST) {
            return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW);
        } else {
            return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH);
        }
    }

    public static int getZoomIdxForZoomFactor(List<Integer> zoomRatios, float zoom) {
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
}
