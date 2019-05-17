package com.ustwo.openglwatchfaceexploration

import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer


class MirrorCube (val context: Context) {

    companion object {

        // number of coordinates per vertex in this array
        private val COORDS_PER_VERTEX = 6
        private var cubeCoords = floatArrayOf(
            // positions         // normal coords
            -0.5f, -0.5f, -0.5f,  0.0f,  0.0f, -1.0f,
            0.5f, -0.5f, -0.5f,  0.0f,  0.0f, -1.0f,
            0.5f,  0.5f, -0.5f,  0.0f,  0.0f, -1.0f,
            0.5f,  0.5f, -0.5f,  0.0f,  0.0f, -1.0f,
            -0.5f,  0.5f, -0.5f,  0.0f,  0.0f, -1.0f,
            -0.5f, -0.5f, -0.5f,  0.0f,  0.0f, -1.0f,

            -0.5f, -0.5f,  0.5f,  0.0f,  0.0f, 1.0f,
            0.5f, -0.5f,  0.5f,  0.0f,  0.0f, 1.0f,
            0.5f,  0.5f,  0.5f,  0.0f,  0.0f, 1.0f,
            0.5f,  0.5f,  0.5f,  0.0f,  0.0f, 1.0f,
            -0.5f,  0.5f,  0.5f,  0.0f,  0.0f, 1.0f,
            -0.5f, -0.5f,  0.5f,  0.0f,  0.0f, 1.0f,

            -0.5f,  0.5f,  0.5f, -1.0f,  0.0f,  0.0f,
            -0.5f,  0.5f, -0.5f, -1.0f,  0.0f,  0.0f,
            -0.5f, -0.5f, -0.5f, -1.0f,  0.0f,  0.0f,
            -0.5f, -0.5f, -0.5f, -1.0f,  0.0f,  0.0f,
            -0.5f, -0.5f,  0.5f, -1.0f,  0.0f,  0.0f,
            -0.5f,  0.5f,  0.5f, -1.0f,  0.0f,  0.0f,

            0.5f,  0.5f,  0.5f,  1.0f,  0.0f,  0.0f,
            0.5f,  0.5f, -0.5f,  1.0f,  0.0f,  0.0f,
            0.5f, -0.5f, -0.5f,  1.0f,  0.0f,  0.0f,
            0.5f, -0.5f, -0.5f,  1.0f,  0.0f,  0.0f,
            0.5f, -0.5f,  0.5f,  1.0f,  0.0f,  0.0f,
            0.5f,  0.5f,  0.5f,  1.0f,  0.0f,  0.0f,

            -0.5f, -0.5f, -0.5f,  0.0f, -1.0f,  0.0f,
            0.5f, -0.5f, -0.5f,  0.0f, -1.0f,  0.0f,
            0.5f, -0.5f,  0.5f,  0.0f, -1.0f,  0.0f,
            0.5f, -0.5f,  0.5f,  0.0f, -1.0f,  0.0f,
            -0.5f, -0.5f,  0.5f,  0.0f, -1.0f,  0.0f,
            -0.5f, -0.5f, -0.5f,  0.0f, -1.0f,  0.0f,

            -0.5f,  0.5f, -0.5f,  0.0f,  1.0f,  0.0f,
            0.5f,  0.5f, -0.5f,  0.0f,  1.0f,  0.0f,
            0.5f,  0.5f,  0.5f,  0.0f,  1.0f,  0.0f,
            0.5f,  0.5f,  0.5f,  0.0f,  1.0f,  0.0f,
            -0.5f,  0.5f,  0.5f,  0.0f,  1.0f,  0.0f,
            -0.5f,  0.5f, -0.5f,  0.0f,  1.0f,  0.0f
        )

        private val vertexStride = COORDS_PER_VERTEX * 4
        private val textures = IntArray(1)
        private const val mPositionDataSize = 3
        private const val mTexDataSize = 3
    }

    private val VBO: FloatBuffer

