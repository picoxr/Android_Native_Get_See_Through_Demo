package com.picovr.picovr_GetSeeThrough;

import android.graphics.Bitmap;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;

import com.picovr.vractivity.Eye;
import com.picovr.vractivity.HmdState;
import com.picovr.vractivity.RenderInterface;
import com.picovr.vractivity.VRActivity;
import com.psmart.vrlib.PicovrSDK;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class GetSeeThrough extends VRActivity implements RenderInterface {

    private static final String TAG_TH = "PVRGetSeeThrough";
    private static final String DBG_LC = "LifeCycle :";
    private static final int UER_EVENT = 100;
    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 1000.0f;
    private static final float CAMERA_Z = 0.01f;
    // We keep the light always position just above the user.
    private static final float[] LIGHT_POS_IN_WORLD_SPACE = new float[]{0.0f, 2.0f, 0.0f, 1.0f};
    private static final float MAX_MODEL_DISTANCE = 17.0f;
    private static int textureIdForCamera = 0;
    private static int textureIdForCameraRight = 0;
    final int FLOOR_VERTEX_POS_SIZE = 3;    // x, y, z
    final int FLOOR_VERTEX_COLOR_SIZE = 4;  // r, g, b, a
    final int FLOOR_VERTEX_NORMAL_SIZE = 3; // nx, ny, nz
    final int FLOOR_VERTEX_POS_INDEX = 0;
    final int FLOOR_VERTEX_COLOR_INDEX = 1;
    final int FLOOR_VERTEX_NORMAL_INDEX = 2;
    final int FLOOR_VERTEX_STRIDE = (4 * (FLOOR_VERTEX_POS_SIZE + FLOOR_VERTEX_COLOR_SIZE + FLOOR_VERTEX_NORMAL_SIZE));
    //zgt add for camera preview{
    final int CAM_WIDTH = 640;
    final int CAM_HEIGHT = 480;
    final Float CAM_DEPTH = -0.8f;
    final int CAM_VERTEX_POS_SIZE = 3;    // x, y, z
    final int CAM_VERTEX_COLOR_SIZE = 3;  // r, g, b
    final int CAM_VERTEX_TEXTURE_SIZE = 2; // u, v
    final int CAM_VERTEX_POS_INDEX = 0;
    final int CAM_VERTEX_COLOR_INDEX = 1;
    final int CAM_VERTEX_TEXTURE_INDEX = 2;
    final int CAM_VERTEX_STRIDE = (4 * (CAM_VERTEX_POS_SIZE + CAM_VERTEX_COLOR_SIZE + CAM_VERTEX_TEXTURE_SIZE));
    private final float[] lightPosInEyeSpace = new float[4];
    protected float[] modelPosition;
    protected float[] modelCam;
    protected float[] modelCamRight;
    private FloatBuffer floor_Vertics;
    private ShortBuffer floor_Indexs;
    private int screenWidth;
    private int screenHeight;
    private int floorProgram;
    private int[] floorVBOIds = new int[2];
    private int[] floorVAOId = new int[1];
    // Add by Enoch for : calculate the FPS and log it.
    private int floorModelParam;
    private int floorModelViewParam;
    private int floorModelViewProjectionParam;
    private int floorLightPosParam;
    private float[] camera;
    private float[] view;
    private float[] headView;
    private float[] headOritation;
    private float[] modelViewProjection;
    private float[] modelView;
    private float[] modelFloor;
    private float floorDepth = 40f;
    // Add by Enoch for : calculate the FPS and log it.
    private int mFPS;
    private long mTime0;
    private int camDraProgram;
    private int[] camVBOIds = new int[2];
    private int[] camVAOId = new int[1];
    private FloatBuffer cam_General_Vertics;
    private ShortBuffer cam_Indexs;
    private Bitmap textureBmp = null;
    private int camModelViewProjectionParam;
    private int camDraProgramRight;
    private int[] camVBOIdsRight = new int[2];
    private int[] camVAOIdRight = new int[1];
    private FloatBuffer cam_General_VerticsRight;
    private ShortBuffer cam_IndexsRight;
    private Bitmap textureBmpRight = null;
    private int camModelViewProjectionParamRight;
    private boolean use_once = true;
    private boolean showCamera_once = false;//first should be false, when once is true for compatible with old system
    private boolean showCamera = true;

    private Bitmap getSavedBitmap(int hand) {
        return hand == 0 ? textureBmp : textureBmpRight;
    }

    private int createTextureForCamera(int hand) {
        int texture = getTexture(hand);
        Bitmap bm = getSavedBitmap(hand);

        if (texture == 0) {
            int[] textures = new int[1];
            GLES30.glGenTextures(1, textures, 0);
            int textureId = textures[0];
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

            PicovrSDK.SetCameraImageRect(CAM_WIDTH, CAM_HEIGHT);

            if (bm == null) {
                Bitmap bitmap = Bitmap.createBitmap(CAM_WIDTH, CAM_HEIGHT, Bitmap.Config.ARGB_8888);
                //bitmap.eraseColor(Color.parseColor("#000000"));

                GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
                Log.d("OPENGL", "bitmap:" + bitmap);
                bitmap.recycle();
            } else {
                GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bm, 0);
                Log.d("OPENGL", "bitmap:" + bm);
            }

            setTexture(hand, textureId);
            Log.d(TAG_TH, DBG_LC + "textureIdForCamera:(" + getTexture(hand) + ")created");
            return textureId;
        } else {
            return texture;
        }
    }

    private void initGLDataCam(int hand) {
        if (hand == 0) {
            PicovrSDK.StartCameraPreview(1);
            showCamera_once = false;

            camDraProgram = loadProgram(R.raw.camera_vertex, R.raw.camera_fragment);
            checkGLError("camera draw program");
            camModelViewProjectionParam = GLES30.glGetUniformLocation(camDraProgram, "u_MVP");
            GLES30.glGenVertexArrays(1, camVAOId, 0);
            GLES30.glBindVertexArray(camVAOId[0]);

            GLES30.glGenBuffers(2, camVBOIds, 0);
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, camVBOIds[0]);
            GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, camVBOIds[1]);
            cam_General_Vertics.position(0);
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, WorldLayoutData.CAM_GENERAL.length * 4,
                    cam_General_Vertics, GLES30.GL_STATIC_DRAW);
            cam_Indexs.position(0);
            GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, 2 * WorldLayoutData.CAM_INDEX.length,
                    cam_Indexs, GLES30.GL_STATIC_DRAW);

            //// position attribute
            GLES30.glVertexAttribPointer(CAM_VERTEX_POS_INDEX, CAM_VERTEX_POS_SIZE,
                    GLES30.GL_FLOAT, false, CAM_VERTEX_STRIDE, 0);
            GLES30.glEnableVertexAttribArray(CAM_VERTEX_POS_INDEX);

            // color attribute
            GLES30.glVertexAttribPointer(CAM_VERTEX_COLOR_INDEX, CAM_VERTEX_COLOR_SIZE,
                    GLES30.GL_FLOAT, false, CAM_VERTEX_STRIDE, (CAM_VERTEX_POS_SIZE * 4));
            GLES30.glEnableVertexAttribArray(CAM_VERTEX_COLOR_INDEX);

            // texture coord attribute
            GLES30.glVertexAttribPointer(CAM_VERTEX_TEXTURE_INDEX, CAM_VERTEX_TEXTURE_SIZE,
                    GLES30.GL_FLOAT, false, CAM_VERTEX_STRIDE, (CAM_VERTEX_POS_SIZE * 4 + CAM_VERTEX_COLOR_SIZE * 4));
            GLES30.glEnableVertexAttribArray(CAM_VERTEX_TEXTURE_INDEX);

            Matrix.setIdentityM(modelCam, 0);
            Matrix.translateM(modelCam, 0, 0, 0f, CAM_DEPTH);
        } else {
            camDraProgramRight = loadProgram(R.raw.camera_vertex, R.raw.camera_fragment);
            checkGLError("camera draw program");
            camModelViewProjectionParamRight = GLES30.glGetUniformLocation(camDraProgram, "u_MVP");
            GLES30.glGenVertexArrays(1, camVAOIdRight, 0);
            GLES30.glBindVertexArray(camVAOIdRight[0]);

            GLES30.glGenBuffers(2, camVBOIdsRight, 0);
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, camVBOIdsRight[0]);
            GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, camVBOIdsRight[1]);
            cam_General_VerticsRight.position(0);
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, WorldLayoutData.CAM_GENERAL_RIGHT.length * 4,
                    cam_General_VerticsRight, GLES30.GL_STATIC_DRAW);
            cam_IndexsRight.position(0);
            GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, 2 * WorldLayoutData.CAM_INDEX_RIGHT.length,
                    cam_IndexsRight, GLES30.GL_STATIC_DRAW);

            //// position attribute
            GLES30.glVertexAttribPointer(CAM_VERTEX_POS_INDEX, CAM_VERTEX_POS_SIZE,
                    GLES30.GL_FLOAT, false, CAM_VERTEX_STRIDE, 0);
            GLES30.glEnableVertexAttribArray(CAM_VERTEX_POS_INDEX);

            // color attribute
            GLES30.glVertexAttribPointer(CAM_VERTEX_COLOR_INDEX, CAM_VERTEX_COLOR_SIZE,
                    GLES30.GL_FLOAT, false, CAM_VERTEX_STRIDE, (CAM_VERTEX_POS_SIZE * 4));
            GLES30.glEnableVertexAttribArray(CAM_VERTEX_COLOR_INDEX);

            // texture coord attribute
            GLES30.glVertexAttribPointer(CAM_VERTEX_TEXTURE_INDEX, CAM_VERTEX_TEXTURE_SIZE,
                    GLES30.GL_FLOAT, false, CAM_VERTEX_STRIDE, (CAM_VERTEX_POS_SIZE * 4 + CAM_VERTEX_COLOR_SIZE * 4));
            GLES30.glEnableVertexAttribArray(CAM_VERTEX_TEXTURE_INDEX);

            Matrix.setIdentityM(modelCamRight, 0);
            Matrix.translateM(modelCamRight, 0, 0, 0, CAM_DEPTH);
        }

        int textureId = createTextureForCamera(hand);
        // Reset to the default VAO
        GLES30.glBindVertexArray(0);


        Log.d(TAG_TH, DBG_LC + "initGLDataCam");
    }

    @Override
    public void onFrameBegin(HmdState headTransform) {
//        Log.d(TAG_TH, DBG_LC + "onFrameBegin BEGIN");

        Matrix.setLookAtM(camera, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);

        float[] ori = headTransform.getOrientation();
        float[] pos = headTransform.getPos();
        quternion2Matrix(ori, headView);
        //Matrix.translateM(headView,0, pos[0], pos[1], -pos[2]);
        checkGLError("onReadyToDraw");

    }

    @Override
    public void onDrawEye(Eye eye) {
//        Log.d(TAG_TH, DBG_LC + "onDrawEye BEGIN");

        GLES30.glViewport(0, 0, screenWidth, screenHeight);
        GLES30.glEnable(GLES30.GL_SCISSOR_TEST);
        GLES30.glDisable(GLES30.GL_CULL_FACE);
        GLES30.glScissor(1, 1, screenWidth - 2, screenHeight - 2);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        GLES30.glClearColor(49.0f / 255.0f, 77.0f / 255.0f, 121.0f / 255.0f, 1.0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);


        Matrix.multiplyMM(view, 0, headView, 0, camera, 0);
        Matrix.multiplyMV(lightPosInEyeSpace, 0, view, 0, LIGHT_POS_IN_WORLD_SPACE, 0);

        float[] perspective = new float[16];
        getPerspective(Z_NEAR, Z_FAR, 51.f, 51.f, 51.f, 51.f, perspective, 0);

        Matrix.multiplyMM(modelView, 0, view, 0, modelFloor, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        drawFloor();

        //zgt{
        Matrix.multiplyMM(modelView, 0, view, 0, modelCam, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        drawCamera(0);

        Matrix.multiplyMM(modelView, 0, view, 0, modelCamRight, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        drawCamera(1);
        //zgt}


        GLES30.glFinish();
//        Log.d(TAG_TH, DBG_LC + "onDrawEye END");
    }

    @Override
    public void onFrameEnd() {
//        Log.d(TAG_TH, DBG_LC +" onFrameEnd CALLED");
        // Add by Enoch for : calculate FPS
        long time1 = System.currentTimeMillis();
        mFPS++;
        if (time1 - mTime0 > 1000) {
            mFPS = mFPS * 1000 / (int) (time1 - mTime0);
            Log.d(TAG_TH, "FPS:::::::::" + mFPS);
            mTime0 = time1;
            mFPS = 1;
        }
        // Add by Enoch for : calculate FPS
    }

    @Override
    public void onTouchEvent() {
//        Log.d(TAG_TH, DBG_LC +" onTouchEvent");
    }

    @Override
    public void onRenderPause() {
        Log.d(TAG_TH, DBG_LC + " onRenderPause CALLED");
    }

    @Override
    public void onRenderResume() {
        Log.d(TAG_TH, DBG_LC + " onRenderResume CALLED");
    }

    @Override
    public void onRendererShutdown() {
        Log.d(TAG_TH, DBG_LC + " onRendererShutdown CALLED");
    }

    @Override
    public void initGL(int w, int h) {
//        Log.d(TAG_TH, DBG_LC + "initGL BEGIN");
        screenHeight = h;
        screenWidth = w;

        GLES30.glEnableVertexAttribArray(FLOOR_VERTEX_POS_INDEX);
        GLES30.glEnableVertexAttribArray(FLOOR_VERTEX_COLOR_INDEX);
        GLES30.glEnableVertexAttribArray(FLOOR_VERTEX_NORMAL_INDEX);
        GLES30.glVertexAttribPointer(FLOOR_VERTEX_POS_INDEX, FLOOR_VERTEX_POS_SIZE,
                GLES30.GL_FLOAT, false, FLOOR_VERTEX_STRIDE,
                0);
        GLES30.glVertexAttribPointer(FLOOR_VERTEX_COLOR_INDEX, FLOOR_VERTEX_COLOR_SIZE,
                GLES30.GL_FLOAT, false, FLOOR_VERTEX_STRIDE,
                (FLOOR_VERTEX_POS_SIZE * 4));
        GLES30.glVertexAttribPointer(FLOOR_VERTEX_NORMAL_INDEX, FLOOR_VERTEX_NORMAL_SIZE,
                GLES30.GL_FLOAT, false, FLOOR_VERTEX_STRIDE,
                (FLOOR_VERTEX_POS_SIZE * 4 + FLOOR_VERTEX_COLOR_SIZE * 4));
        // Reset to the default VAO
        GLES30.glBindVertexArray(0);

        GLES30.glEnableVertexAttribArray(FLOOR_VERTEX_POS_INDEX);
        GLES30.glEnableVertexAttribArray(FLOOR_VERTEX_COLOR_INDEX);
        GLES30.glEnableVertexAttribArray(FLOOR_VERTEX_NORMAL_INDEX);
        GLES30.glVertexAttribPointer(FLOOR_VERTEX_POS_INDEX, FLOOR_VERTEX_POS_SIZE,
                GLES30.GL_FLOAT, false, FLOOR_VERTEX_STRIDE,
                0);
        GLES30.glVertexAttribPointer(FLOOR_VERTEX_COLOR_INDEX, FLOOR_VERTEX_COLOR_SIZE,
                GLES30.GL_FLOAT, false, FLOOR_VERTEX_STRIDE,
                (FLOOR_VERTEX_POS_SIZE * 4));
        GLES30.glVertexAttribPointer(FLOOR_VERTEX_NORMAL_INDEX, FLOOR_VERTEX_NORMAL_SIZE,
                GLES30.GL_FLOAT, false, FLOOR_VERTEX_STRIDE,
                (FLOOR_VERTEX_POS_SIZE * 4 + FLOOR_VERTEX_COLOR_SIZE * 4));
        // Reset to the default VAO
        GLES30.glBindVertexArray(0);

        floorProgram = loadProgram(R.raw.light_vertex, R.raw.grid_fragment);
        checkGLError("Floor program");

        floorModelParam = GLES30.glGetUniformLocation(floorProgram, "u_Model");
        floorModelViewParam = GLES30.glGetUniformLocation(floorProgram, "u_MVMatrix");
        floorModelViewProjectionParam = GLES30.glGetUniformLocation(floorProgram, "u_MVP");
        floorLightPosParam = GLES30.glGetUniformLocation(floorProgram, "u_LightPos");
        checkGLError("Floor program params");

        // Generate VBO Ids and load the VBOs with data
        GLES30.glGenBuffers(2, floorVBOIds, 0);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, floorVBOIds[0]);
        floor_Vertics.position(0);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, WorldLayoutData.FLOOR.length * 4,
                floor_Vertics, GLES30.GL_STATIC_DRAW);

        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, floorVBOIds[1]);
        floor_Indexs.position(0);
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, 2 * WorldLayoutData.FLOOR_INDEX.length,
                floor_Indexs, GLES30.GL_STATIC_DRAW);

        // Generate VAO Id
        GLES30.glGenVertexArrays(1, floorVAOId, 0);
        // Bind the VAO and then setup the vertex attributes
        GLES30.glBindVertexArray(floorVAOId[0]);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, floorVBOIds[0]);
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, floorVBOIds[1]);

        GLES30.glEnableVertexAttribArray(FLOOR_VERTEX_POS_INDEX);
        GLES30.glEnableVertexAttribArray(FLOOR_VERTEX_COLOR_INDEX);
        GLES30.glEnableVertexAttribArray(FLOOR_VERTEX_NORMAL_INDEX);
        GLES30.glVertexAttribPointer(FLOOR_VERTEX_POS_INDEX, FLOOR_VERTEX_POS_SIZE,
                GLES30.GL_FLOAT, false, FLOOR_VERTEX_STRIDE,
                0);
        GLES30.glVertexAttribPointer(FLOOR_VERTEX_COLOR_INDEX, FLOOR_VERTEX_COLOR_SIZE,
                GLES30.GL_FLOAT, false, FLOOR_VERTEX_STRIDE,
                (FLOOR_VERTEX_POS_SIZE * 4));
        GLES30.glVertexAttribPointer(FLOOR_VERTEX_NORMAL_INDEX, FLOOR_VERTEX_NORMAL_SIZE,
                GLES30.GL_FLOAT, false, FLOOR_VERTEX_STRIDE,
                (FLOOR_VERTEX_POS_SIZE * 4 + FLOOR_VERTEX_COLOR_SIZE * 4));
        // Reset to the default VAO
        GLES30.glBindVertexArray(0);

        Matrix.setIdentityM(modelFloor, 0);
        Matrix.translateM(modelFloor, 0, 0, -floorDepth, 0); // Floor appears below user.

        //zgt{
        initGLDataCam(0);
        initGLDataCam(1);
        //zgt}
        Log.d(TAG_TH, DBG_LC + "initGL END");
    }

    @Override
    public void deInitGL() {
        Log.d(TAG_TH, DBG_LC + " deInitGL CALLED");
        GLES30.glDeleteProgram(floorProgram);
        GLES30.glDeleteVertexArrays(1, floorVAOId, 0);
        GLES30.glDeleteBuffers(2, floorVBOIds, 0);
        deinitGLDataCam(0);
        deinitGLDataCam(1);
    }

    private void deinitGLDataCam(int hand) {

        int texture = getTexture(hand);
        if (texture != 0) {
            int[] textures = new int[1];
            textures[0] = texture;
            if (use_once) {
                TextureToBitmap(hand, texture, CAM_WIDTH, CAM_HEIGHT);
            }
            GLES30.glDeleteTextures(1, textures, 0);
            setTexture(hand, 0);
        }

        if (hand == 0) {
            GLES30.glDeleteProgram(camDraProgram);
            GLES30.glDeleteBuffers(2, camVBOIds, 0);
            GLES30.glDeleteVertexArrays(1, camVAOId, 0);
        } else {
            GLES30.glDeleteProgram(camDraProgramRight);
            GLES30.glDeleteBuffers(2, camVBOIdsRight, 0);
            GLES30.glDeleteVertexArrays(1, camVAOIdRight, 0);
        }

        //if(!use_once)
        {
            //PicovrSDK.StartCameraPreview(0);
        }
        Log.d(TAG_TH, DBG_LC + "deinitGLDataCam");
    }
    //zgt}

    private void TextureToBitmap(int hand, int textureID, int width, int height) {

        int[] frameBuffer = new int[1];
        GLES30.glGenFramebuffers(1, frameBuffer, 0);

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, frameBuffer[0]);
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, textureID, 0);

        GLES30.glDrawBuffers(1, new int[]{GLES30.GL_COLOR_ATTACHMENT0}, 0);

        if (GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER) != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            Log.e("OPENGL", "framebuffer not complete");
            return;
        }

        ByteBuffer rgbaBuf = ByteBuffer.allocateDirect(width * height * 4);
        rgbaBuf.position(0);
        GLES30.glReadPixels(0, 0, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, rgbaBuf);

        saveRgb2Bitmap(hand, rgbaBuf, width, height);

        GLES30.glDeleteFramebuffers(1, IntBuffer.wrap(frameBuffer));
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
        checkGLError("saveTextureToImage end.");
    }

    private void setTexture(int hand, int id) {
        if (hand == 0)//left
        {
            textureIdForCamera = id;
        } else {
            textureIdForCameraRight = id;
        }
    }

    private void saveRgb2Bitmap(int hand, Buffer buf, int width, int height) {

        if (hand == 0)//left
        {
            if (textureBmp != null) {
                textureBmp.recycle();
                textureBmp = null;
            }

            textureBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            textureBmp.copyPixelsFromBuffer(buf);
        } else //right
        {
            if (textureBmpRight != null) {
                textureBmpRight.recycle();
                textureBmpRight = null;
            }

            textureBmpRight = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            textureBmpRight.copyPixelsFromBuffer(buf);
        }
    }

    @Override
    public void renderEventCallBack(int var) {
//        Log.d(TAG_TH, DBG_LC + "renderEventCallBack BEGIN");
        if (var == 101) {
            Log.d("", "UER_EVENT!!" + var);
        }

    }

    @Override
    public void surfaceChangedCallBack(int i, int i1) {

    }

    public void getPerspective(float near, float far, float left, float right, float bottom, float top, float[] perspective, int offset) {
//        Log.d(TAG_TH, DBG_LC + "getPerspective BEGIN");
        float l = (float) (-Math.tan(Math.toRadians((double) left))) * near;
        float r = (float) Math.tan(Math.toRadians((double) right)) * near;
        float b = (float) (-Math.tan(Math.toRadians((double) bottom))) * near;
        float t = (float) Math.tan(Math.toRadians((double) top)) * near;
        Matrix.frustumM(perspective, offset, l, r, b, t, near, far);
//        Log.d(TAG_TH, DBG_LC + "getPerspective END");
    }

    public void drawFloor() {
//        Log.d(TAG_TH, DBG_LC + "drawFloor BEGIN");
        GLES30.glUseProgram(floorProgram);
        GLES30.glUniform3fv(floorLightPosParam, 1, lightPosInEyeSpace, 0);
        GLES30.glUniformMatrix4fv(floorModelParam, 1, false, modelFloor, 0);
        GLES30.glUniformMatrix4fv(floorModelViewParam, 1, false, modelView, 0);
        GLES30.glUniformMatrix4fv(floorModelViewProjectionParam, 1, false, modelViewProjection, 0);
        GLES30.glBindVertexArray(floorVAOId[0]);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, WorldLayoutData.FLOOR_INDEX.length, GLES30.GL_UNSIGNED_SHORT, 0);
        GLES30.glBindVertexArray(0);
        checkGLError("drawing floor");
