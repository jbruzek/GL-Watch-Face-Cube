package com.ustwo.openglwatchfaceexploration

import android.content.Context
import android.opengl.GLES20
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import android.opengl.GLUtils
import android.opengl.GLU
import android.util.Log


class Utils {
    companion object {
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

        fun loadShader(type: Int, shaderCode: String): Int {

            // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
            // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
            val shader = GLES20.glCreateShader(type)

            // add the source code to the shader and compile it
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)

            return shader
        }

        fun checkGlError(glOperation: String) {
            if (!isDebug) {
                return
            }
            val error = GLES20.glGetError()
            if (error != GLES20.GL_NO_ERROR) {
                var errorString: String? = GLU.gluErrorString(error)
                if (errorString == null) {
                    errorString = GLUtils.getEGLErrorString(error)
                }
                val message = glOperation + " caused GL error 0x" + Integer.toHexString(error) +
                        ": " + errorString
                Log.e("GL-Error", message)
                throw RuntimeException(message)
            }
        }

        fun setDebug(debug: Boolean) {
            isDebug = debug
        }

        private var isDebug = true
    }


}