    private var aPosHandle: Int
    private var aNormalHandle: Int
    private var uCameraPosHandle: Int
    private var uModelHandle: Int
    private var uViewHandle: Int
    private var uProjectionHandle: Int

    private var degrees = 0f


    private val vertexShaderCode : String
    private val fragmentShaderCode : String

    private var mProgram: Int = 0

    init {
        // initialize vertex byte buffer for shape coordinates
        val bb = ByteBuffer.allocateDirect(cubeCoords.size * 4) // (# of coordinate values * 4 bytes per float)
        bb.order(ByteOrder.nativeOrder())
        VBO = bb.asFloatBuffer()
        VBO.put(cubeCoords)
        VBO.position(0)

        // position attribute
        GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, vertexStride, VBO)
        GLES20.glEnableVertexAttribArray(0)
        // normal attribute
        GLES20.glVertexAttribPointer(1, 3, GLES20.GL_FLOAT, false, vertexStride, 3)
        GLES20.glEnableVertexAttribArray(1)

        vertexShaderCode = Utils.readStringAsset(context, "mirror_cube_vs.glsl")
        fragmentShaderCode = Utils.readStringAsset(context, "mirror_cube_fs.glsl")

        val vertexShader = Utils.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = Utils.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        mProgram = GLES20.glCreateProgram()             // create empty OpenGL ES Program
        GLES20.glAttachShader(mProgram, vertexShader)   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader) // add the fragment shader to program

        // Bind attributes
        GLES20.glBindAttribLocation(mProgram, 0, "aPosition")
        GLES20.glBindAttribLocation(mProgram, 1, "aNormal")

        GLES20.glLinkProgram(mProgram)                  // creates OpenGL ES program executables

        // get handle to vertex shader's vPosition member
        aPosHandle = GLES20.glGetAttribLocation(mProgram, "aPosition")
        // get handle to fragment shader's vColor member
        aNormalHandle = GLES20.glGetAttribLocation(mProgram, "aNormal")
        // get handle to mvp matrix uniform
        uModelHandle = GLES20.glGetUniformLocation(mProgram, "model")
        uViewHandle = GLES20.glGetUniformLocation(mProgram, "view")
        uProjectionHandle = GLES20.glGetUniformLocation(mProgram, "projection")
        uCameraPosHandle = GLES20.glGetUniformLocation(mProgram, "cameraPos")
    }

    fun draw(modelMatrix: FloatArray, viewMatrix: FloatArray, projectionMatrix: FloatArray) {
        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram)

//        GLES20.glPolygonMode(GL_FRONT_AND_BACK,GL_LINE);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(aPosHandle)

        VBO.position(0)
        GLES20.glVertexAttribPointer(aPosHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
            vertexStride, VBO)
        GLES20.glEnableVertexAttribArray(aPosHandle)

        VBO.position(3)
        GLES20.glVertexAttribPointer(aNormalHandle, mTexDataSize, GLES20.GL_FLOAT, false,
            vertexStride, VBO)
        GLES20.glEnableVertexAttribArray(aNormalHandle)

        val rotateMatrix = FloatArray(16)
        Matrix.setIdentityM(rotateMatrix, 0)
        Matrix.rotateM(rotateMatrix, 0, degrees, 0.5f, 1f, 0f)
        degrees += 2
        if (degrees >= 360f) { degrees = 0f }

        // get handle to shape's transformation matrix
        uModelHandle = GLES20.glGetUniformLocation(mProgram, "model")
        uViewHandle = GLES20.glGetUniformLocation(mProgram, "view")
        uProjectionHandle = GLES20.glGetUniformLocation(mProgram, "projection")
        uCameraPosHandle = GLES20.glGetUniformLocation(mProgram, "cameraPos")
        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(uModelHandle, 1, false, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(uViewHandle, 1, false, viewMatrix, 0)
        GLES20.glUniformMatrix4fv(uProjectionHandle, 1, false, projectionMatrix, 0)
        GLES20.glUniform3f(0, viewMatrix[0], viewMatrix[1], viewMatrix[2])

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36)

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(aPosHandle)
    }
}