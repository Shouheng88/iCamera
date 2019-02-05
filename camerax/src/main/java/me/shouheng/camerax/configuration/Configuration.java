package me.shouheng.camerax.configuration;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntRange;
import me.shouheng.camerax.enums.Camera;
import me.shouheng.camerax.enums.Media;

import static me.shouheng.camerax.enums.Media.MEDIA_QUALITY_AUTO;

/**
 * TODO Check the builder api when release!!
 *
 * The configurations for camera and media.
 */
public final class Configuration implements Parcelable {

    @Camera.CameraFace
    private int cameraFace = Camera.CAMERA_FACE_REAR;

    @Camera.FlashMode
    private int flashMode = Camera.FLASH_AUTO;

    @Camera.SensorPosition
    private int sensorPosition = Camera.SENSOR_POSITION_UNSPECIFIED;

    @Camera.ScreenOrientation
    private int screenOrientation = Camera.ORIENTATION_PORTRAIT;

    @Camera.AdjustType
    private int adjustType = Camera.NONE;

    @Camera.FocusMode
    private int focusMode = Camera.FOCUS_MODE_AUTO;

    private boolean autoFocus;

    private boolean supportZoom = false;

    private float zoom;

    @Camera.PreviewFormat
    private int previewFormat = Camera.NV21;

    private int videoDuration = 0;

    private long videoFileSize = 0;

    private int minimumVideoDuration = 0;

    @Media.MediaAction
    private int mediaAction = Media.MEDIA_ACTION_UNSPECIFIED;

    @Media.MediaQuality
    private int mediaQuality = MEDIA_QUALITY_AUTO;

    private Configuration() {
    }

    private Configuration(Parcel in) {
        cameraFace = in.readInt();
        flashMode = in.readInt();
        sensorPosition = in.readInt();
        screenOrientation = in.readInt();
        adjustType = in.readInt();
        videoDuration = in.readInt();
        videoFileSize = in.readLong();
        minimumVideoDuration = in.readInt();
        mediaAction = in.readInt();
        mediaQuality = in.readInt();
    }

    public static final Creator<Configuration> CREATOR = new Creator<Configuration>() {
        @Override
        public Configuration createFromParcel(Parcel in) {
            return new Configuration(in);
        }

        @Override
        public Configuration[] newArray(int size) {
            return new Configuration[size];
        }
    };

    // TODO
    public boolean isSupportZoom() {
        return true;
    }

    public void setSupportZoom(boolean supportZoom) {
        this.supportZoom = supportZoom;
    }

    public float getZoom() {
        return zoom;
    }

    public void setZoom(float zoom) {
        this.zoom = zoom;
    }

    public void setCameraFace(int cameraFace) {
        this.cameraFace = cameraFace;
    }

    public void setFlashMode(int flashMode) {
        this.flashMode = flashMode;
    }

    public void setSensorPosition(int sensorPosition) {
        this.sensorPosition = sensorPosition;
    }

    public void setScreenOrientation(int screenOrientation) {
        this.screenOrientation = screenOrientation;
    }

    public void setAdjustType(int adjustType) {
        this.adjustType = adjustType;
    }

    public void setAutoFocus(boolean autoFocus) {
        this.autoFocus = autoFocus;
    }

    public void setPreviewFormat(int previewFormat) {
        this.previewFormat = previewFormat;
    }

    public void setVideoDuration(int videoDuration) {
        this.videoDuration = videoDuration;
    }

    public void setVideoFileSize(long videoFileSize) {
        this.videoFileSize = videoFileSize;
    }

    public void setMinimumVideoDuration(int minimumVideoDuration) {
        this.minimumVideoDuration = minimumVideoDuration;
    }

    public void setMediaAction(int mediaAction) {
        this.mediaAction = mediaAction;
    }

    public void setMediaQuality(int mediaQuality) {
        this.mediaQuality = mediaQuality;
    }

    public int getFocusMode() {
        return focusMode;
    }

    public void setFocusMode(int focusMode) {
        this.focusMode = focusMode;
    }

    @Camera.CameraFace
    public int getCameraFace() {
        return cameraFace;
    }

