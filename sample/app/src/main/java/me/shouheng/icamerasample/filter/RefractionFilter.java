package me.shouheng.icamerasample.filter;

import android.content.Context;
import android.opengl.GLES20;

import me.shouheng.icamerasample.R;
import me.shouheng.icamerasample.render.MyGLUtils;


public class RefractionFilter extends CameraFilter {
    private int program;
    private int texture2Id;

    public RefractionFilter(Context context) {
        super(context);

        // Build shaders
        program = MyGLUtils.buildProgram(context, R.raw.vertext, R.raw.refraction);

        // Load the texture will need for the shader
        texture2Id = MyGLUtils.loadTexture(context, R.raw.tex11, new int[2]);
    }

    @Override
    public void onDraw(int cameraTexId, int canvasWidth, int canvasHeight) {
        setupShaderInputs(program,
                new int[]{canvasWidth, canvasHeight},
                new int[]{cameraTexId, texture2Id},
                new int[][]{});
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }
}
