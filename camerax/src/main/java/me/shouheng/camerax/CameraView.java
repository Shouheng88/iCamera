package me.shouheng.camerax;

import android.content.Context;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import me.shouheng.camerax.configuration.Configuration;
import me.shouheng.camerax.configuration.SizeCalculateStrategy;
import me.shouheng.camerax.enums.Camera;
import me.shouheng.camerax.enums.Media;
import me.shouheng.camerax.listeners.StateListener;
import me.shouheng.camerax.manager.CameraManager;
import me.shouheng.camerax.manager.CameraManagerFactory;
import me.shouheng.camerax.preview.CameraPreview;
import me.shouheng.camerax.preview.CameraPreviewFactory;
import me.shouheng.camerax.utils.AspectRatio;

import java.util.LinkedList;
import java.util.List;

public class CameraView extends FrameLayout {

    private Context context;

    private CallbackBridge callbackBridge;
    private CameraPreview cameraPreview;
    private CameraManager cameraManager;
    private Configuration configuration;
    private SizeCalculateStrategy sizeCalculateStrategy = null;

    @Camera.AdjustType
    private int adjustType = Camera.NONE;
    private int widthRatio = 4;
    private int heightRatio = 3;
    private boolean adjustViewBounds;
    private boolean clipScreen;

    public CameraView(@NonNull Context context) {
        this(context, null);
    }

    public CameraView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public CameraView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        this.context = context;

        callbackBridge = new CallbackBridge();
        cameraPreview = CameraPreviewFactory.getCameraPreview(context, this);
        cameraManager = CameraManagerFactory.getCameraManager(context, callbackBridge, cameraPreview);
        cameraManager.initializeCameraManager(configuration, sizeCalculateStrategy);

//        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CameraView, defStyleAttr, R.style.Widget_CameraView);
        // TODO define the attributes used for widget in xml.
//        a.recycle();

