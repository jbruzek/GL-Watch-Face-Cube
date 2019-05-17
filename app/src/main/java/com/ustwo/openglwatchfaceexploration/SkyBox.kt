package com.ustwo.openglwatchfaceexploration

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class SkyBox(val context: Context, val images: IntArray) {

    companion object {
        private val boxCoords = floatArrayOf(
            -1.0f,  1.0f, -1.0f,
            -1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,
            1.0f,  1.0f, -1.0f,
            -1.0f,  1.0f, -1.0f,

            -1.0f, -1.0f,  1.0f,
            -1.0f, -1.0f, -1.0f,
            -1.0f,  1.0f, -1.0f,
            -1.0f,  1.0f, -1.0f,
            -1.0f,  1.0f,  1.0f,
            -1.0f, -1.0f,  1.0f,

            1.0f, -1.0f, -1.0f,
            1.0f, -1.0f,  1.0f,
            1.0f,  1.0f,  1.0f,
            1.0f,  1.0f,  1.0f,
            1.0f,  1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,

            -1.0f, -1.0f,  1.0f,
            -1.0f,  1.0f,  1.0f,
            1.0f,  1.0f,  1.0f,
            1.0f,  1.0f,  1.0f,
            1.0f, -1.0f,  1.0f,
            -1.0f, -1.0f,  1.0f,

            -1.0f,  1.0f, -1.0f,
            1.0f,  1.0f, -1.0f,
            1.0f,  1.0f,  1.0f,
            1.0f,  1.0f,  1.0f,
            -1.0f,  1.0f,  1.0f,
            -1.0f,  1.0f, -1.0f,

            -1.0f, -1.0f, -1.0f,
            -1.0f, -1.0f,  1.0f,
            1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,
            -1.0f, -1.0f,  1.0f,
            1.0f, -1.0f,  1.0f
        )
        internal val COORDS_PER_VERTEX = 3
        internal val vertexStride = COORDS_PER_VERTEX * 4


        private val textures = IntArray(1)
    }

    private val VBO: FloatBuffer

    private val vertexShaderCode : String
    private val fragmentShaderCode : String
    private var mProgram: Int = 0

    private var aPosHandle: Int
    private var uViewHandle: Int
    private var uProjectionHandle: Int

    init {
        // initialize vertex byte buffer for shape coordinates
        val bb = ByteBuffer.allocateDirect(boxCoords.size * 4) // (# of coordinate values * 4 bytes per float)
        bb.order(ByteOrder.nativeOrder())
        VBO = bb.asFloatBuffer()
        VBO.put(boxCoords)
        VBO.position(0)

        // position attribute
        GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, vertexStride, VBO)
        Utils.checkGlError("glVertexAttribPointer")
        GLES20.glEnableVertexAttribArray(0)
        Utils.checkGlError("glEnableVertexAttribArray")

        vertexShaderCode = Utils.readStringAsset(context, "skybox_vs.glsl")
        fragmentShaderCode = Utils.readStringAsset(context, "skybox_fs.glsl")

        val vertexShader = Utils.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = Utils.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        mProgram = GLES20.glCreateProgram()             // create empty OpenGL ES Program
        Utils.checkGlError("glCreateProgram")
        GLES20.glAttachShader(mProgram, vertexShader)   // add the vertex shader to program
        Utils.checkGlError("glAttachShader")
        GLES20.glAttachShader(mProgram, fragmentShader) // add the fragment shader to program
        Utils.checkGlError("glAttachShader")

        // Bind attributes
        GLES20.glBindAttribLocation(mProgram, 0, "aPos")
        Utils.checkGlError("glBindAttribLocation")

        // creates OpenGL ES program executables
        GLES20.glLinkProgram(mProgram)
        Utils.checkGlError("glLinkProgram")
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(mProgram, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == GLES20.GL_FALSE) {
            Log.e("glLinkProgram", "linking program failed")
        }

        // get handle to vertex shader's vPosition member
        aPosHandle = GLES20.glGetAttribLocation(mProgram, "aPos")
        Utils.checkGlError("glGetAttribLocation")
        uViewHandle = GLES20.glGetUniformLocation(mProgram, "view")
        Utils.checkGlError("glGetUniformLocation")
        uProjectionHandle = GLES20.glGetUniformLocation(mProgram, "projection")
        Utils.checkGlError("glGetUniformLocation")

        generateTextures()
    }

    fun draw(viewMatrix: FloatArray, projectionMatrix: FloatArray) {
        //change depth function so skybox is only drawn when it isn't overlapped by an object
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        Utils.checkGlError("glDepthFunc")

        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram)
        Utils.checkGlError("glUseProgram")

        GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, textures[0])
        Utils.checkGlError("glBindTexture")

        VBO.position(0)
        GLES20.glVertexAttribPointer(aPosHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false,
            vertexStride, VBO)
        Utils.checkGlError("glVertexAttribPointer")
        GLES20.glEnableVertexAttribArray(aPosHandle)
        Utils.checkGlError("glEnableVertexAttribArray")

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(uViewHandle, 1, false, viewMatrix, 0)
        Utils.checkGlError("glUniformMatrix4fv")
        GLES20.glUniformMatrix4fv(uProjectionHandle, 1, false, projectionMatrix, 0)
        Utils.checkGlError("glUniformMatrix4fv")

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36)
        Utils.checkGlError("glDrawArrays")

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(aPosHandle)
        Utils.checkGlError("glDisableVertexAttribArray")

        //reset depth func
        GLES20.glDepthFunc(GLES20.GL_LESS)
        Utils.checkGlError("glDepthFunc")
    }

    private fun generateTextures() {
        //generate texture
        GLES20.glGenTextures(1, textures, 0)
        Utils.checkGlError("glGenTextures")
        GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, textures[0])
        Utils.checkGlError("glBindTexture")

        //get the images into textures
        val bo = BitmapFactory.Options()
        for (i in images.indices) {
            val tex = BitmapFactory.decodeResource(context.resources, images[i], bo)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, tex, 0)
            tex.recycle()
        }

        //set texture wrapping parameters
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        Utils.checkGlError("glTexParameteri")
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        Utils.checkGlError("glTexParameteri")

        //set texture filtering parameters
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        Utils.checkGlError("glTexParameteri")
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        Utils.checkGlError("glTexParameteri")

        if(textures[0] == 0){
            Log.d("Textures", "Error loading texture")
        }
    }
}