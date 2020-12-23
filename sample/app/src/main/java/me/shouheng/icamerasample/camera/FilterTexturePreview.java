package me.shouheng.icamerasample.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import me.shouheng.icamera.enums.PreviewViewType;
import me.shouheng.icamera.preview.impl.BaseCameraPreview;
import me.shouheng.icamerasample.R;
import me.shouheng.icamerasample.filter.AsciiArtFilter;
import me.shouheng.icamerasample.filter.BasicDeformFilter;
import me.shouheng.icamerasample.filter.BlackAndWhiteFilter;
import me.shouheng.icamerasample.filter.BlueorangeFilter;
import me.shouheng.icamerasample.filter.CameraFilter;
import me.shouheng.icamerasample.filter.CartoonFilter;
import me.shouheng.icamerasample.filter.CastingFilter;
import me.shouheng.icamerasample.filter.ChromaticAberrationFilter;
import me.shouheng.icamerasample.filter.ContrastFilter;
import me.shouheng.icamerasample.filter.CrackedFilter;
import me.shouheng.icamerasample.filter.CrosshatchFilter;
import me.shouheng.icamerasample.filter.EMInterferenceFilter;
import me.shouheng.icamerasample.filter.EdgeDetectionFilter;
import me.shouheng.icamerasample.filter.GrayFilter;
import me.shouheng.icamerasample.filter.HexagonMosaicFilter;
import me.shouheng.icamerasample.filter.JFAVoronoiFilter;
import me.shouheng.icamerasample.filter.LegofiedFilter;
import me.shouheng.icamerasample.filter.LichtensteinEsqueFilter;
import me.shouheng.icamerasample.filter.MappingFilter;
import me.shouheng.icamerasample.filter.MirrorFilter;
import me.shouheng.icamerasample.filter.MoneyFilter;
import me.shouheng.icamerasample.filter.NegativeFilter;
import me.shouheng.icamerasample.filter.NoiseWarpFilter;
import me.shouheng.icamerasample.filter.NostalgiaFilter;
import me.shouheng.icamerasample.filter.OriginalFilter;
import me.shouheng.icamerasample.filter.PixelizeFilter;
import me.shouheng.icamerasample.filter.PolygonizationFilter;
import me.shouheng.icamerasample.filter.RefractionFilter;
import me.shouheng.icamerasample.filter.ReliefFilter;
import me.shouheng.icamerasample.filter.SwirlFilter;
import me.shouheng.icamerasample.filter.TileMosaicFilter;
import me.shouheng.icamerasample.filter.TrianglesMosaicFilter;
import me.shouheng.icamerasample.filter.TripleFilter;
import me.shouheng.icamerasample.filter.WaterReflectionFilter;
import me.shouheng.icamerasample.render.MyGLUtils;
import me.shouheng.utils.stability.L;

/**
 * Camera preview implementation based on {@link TextureView}.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:54
 */
public class FilterTexturePreview extends BaseCameraPreview implements Runnable {

    private static final int EGL_OPENGL_ES2_BIT = 4;
    private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
    private static final int DRAW_INTERVAL = 1000 / 30;

    private Context context;
    private SurfaceTexture surfaceTexture;
    private TextureView textureView;

    private Thread renderThread;
    private EGLDisplay eglDisplay;
    private EGLSurface eglSurface;
    private EGLContext eglContext;
    private EGL10 egl10;
    private int gwidth, gheight;
    private int cameraTextureId;
    private CameraFilter selectedFilter;
    private int selectedFilterId = R.id.filter3;
    private SurfaceTexture cameraSurfaceTexture;
    private SparseArray<CameraFilter> cameraFilterMap = new SparseArray<>();