    @Camera.FlashMode
    public int getFlashMode() {
        return flashMode;
    }

    @Camera.SensorPosition
    public int getSensorPosition() {
        return sensorPosition;
    }

    @Camera.ScreenOrientation
    public int getScreenOrientation() {
        return screenOrientation;
    }

    @Camera.AdjustType
    public int getAdjustType() {
        return adjustType;
    }

    public boolean isAutoFocus() {
        return autoFocus;
    }

    @Camera.PreviewFormat
    public int getPreviewFormat() {
        return previewFormat;
    }

    @Media.MediaAction
    public int getMediaAction() {
        return mediaAction;
    }

    @Media.MediaQuality
    public int getMediaQuality() {
        return mediaQuality;
    }

    public int getVideoDuration() {
        return videoDuration;
    }

    public long getVideoFileSize() {
        return videoFileSize;
    }

    public int getMinimumVideoDuration() {
        return minimumVideoDuration;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(cameraFace);
        dest.writeInt(flashMode);
        dest.writeInt(sensorPosition);
        dest.writeInt(screenOrientation);
        dest.writeInt(adjustType);
        dest.writeInt(videoDuration);
        dest.writeLong(videoFileSize);
        dest.writeInt(minimumVideoDuration);
        dest.writeInt(mediaAction);
        dest.writeInt(mediaQuality);
    }

    @Override
    public String toString() {
        return "Configuration{" +
                "cameraFace=" + cameraFace +
                ", flashMode=" + flashMode +
                ", sensorPosition=" + sensorPosition +
                ", screenOrientation=" + screenOrientation +
                ", adjustType=" + adjustType +
                ", videoDuration=" + videoDuration +
                ", videoFileSize=" + videoFileSize +
                ", minimumVideoDuration=" + minimumVideoDuration +
                ", mediaAction=" + mediaAction +
                ", mediaQuality=" + mediaQuality +
                '}';
    }

    public static class Builder {

        private Configuration configuration;

        public Builder() {
            this.configuration = new Configuration();
        }

        public Builder setCameraFace(@Camera.CameraFace int cameraFace) {
            this.configuration.cameraFace = cameraFace;
            return this;
        }

        public Builder setFlashMode(@Camera.FlashMode int flashMode) {
            this.configuration.flashMode = flashMode;
            return this;
        }

        public Builder setSensorPosition(@Camera.SensorPosition int sensorPosition) {
            this.configuration.sensorPosition = sensorPosition;
            return this;
        }

        public Builder setScreenOrientation(@Camera.ScreenOrientation int screenOrientation) {
            this.configuration.screenOrientation = screenOrientation;
            return this;
        }

        public Builder setFocusMode(@Camera.FocusMode int focusMode) {
            this.configuration.focusMode = focusMode;
            return this;
        }

        public Builder setAdjustType(@Camera.AdjustType int adjustType) {
            this.configuration.adjustType = adjustType;
            return this;
        }

        public Builder setVideoDuration(@IntRange(from = 1000, to = Integer.MAX_VALUE) int videoDuration) {
            this.configuration.videoDuration = videoDuration;
            return this;
        }

        public Builder setVideoFileSize(@IntRange(from = 1048576) long videoFileSize) {
            this.configuration.videoFileSize = videoFileSize;
            return this;
        }

        public Builder setMinimumVideoDuration(@IntRange(from = 1000, to = Integer.MAX_VALUE) int minimumVideoDuration) {
            this.configuration.minimumVideoDuration = minimumVideoDuration;
            return this;
        }

        public Builder setMediaAction(@Media.MediaAction int mediaAction) {
            this.configuration.mediaAction = mediaAction;
            return this;
        }

        public Builder setMediaQuality(@Media.MediaQuality int mediaQuality) {
            this.configuration.mediaQuality = mediaQuality;
            return this;
        }

        public Configuration build() {
            if (configuration.mediaQuality == MEDIA_QUALITY_AUTO && configuration.minimumVideoDuration < 0) {
                throw new IllegalArgumentException("Please provide minimum video duration in milliseconds to use auto quality.");
            }
            return configuration;
        }
    }
}
