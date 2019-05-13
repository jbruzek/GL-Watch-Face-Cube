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
import java.nio.ShortBuffer


class Cube (val context: Context) {

    companion object {

        // number of coordinates per vertex in this array
        internal val COORDS_PER_VERTEX = 5
        internal var cubeCoords = floatArrayOf(
                // positions         // texture coords
                -0.5f, -0.5f, -0.5f,  0.0f, 1.0f,
                0.5f, -0.5f, -0.5f,  1.0f, 1.0f,
                0.5f,  0.5f, -0.5f,  1.0f, 0.0f,
                0.5f,  0.5f, -0.5f,  1.0f, 0.0f,
                -0.5f,  0.5f, -0.5f,  0.0f, 0.0f,
                -0.5f, -0.5f, -0.5f,  0.0f, 1.0f,

                -0.5f, -0.5f,  0.5f,  0.0f, 1.0f,
                0.5f, -0.5f,  0.5f,  1.0f, 1.0f,
                0.5f,  0.5f,  0.5f,  1.0f, 0.0f,
                0.5f,  0.5f,  0.5f,  1.0f, 0.0f,
                -0.5f,  0.5f,  0.5f,  0.0f, 0.0f,
                -0.5f, -0.5f,  0.5f,  0.0f, 1.0f,

                -0.5f,  0.5f,  0.5f,  1.0f, 1.0f,
                -0.5f,  0.5f, -0.5f,  1.0f, 0.0f,
                -0.5f, -0.5f, -0.5f,  0.0f, 0.0f,
                -0.5f, -0.5f, -0.5f,  0.0f, 0.0f,
                -0.5f, -0.5f,  0.5f,  0.0f, 1.0f,
                -0.5f,  0.5f,  0.5f,  1.0f, 1.0f,

                0.5f,  0.5f,  0.5f,  1.0f, 1.0f,
                0.5f,  0.5f, -0.5f,  1.0f, 0.0f,
                0.5f, -0.5f, -0.5f,  0.0f, 0.0f,
                0.5f, -0.5f, -0.5f,  0.0f, 0.0f,
                0.5f, -0.5f,  0.5f,  0.0f, 1.0f,
                0.5f,  0.5f,  0.5f,  1.0f, 1.0f,

                -0.5f, -0.5f, -0.5f,  0.0f, 0.0f,
                0.5f, -0.5f, -0.5f,  1.0f, 0.0f,
                0.5f, -0.5f,  0.5f,  1.0f, 1.0f,
                0.5f, -0.5f,  0.5f,  1.0f, 1.0f,
                -0.5f, -0.5f,  0.5f,  0.0f, 1.0f,
                -0.5f, -0.5f, -0.5f,  0.0f, 0.0f,

                -0.5f,  0.5f, -0.5f,  0.0f, 0.0f,
                0.5f,  0.5f, -0.5f,  1.0f, 0.0f,
                0.5f,  0.5f,  0.5f,  1.0f, 1.0f,
                0.5f,  0.5f,  0.5f,  1.0f, 1.0f,
                -0.5f,  0.5f,  0.5f,  0.0f, 1.0f,
                -0.5f,  0.5f, -0.5f,  0.0f, 0.0f
        )

        internal val vertexStride = COORDS_PER_VERTEX * 4
        internal val vertexCount = cubeCoords.size/COORDS_PER_VERTEX
        private val textures = IntArray(1)
        private const val mPositionDataSize = 3
        private const val mTexDataSize = 2
    }

    private val VBO: FloatBuffer
    private val EBO: ShortBuffer

    private var aPosHandle: Int
    private var aTexHandle: Int
    private var uModelHandle: Int
    private var uViewHandle: Int
    private var uProjectionHandle: Int

    private var degrees = 0f