    public FilterTexturePreview(Context context, ViewGroup parent) {
        super(context, parent);
        this.context = context;
        textureView = new TextureView(context);
        textureView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        parent.addView(textureView);

        setupFilters();

        setSelectedFilter(selectedFilterId);

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                L.i("onSurfaceTextureAvailable " + surface);
                updateSurfaceTexture(surface, width, height);
                notifyPreviewAvailable();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                L.i("onSurfaceTextureSizeChanged " + surface);
                updateSurfaceTexture(surface, width, height);
                if (renderThread != null && renderThread.isAlive()) {
                    renderThread.interrupt();
                }
                renderThread = new Thread(FilterTexturePreview.this);
                gwidth = -width;
                gheight = -height;
                renderThread.start();
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                setSize(0, 0);
                if (renderThread != null && renderThread.isAlive()) {
                    renderThread.interrupt();
                }
                CameraFilter.release();
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {/*noop*/}
        });
    }

    public void setSelectedFilter(int id) {
        selectedFilterId = id;
        selectedFilter = cameraFilterMap.get(id);
        if (selectedFilter != null) selectedFilter.onAttach();
    }

    @Override
    public Surface getSurface() {
        return new Surface(surfaceTexture);
    }

    @Override
    @PreviewViewType
    public int getPreviewType() {
        return PreviewViewType.TEXTURE_VIEW;
    }

    @Nullable
    @Override
    public SurfaceHolder getSurfaceHolder() {
        return null;
    }

    @Nullable
    @Override
    public SurfaceTexture getSurfaceTexture() {
        return cameraSurfaceTexture;
    }

    @Override
    public View getView() {
        return textureView;
    }

    /*-----------------------------------------inner methods---------------------------------------------*/

    @Override
    public void run() {
        L.d("run ..... ");

        initGL(surfaceTexture);
        cameraTextureId = MyGLUtils.genTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        cameraSurfaceTexture = new SurfaceTexture(cameraTextureId);

        notifyPreviewAvailable();

        // Render loop
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (gwidth < 0 && gheight < 0)
                    GLES20.glViewport(0, 0, gwidth = -gwidth, gheight = -gheight);

                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

                // Update the camera preview texture
                synchronized (this) {
                    cameraSurfaceTexture.updateTexImage();
                }

                // Draw camera preview
                selectedFilter.draw(cameraTextureId, gwidth, gheight);

                // Flush
                GLES20.glFlush();
                egl10.eglSwapBuffers(eglDisplay, eglSurface);

                Thread.sleep(DRAW_INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        cameraSurfaceTexture.release();
        GLES20.glDeleteTextures(1, new int[]{cameraTextureId}, 0);
    }

    private void setupFilters() {
        // Setup camera filters map
        cameraFilterMap.append(R.id.filter0, new OriginalFilter(context));
        cameraFilterMap.append(R.id.filter1, new EdgeDetectionFilter(context));
        cameraFilterMap.append(R.id.filter2, new PixelizeFilter(context));
        cameraFilterMap.append(R.id.filter3, new EMInterferenceFilter(context));
        cameraFilterMap.append(R.id.filter4, new TrianglesMosaicFilter(context));
        cameraFilterMap.append(R.id.filter5, new LegofiedFilter(context));
        cameraFilterMap.append(R.id.filter6, new TileMosaicFilter(context));
        cameraFilterMap.append(R.id.filter7, new BlueorangeFilter(context));
        cameraFilterMap.append(R.id.filter8, new ChromaticAberrationFilter(context));
        cameraFilterMap.append(R.id.filter9, new BasicDeformFilter(context));
        cameraFilterMap.append(R.id.filter10, new ContrastFilter(context));
        cameraFilterMap.append(R.id.filter11, new NoiseWarpFilter(context));
        cameraFilterMap.append(R.id.filter12, new RefractionFilter(context));
        cameraFilterMap.append(R.id.filter13, new MappingFilter(context));
        cameraFilterMap.append(R.id.filter14, new CrosshatchFilter(context));
        cameraFilterMap.append(R.id.filter15, new LichtensteinEsqueFilter(context));
        cameraFilterMap.append(R.id.filter16, new AsciiArtFilter(context));
        cameraFilterMap.append(R.id.filter17, new MoneyFilter(context));
        cameraFilterMap.append(R.id.filter18, new CrackedFilter(context));
        cameraFilterMap.append(R.id.filter19, new PolygonizationFilter(context));
        cameraFilterMap.append(R.id.filter20, new JFAVoronoiFilter(context));
        cameraFilterMap.append(R.id.filter21, new BlackAndWhiteFilter(context));
        cameraFilterMap.append(R.id.filter22, new GrayFilter(context));
        cameraFilterMap.append(R.id.filter23, new NegativeFilter(context));
        cameraFilterMap.append(R.id.filter24, new NostalgiaFilter(context));
        cameraFilterMap.append(R.id.filter25, new CastingFilter(context));
        cameraFilterMap.append(R.id.filter26, new ReliefFilter(context));
        cameraFilterMap.append(R.id.filter27, new SwirlFilter(context));
        cameraFilterMap.append(R.id.filter28, new HexagonMosaicFilter(context));
        cameraFilterMap.append(R.id.filter29, new MirrorFilter(context));
        cameraFilterMap.append(R.id.filter30, new TripleFilter(context));
        cameraFilterMap.append(R.id.filter31, new CartoonFilter(context));
        cameraFilterMap.append(R.id.filter32, new WaterReflectionFilter(context));
    }

    private void updateSurfaceTexture(SurfaceTexture surfaceTexture, int width, int height) {
        this.surfaceTexture = surfaceTexture;
        setSize(width, height);
    }

    private void initGL(SurfaceTexture texture) {
        egl10 = (EGL10) EGLContext.getEGL();

        eglDisplay = egl10.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        if (eglDisplay == EGL10.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglGetDisplay failed " +
                    android.opengl.GLUtils.getEGLErrorString(egl10.eglGetError()));
        }

        int[] version = new int[2];
        if (!egl10.eglInitialize(eglDisplay, version)) {
            throw new RuntimeException("eglInitialize failed " +
                    android.opengl.GLUtils.getEGLErrorString(egl10.eglGetError()));
        }

        int[] configsCount = new int[1];
        EGLConfig[] configs = new EGLConfig[1];
        int[] configSpec = {
                EGL10.EGL_RENDERABLE_TYPE,
                EGL_OPENGL_ES2_BIT,
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 0,
                EGL10.EGL_STENCIL_SIZE, 0,
                EGL10.EGL_NONE
        };

        EGLConfig eglConfig = null;
        if (!egl10.eglChooseConfig(eglDisplay, configSpec, configs, 1, configsCount)) {
            throw new IllegalArgumentException("eglChooseConfig failed " +
                    android.opengl.GLUtils.getEGLErrorString(egl10.eglGetError()));
        } else if (configsCount[0] > 0) {
            eglConfig = configs[0];
        }
        if (eglConfig == null) {
            throw new RuntimeException("eglConfig not initialized");
        }

        int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE};
        eglContext = egl10.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list);
        eglSurface = egl10.eglCreateWindowSurface(eglDisplay, eglConfig, texture, null);

        if (eglSurface == null || eglSurface == EGL10.EGL_NO_SURFACE) {
            int error = egl10.eglGetError();
            if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
                L.e("eglCreateWindowSurface returned EGL10.EGL_BAD_NATIVE_WINDOW");
                return;
            }
            throw new RuntimeException("eglCreateWindowSurface failed " +
                    android.opengl.GLUtils.getEGLErrorString(error));
        }

        if (!egl10.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            throw new RuntimeException("eglMakeCurrent failed " +
                    android.opengl.GLUtils.getEGLErrorString(egl10.eglGetError()));
        }
    }
}
