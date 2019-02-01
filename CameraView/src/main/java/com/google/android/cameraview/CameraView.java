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

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.ImageFormat;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.os.ParcelableCompat;
import android.support.v4.os.ParcelableCompatCreatorCallbacks;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * TODO 问题列表：
 * 1. 设置照片的参数是如何设置的？
 * 2. 考虑使用异步的操作来设置相机以提升打开相机的速度
 */
public class CameraView extends FrameLayout {

    /**
     * The camera device faces the opposite direction as the device's screen.
     */
    public static final int FACING_BACK = Constants.FACING_BACK;

    /**
     * The camera device faces the same direction as the device's screen.
     */
    public static final int FACING_FRONT = Constants.FACING_FRONT;

    /**
     * Direction the camera faces relative to device screen.
     */
    @IntDef({FACING_BACK, FACING_FRONT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Facing {
    }

    /**
     * Flash will not be fired.
     */
    public static final int FLASH_OFF = Constants.FLASH_OFF;

    /**
     * Flash will always be fired during snapshot.
     */
    public static final int FLASH_ON = Constants.FLASH_ON;

    /**
     * Constant emission of light during preview, auto-focus and snapshot.
     */
    public static final int FLASH_TORCH = Constants.FLASH_TORCH;

    /**
     * Flash will be fired automatically when required.
     */
    public static final int FLASH_AUTO = Constants.FLASH_AUTO;

    /**
     * Flash will be fired in red-eye reduction mode.
     */
    public static final int FLASH_RED_EYE = Constants.FLASH_RED_EYE;

    static final int NONE = 0;
    static final int FIXED_WIDTH = 1;
    static final int FIXED_HEIGHT = 2;
    static final int SCALE_SMALLER = 3;
    static final int SCALE_LARGER = 4;

    /**
     * The mode for for the camera device's flash control
     */
    @IntDef({FLASH_OFF, FLASH_ON, FLASH_TORCH, FLASH_AUTO, FLASH_RED_EYE})
    public @interface Flash { }

    @IntDef({NONE, FIXED_WIDTH, FIXED_HEIGHT, SCALE_SMALLER, SCALE_LARGER})
    public @interface AdjustType { }

    CameraViewImpl mImpl;

    private int widthRatio = 4;
    private int heightRatio = 3;
    private int adjustType = NONE;

    private final CallbackBridge mCallbacks;

    private boolean mAdjustViewBounds;
    private boolean mClipScreen;
    boolean multiTouch = false;

    private final DisplayOrientationDetector mDisplayOrientationDetector;

    FocusMarkerLayout focusMarkerLayout;
    PreviewImpl preview;
    OnTouchListener onTouchListener;

    Context mContext;

    public interface OnMoveListener {
        void onMove(boolean left);
    }

    public interface OnFocusListener {
        void onFocus();
    }

    OnMoveListener moveListener;
    OnFocusListener focusListener;

    private int mDownViewX, mDownViewY, dx, dy;

    //UI旋转角度
    private int touchAngle = 0;

    public void setTouchAngle(int touchAngle) {
        this.touchAngle = touchAngle;
    }

    public CameraView(Context context) {
        this(context, null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        if (isInEditMode()) {
            mCallbacks = null;
            mDisplayOrientationDetector = null;
            return;
        }

        // region TODO 考虑使用工厂方法重构这部分代码
        preview = createPreviewImpl(context);
        mCallbacks = new CallbackBridge();
        if (Build.VERSION.SDK_INT < 21) {
            mImpl = new Camera1(mCallbacks, preview);
        } else if (Build.VERSION.SDK_INT < 23) {
            mImpl = new Camera1(mCallbacks, preview);
        } else {
            mImpl = new Camera1(mCallbacks, preview);
        }
        // endregion

        // region 获取布局属性的操作，保留这部分逻辑
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CameraView, defStyleAttr, R.style.Widget_CameraView);
        mAdjustViewBounds = a.getBoolean(R.styleable.CameraView_android_adjustViewBounds, false);
        mClipScreen = a.getBoolean(R.styleable.CameraView_clipScreen, false);
        adjustType = a.getInt(R.styleable.CameraView_cameraAdjustType, adjustType);
        // 设置前后摄像头
        setFacing(a.getInt(R.styleable.CameraView_facing, FACING_BACK));
        // 设置预览的格式
        setPreviewFormat(a.getInt(R.styleable.CameraView_preferredPreviewFormat, Build.VERSION.SDK_INT < 21 ? ImageFormat.NV21 : ImageFormat.YUV_420_888));
        // 设置宽高比
        String aspectRatio = a.getString(R.styleable.CameraView_aspectRatio);
        if (aspectRatio != null) {
            setAspectRatio(AspectRatio.parse(aspectRatio));
        } else {
            setAspectRatio(Constants.DEFAULT_ASPECT_RATIO);
        }
        // 设置自动对焦
        setAutoFocus(a.getBoolean(R.styleable.CameraView_autoFocus, true));
        // 设置闪光灯
        setFlash(a.getInt(R.styleable.CameraView_flash, Constants.FLASH_AUTO));
        // 类似于放缩的感觉
        String zoomString = a.getString(R.styleable.CameraView_zoom);
        if (!TextUtils.isEmpty(zoomString)) {
            try {
                setZoom(Float.valueOf(zoomString));
            } catch (NumberFormatException e) {
                setZoom(1.0f);
            }
        } else {
            setZoom(1.0f);
        }
        a.recycle();
        // endregion

        // 检测屏幕的方向
        mDisplayOrientationDetector = new DisplayOrientationDetector(context) {
            @Override
            public void onDisplayOrientationChanged(int displayOrientation) {
                mImpl.setDisplayOrientation(displayOrientation);
            }
        };

        // region 设置触摸的效果
        focusMarkerLayout = new FocusMarkerLayout(getContext());
        addView(focusMarkerLayout);
        onTouchListener = new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                if (motionEvent.getPointerCount() > 1) {
                    Log.e("camera1", "多指点击");
                    multiTouch = true;
                    // TODO 多点触控的时候事件是如何被处理的？？
                    preview.getView().dispatchTouchEvent(motionEvent);
                } else {
                    if (motionEvent.getPointerCount() == 1) {
                        Log.e("camera1", "单指点击");
                        int x = (int) motionEvent.getRawX();
                        int y = (int) motionEvent.getRawY();
                        if (action == MotionEvent.ACTION_DOWN) {
                            mDownViewX = x;
                            mDownViewY = y;
                        }
                        if (action == MotionEvent.ACTION_MOVE) {
                            dx = x - mDownViewX;
                            dy = y - mDownViewY;
                        }
                        if (action == MotionEvent.ACTION_UP) {
                            if (focusListener != null) {
                                focusListener.onFocus();
                            }
                            Log.e("camera1", "手抬起");
                            if (multiTouch) {
                                multiTouch = false;
                            } else {
                                if (Math.abs(dx) > 100 && touchAngle == 0) {
                                    if (dx < -100) {//向左
                                        moveListener.onMove(true);
                                    } else {//向右1
                                        moveListener.onMove(false);
                                    }
                                    return true;
                                }

                                if (Math.abs(dy) > 100 && touchAngle == 90){
                                    if (dy < -100) {//向左
                                        moveListener.onMove(false);
                                    } else {//向右
                                        moveListener.onMove(true);
                                    }
                                    return true;
                                }

                                if (Math.abs(dy) > 100 && touchAngle == -90){
                                    if (dy > 100) {//向左
                                        moveListener.onMove(false);
                                    } else {//向右1
                                        moveListener.onMove(true);
                                    }
                                    return true;
                                }

                                focusMarkerLayout.focus(motionEvent.getX(), motionEvent.getY());
                            }
                        }
                    }
                    preview.getView().dispatchTouchEvent(motionEvent);
                }
                return true;
            }
        };

        focusMarkerLayout.setOnTouchListener(onTouchListener);
        // endregion
    }