        configuration = new Configuration.Builder()
                .setMediaQuality(Media.MEDIA_QUALITY_MEDIUM)
                .build();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (clipScreen) {
            // region handle measure for clip screen mode;
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = MeasureSpec.getSize(heightMeasureSpec);
            switch (adjustType) {
                case Camera.FIXED_WIDTH:
                    height = width * heightRatio / widthRatio;
                    break;
                case Camera.FIXED_HEIGHT:
                    width = height * widthRatio / heightRatio;
                    break;
                case Camera.SCALE_SMALLER:
                    if (width * heightRatio < height * widthRatio) {
                        height = width * heightRatio / widthRatio;
                    } else {
                        width = height * widthRatio / heightRatio;
                    }
                    break;
                case Camera.SCALE_LARGER:
                    if (width * heightRatio < height * widthRatio) {
                        width = height * widthRatio / heightRatio;
                    } else {
                        height = width * heightRatio / widthRatio;
                    }
                    break;
                case Camera.NONE:
                default:
                    // do nothing
            }
            super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            // endregion handle measure for clip screen.
        } else {
            if (adjustViewBounds) {
                // region handle measure for adjust view bounds
                if (!isCameraOpened()) {
                    callbackBridge.setRequestLayoutOnOpen(true);
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                    return;
                }
                final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
                final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
                if (widthMode == MeasureSpec.EXACTLY && heightMode != MeasureSpec.EXACTLY) {
                    final AspectRatio ratio = getAspectRatio();
                    assert ratio != null;
                    int height = (int) (MeasureSpec.getSize(widthMeasureSpec) * ratio.toFloat());
                    if (heightMode == MeasureSpec.AT_MOST) {
                        height = Math.min(height, MeasureSpec.getSize(heightMeasureSpec));
                    }
                    super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
                } else if (widthMode != MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
                    final AspectRatio ratio = getAspectRatio();
                    assert ratio != null;
                    int width = (int) (MeasureSpec.getSize(heightMeasureSpec) * ratio.toFloat());
                    if (widthMode == MeasureSpec.AT_MOST) {
                        width = Math.min(width, MeasureSpec.getSize(widthMeasureSpec));
                    }
                    super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), heightMeasureSpec);
                } else {
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                }
                // endregion
            } else {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
            // region handle measure for left
            int width = getMeasuredWidth();
            int height = getMeasuredHeight();
            AspectRatio ratio = getAspectRatio();
            // TODO orientation detect logic
//            if (mDisplayOrientationDetector.getLastKnownDisplayOrientation() % 180 == 0) {
//                ratio = ratio.inverse();
//            }
            assert ratio != null;
            if (height < width * ratio.getY() / ratio.getX()) {
                cameraPreview.getView().measure(
                        MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(width * ratio.getY() / ratio.getX(), MeasureSpec.EXACTLY));
            } else {
                cameraPreview.getView().measure(
                        MeasureSpec.makeMeasureSpec(height * ratio.getX() / ratio.getY(), MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            }
            // endregion handle measure for left
        }
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        savedState.configuration = configuration;
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(((SavedState) state).getSuperState());
        configuration = savedState.configuration;
    }

    /* ========================================== Public API Methods ===============================================*/

    public void start(@Camera.CameraFace int cameraFace) {
        if (!cameraManager.openCamera(cameraFace)) {
            cameraManager = CameraManagerFactory.getCameraManager(context, callbackBridge, cameraPreview);
            cameraManager.initializeCameraManager(configuration, sizeCalculateStrategy);
            cameraManager.openCamera(cameraFace);
        }
    }

    public void setWidthHeightRatio(int widthRatio, int heightRatio) {
        this.widthRatio = widthRatio;
        this.heightRatio = heightRatio;
        if (adjustType != Camera.NONE) {
            requestLayout();
        }
    }

    // TODO set the param to the camera manger.
    public void setSizeCalculateStrategy(SizeCalculateStrategy sizeCalculateStrategy) {
        this.sizeCalculateStrategy = sizeCalculateStrategy;
    }

    public boolean isCameraOpened() {
        return cameraManager.isCameraOpened();
    }

    public AspectRatio getAspectRatio() {
        return cameraManager.getAspectRatio();
    }

    public void addStateListener(@NonNull StateListener stateListener) {
        callbackBridge.add(stateListener);
    }

    public void removeStateListener(@NonNull StateListener stateListener) {
        callbackBridge.remove(stateListener);
    }

    /* ========================================== Inner classes ===============================================*/

    /**
     * Event state bridge, used to manage all the {@link StateListener} registered.
     */
    private class CallbackBridge implements CameraManager.Callback {

        private final List<StateListener> stateListeners;

        /**
         * Should call {@link View#requestLayout()} when camera open.
         */
        private boolean requestLayoutOnOpen = true;

        CallbackBridge() {
            this.stateListeners = new LinkedList<>();
        }

        void add(StateListener stateListener) {
            stateListeners.add(stateListener);
        }

        void remove(StateListener stateListener) {
            stateListeners.remove(stateListener);
        }

        void setRequestLayoutOnOpen(boolean requestLayoutOnOpen) {
            this.requestLayoutOnOpen = requestLayoutOnOpen;
        }

        @Override
        public void onCameraOpened() {
            if (requestLayoutOnOpen) {
                requestLayoutOnOpen = false;
                // change the camera preview layout when camera open.
                requestLayout();
            }
            for (StateListener stateListener : stateListeners) {
                stateListener.onCameraOpened(CameraView.this);
            }
        }

        @Override
        public void onCameraClosed() {
            // empty
        }

        @Override
        public void onPictureTaken(byte[] data) {
            // empty
        }

        @Override
        public void onPreviewFrame(byte[] data, int width, int height, int format) {
            // empty
        }

        @Override
        public void notPermission() {
            // empty
        }
    }

    private static class SavedState extends BaseSavedState {

        private Configuration configuration;

        SavedState(Parcel source) {
            super(source);
        }

        SavedState(Parcelable superState) {
            super(superState);
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        SavedState(Parcel source, ClassLoader loader) {
            super(source, loader);
            configuration = source.readParcelable(loader);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeParcelable(configuration, 0);
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.ClassLoaderCreator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new SavedState(in, loader);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
