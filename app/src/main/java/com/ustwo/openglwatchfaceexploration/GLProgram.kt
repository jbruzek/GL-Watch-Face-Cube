package com.ustwo.openglwatchfaceexploration

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLU
import android.opengl.GLUtils
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.FloatBuffer

/**
 * openGL Program.
 *
 * Code gathered as a combination of the Android example code and GL-2D-watchface
 */
class GLProgram(context: Context, vertexAssetFilename: String, fragmentAssetFilename: String) {

    companion object {
        // Flag to set if we want to check for errors
        // error checking is slow and should not be done on a production build
        const val CHECK_GL_ERRORS = false
    }

    //Shaders
    private val vertexShader: Int
    private val fragmentShader: Int

    //The GL Program object
    private val program: Int

    // Handle for uMvpMatrix uniform in vertex shader
    private val mvpMatrixHandle: Int

    // Handle for aPosition attribute in vertex shader
    private val positionHandle: Int

    // Handle for uColor uniform in fragment shader
    private val colorHandle: Int

    /**
     * Constructor
     */
    init {
        try {
            //Build string versions of the vertex and fragment shaders
            val vertexString = readStringAsset(context, vertexAssetFilename)
            val fragmentString = readStringAsset(context, fragmentAssetFilename)

            //build the shaders from the strings
            vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexString)
            fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentString)
        } catch (ioe: IOException) {
            throw RuntimeException(ioe)
        }

        //Create the program
        program = GLES20.glCreateProgram()
        if (CHECK_GL_ERRORS) checkGlError("glCreateProgram")
        if (program == 0) {
            throw IllegalStateException("glCreateProgram failed")
        }

        // Add the shaders to the program.
        GLES20.glAttachShader(program, vertexShader)
        if (CHECK_GL_ERRORS) checkGlError("glAttachShader")
        GLES20.glAttachShader(program, fragmentShader)
        if (CHECK_GL_ERRORS) checkGlError("glAttachShader")

        // Link the program so it can be executed.
        GLES20.glLinkProgram(program)
        if (CHECK_GL_ERRORS) checkGlError("glLinkProgram")

        // Get a handle to the uMvpMatrix uniform in the vertex shader.
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMvpMatrix")
        if (CHECK_GL_ERRORS) checkGlError("glGetUniformLocation")

        // Get a handle to the vertex shader's aPosition attribute.
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        if (CHECK_GL_ERRORS) checkGlError("glGetAttribLocation")

        // Enable vertex array (VBO).
        GLES20.glEnableVertexAttribArray(positionHandle)
        if (CHECK_GL_ERRORS) checkGlError("glEnableVertexAttribArray")

        // Get a handle to fragment shader's uColor uniform.
        colorHandle = GLES20.glGetUniformLocation(program, "uColor")
        if (CHECK_GL_ERRORS) checkGlError("glGetUniformLocation")
    }

    /**
     * Tells OpenGL to use this program. Call this method before drawing
     */
    fun use() {
        GLES20.glUseProgram(program)
        if (CHECK_GL_ERRORS) checkGlError("glUseProgram")
    }

    /**
     *  Sends the given MVP matrix, vertex data, and color to OpenGL.
     */
    fun bind(mvpMatrix: FloatArray, vertexBuffer: FloatBuffer, color: FloatArray, coordsPerVertex: Int, vertexStride: Int) {
        // Pass the MVP matrix to OpenGL.
        GLES20.glUniformMatrix4fv(
            mvpMatrixHandle, 1 /* count */, false /* transpose */,
            mvpMatrix, 0 /* offset */
        )
        if (CHECK_GL_ERRORS) checkGlError("glUniformMatrix4fv")

        // Pass the VBO with the triangle list's vertices to OpenGL.
        GLES20.glEnableVertexAttribArray(positionHandle)
        if (CHECK_GL_ERRORS) checkGlError("glEnableVertexAttribArray")
        GLES20.glVertexAttribPointer(
            positionHandle, coordsPerVertex, GLES20.GL_FLOAT,
            false /* normalized */, vertexStride, vertexBuffer
        )
        if (CHECK_GL_ERRORS) checkGlError("glVertexAttribPointer")

        // Pass the triangle list's color to OpenGL.
        GLES20.glUniform4fv(colorHandle, 1 /* count */, color, 0 /* offset */)
        if (CHECK_GL_ERRORS) checkGlError("glUniform4fv")
    }

    /**
     * Reads an asset file into a string, returning the resulting string
     * @param context The asset's context
     * @param asset The name of the asset
     * @return The string if successful
     */
    @Throws(IOException::class)
    fun readStringAsset(context: Context, asset: String): String {
        val stringBuilder = StringBuilder()
        val inputStream = context.assets.open(asset)
        val bufferedReader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
        var line: String? = bufferedReader.readLine()
        while (line != null) {
            stringBuilder.append(line)
            stringBuilder.append('\n')
            line = bufferedReader.readLine()
        }
        return stringBuilder.toString()
    }

    /**
     * Compiles an OpenGL shader.
     *
     * @param type [GLES20.GL_VERTEX_SHADER] or [GLES20.GL_FRAGMENT_SHADER]
     * @param shaderCode string containing the shader source code
     * @return ID for the shader
     */
    private fun loadShader(type: Int, shaderCode: String): Int {
        // Create a vertex or fragment shader.
        val shader = GLES20.glCreateShader(type)
        if (CHECK_GL_ERRORS) checkGlError("glCreateShader")
        if (shader == 0) {
            throw IllegalStateException("glCreateShader failed")
        }

        // Add the source code to the shader and compile it.
        GLES20.glShaderSource(shader, shaderCode)
        if (CHECK_GL_ERRORS) checkGlError("glShaderSource")
        GLES20.glCompileShader(shader)
        if (CHECK_GL_ERRORS) checkGlError("glCompileShader")

        return shader
    }

    /**
     * Checks if any of the GL calls since the last time this method was called set an error
     * condition. Call this method immediately after calling a GL method. Pass the name of the GL
     * operation. For example:
     *
     * <pre>
     * colorHandle = GLES20.glGetUniformLocation(mProgram, "uColor");
     * MyGLRenderer.checkGlError("glGetUniformLocation");</pre>
     *
     * If the operation is not successful, the check throws an exception.
     *
     *
     * *Note* This is quite slow so it's best to use it sparingly in production builds.
     *
     * @param glOperation name of the OpenGL call to check
     */
    private fun checkGlError(glOperation: String) {
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            var errorString: String? = GLU.gluErrorString(error)
            if (errorString == null) {
                errorString = GLUtils.getEGLErrorString(error)
            }
            val message = glOperation + " caused GL error 0x" + Integer.toHexString(error) +
                    ": " + errorString
            Log.e("GL_Error", message)
            throw RuntimeException(message)
        }
    }
}