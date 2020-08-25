package me.shouheng.icamera.listener;

import android.content.Context;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/17 22:26
 */
public abstract class DisplayOrientationDetector {

    private final OrientationEventListener orientationEventListener;

    private static final SparseIntArray DISPLAY_ORIENTATIONS = new SparseIntArray();

    static {
        DISPLAY_ORIENTATIONS.put(Surface.ROTATION_0,     0);
        DISPLAY_ORIENTATIONS.put(Surface.ROTATION_90,   90);
        DISPLAY_ORIENTATIONS.put(Surface.ROTATION_180, 180);
        DISPLAY_ORIENTATIONS.put(Surface.ROTATION_270, 270);
    }

    private Display display;

    private int lastKnownDisplayOrientation = 0;

    protected DisplayOrientationDetector(Context context) {
        orientationEventListener = new OrientationEventListener(context) {

            private int mLastKnownRotation = -1;

            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN || display == null) {
                    return;
                }
                final int rotation = display.getRotation();
                if (mLastKnownRotation != rotation) {
                    mLastKnownRotation = rotation;
                    dispatchOnDisplayOrientationChanged(DISPLAY_ORIENTATIONS.get(rotation));
                }
            }
        };
    }

    public void enable(Display display) {
        this.display = display;
        orientationEventListener.enable();
        // Immediately dispatch the first callback
        dispatchOnDisplayOrientationChanged(DISPLAY_ORIENTATIONS.get(display.getRotation()));
    }

    public void disable() {
        orientationEventListener.disable();
        display = null;
    }

    public int getLastKnownDisplayOrientation() {
        return lastKnownDisplayOrientation;
    }

    private void dispatchOnDisplayOrientationChanged(int displayOrientation) {
        lastKnownDisplayOrientation = displayOrientation;
        onDisplayOrientationChanged(displayOrientation);
    }

    /**
     * Called when display orientation is changed.
     *
     * @param displayOrientation One of 0, 90, 180, and 270.
     */
    public abstract void onDisplayOrientationChanged(int displayOrientation);

}
