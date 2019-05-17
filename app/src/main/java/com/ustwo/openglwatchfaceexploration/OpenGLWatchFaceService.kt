package com.ustwo.openglwatchfaceexploration

import android.graphics.*
import android.opengl.GLES20
import android.opengl.Matrix
import android.support.wearable.watchface.WatchFaceStyle
import android.util.Log
import android.view.Gravity
import android.view.SurfaceHolder
import java.lang.invoke.MutableCallSite
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


/**
 *  Trying to create my first watchface in openGL
 */
class OpenGLWatchFaceService : Gles2DepthWatchFaceService() {

    private val TAG = "OpenGLWatchFaceService"

    // Expected frame rate in interactive mode.
    private val FPS: Long = 60

    // Z distance from the camera to the watchface.
    private val EYE_Z = -5.0f

    // Model matrix converts Local (Object) Space to World Space
    private val modelMatrix = FloatArray(16)

    //View Matrix converts world space to view space (camera view)
    private val viewMatrix = FloatArray(16)
    private val viewMatrixSkyBox = FloatArray(16)

    //Projection matrix transforms from view space to clip space
    private val projectionMatrix = FloatArray(16)

    private lateinit var cube: TextureCube
    private lateinit var skybox: SkyBox

    private var angle = 0f

    private val timeHeight = 40
    private val timeWidth = 40
    private val timeBitmap: Bitmap = Bitmap.createBitmap(timeWidth, timeHeight, Bitmap.Config.ARGB_8888)
    private val timeCanvas: Canvas = Canvas(timeBitmap)
    private val mTimePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mTimeFormat12 = SimpleDateFormat("hh:mm", Locale.getDefault())
    private val calendar: Calendar = Calendar.getInstance()
    private var timeString: String = ""
    private val mTimeFormat24 = SimpleDateFormat("HH:mm", Locale.getDefault())

    private val cubePositions = arrayOf(
        floatArrayOf( 0.0f,  0.0f,  0.0f),
        floatArrayOf( 2.0f,  5.0f, -15.0f),
        floatArrayOf(-1.5f, -2.2f, -2.5f),
        floatArrayOf(-3.8f, -2.0f, -12.3f),
        floatArrayOf (2.4f, -0.4f, -3.5f),
        floatArrayOf(-1.7f,  3.0f, -7.5f),
        floatArrayOf( 1.3f, -2.0f, -2.5f),
        floatArrayOf( 1.5f,  2.0f, -2.5f),
        floatArrayOf( 1.5f,  0.2f, -1.5f),
        floatArrayOf(-1.3f,  1.0f, -1.5f),
        floatArrayOf( 2.0f,  5.0f, 15.0f),
        floatArrayOf(-1.5f, -2.2f, 2.5f),
        floatArrayOf(-3.8f, -2.0f, 12.3f),
        floatArrayOf (2.4f, -0.4f, 3.5f),
        floatArrayOf(-1.7f,  3.0f, 7.5f),
        floatArrayOf( 1.3f, -2.0f, 2.5f),
        floatArrayOf( 1.5f,  2.0f, 2.5f),
        floatArrayOf( 1.5f,  0.2f, 1.5f),
        floatArrayOf(-1.3f,  1.0f, 1.5f),

        floatArrayOf(-15.0f,  2.0f,  5.0f),
        floatArrayOf(-2.5f , -1.5f, -2.2f),
        floatArrayOf(-12.3f, -3.8f, -2.0f),
        floatArrayOf( -3.5f , 2.4f, -0.4f),
        floatArrayOf(-7.5f , -1.7f,  3.0f),
        floatArrayOf(-2.5f ,  1.3f, -2.0f),
        floatArrayOf(-2.5f ,  1.5f,  2.0f),
        floatArrayOf(-1.5f ,  1.5f,  0.2f),
        floatArrayOf(-1.5f , -1.3f,  1.0f),
        floatArrayOf(15.0f ,  2.0f,  5.0f),
        floatArrayOf(2.5f  , -1.5f, -2.2f),
        floatArrayOf(12.3f , -3.8f, -2.0f),
        floatArrayOf( 3.5f  , 2.4f, -0.4f),
        floatArrayOf(7.5f  , -1.7f,  3.0f),
        floatArrayOf(2.5f  ,  1.3f, -2.0f),
        floatArrayOf(2.5f  ,  1.5f,  2.0f),
        floatArrayOf(1.5f  ,  1.5f,  0.2f),
        floatArrayOf(1.5f  , -1.3f,  1.0f),

        floatArrayOf( 2.0f, -15.0f,  5.0f ),
        floatArrayOf(-1.5f, -2.5f , -2.2f ),
        floatArrayOf(-3.8f, -12.3f, -2.0f ),
        floatArrayOf( 2.4f, -3.5f , -0.4f ),
        floatArrayOf(-1.7f, -7.5f ,  3.0f ),
        floatArrayOf( 1.3f, -2.5f , -2.0f ),
        floatArrayOf( 1.5f, -2.5f ,  2.0f ),
        floatArrayOf( 1.5f, -1.5f ,  0.2f ),
        floatArrayOf(-1.3f, -1.5f ,  1.0f ),
        floatArrayOf( 2.0f, 15.0f ,  5.0f ),
        floatArrayOf(-1.5f, 2.5f  , -2.2f ),
        floatArrayOf(-3.8f, 12.3f , -2.0f ),
        floatArrayOf( 2.4f, 3.5f  , -0.4f ),
        floatArrayOf(-1.7f, 7.5f  ,  3.0f ),
        floatArrayOf( 1.3f, 2.5f  , -2.0f ),
        floatArrayOf( 1.5f, 2.5f  ,  2.0f ),
        floatArrayOf( 1.5f, 1.5f  ,  0.2f ),
        floatArrayOf(-1.3f, 1.5f  ,  1.0f )
    )