    public void setOnMoveListener(OnMoveListener listener) {
        this.moveListener = listener;
    }

    public void setOnFocusListener(OnFocusListener listener) {
        this.focusListener = listener;
    }

    private void setPreviewFormat(int anInt) {
        mImpl.setPreferredPreviewFormat(anInt);
    }

    /**
     * 创建用于预览相机的控件，根据系统版本，考虑使用工厂模式优化
     *
     * @param context 上下文
     * @return 相机预览控件
     */
    @NonNull
    private PreviewImpl createPreviewImpl(Context context) {
        PreviewImpl preview;
        if (Build.VERSION.SDK_INT < 14) {
            preview = new SurfaceViewPreview(context, this);
        } else {
            preview = new TextureViewPreview(context, this);
        }
        return preview;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            mDisplayOrientationDetector.enable(ViewCompat.getDisplay(this));
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (!isInEditMode()) {
            mDisplayOrientationDetector.disable();
        }
        super.onDetachedFromWindow();
    }

    public void setWidthHeightRatio(int widthRatio, int heightRatio) {
        this.widthRatio = widthRatio;
        this.heightRatio = heightRatio;
        if (adjustType != NONE) {
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (isInEditMode()) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        if (mClipScreen) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = MeasureSpec.getSize(heightMeasureSpec);
            switch (adjustType) {
                case FIXED_WIDTH:
                    height = width * heightRatio / widthRatio;
                    break;
                case FIXED_HEIGHT:
                    width = height * widthRatio / heightRatio;
                    break;
                case SCALE_SMALLER:
                    if (width * heightRatio < height * widthRatio) {
                        height = width * heightRatio / widthRatio;
                    } else {
                        width = height * widthRatio / heightRatio;
                    }
                    break;
                case SCALE_LARGER:
                    if (width * heightRatio < height * widthRatio) {
                        width = height * widthRatio / heightRatio;
                    } else {
                        height = width * heightRatio / widthRatio;
                    }
                    break;
            }
            super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            return;
        }
        // Handle android:adjustViewBounds
        if (mAdjustViewBounds) {
            if (!isCameraOpened()) {
                mCallbacks.reserveRequestLayoutOnOpen();
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
                super.onMeasure(widthMeasureSpec,
                        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            } else if (widthMode != MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
                final AspectRatio ratio = getAspectRatio();
                assert ratio != null;
                int width = (int) (MeasureSpec.getSize(heightMeasureSpec) * ratio.toFloat());
                if (widthMode == MeasureSpec.AT_MOST) {
                    width = Math.min(width, MeasureSpec.getSize(widthMeasureSpec));
                }
                super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                        heightMeasureSpec);
            } else {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
        // Measure the TextureView
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        AspectRatio ratio = getAspectRatio();
        if (mDisplayOrientationDetector.getLastKnownDisplayOrientation() % 180 == 0) {
            ratio = ratio.inverse();
        }
        assert ratio != null;
        if (height < width * ratio.getY() / ratio.getX()) {
            mImpl.getView().measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(width * ratio.getY() / ratio.getX(),
                            MeasureSpec.EXACTLY));
        } else {
            mImpl.getView().measure(
                    MeasureSpec.makeMeasureSpec(height * ratio.getX() / ratio.getY(),
                            MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState state = new SavedState(super.onSaveInstanceState());
        state.facing = getFacing();
        state.ratio = getAspectRatio();
        state.autoFocus = getAutoFocus();
        state.flash = getFlash();
        state.zoom = getZoom();
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setFacing(ss.facing);
        setAspectRatio(ss.ratio);
        setAutoFocus(ss.autoFocus);
        setFlash(ss.flash);
        setZoom(ss.zoom);
    }

    public void resumePreview() {
        mImpl.resumePreview();
    }

    /**
     * 打开相机并开启预览模式
     *
     * Open a camera device and start showing camera preview. This is typically called from
     * {@link Activity#onResume()}.
     */
    public void start() {
        if (!mImpl.start()) {
            // Store the state, and restore this state after fall back to Camera1
            // 保存状态
            Parcelable state = onSaveInstanceState();
            // Camera2 uses legacy hardware layer; fall back to Camera1
            // 恢复之后重新创建了相机，并且重新创建了预览控件！！
            mImpl = new Camera1(mCallbacks, createPreviewImpl(getContext()));
            // 恢复状态？？什么鬼？？干嘛这么调！！不就是恢复之前的状态嘛……
            onRestoreInstanceState(state);
            // 调用相机的启动方法
            mImpl.start();
        }
    }

    /**
     * 停止相机
     *
     * Stop camera preview and close the device. This is typically called from
     * {@link Activity#onPause()}.
     */
    public void stop() {
        mImpl.stop();
    }

    // region 一些对外提供的接口方法，实际直接调用了相机实例的方法
    /**
     * @return {@code true} if the camera is opened.
     */
    public boolean isCameraOpened() {
        return mImpl.isCameraOpened();
    }

    /**
     * Add a new callback.
     *
     * @param callback The {@link Callback} to add.
     * @see #removeCallback(Callback)
     */
    public void addCallback(@NonNull Callback callback) {
        mCallbacks.add(callback);
    }

    /**
     * Remove a callback.
     *
     * @param callback The {@link Callback} to remove.
     * @see #addCallback(Callback)
     */
    public void removeCallback(@NonNull Callback callback) {
        mCallbacks.remove(callback);
    }

    /**
     * @param adjustViewBounds {@code true} if you want the CameraView to adjust its bounds to
     *                         preserve the aspect ratio of camera.
     * @see #getAdjustViewBounds()
     */
    public void setAdjustViewBounds(boolean adjustViewBounds) {
        if (mAdjustViewBounds != adjustViewBounds) {
            mAdjustViewBounds = adjustViewBounds;
            requestLayout();
        }
    }

    /**
     * @return True when this CameraView is adjusting its bounds to preserve the aspect ratio of
     * camera.
     * @see #setAdjustViewBounds(boolean)
     */
    public boolean getAdjustViewBounds() {
        return mAdjustViewBounds;
    }

    /**
     * Chooses camera by the direction it faces.
     *
     * @param facing The camera facing. Must be either {@link #FACING_BACK} or {@link #FACING_FRONT}.
     */
    public void setFacing(@Facing int facing) {
        mImpl.setFacing(facing);
    }

    /**
     * Gets the direction that the current camera faces.
     *
     * @return The camera facing.
     */
    @Facing
    public int getFacing() {
        //noinspection WrongConstant
        return mImpl.getFacing();
    }

    /**
     * Gets all the aspect ratios supported by the current camera.
     */
    public Set<AspectRatio> getSupportedAspectRatios() {
        return mImpl.getSupportedAspectRatios();
    }

    /**
     * Sets the aspect ratio of camera.
     *
     * @param ratio The {@link AspectRatio} to be set.
     */
    public void setAspectRatio(@NonNull AspectRatio ratio) {
        if (mImpl.setAspectRatio(ratio)) {
            requestLayout();
        }
    }

    /**
     * Gets the current aspect ratio of camera.
     *
     * @return The current {@link AspectRatio}. Can be {@code null} if no camera is opened yet.
     */
    @Nullable
    public AspectRatio getAspectRatio() {
        return mImpl.getAspectRatio();
    }

    /**
     * Enables or disables the continuous auto-focus mode. When the current camera doesn't support
     * auto-focus, calling this method will be ignored.
     *
     * @param autoFocus {@code true} to enable continuous auto-focus mode. {@code false} to
     *                  disable it.
     */
    public void setAutoFocus(boolean autoFocus) {
        mImpl.setAutoFocus(autoFocus);
    }

    /**
     * Returns whether the continuous auto-focus mode is enabled.
     *
     * @return {@code true} if the continuous auto-focus mode is enabled. {@code false} if it is
     * disabled, or if it is not supported by the current camera.
     */
    public boolean getAutoFocus() {
        return mImpl.getAutoFocus();
    }

    /**
     * Sets the flash mode.
     *
     * @param flash The desired flash mode.
     */
    public void setFlash(@Flash int flash) {
        mImpl.setFlash(flash);
    }

    public void setPictureSize(int width, int height) {
        mImpl.setPictureSize(width, height);
    }

    public void setZoom(float zoom) {
        mImpl.setZoom(zoom);
    }

    public float getZoom() {
        return mImpl.getZoom();
    }

    public float getMaxZoom() {
        return mImpl.getMaxZoom();
    }

    /**
     * Gets the current flash mode.
     *
     * @return The current flash mode.
     */
    @Flash
    public int getFlash() {
        //noinspection WrongConstant
        return mImpl.getFlash();
    }

    /**
     * Take a picture. The result will be returned to
     * {@link Callback#onPictureTaken(CameraView, byte[])}.
     */
    public void takePicture() {
        mImpl.takePicture();
    }

    public void takePicture(boolean needFocus) {
        mImpl.takePicture(needFocus);
    }

    public void setOpenVoice(boolean open){
        mImpl.openVoice(open);
    }

    public void autoFocus() {
        mImpl.autoFocus();
    }
    // endregion

    // region 内部类和接口定义

    /**
     * 该类用来维护属于 CameraView 的回调的列表，两者所拥有的方法是一致的
     * 该类只在 CameraView 中声明和实例化了一个实例
     */
    private class CallbackBridge implements CameraViewImpl.Callback {

        private final List<Callback> mCallbacks = new LinkedList<>();

        private boolean mRequestLayoutOnOpen;

        public void add(Callback callback) {
            mCallbacks.add(callback);
        }

        void remove(Callback callback) {
            mCallbacks.remove(callback);
        }

        @Override
        public void onCameraOpened() {
            if (mRequestLayoutOnOpen) {
                mRequestLayoutOnOpen = false;
                requestLayout();
            }
            for (Callback callback : mCallbacks) {
                callback.onCameraOpened(CameraView.this);
            }
        }

        @Override
        public void onCameraClosed() {
            for (Callback callback : mCallbacks) {
                callback.onCameraClosed(CameraView.this);
            }
        }

        @Override
        public void onPictureTaken(byte[] data) {
            for (Callback callback : mCallbacks) {
                callback.onPictureTaken(CameraView.this, data);
            }
        }

        @Override
        public void onPreviewFrame(byte[] data, int width, int height, int format) {
            for (Callback callback : mCallbacks) {
                callback.onPreviewFrame(CameraView.this, data, width, height, format);
            }
        }

        @Override
        public void notPermission() {
            for (Callback callback : mCallbacks) {
                callback.notPermission();
            }
        }

        void reserveRequestLayoutOnOpen() {
            mRequestLayoutOnOpen = true;
        }
    }

    /**
     * 用于缓存状态信息的类
     */
    protected static class SavedState extends BaseSavedState {

        @Facing
        int facing;

        AspectRatio ratio;

        boolean autoFocus;

        @Flash
        int flash;

        float zoom;

        SavedState(Parcel source, ClassLoader loader) {
            super(source);
            facing = source.readInt();
            ratio = source.readParcelable(loader);
            autoFocus = source.readByte() != 0;
            flash = source.readInt();
            zoom = source.readFloat();
        }

        SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(facing);
            out.writeParcelable(ratio, 0);
            out.writeByte((byte) (autoFocus ? 1 : 0));
            out.writeInt(flash);
            out.writeFloat(zoom);
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = ParcelableCompat.newCreator(new ParcelableCompatCreatorCallbacks<SavedState>() {

            @Override
            public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new SavedState(in, loader);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        });

    }

    /**
     * CameraView 的回调类
     *
     * Callback for monitoring events about {@link CameraView}.
     */
    public abstract static class Callback {

        /**
         * Called when camera is opened.
         *
         * @param cameraView The associated {@link CameraView}.
         */
        public void onCameraOpened(CameraView cameraView) {
        }

        /**
         * Called when camera is closed.
         *
         * @param cameraView The associated {@link CameraView}.
         */
        public void onCameraClosed(CameraView cameraView) {
        }

        /**
         * Called when a picture is taken.
         *
         * @param cameraView The associated {@link CameraView}.
         * @param data       JPEG data.
         */
        public void onPictureTaken(CameraView cameraView, byte[] data) {
        }

        public void notPermission() {
        }

        public void onPreviewFrame(CameraView cameraView, byte[] data, int width, int height, int format) {
        }

        public void onTouchMove(boolean left) {
        }
    }
    // endregion
}
