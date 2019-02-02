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
import me.shouheng.camerax.listeners.StateListener;
import me.shouheng.camerax.manager.CameraManager;
import me.shouheng.camerax.manager.CameraManagerFactory;
import me.shouheng.camerax.preview.CameraPreview;
import me.shouheng.camerax.preview.CameraPreviewFactory;

import java.util.LinkedList;
import java.util.List;

public class CameraView extends FrameLayout {

    private Context context;

    private CallbackBridge callbackBridge;
    private CameraPreview cameraPreview;
    private CameraManager cameraManager;
    private Configuration configuration;

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
        cameraPreview = CameraPreviewFactory.getCameraPreview();
        cameraManager = CameraManagerFactory.getCameraManager(context, callbackBridge, cameraPreview);
        cameraManager.initializeCameraManager(configuration);

//        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CameraView, defStyleAttr, R.style.Widget_CameraView);
        // TODO define the attributes used for widget in xml.
//        a.recycle();

        configuration = new Configuration.Builder().build();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
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

    public void start() {
        if (!cameraManager.start()) {
            cameraManager = CameraManagerFactory.getCameraManager(context, callbackBridge, cameraPreview);
            cameraManager.initializeCameraManager(configuration);
            cameraManager.start();
        }
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

        public void setRequestLayoutOnOpen(boolean requestLayoutOnOpen) {
            this.requestLayoutOnOpen = requestLayoutOnOpen;
        }

        @Override
        public void onCameraOpened() {
            if (requestLayoutOnOpen) {
                requestLayoutOnOpen = false;
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