    private fun getTimeFormat(): DateFormat {
        return mTimeFormat12
//        return if (android.text.format.DateFormat.is24HourFormat(this@OpenGLWatchFaceService)) mTimeFormat24 else mTimeFormat12
    }


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

        private fun updateTimeIfChanged(newTimeString: String) {
            if (newTimeString == timeString) {
                return
            }
            timeString = newTimeString
            timeCanvas.drawColor(Color.WHITE)
            val xOffset = mTimePaint.measureText(timeString) / 2
            val pixelsFromBaselineToCenterOfText = (mTimePaint.descent() + mTimePaint.ascent()) / 2
            timeCanvas.drawText(
                timeString,
                timeWidth.toFloat() / 2 - xOffset,
                timeHeight / 2 - pixelsFromBaselineToCenterOfText,
                mTimePaint
            )
            cube.setTexture(timeBitmap)
        }


        override fun onGlContextCreated() {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onGlContextCreated")
            }
            super.onGlContextCreated()

            Matrix.setLookAtM(
            viewMatrix,
            0, // dest index
            0f, 0f, EYE_Z, // eye
            0f, 0f, 0f, // center
            0f, 1f, 0f
            ) // up vector

            //remove translation from the skybox view matrix
            viewMatrix.copyInto(viewMatrixSkyBox)
            viewMatrixSkyBox[3] = 0.0f
            viewMatrixSkyBox[7] = 0.0f
            viewMatrixSkyBox[11] = 0.0f
            viewMatrixSkyBox[12] = 0.0f
            viewMatrixSkyBox[13] = 0.0f
            viewMatrixSkyBox[14] = 0.0f
            viewMatrixSkyBox[15] = 0.0f

            //configure global gl state
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)

            cube = TextureCube(this@OpenGLWatchFaceService)
            skybox = SkyBox(this@OpenGLWatchFaceService,
                intArrayOf(
                    R.drawable.box_right,
                    R.drawable.box_left,
                    R.drawable.box_top,
                    R.drawable.box_bottom,
                    R.drawable.box_front,
                    R.drawable.box_back))

            updateTimeIfChanged(getTimeFormat().format(calendar.time))
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
            calendar.time = Date(System.currentTimeMillis())
            updateTimeIfChanged(getTimeFormat().format(calendar.time))
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
                GLES20.glClearColor(1f, 0.1f, 0.2f, 1f)
            }

            val radius = 20.0f
            val camX = Math.sin(angle / 200.0) * radius
            val camY = Math.sin(angle / 300.0) * radius / 3
            val camZ = Math.cos(angle / 200.0) * radius - 10
            Matrix.setLookAtM(viewMatrix,0,
                camX.toFloat(), camY.toFloat(), camZ.toFloat(), // eye
                0f, 0f, 0f, // center
                0f, 1f, 0f
            ) // up vector

            //remove translation from the skybox view matrix
            Utils.generateSkyBoxViewMatrix(viewMatrix, viewMatrixSkyBox)

            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)

            //draw a bunch of cubes
            for (translation in cubePositions) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, translation[0], translation[1], translation[2])
                Matrix.rotateM(modelMatrix, 0, angle * translation[0], translation[0], 1f, translation[2])
                cube.draw(modelMatrix, viewMatrix, projectionMatrix)
            }
//            Matrix.setIdentityM(modelMatrix, 0)
//            Matrix.rotateM(modelMatrix, 0, angle, 0.5f, 1f, 0f)
            angle += 2
//            if (angle >= 360) angle = 0f


            //Draw the skybox last
            skybox.draw(viewMatrixSkyBox, projectionMatrix)

            // Draw every frame as long as we're visible and in interactive mode.
            if (isVisible && !isInAmbientMode) {
                invalidate()
            }
        }

    }

}


