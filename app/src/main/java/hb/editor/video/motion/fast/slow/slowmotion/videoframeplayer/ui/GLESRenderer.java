package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.ui;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.GLText.BLUR;
import static hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.GLText.EDGE;
import static hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.GLText.EMBOSS;
import static hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.GLText.CONTRAST;
import static hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.GLText.FLIP_HZ;
import static hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.GLText.FLIP_VT;
import static hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.GLText.FRAGMENT_SHADER_BW;
import static hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.GLText.HUESHIFT;
import static hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.GLText.LUMINANCE;
import static hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.GLText.NEGATIVE;
import static hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.GLText.TOON;
import static hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.GLText.TWIRL;
import static hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.GLText.WARP;

public class GLESRenderer {
    private String GLESrendercode = "https://github.com/yulu/GLtext";
    private static final int FLOAT_SIZE_BYTES = 4;

    private static final String FRAGMENT_SHADER_TEXURE = "#extension GL_OES_EGL_image_external : require\nprecision mediump float;\nvarying vec2 vTextureCoord;\nuniform samplerExternalOES sTexture;\nvoid main() {\n  gl_FragColor = texture2D(sTexture, vTextureCoord);\n}\n";
    private static final String LOG_TAG = "GLESRenderer";
    public static final int SHADER_TYPE_BW = 1;
    public static final int SHADER_TYPE_NORMAL = 0;
    private static final String VERTEX_SHADER = "uniform mat4 uMVPMatrix;\nuniform mat4 uSTMatrix;\nattribute vec4 aPosition;\nattribute vec4 aTextureCoord;\nvarying vec2 vTextureCoord;\nvoid main() {\n  gl_Position = uMVPMatrix * aPosition;\n  vTextureCoord = (uSTMatrix*aTextureCoord).xy;\n}\n";
    private int mBWProgram,mBWProgram1,mBWProgram2,mBWProgram3,mBWProgram4,mBWProgram5,mBWProgram6,mBWProgram7,mBWProgram8,mBWProgram9,mBWProgram10,mBWProgram11,mBWProgram12;
    private int mCropBottom;
    private int mCropLeft;
    private int mCropRight;
    private int mCropTop;
    private int mCropingHeight;
    private int mCropingWidth;
    private int mHeight;
    private float[] mMVPMatrix = new float[16];
    private boolean mNeedCroping = false;
    private int mNormalProgram;
    private int mProgram;
    private float[] mSTMatrix = new float[16];
    private int mShaderType = 0;
    private FloatBuffer mTexCords;
    private final float[] mTexCordsData = new float[]{0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f};
    private int mTextureID = -1;
    private FloatBuffer mVertices = null;
    private final float[] mVerticesData = new float[]{-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f};
    private int mVideoHeight;
    private int mVideoOrientation;
    private int mVideoWidth;
    private int mWidth;
    private int maPositionHandle;
    private int maTextureHandle;
    private int muMVPMatrixHandle;
    private int muSTMatrixHandle;

