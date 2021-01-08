//package me.shouheng.icamerasample.camera
//
//import android.content.Context
//import android.graphics.SurfaceTexture
//import android.opengl.GLES11Ext
//import android.opengl.GLES20
//import android.opengl.GLUtils
//import android.util.SparseArray
//import android.view.*
//import android.view.TextureView.SurfaceTextureListener
//import me.shouheng.icamera.enums.PreviewViewType
//import me.shouheng.icamera.preview.impl.BaseCameraPreview
//import me.shouheng.icamerasample.R
//import me.shouheng.icamerasample.camera.FilterTexturePreview
//import me.shouheng.icamerasample.filter.*
//import me.shouheng.icamerasample.render.MyGLUtils
//import me.shouheng.utils.stability.L
//import javax.microedition.khronos.egl.*
//
///**
// * Camera preview implementation based on [TextureView].
// *
// * @author WngShhng (shouheng2015@gmail.com)
// * @version 2019/4/13 22:54
// */
//class FilterTexturePreview(private val context: Context, parent: ViewGroup) :
//    BaseCameraPreview(), Runnable {
//    override var surfaceTexture: SurfaceTexture? = null
//    private val textureView: TextureView
//    private var renderThread: Thread? = null
//    private var eglDisplay: EGLDisplay? = null
//    private var eglSurface: EGLSurface? = null
//    private var eglContext: EGLContext? = null
//    private var egl10: EGL10? = null
//    private var gwidth = 0
//    private var gheight = 0
//    private var cameraTextureId = 0
//    private var selectedFilter: CameraFilter? = null
//    private var selectedFilterId = R.id.filter3
//    private var cameraSurfaceTexture: SurfaceTexture? = null
//    private val cameraFilterMap = SparseArray<CameraFilter>()
//    fun setSelectedFilter(id: Int) {
//        selectedFilterId = id
//        selectedFilter = cameraFilterMap[id]
//        if (selectedFilter != null) selectedFilter!!.onAttach()
//    }
//
//    override val surface: Surface
//        get() = Surface(surfaceTexture)
//
//    @get:PreviewViewType
//    override val previewType: Int
//        get() = PreviewViewType.TEXTURE_VIEW
//
//    override val surfaceHolder: SurfaceHolder?
//        get() = null
//
//    override fun getSurfaceTexture(): SurfaceTexture? {
//        return cameraSurfaceTexture
//    }
//
//    override val view: View
//        get() = textureView
//
//    /*-----------------------------------------inner methods---------------------------------------------*/
//    override fun run() {
//        L.d("run ..... ")
//        initGL(surfaceTexture)
//        cameraTextureId = MyGLUtils.genTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES)
//        cameraSurfaceTexture = SurfaceTexture(cameraTextureId)
//        notifyPreviewAvailable()
//
//        // Render loop
//        while (!Thread.currentThread().isInterrupted) {
//            try {
//                if (gwidth < 0 && gheight < 0) GLES20.glViewport(
//                    0,
//                    0,
//                    -gwidth.also { gwidth = it },
//                    -gheight.also { gheight = it }
//                )
//                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
//
//                // Update the camera preview texture
//                synchronized(this) { cameraSurfaceTexture!!.updateTexImage() }
//
//                // Draw camera preview
//                selectedFilter!!.draw(cameraTextureId, gwidth, gheight)
//
//                // Flush
//                GLES20.glFlush()
//                egl10!!.eglSwapBuffers(eglDisplay, eglSurface)
//                Thread.sleep(DRAW_INTERVAL.toLong())
//            } catch (e: InterruptedException) {
//                Thread.currentThread().interrupt()
//            }
//        }
//        cameraSurfaceTexture!!.release()
//        GLES20.glDeleteTextures(1, intArrayOf(cameraTextureId), 0)
//    }
//
//    private fun setupFilters() {
//        // Setup camera filters map
//        cameraFilterMap.append(R.id.filter0, OriginalFilter(context))
//        cameraFilterMap.append(R.id.filter1, EdgeDetectionFilter(context))
//        cameraFilterMap.append(R.id.filter2, PixelizeFilter(context))
//        cameraFilterMap.append(R.id.filter3, EMInterferenceFilter(context))
//        cameraFilterMap.append(R.id.filter4, TrianglesMosaicFilter(context))
//        cameraFilterMap.append(R.id.filter5, LegofiedFilter(context))
//        cameraFilterMap.append(R.id.filter6, TileMosaicFilter(context))
//        cameraFilterMap.append(R.id.filter7, BlueorangeFilter(context))
//        cameraFilterMap.append(R.id.filter8, ChromaticAberrationFilter(context))
//        cameraFilterMap.append(R.id.filter9, BasicDeformFilter(context))
//        cameraFilterMap.append(R.id.filter10, ContrastFilter(context))
//        cameraFilterMap.append(R.id.filter11, NoiseWarpFilter(context))
//        cameraFilterMap.append(R.id.filter12, RefractionFilter(context))
//        cameraFilterMap.append(R.id.filter13, MappingFilter(context))
//        cameraFilterMap.append(R.id.filter14, CrosshatchFilter(context))
//        cameraFilterMap.append(R.id.filter15, LichtensteinEsqueFilter(context))
//        cameraFilterMap.append(R.id.filter16, AsciiArtFilter(context))
//        cameraFilterMap.append(R.id.filter17, MoneyFilter(context))
//        cameraFilterMap.append(R.id.filter18, CrackedFilter(context))
//        cameraFilterMap.append(R.id.filter19, PolygonizationFilter(context))
//        cameraFilterMap.append(R.id.filter20, JFAVoronoiFilter(context))
//        cameraFilterMap.append(R.id.filter21, BlackAndWhiteFilter(context))
//        cameraFilterMap.append(R.id.filter22, GrayFilter(context))
//        cameraFilterMap.append(R.id.filter23, NegativeFilter(context))
//        cameraFilterMap.append(R.id.filter24, NostalgiaFilter(context))
//        cameraFilterMap.append(R.id.filter25, CastingFilter(context))
//        cameraFilterMap.append(R.id.filter26, ReliefFilter(context))
//        cameraFilterMap.append(R.id.filter27, SwirlFilter(context))
//        cameraFilterMap.append(R.id.filter28, HexagonMosaicFilter(context))
//        cameraFilterMap.append(R.id.filter29, MirrorFilter(context))
//        cameraFilterMap.append(R.id.filter30, TripleFilter(context))
//        cameraFilterMap.append(R.id.filter31, CartoonFilter(context))
//        cameraFilterMap.append(R.id.filter32, WaterReflectionFilter(context))
//    }
//
//    private fun updateSurfaceTexture(
//        surfaceTexture: SurfaceTexture,
//        width: Int,
//        height: Int
//    ) {
//        this.surfaceTexture = surfaceTexture
//        setSize(width, height)
//    }
//
//    private fun initGL(texture: SurfaceTexture?) {
//        egl10 = EGLContext.getEGL() as EGL10
//        eglDisplay = egl10!!.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
//        if (eglDisplay === EGL10.EGL_NO_DISPLAY) {
//            throw RuntimeException(
//                "eglGetDisplay failed " +
//                        GLUtils.getEGLErrorString(egl10!!.eglGetError())
//            )
//        }
//        val version = IntArray(2)
//        if (!egl10!!.eglInitialize(eglDisplay, version)) {
//            throw RuntimeException(
//                "eglInitialize failed " +
//                        GLUtils.getEGLErrorString(egl10!!.eglGetError())
//            )
//        }
//        val configsCount = IntArray(1)
//        val configs =
//            arrayOfNulls<EGLConfig>(1)
//        val configSpec = intArrayOf(
//            EGL10.EGL_RENDERABLE_TYPE,
//            EGL_OPENGL_ES2_BIT,
//            EGL10.EGL_RED_SIZE, 8,
//            EGL10.EGL_GREEN_SIZE, 8,
//            EGL10.EGL_BLUE_SIZE, 8,
//            EGL10.EGL_ALPHA_SIZE, 8,
//            EGL10.EGL_DEPTH_SIZE, 0,
//            EGL10.EGL_STENCIL_SIZE, 0,
//            EGL10.EGL_NONE
//        )
//        var eglConfig: EGLConfig? = null
//        require(egl10!!.eglChooseConfig(eglDisplay, configSpec, configs, 1, configsCount)) {
//            "eglChooseConfig failed " +
//                    GLUtils.getEGLErrorString(egl10!!.eglGetError())
//        }
//        if (configsCount[0] > 0) {
//            eglConfig = configs[0]
//        }
//        if (eglConfig == null) {
//            throw RuntimeException("eglConfig not initialized")
//        }
//        val attrib_list =
//            intArrayOf(EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE)
//        eglContext =
//            egl10!!.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list)
//        eglSurface = egl10!!.eglCreateWindowSurface(eglDisplay, eglConfig, texture, null)
//        if (eglSurface == null || eglSurface === EGL10.EGL_NO_SURFACE) {
//            val error = egl10!!.eglGetError()
//            if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
//                L.e("eglCreateWindowSurface returned EGL10.EGL_BAD_NATIVE_WINDOW")
//                return
//            }
//            throw RuntimeException(
//                "eglCreateWindowSurface failed " +
//                        GLUtils.getEGLErrorString(error)
//            )
//        }
//        if (!egl10!!.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
//            throw RuntimeException(
//                "eglMakeCurrent failed " +
//                        GLUtils.getEGLErrorString(egl10!!.eglGetError())
//            )
//        }
//    }
//
//    companion object {
//        private const val EGL_OPENGL_ES2_BIT = 4
//        private const val EGL_CONTEXT_CLIENT_VERSION = 0x3098
//        private const val DRAW_INTERVAL = 1000 / 30
//    }
//
//    init {
//        textureView = TextureView(context)
//        textureView.layoutParams = ViewGroup.LayoutParams(
//            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
//        )
//        parent.addView(textureView)
//        setupFilters()
//        setSelectedFilter(selectedFilterId)
//        textureView.surfaceTextureListener = object : SurfaceTextureListener {
//            override fun onSurfaceTextureAvailable(
//                surface: SurfaceTexture,
//                width: Int,
//                height: Int
//            ) {
//                L.i("onSurfaceTextureAvailable $surface")
//                updateSurfaceTexture(surface, width, height)
//                notifyPreviewAvailable()
//            }
//
//            override fun onSurfaceTextureSizeChanged(
//                surface: SurfaceTexture,
//                width: Int,
//                height: Int
//            ) {
//                L.i("onSurfaceTextureSizeChanged $surface")
//                updateSurfaceTexture(surface, width, height)
//                if (renderThread != null && renderThread.isAlive()) {
//                    renderThread.interrupt()
//                }
//                renderThread = Thread(this@FilterTexturePreview)
//                gwidth = -width
//                gheight = -height
//                renderThread.start()
//            }
//
//            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
//                setSize(0, 0)
//                if (renderThread != null && renderThread.isAlive()) {
//                    renderThread.interrupt()
//                }
//                CameraFilter.release()
//                return true
//            }
//
//            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) { /*noop*/
//            }
//        }
//    }
//}