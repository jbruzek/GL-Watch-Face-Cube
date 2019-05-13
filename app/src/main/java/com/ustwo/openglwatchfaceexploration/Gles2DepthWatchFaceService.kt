package com.ustwo.openglwatchfaceexploration

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLDisplay
import android.support.wearable.watchface.Gles2WatchFaceService

open class Gles2DepthWatchFaceService : Gles2WatchFaceService() {

    companion object {
        private val CONFIG_ATTRIB_LIST = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, 4,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 16, // this was missing
            EGL14.EGL_NONE
        )
    }

    open inner class Engine : Gles2WatchFaceService.Engine() {

        override fun chooseEglConfig(eglDisplay: EGLDisplay): EGLConfig {
            val numEglConfigs = IntArray(1)
            val eglConfigs = arrayOfNulls<EGLConfig>(1)
            if (!EGL14.eglChooseConfig(
                    eglDisplay,
                    Gles2DepthWatchFaceService.CONFIG_ATTRIB_LIST, 0,
                    eglConfigs, 0, eglConfigs.size,
                    numEglConfigs, 0)) {
                throw RuntimeException("eglChooseConfig failed")
            } else if (numEglConfigs[0] == 0) {
                throw RuntimeException("no matching EGL configs")
            }

            return eglConfigs[0]!!
        }

    }
}