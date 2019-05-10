package com.ustwo.openglwatchfaceexploration

import android.content.Intent
import android.content.IntentFilter
import android.opengl.GLES20
import android.opengl.Matrix
import android.support.wearable.watchface.Gles2WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.util.Log
import android.view.Gravity
import android.view.SurfaceHolder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.*
import java.util.concurrent.TimeUnit


/**
 *  Trying to create my first watchface in openGL
 */
class OpenGLWatchFaceService : Gles2WatchFaceService() {
    private val TAG = "OpenGLWatchFaceService"

    // Expected frame rate in interactive mode.
    private val FPS: Long = 60

    // Z distance from the camera to the watchface.
    private val EYE_Z = -2.3f

    /** Number of bytes to store a float in GL.  */
    val BYTES_PER_FLOAT = 4

    // How long each frame is displayed at expected frame rate.
    private val FRAME_PERIOD_MS = TimeUnit.SECONDS.toMillis(1) / FPS


    private lateinit var program : GLProgram



    // The VBO containing the vertex coordinates
    private lateinit var vertexBuffer: FloatBuffer




    // Model matrix converts Local (Object) Space to World Space
    private val modelMatrix = FloatArray(16)

    //View Matrix converts world space to view space (camera view)
    private val viewMatrix = FloatArray(16)

    //Projection matrix transforms from view space to clip space
    private val projectionMatrix = FloatArray(16)

    //intermediate step matrix, view⋅projection
    private val vpMatrix = FloatArray(16)

    // MVP = Model View Projection
    // Product of the transformation matrices
    // M_mvp = M_projection ⋅ M_view ⋅ M_model (matrix dot products work right to left)
    private val mvpMatrix = FloatArray(16)


    override fun onCreateEngine(): Engine {
        return Engine()
    }

    inner class Engine : Gles2WatchFaceService.Engine() {

        //cube vertices
        val vertices = CubeVertices.data

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

            //initialize the VBO
            val bb = ByteBuffer.allocateDirect(vertices.size * BYTES_PER_FLOAT)

            // Use the device hardware's native byte order.
            bb.order(ByteOrder.nativeOrder())

            // Create a FloatBuffer that wraps the ByteBuffer.
            vertexBuffer = bb.asFloatBuffer()

            // Add the coordinates to the FloatBuffer.
            vertexBuffer.put(vertices)

            // Go back to the start for reading.
            vertexBuffer.position(0)
        }


        override fun onGlContextCreated() {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onGlContextCreated")
            }
            super.onGlContextCreated()

            //configure global gl state
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)

            Matrix.setLookAtM(
                viewMatrix,
                0, // dest index
                0f, 0f, EYE_Z, // eye
                0f, 0f, 0f, // center
                0f, 1f, 0f
            ) // up vector

            //Create our GL Program with our shaders
            program = GLProgram(applicationContext, "vertex.glsl", "fragment.glsl")
        }

        override fun onGlSurfaceCreated(width: Int, height: Int) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onGlSurfaceCreated: $width x $height")
            }
            super.onGlSurfaceCreated(width, height)

            // Update the projection matrix based on the new aspect ratio.
            val aspectRatio = width.toFloat() / height
            Matrix.frustumM(
                projectionMatrix,
                0 /* offset */,
                -aspectRatio /* left */,
                aspectRatio /* right */,
                -1f /* bottom */,
                1f /* top */,
                2f /* near */,
                7f /* far */
            )

            //pre-compute vpMatrix since view and perspective are fixed
            Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
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
                registerReceiver()

                // Update time zone in case it changed while we were detached.
                mCalendar.setTimeZone(TimeZone.getDefault())

                invalidate()
            } else {
                unregisterReceiver()
            }
        }


        private fun registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return
            }
            mRegisteredTimeZoneReceiver = true
            val filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
            this@OpenGLWatchFaceService.registerReceiver(mTimeZoneReceiver, filter)
        }

        private fun unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return
            }
            mRegisteredTimeZoneReceiver = false
            this@OpenGLWatchFaceService.unregisterReceiver(mTimeZoneReceiver)
        }

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
            val vpMatrix: FloatArray

            // Draw background color
            // The background should always be black in ambient mode.
            if (isInAmbientMode) {
                GLES20.glClearColor(0f, 0f, 0f, 1f)
            } else {
                GLES20.glClearColor(0.5f, 0.2f, 0.2f, 1f)
            }

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)


            // Generate full mvp matrix
            Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)






            //draw your stuff
            program.bind(mvpMatrix, vertexBuffer, floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f), CubeVertices.COORDS_PER, CubeVertices.STRIDE)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, CubeVertices.NUM_COORDS)




            // Draw every frame as long as we're visible and in interactive mode.
            if (isVisible && !isInAmbientMode) {
                invalidate()
            }
        }

    }

}