    public GLESRenderer(int width, int height) {
        this.mVertices = ByteBuffer.allocateDirect(this.mVerticesData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();;
        this.mTexCords = ByteBuffer.allocateDirect(this.mTexCordsData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.mVertices.put(this.mVerticesData).position(0);
        this.mTexCords.put(this.mTexCordsData).position(0);
        Matrix.setIdentityM(this.mSTMatrix, 0);
        Matrix.setIdentityM(this.mMVPMatrix, 0);
        this.mWidth = width;
        this.mHeight = height;
        this.mVideoHeight = 1;
        this.mVideoWidth = 1;
        this.mVideoOrientation = 0;
    }

    public int getTextureId() {
        return this.mTextureID;
    }

    public void drawFrame(SurfaceTexture st) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(16640);
        GLES20.glUseProgram(this.mProgram);
        GLES20.glActiveTexture(33984);
        GLES20.glBindTexture(36197, this.mTextureID);
        GLES20.glVertexAttribPointer(this.maPositionHandle, 2, 5126, false, 0, this.mVertices);
        GLES20.glEnableVertexAttribArray(this.maPositionHandle);
        GLES20.glVertexAttribPointer(this.maTextureHandle, 2, 5126, false, 0, this.mTexCords);
        GLES20.glEnableVertexAttribArray(this.maTextureHandle);
        GLES20.glUniformMatrix4fv(this.muMVPMatrixHandle, 1, false, this.mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(this.muSTMatrixHandle, 1, false, this.mSTMatrix, 0);
        GLES20.glDrawArrays(5, 0, 4);
        checkGlError("glDrawArrays");
        GLES20.glFinish();
    }

    public void surfaceCreated() {
        setViewport(0, 0, this.mWidth, this.mHeight);
        this.mNormalProgram = createProgram(VERTEX_SHADER, FRAGMENT_SHADER_TEXURE);
        if (this.mNormalProgram == 0) {
            throw new RuntimeException("failed creating program");
        }
        this.mBWProgram = createProgram(VERTEX_SHADER, FRAGMENT_SHADER_BW);//FRAGMENT_SHADER_BW
        if (this.mBWProgram == 0) {
            throw new RuntimeException("failed creating program");
        }
        this.mBWProgram1 = createProgram(VERTEX_SHADER, BLUR);//FRAGMENT_SHADER_BW
        if (this.mBWProgram1 == 0) {
            throw new RuntimeException("failed creating program");
        }
        this.mBWProgram2 = createProgram(VERTEX_SHADER, EDGE);//FRAGMENT_SHADER_BW
        if (this.mBWProgram2 == 0) {
            throw new RuntimeException("failed creating program");
        }
        this.mBWProgram3 = createProgram(VERTEX_SHADER, EMBOSS);//FRAGMENT_SHADER_BW
        if (this.mBWProgram3 == 0) {
            throw new RuntimeException("failed creating program");
        }
        this.mBWProgram4 = createProgram(VERTEX_SHADER, CONTRAST);//FRAGMENT_SHADER_BW
        if (this.mBWProgram4 == 0) {
            throw new RuntimeException("failed creating program");
        }
        this.mBWProgram5 = createProgram(VERTEX_SHADER, FLIP_VT);//FRAGMENT_SHADER_BW
        if (this.mBWProgram5 == 0) {
            throw new RuntimeException("failed creating program");
        }
        this.mBWProgram6 = createProgram(VERTEX_SHADER, HUESHIFT);//FRAGMENT_SHADER_BW
        if (this.mBWProgram6 == 0) {
            throw new RuntimeException("failed creating program");
        }
        this.mBWProgram7 = createProgram(VERTEX_SHADER, LUMINANCE);//FRAGMENT_SHADER_BW
        if (this.mBWProgram7 == 0) {
            throw new RuntimeException("failed creating program");
        }
        this.mBWProgram8 = createProgram(VERTEX_SHADER, NEGATIVE);//FRAGMENT_SHADER_BW
        if (this.mBWProgram8 == 0) {
            throw new RuntimeException("failed creating program");
        }
        this.mBWProgram9 = createProgram(VERTEX_SHADER, TOON);//FRAGMENT_SHADER_BW
        if (this.mBWProgram9 == 0) {
            throw new RuntimeException("failed creating program");
        }
        this.mBWProgram10 = createProgram(VERTEX_SHADER, TWIRL);//FRAGMENT_SHADER_BW
        if (this.mBWProgram10 == 0) {
            throw new RuntimeException("failed creating program");
        }
        this.mBWProgram11 = createProgram(VERTEX_SHADER, WARP);//FRAGMENT_SHADER_BW
        if (this.mBWProgram11 == 0) {
            throw new RuntimeException("failed creating program");
        }
        this.mBWProgram12 = createProgram(VERTEX_SHADER, FLIP_HZ);//FRAGMENT_SHADER_BW
        if (this.mBWProgram12 == 0) {
            throw new RuntimeException("failed creating program");
        }

        this.mProgram = this.mNormalProgram;
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        this.mTextureID = textures[0];
        GLES20.glBindTexture(36197, this.mTextureID);
        checkGlError("glBindTexture mTextureID");
        GLES20.glTexParameterf(36197, 10241, 9728.0f);
        GLES20.glTexParameterf(36197, 10240, 9729.0f);
        GLES20.glTexParameteri(36197, 10242, 33071);
        GLES20.glTexParameteri(36197, 10243, 33071);
        checkGlError("glTexParameter");
    }

    public void changeFragmentShader(int type) {
//        int shaderType = 0;
//        if (this.mShaderType == 0) {
//            shaderType = 1;
//        } else if (this.mShaderType == 1) {
//            shaderType = 0;
//        }
//        if (this.mShaderType != shaderType) {
//            this.mShaderType = shaderType;
            switch (type) {
                case 0:
                    this.mProgram = this.mNormalProgram;
                    break;
                case 1:
                    this.mProgram = this.mNormalProgram;
                    this.mProgram = this.mBWProgram;
                    break;
                case 2:
                    this.mProgram = this.mNormalProgram;
                    this.mProgram = this.mBWProgram1;
                    break;
                case 3:
                    this.mProgram = this.mNormalProgram;
                    this.mProgram = this.mBWProgram2;
                    break;
                case 4:
                    this.mProgram = this.mNormalProgram;
                    this.mProgram = this.mBWProgram3;
                    break;
                case 5:
                    this.mProgram = this.mNormalProgram;
                    this.mProgram = this.mBWProgram4;
                    break;
                case 6:
                    this.mProgram = this.mNormalProgram;
                    this.mProgram = this.mBWProgram5;
                    break;
                case 7:
                    this.mProgram = this.mNormalProgram;
                    this.mProgram = this.mBWProgram6;
                    break;
                case 8:
                    this.mProgram = this.mNormalProgram;
                    this.mProgram = this.mBWProgram7;
                    break;
                case 9:
                    this.mProgram = this.mNormalProgram;
                    this.mProgram = this.mBWProgram8;
                    break;
                case 10:
                    this.mProgram = this.mNormalProgram;
                    this.mProgram = this.mBWProgram9;
                    break;
                case 11:
                    this.mProgram = this.mNormalProgram;
                    this.mProgram = this.mBWProgram10;
                    break;
                case 12:
                    this.mProgram = this.mNormalProgram;
                    this.mProgram = this.mBWProgram11;
                    break;
                case 13:
                    this.mProgram = this.mNormalProgram;
                    this.mProgram = this.mBWProgram12;
                    break;


                default:
                    Log.d(LOG_TAG, "Wrong program.");
                    break;
            }
            GLES20.glUseProgram(this.mProgram);

    }

    private int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        checkGlError("glCreateShader type=" + shaderType);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, 35713, compiled, 0);
        if (compiled[0] != 0) {
            return shader;
        }
        Log.e(LOG_TAG, "Could not compile shader " + shaderType + ":");
        Log.e(LOG_TAG, " " + GLES20.glGetShaderInfoLog(shader));
        GLES20.glDeleteShader(shader);
        return 0;
    }

