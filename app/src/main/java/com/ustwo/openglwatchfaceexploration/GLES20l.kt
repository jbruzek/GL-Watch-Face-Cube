package com.ustwo.openglwatchfaceexploration

import android.opengl.GLES20
import android.opengl.GLU
import android.opengl.GLUtils
import android.util.Log

class GLES20l {

    companion object {

        /**
         * Flag for whether or not we are checking for an error. Since error checking is relatively slow, we do not
         * want to be checking for errors in production
         */
        var isDebug = false

        /**
         * Check to see if there has been a GL error. If there has, capture the error message and throw an exception
         * @param operation - a String title for the operation that may or may not have caused an error
         */
        private fun logError(operation: String) {
            val error = GLES20.glGetError()
            if (error != GLES20.GL_NO_ERROR) {
                var errorString: String? = GLU.gluErrorString(error)
                if (errorString == null) {
                    errorString = GLUtils.getEGLErrorString(error)
                }
                val message = operation + " caused GL error 0x" + Integer.toHexString(error) +
                        ": " + errorString
                Log.e("GL-Error", message)
                throw RuntimeException(message)
            }
        }

        /**************************************************************************************************************
         *          GLES20 Methods
         **************************************************************************************************************/

        fun glActiveTexture(texture: Int) {
            GLES20.glActiveTexture(texture)
            if (isDebug) logError("glActiveTexture")
        }

        fun glAttachShader(program: Int, shader: Int) {
            GLES20.glAttachShader(program, shader)
            if (isDebug) logError("glAttachShader")
        }

        fun glBindAttribLocation(program: Int, index: Int, name: String) {
            GLES20.glBindAttribLocation(program, index, name)
            if (isDebug) logError("glBindAttribLocation")
        }

        fun glBindBuffer(target: Int, buffer: Int) {
            GLES20.glBindBuffer(target, buffer)
            if (isDebug) logError("glBindBuffer")
        }

        fun glBindFramebuffer(target: Int, framebuffer: Int) {
            GLES20.glBindFramebuffer(target, framebuffer)
            if (isDebug) logError("glBindFramebuffer")
        }

        fun glBindRenderbuffer(target: Int, renderbuffer: Int) {
            GLES20.glBindRenderbuffer(target, renderbuffer)
            if (isDebug) logError("glBindRenderbuffer")
        }

    }
}