    private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3) // order to draw vertices
    internal var color = floatArrayOf(0.63671875f, 0.76953125f, 0.22265625f, 1.0f)


    private val vertexShaderCode = "uniform mat4 model;" +
            "uniform mat4 view;" +
            "uniform mat4 projection;" +
            "attribute vec4 vPosition;" +
            "attribute vec2 aTexCoord;" +
            "varying vec2 TexCoord;" +
            "void main() {" +
            "  gl_Position = projection * view * model * vPosition;" +
            "  TexCoord = aTexCoord;" +
            "}"

    private val fragmentShaderCode = "precision mediump float;" +
            "varying vec2 TexCoord;" +
            "uniform sampler2D texture1;" +
            "void main() {" +
            "  gl_FragColor = texture2D(texture1, TexCoord);" +
            "}"

    internal var mProgram: Int = 0

    init {
        // initialize vertex byte buffer for shape coordinates
        val bb = ByteBuffer.allocateDirect(cubeCoords.size * 4) // (# of coordinate values * 4 bytes per float)
        bb.order(ByteOrder.nativeOrder())
        VBO = bb.asFloatBuffer()
        VBO.put(cubeCoords)
        VBO.position(0)

        // initialize byte buffer for the draw list
        val dlb = ByteBuffer.allocateDirect(drawOrder.size * 2) // (# of coordinate values * 2 bytes per short)
        dlb.order(ByteOrder.nativeOrder())
        EBO = dlb.asShortBuffer()
        EBO.put(drawOrder)
        EBO.position(0)

        // position attribute
        GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, vertexStride, VBO)
        GLES20.glEnableVertexAttribArray(0)
        // texture attribute
        GLES20.glVertexAttribPointer(1, 3, GLES20.GL_FLOAT, false, vertexStride, 3)
        GLES20.glEnableVertexAttribArray(1)

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        mProgram = GLES20.glCreateProgram()             // create empty OpenGL ES Program
        GLES20.glAttachShader(mProgram, vertexShader)   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader) // add the fragment shader to program

        // Bind attributes
        GLES20.glBindAttribLocation(mProgram, 0, "vPosition")
        GLES20.glBindAttribLocation(mProgram, 1, "aTexCoord")

        GLES20.glLinkProgram(mProgram)                  // creates OpenGL ES program executables

        // get handle to vertex shader's vPosition member
        aPosHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
        // get handle to fragment shader's vColor member
        aTexHandle = GLES20.glGetAttribLocation(mProgram, "aTexCoord")
        // get handle to mvp matrix uniform
        uModelHandle = GLES20.glGetUniformLocation(mProgram, "model")
        uViewHandle = GLES20.glGetUniformLocation(mProgram, "view")
        uProjectionHandle = GLES20.glGetUniformLocation(mProgram, "projection")

        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram)

        generateTextures()
    }

    private fun loadShader(type: Int, shaderCode: String): Int {

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        val shader = GLES20.glCreateShader(type)

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        return shader
    }

    fun draw(modelMatrix: FloatArray, viewMatrix: FloatArray, projectionMatrix: FloatArray) {

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(aPosHandle)

        VBO.position(0)
        GLES20.glVertexAttribPointer(aPosHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
            vertexStride, VBO)
        GLES20.glEnableVertexAttribArray(aPosHandle)

        VBO.position(3)
        GLES20.glVertexAttribPointer(aTexHandle, mTexDataSize, GLES20.GL_FLOAT, false,
            vertexStride, VBO)
        GLES20.glEnableVertexAttribArray(aTexHandle)

        val rotateMatrix = FloatArray(16)
        Matrix.setIdentityM(rotateMatrix, 0)
        Matrix.rotateM(rotateMatrix, 0, degrees, 0.5f, 1f, 0f)
        degrees += 2
        if (degrees >= 360f) { degrees = 0f }

        // get handle to shape's transformation matrix
        uModelHandle = GLES20.glGetUniformLocation(mProgram, "model")
        uViewHandle = GLES20.glGetUniformLocation(mProgram, "view")
        uProjectionHandle = GLES20.glGetUniformLocation(mProgram, "projection")
        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(uModelHandle, 1, false, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(uViewHandle, 1, false, viewMatrix, 0)
        GLES20.glUniformMatrix4fv(uProjectionHandle, 1, false, projectionMatrix, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36)

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(aPosHandle)
    }

    private fun generateTextures() {
        //generate texture
        GLES20.glGenTextures(1, textures, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])

        //set texture wrapping parameters
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)

        //set texture filtering parameters
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        //get our texture bitmap
        val bo = BitmapFactory.Options()
        val tex = BitmapFactory.decodeResource(context.resources, R.drawable.bg, bo)

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, tex, 0)
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
        tex.recycle()

        if(textures[0] == 0){
            Log.d("Textures", "Error loading texture")
        }
    }
}