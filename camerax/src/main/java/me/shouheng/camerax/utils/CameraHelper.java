package me.shouheng.camerax.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.CamcorderProfile;
import android.os.Build;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import me.shouheng.camerax.enums.Media;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class CameraHelper {

    private CameraHelper() {
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static CamcorderProfile getCamcorderProfile(String cameraId, long maximumFileSize, int minimumDurationInSeconds) {
        if (TextUtils.isEmpty(cameraId)) {
            return null;
        }
        int cameraIdInt = Integer.parseInt(cameraId);
        return getCamcorderProfile(cameraIdInt, maximumFileSize, minimumDurationInSeconds);
    }

    /**
     * Get camcorder profile for video.
     *
     * @param cameraId camera id.
     * @param maximumFileSize maximum file size.
     * @param minimumDurationInSeconds minimum duration in seconds.
     * @return the camcorder profile.
     */
    public static CamcorderProfile getCamcorderProfile(int cameraId, long maximumFileSize, int minimumDurationInSeconds) {
        if (maximumFileSize <= 0)
            return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH);

        int[] qualities = new int[]{Media.MEDIA_QUALITY_HIGHEST,
                Media.MEDIA_QUALITY_HIGH, Media.MEDIA_QUALITY_MEDIUM,
                Media.MEDIA_QUALITY_LOW, Media.MEDIA_QUALITY_LOWEST};

        CamcorderProfile camcorderProfile;
        for (int quality : qualities) {
            camcorderProfile = CameraHelper.getCamcorderProfile(quality, cameraId);
            double fileSize = CameraHelper.calculateApproximateVideoSize(camcorderProfile, minimumDurationInSeconds);
            if (fileSize > maximumFileSize) {
                long minimumRequiredBitRate = calculateMinimumRequiredBitRate(camcorderProfile, maximumFileSize, minimumDurationInSeconds);
                if (minimumRequiredBitRate >= camcorderProfile.videoBitRate / 4 && minimumRequiredBitRate <= camcorderProfile.videoBitRate) {
                    camcorderProfile.videoBitRate = (int) minimumRequiredBitRate;
                    return camcorderProfile;
                }
            } else return camcorderProfile;
        }
        return CameraHelper.getCamcorderProfile(Media.MEDIA_QUALITY_LOWEST, cameraId);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static CamcorderProfile getCamcorderProfile(@Media.MediaQuality int mediaQuality, String cameraId) {
        if (TextUtils.isEmpty(cameraId)) {
            return null;
        }
        int cameraIdInt = Integer.parseInt(cameraId);
        return getCamcorderProfile(mediaQuality, cameraIdInt);
    }

    /**
     * Get camcorder profile according to media quality and camera id.
     *
     * @param mediaQuality media quality.
     * @param cameraId camera id.
     * @return camcorder profile.
     */
    public static CamcorderProfile getCamcorderProfile(@Media.MediaQuality int mediaQuality, int cameraId) {
        switch (mediaQuality) {
            case Media.MEDIA_QUALITY_HIGHEST:
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH);
            case Media.MEDIA_QUALITY_HIGH:
                if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_1080P)) {
                    return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_1080P);
                } else if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_720P)) {
                    return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_720P);
                } else {
                    return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH);
                }
            case Media.MEDIA_QUALITY_MEDIUM:
                if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_720P)) {
                    return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_720P);
                } else if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_480P)) {
                    return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_480P);
                } else {
                    return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW);
                }
            case Media.MEDIA_QUALITY_LOW:
                if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_480P)) {
                    return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_480P);
                } else {
                    return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW);
                }
            case Media.MEDIA_QUALITY_LOWEST:
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW);
            case Media.MEDIA_QUALITY_AUTO:
            default:
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH);
        }
    }

    /**
     * Detect if camera2 is available.
     *
     * @param context the context
     * @return true if available.
     */
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
                    final Integer supportLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                    if (supportLevel == null || supportLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                        notNull = false;
                        break;
                    }
                }
            }
            return notNull;
        } catch (Exception ignore) {
            return false;
        }
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

    /**
     * Get closet ratio according to size and height.
     * <b>Strategy:</b> Ratio first and then the largest image, otherwise then min height difference.
     *
     * @param sizes support sizes.
     * @param width the width desired.
     * @param height the height desired.
     * @return the result size.
     */
    public static Size getSizeWithClosestRatio(List<Size> sizes, int width, int height) {
        if (sizes == null || sizes.isEmpty()) return null;

        double targetRatio = (double) height / width;
        double minTolerance = 100;
        Size optimalSize = null;

        // TODO check if the size is sorted from smaller to bigger?
        for (Size size : sizes) {
            // 1. The same size, return directly;
            if (size.getWidth() == width && size.getHeight() == height) {
                return size;
            }

            // 2. Ratio first;
            double ratio = (double) size.getHeight() / size.getWidth();
            if (Math.abs(ratio - targetRatio) < minTolerance) {
                minTolerance = Math.abs(ratio - targetRatio);
                optimalSize = size;
            }
        }

        // Step 2. Find the Size with min height difference.
        double minDiff;
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.getHeight() - height) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.getHeight() - height);
                }
            }
        }
        return optimalSize;
    }

    /**
     * Get output image size.
     *
     * @param sizes support sizes.
     * @param mediaQuality the media quality.
     * @return the output picture size.
     */
    public static Size getPictureSize(List<Size> sizes, @Media.MediaQuality int mediaQuality) {
        if (sizes == null || sizes.isEmpty()) return null;
        if (sizes.size() == 1) return sizes.get(0);

        Size result = null;
        Size maxPictureSize = Collections.max(sizes, new CompareSizesByArea2());
        Size minPictureSize = Collections.min(sizes, new CompareSizesByArea2());

        Collections.sort(sizes, new CompareSizesByArea2());

        switch (mediaQuality) {
            case Media.MEDIA_QUALITY_HIGHEST:
                result = maxPictureSize;
                break;
            case Media.MEDIA_QUALITY_HIGH:
                if (sizes.size() == 2) {
                    result = maxPictureSize;
                } else {
                    // Approximately, 3/4 maxPictureSize.
                    int half = sizes.size() / 2;
                    int highQualityIndex = (sizes.size() - half) / 2;
                    result = sizes.get(sizes.size() - highQualityIndex - 1);
                }
                break;
            case Media.MEDIA_QUALITY_MEDIUM:
                if (sizes.size() == 2) {
                    result = minPictureSize;
                } else {
                    // Approximately, 2/4 maxPictureSize.
                    int mediumQualityIndex = sizes.size() / 2;
                    result = sizes.get(mediumQualityIndex);
                }
                break;
            case Media.MEDIA_QUALITY_LOW:
                if (sizes.size() == 2) {
                    result = minPictureSize;
                } else {
                    // Approximately, 1/4 maxPictureSize.
                    int half = sizes.size() / 2;
                    int lowQualityIndex = (sizes.size() - half) / 2;
                    result = sizes.get(lowQualityIndex + 1);
                }
                break;
            case Media.MEDIA_QUALITY_LOWEST:
            case Media.MEDIA_QUALITY_AUTO:
                result = minPictureSize;
                break;
        }

        return result;
    }

    public static Size getPictureSize(Size[] sizes, @Media.MediaQuality int mediaQuality) {
        if (sizes == null || sizes.length == 0) return null;
        List<Size> choices = Arrays.asList(sizes);
        return getPictureSize(choices, mediaQuality);
    }

    /**
     * Get index of given zoom value in zoom ratios got from camera support values.
     *
     * @param zoom zoom.
     * @param zoomRatios support zoom ratios.
     * @return the index of zoom in zoom ratios.
     */
    public static int getZoomIdxForZoomFactor(float zoom, List<Integer> zoomRatios) {
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
     * Get the spacing between two fingers by calculate the distance.
     *
     * @param event motion event
     * @return spacing
     */
    public static float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * Calculate focus area according to the coordinate and preview view.
     *
     * @param view the preview view
     * @param x the x coordinate
     * @param y the y coordinate
     * @return the area rectangle
     */
    public static Rect calculateFocusArea(View view, float x, float y) {
        int buffer = Constants.FOCUS_AREA_SIZE_DEFAULT / 2;
        int centerX = calculateCenter(x, view.getWidth(), buffer);
        int centerY = calculateCenter(y, view.getHeight(), buffer);
        return new Rect(centerX - buffer, centerY - buffer, centerX + buffer, centerY + buffer);
    }

    private static int calculateCenter(float coord, int dimen, int buffer) {
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

    /**
     * Size comparator by the area2 of the size.
     */
    private static class CompareSizesByArea2 implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }
}