//        Log.d(TAG_TH, DBG_LC + "drawFloor END");
    }

    public void drawCamera(int hand) {

        if ((!use_once) && (!showCamera)) {
            return;
        }

        updateCamera(hand);

        if (hand == 0)//left
        {
            GLES30.glUseProgram(camDraProgram);
            GLES30.glBindVertexArray(camVAOId[0]);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, getTexture(0));
            GLES30.glUniformMatrix4fv(camModelViewProjectionParam, 1, false, modelViewProjection, 0);
            GLES30.glDrawElements(GLES30.GL_TRIANGLES, WorldLayoutData.CAM_INDEX.length, GLES30.GL_UNSIGNED_SHORT, 0);
        } else {
            GLES30.glUseProgram(camDraProgramRight);
            GLES30.glBindVertexArray(camVAOIdRight[0]);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, getTexture(1));
            GLES30.glUniformMatrix4fv(camModelViewProjectionParamRight, 1, false, modelViewProjection, 0);
            GLES30.glDrawElements(GLES30.GL_TRIANGLES, WorldLayoutData.CAM_INDEX_RIGHT.length, GLES30.GL_UNSIGNED_SHORT, 0);
        }
        GLES30.glBindVertexArray(0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        checkGLError("drawing camera");
    }

    public void updateCamera(int hand) {

        if (use_once && showCamera_once) {
            Log.d(TAG_TH, DBG_LC + "------------------------------------------update camera:");
            /*try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
            PicovrSDK.GetSeeThroughFrameFU(hand, getTexture(hand));
            if (hand == 1) {
                showCamera_once = false;
            }
        } else if (!use_once) {
            PicovrSDK.GetSeeThroughFrameFU(hand, getTexture(hand));
        }
    }

    private int getTexture(int hand) {
        return hand == 0 ? textureIdForCamera : textureIdForCameraRight;
    }

    public void quternion2Matrix(float Q[], float M[]) {
        float x = Q[0];
        float y = Q[1];
        float z = Q[2];
        float w = Q[3];
        float ww = w * w;
        float xx = x * x;
        float yy = y * y;
        float zz = z * z;

        M[0] = ww + xx - yy - zz;
        M[1] = 2 * (x * y - w * z);
        M[2] = 2 * (x * z + w * y);
        M[3] = 0.f;


        M[4] = 2 * (x * y + w * z);
        M[5] = ww - xx + yy - zz;
        M[6] = 2 * (y * z - w * x);
        M[7] = 0.f;


        M[8] = 2 * (x * z - w * y);
        ;
        M[9] = 2 * (y * z + w * x);
        M[10] = ww - xx - yy + zz;
        M[11] = 0.f;


        M[12] = 0.0f;
        M[13] = 0.0f;
        M[14] = 0.0f;
        M[15] = 1.f;
    }

    private static void checkGLError(String label) {
//        Log.d(TAG_TH, DBG_LC + " checkGLError BEGIN");
        int error;
        while ((error = GLES30.glGetError()) != GLES30.GL_NO_ERROR) {
            Log.e(TAG_TH, label + ": glError " + error);
            throw new RuntimeException(label + ": glError " + error);
        }
//        Log.d(TAG_TH, DBG_LC + " checkGLError END");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        Log.d(TAG_TH, DBG_LC + "onCreate BEGIN");

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);

        camera = new float[16];
        view = new float[16];
        modelViewProjection = new float[16];
        modelView = new float[16];
        modelFloor = new float[16];
        modelPosition = new float[]{0.0f, 0.0f, -MAX_MODEL_DISTANCE / 2.0f};
        headView = new float[16];
        headOritation = new float[4];
        /*-----------------------------------------------------------------------------------------*/
        floor_Vertics = ByteBuffer.allocateDirect(WorldLayoutData.FLOOR.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        floor_Vertics.put(WorldLayoutData.FLOOR).position(0);

        floor_Indexs = ByteBuffer.allocateDirect(WorldLayoutData.FLOOR_INDEX.length * 2)
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        floor_Indexs.put(WorldLayoutData.FLOOR_INDEX).position(0);
        //zgt{
        /*-----------------------------------------------------------------------------------------*/
        modelCam = new float[16];
        cam_General_Vertics = ByteBuffer.allocateDirect(WorldLayoutData.CAM_GENERAL.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        cam_General_Vertics.put(WorldLayoutData.CAM_GENERAL).position(0);

        cam_Indexs = ByteBuffer.allocateDirect(WorldLayoutData.CAM_INDEX.length * 2)
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        cam_Indexs.put(WorldLayoutData.CAM_INDEX).position(0);
        /*-----------------------------------------------------------------------------------------*/
        modelCamRight = new float[16];
        cam_General_VerticsRight = ByteBuffer.allocateDirect(WorldLayoutData.CAM_GENERAL_RIGHT.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        cam_General_VerticsRight.put(WorldLayoutData.CAM_GENERAL_RIGHT).position(0);

        cam_IndexsRight = ByteBuffer.allocateDirect(WorldLayoutData.CAM_INDEX_RIGHT.length * 2)
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        cam_IndexsRight.put(WorldLayoutData.CAM_INDEX_RIGHT).position(0);
        /*-----------------------------------------------------------------------------------------*/
        //zgt}
        Log.d(TAG_TH, DBG_LC + "onCreate END");
    }

    @Override
    protected void onPause() {
        super.RenderEventPush(UER_EVENT + 1);
        super.onPause();
        Log.d(TAG_TH, DBG_LC + "onPause CALLED");
    }

    @Override
    protected void onResume() {
        super.onResume();

        mTime0 = System.currentTimeMillis();    // Add by Enoch : FPS relative.
        mFPS = 0;    // Add by Enoch : FPS relative.
        Log.d(TAG_TH, DBG_LC + "onResume CALLED");
    }

    @Override
    protected void onDestroy() {
        if (textureBmp != null) {
            textureBmp.recycle();
            textureBmp = null;
        }
        super.onDestroy();
        Log.d(TAG_TH, DBG_LC + "onDestroy CALLED");
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG_TH, DBG_LC + " onKeyUp:" + keyCode);
        if (keyCode == 96) {
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG_TH, DBG_LC + " onKeyDown:" + keyCode);

        if (keyCode == 96) {
            showCamera = !showCamera;
            showCamera_once = !showCamera_once;
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private String readRawTextFile(int resId) {
//        Log.d(TAG_TH, DBG_LC +" readRawTextFile BEGIN");
        InputStream inputStream = getResources().openRawResource(resId);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
//            Log.d(TAG_TH, DBG_LC + " readRawTextFile END");
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
//        Log.d(TAG_TH, DBG_LC +" readRawTextFile END");
        return null;
    }

    private int loadGLShader(int type, int resId) {
//        Log.d(TAG_TH, DBG_LC +" loadGLShader BEGIN");
        String code = readRawTextFile(resId);
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, code);
        GLES30.glCompileShader(shader);

        // Get the copilation status.
        final int[] compileStatus = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0);

        // If the compilation failed, delete the shader.
        if (compileStatus[0] == 0) {
            Log.e(TAG_TH, "Error compiling shader: " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            shader = 0;
        }

        if (shader == 0) {
            throw new RuntimeException("Error creating shader.");
        }
//        Log.d(TAG_TH, DBG_LC + " loadGLShader END");
        return shader;
    }

    private int loadProgram(int vertex_resId, int fragment_resId) {
//        Log.d(TAG_TH, DBG_LC + "loadProgram BEGIN");
        int vertexShader;
        int fragmentShader;
        int programObject;
        int[] linked = new int[1];

        // Load the vertex/fragment shaders
        vertexShader = loadGLShader(GLES30.GL_VERTEX_SHADER, vertex_resId);

        if (vertexShader == 0) {
            return 0;
        }

        fragmentShader = loadGLShader(GLES30.GL_FRAGMENT_SHADER, fragment_resId);

        if (fragmentShader == 0) {
            return 0;
        }

        programObject = GLES30.glCreateProgram();

        if (programObject == 0) {
            Log.e(TAG_TH, "Error creating program");
            return 0;
        }

        GLES30.glAttachShader(programObject, vertexShader);
        GLES30.glAttachShader(programObject, fragmentShader);

        // Link the program
        GLES30.glLinkProgram(programObject);

        // Check the link status
        GLES30.glGetProgramiv(programObject, GLES30.GL_LINK_STATUS, linked, 0);

        if (linked[0] == 0) {
            Log.e(TAG_TH, "Error linking program.");
            Log.e(TAG_TH, GLES30.glGetProgramInfoLog(programObject));
            GLES30.glDeleteProgram(programObject);
            return 0;
        }

        // Free up no longer needed shader resources
        GLES30.glDeleteShader(vertexShader);
        GLES30.glDeleteShader(fragmentShader);

//        Log.d(TAG_TH, DBG_LC +" loadProgram END");
        return programObject;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG_TH, DBG_LC + "onStart CALLED");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG_TH, DBG_LC + "onRestart CALLED");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG_TH, DBG_LC + "onStop CALLED");
    }
}