    private int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(35633, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = loadShader(35632, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }
        int program = GLES20.glCreateProgram();
        checkGlError("glCreateProgram");
        if (program == 0) {
            Log.e(LOG_TAG, "Could not create program");
        }
        GLES20.glAttachShader(program, vertexShader);
        checkGlError("glAttachShader");
        GLES20.glAttachShader(program, pixelShader);
        checkGlError("glAttachShader");
        GLES20.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, 35714, linkStatus, 0);
        if (linkStatus[0] != 1) {
            Log.e(LOG_TAG, "Could not link program: ");
            Log.e(LOG_TAG, GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            program = 0;
        }
        this.maPositionHandle = GLES20.glGetAttribLocation(program, "aPosition");
        checkGlError("glGetAttribLocation aPosition");
        if (this.maPositionHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aPosition");
        }
        this.maTextureHandle = GLES20.glGetAttribLocation(program, "aTextureCoord");
        checkGlError("glGetAttribLocation aTextureCoord");
        if (this.maTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aTextureCoord");
        }
        this.muMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        checkGlError("glGetUniformLocation uMVPMatrix");
        if (this.muMVPMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uMVPMatrix");
        }
        this.muSTMatrixHandle = GLES20.glGetUniformLocation(program, "uSTMatrix");
        checkGlError("glGetUniformLocation uSTMatrix");
        if (this.muSTMatrixHandle != -1) {
            return program;
        }
        throw new RuntimeException("Could not get attrib location for uSTMatrix");
    }

