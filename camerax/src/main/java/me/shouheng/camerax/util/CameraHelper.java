package me.shouheng.camerax.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.CamcorderProfile;
import android.os.Build;
import android.text.TextUtils;
import android.view.Surface;
import android.view.WindowManager;
import me.shouheng.camerax.config.sizes.Size;
import me.shouheng.camerax.enums.Camera;
import me.shouheng.camerax.enums.Media;

import java.util.List;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/14 9:34
 */
public final class CameraHelper {

    private static final String TAG = "CameraHelper";

    private CameraHelper() {
        throw new UnsupportedOperationException("U can't initialize me!");
    }

    public static int calDisplayOrientation(Context context, @Camera.Face int cameraFace, int cameraOrientation) {
        int displayRotation;

        final int rotation = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
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
        for (int i = 0; i < qualities.length; ++i) {
            camcorderProfile = CameraHelper.getCamcorderProfile(qualities[i], currentCameraId);
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
        if (Build.VERSION.SDK_INT > 10) {
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
        } else {
            if (mediaQuality == Media.QUALITY_HIGHEST) {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH);
            } else if (mediaQuality == Media.QUALITY_HIGH) {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH);
            } else if (mediaQuality == Media.QUALITY_MEDIUM) {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW);
            } else if (mediaQuality == Media.QUALITY_LOW) {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW);
            } else if (mediaQuality == Media.QUALITY_LOWEST) {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW);
            } else {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH);
            }
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
