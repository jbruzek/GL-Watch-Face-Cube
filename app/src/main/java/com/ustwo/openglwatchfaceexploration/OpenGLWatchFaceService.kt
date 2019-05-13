package com.ustwo.openglwatchfaceexploration

import android.opengl.GLES20
import android.opengl.Matrix
import android.support.wearable.watchface.WatchFaceStyle
import android.util.Log
import android.view.Gravity
import android.view.SurfaceHolder


/**
 *  Trying to create my first watchface in openGL
 */
class OpenGLWatchFaceService : Gles2DepthWatchFaceService() {

    private val TAG = "OpenGLWatchFaceService"

    // Expected frame rate in interactive mode.
    private val FPS: Long = 60

    // Z distance from the camera to the watchface.
    private val EYE_Z = -3.0f

    // Model matrix converts Local (Object) Space to World Space
    private val modelMatrix = FloatArray(16)

    //View Matrix converts world space to view space (camera view)
    private val viewMatrix = FloatArray(16)

    //Projection matrix transforms from view space to clip space
    private val projectionMatrix = FloatArray(16)

    private lateinit var cube: Cube

    private var angle = 0f


    override fun onCreateEngine(): Engine {
        return Engine()
    }

    inner class Engine : Gles2DepthWatchFaceService.Engine() {

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCreate")
            }
            super.onCreate(surfaceHolder)
            setWatchFaceStyle(
                WatchFaceStyle.Builder(this@OpenGLWatchFaceService)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setStatusBarGravity(Gravity.RIGHT or Gravity.TOP)
                    .setHotwordIndicatorGravity(Gravity.LEFT or Gravity.TOP)
                    .setShowSystemUiTime(false)
                    .build()
            )
        }


        override fun onGlContextCreated() {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onGlContextCreated")
            }
            super.onGlContextCreated()

            // Precompute the camera angles.)
            Matrix.setLookAtM(
                viewMatrix,
                0, // dest index
                0f, 0f, EYE_Z, // eye
                0f, 0f, 0f, // center
                0f, 1f, 0f
            ) // up vector

            //configure global gl state
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)

            cube = Cube(this@OpenGLWatchFaceService)
        }

        override fun onGlSurfaceCreated(width: Int, height: Int) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onGlSurfaceCreated: $width x $height")
            }
            super.onGlSurfaceCreated(width, height)

            // Update the projection matrix based on the new aspect ratio.
            val aspectRatio = width.toFloat() / height
            Matrix.perspectiveM(projectionMatrix, 0, 45f, aspectRatio, 0.1f, 100f)
        }


        /**************************************************
                Base Watch Face Lifecycle Functions
         *************************************************/

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onAmbientModeChanged: $inAmbientMode")
            }
            super.onAmbientModeChanged(inAmbientMode)
            invalidate()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onVisibilityChanged: $visible")
            }
            super.onVisibilityChanged(visible)
            if (visible) {
//                registerReceiver()

                // Update time zone in case it changed while we were detached.
//                mCalendar.setTimeZone(TimeZone.getDefault())

                invalidate()
            } else {
//                unregisterReceiver()
            }
        }


//        private fun registerReceiver() {
//            if (mRegisteredTimeZoneReceiver) {
//                return
//            }
//            mRegisteredTimeZoneReceiver = true
//            val filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
//            this@OpenGLWatchFaceService.registerReceiver(mTimeZoneReceiver, filter)
//        }
//
//        private fun unregisterReceiver() {
//            if (!mRegisteredTimeZoneReceiver) {
//                return
//            }
//            mRegisteredTimeZoneReceiver = false
//            this@OpenGLWatchFaceService.unregisterReceiver(mTimeZoneReceiver)
//        }

        override fun onTimeTick() {
            super.onTimeTick()
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onTimeTick: ambient = $isInAmbientMode")
            }
            invalidate()
        }

        /**
         * Draw the watchface
         */
        override fun onDraw() {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "onDraw")
            }
            super.onDraw()

            // Draw background color
            // The background should always be black in ambient mode.
            if (isInAmbientMode) {
                GLES20.glClearColor(0f, 0f, 0f, 1f)
            } else {
                GLES20.glClearColor(0.5f, 0.2f, 1.0f, 1f)
            }

            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)


            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.rotateM(modelMatrix, 0, angle, 0.5f, 1f, 0f)
            angle += 2
            if (angle >= 360) angle = 0f

            cube.draw(modelMatrix, viewMatrix, projectionMatrix)

            // Draw every frame as long as we're visible and in interactive mode.
            if (isVisible && !isInAmbientMode) {
                invalidate()
            }
        }

    }

}