    public void checkGlError(String op) {
        int error = GLES20.glGetError();
        if (error != 0) {
            Log.e(LOG_TAG, op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }

    public void setViewport(int top, int left, int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
        GLES20.glViewport(top, left, this.mWidth, this.mHeight);
        evaluateVertices();
    }

    public void setVideoFrameSize(int width, int height, int orientation) {
        this.mVideoWidth = width;
        this.mVideoHeight = height;
        this.mVideoOrientation = orientation;
        evaluateVertices();
    }

    private boolean isScreenPortrait() {
        return this.mWidth < this.mHeight;
    }

    private boolean isVideoPortrait() {
        int width = this.mVideoWidth;
        int height = this.mVideoHeight;
        if (this.mVideoOrientation == 90 || this.mVideoOrientation == 270) {
            width = this.mVideoHeight;
            height = this.mVideoWidth;
        }
        return height > width;
    }

    private void evaluateVertices() {
        if (this.mWidth != 0 && this.mHeight != 0 && this.mVideoWidth != 0 && this.mVideoHeight != 0) {
            float sw;
            float sh;
            float vw;
            float vh;
            float x0;
            float y0;
            float x1;
            float y1;
            if (this.mVideoOrientation == 90 || this.mVideoOrientation == 270) {
                Log.d(LOG_TAG, "Video orientation=" + this.mVideoOrientation);
                sw = (float) this.mWidth;
                sh = (float) this.mHeight;
                vw = (float) this.mVideoWidth;
                vh = (float) this.mVideoHeight;
            } else {
                Log.d(LOG_TAG, "Video orientation=" + this.mVideoOrientation);
                sw = (float) this.mWidth;
                sh = (float) this.mHeight;
                vw = (float) this.mVideoWidth;
                vh = (float) this.mVideoHeight;
            }
            if (this.mVideoOrientation == 90 || this.mVideoOrientation == 270) {
                float w = vw;
                vw = vh;
                vh = w;
            }
            float rar = (sw / sh) / (vw / vh);
            if (rar > 1.0f) {
                x0 = -1.0f / rar;
                y0 = -1.0f;
                x1 = 1.0f / rar;
                y1 = 1.0f;
            } else {
                x0 = -1.0f;
                y0 = -rar;
                x1 = 1.0f;
                y1 = rar;
            }
            this.mVertices.put(new float[]{x0, y0, x1, y0, x0, y1, x1, y1}).position(0);
            Matrix.setIdentityM(this.mSTMatrix, 0);
            float u0 = 0.0f;
            float v1 = 1.0f;
            float u1 = 1.0f;
            if (this.mNeedCroping) {
                int width = (this.mCropRight - this.mCropLeft) + 1;
                int height = (this.mCropBottom - this.mCropTop) + 1;
                if (this.mCropingWidth > width) {
                    u1 = ((float) (width - 1)) / ((float) this.mCropingWidth);
                }
                if (this.mCropingHeight > height) {
                    v1 = ((float) (height - 1)) / ((float) this.mCropingHeight);
                }
            }
            float[] tex_cords = this.mVideoOrientation == 90 ? new float[]{u1, v1, u1, 0.0f, u0, v1, u0, 0.0f} : this.mVideoOrientation == 180 ? new float[]{u1, 0.0f, u0, 0.0f, u1, v1, u0, v1} : this.mVideoOrientation == 270 ? new float[]{u0, 0.0f, u0, v1, u1, 0.0f, u1, v1} : new float[]{u0, v1, u1, v1, u0, 0.0f, u1, 0.0f};
            this.mTexCords.put(tex_cords).position(0);
        }
    }

    public void setCropRectangle(int cropTop, int cropLeft, int cropBottom, int cropRight, int width, int height) {
        this.mCropLeft = cropLeft;
        this.mCropRight = cropRight;
        this.mCropBottom = cropBottom;
        this.mCropTop = cropTop;
        this.mCropingWidth = width;
        this.mCropingHeight = height;
        this.mNeedCroping = true;
    }
}
