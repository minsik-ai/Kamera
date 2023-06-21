/**

 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("DEPRECATION")

package com.trent.kamera.kamera

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.opengl.GLES20
import android.opengl.Matrix
import android.support.annotation.MainThread
import android.support.annotation.WorkerThread
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.trent.kamera.customtypes.Dimens
import com.trent.kamera.customtypes.XY
import com.trent.kamera.extensions.CameraExtensions
import com.trent.kamera.extensions.setCameraPreviewSize
import com.trent.kamera.extensions.setFocusMode
import com.trent.kamera.gles.*
import com.trent.kamera.gles.shader.ShaderProgram
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * SurfaceView logic for camera preview display.
 * This is inspired by TextureFromCameraActivity in Grafika repository.
 * <p>
 * Main goals of design :
 * <p>
 * 1.   Stay as close to idiomatic Kotlin as possible.
 *      Only place I knowingly depart from idiomatic Kotlin is the use of lateinit.
 *      I decided to use lateinit for critical variables to catch bugs faster & fix them,
 *      rather then letting them pass with use of nullables & `?` keyword.
 * <p>
 * 2.   Reduce complexity using single Executor & single Class.
 *      I intentionally designed the class to be flat, to avoid complexity caused by Handlers & RenderThreads.
 *      glExecutor replaces most of their functionality.
 * <p>
 * 3.   Handle all state machines in a separate non-Activity class.
 *      Activity lifecycle & Surface lifecycle doesn't match, so there is a need to account for both of them.
 *      I do this using Android Architecture Components LifecycleObserver, to handle all in one single class.
 *      Grafika implementation handled all this in Activity, which is fine for a sample app, but lacks extensibility.
 * <p>
 * 4.   Overcome possible thread problems using Annotations.
 *      @WorkerThread, @MainThread annotations are strategically placed to avoid GL operations on wrong threads.
 *

 */
internal class KameraPreviewObserver(private val surfaceView: SurfaceView, private val deviceRotation: Int) :
        LifecycleObserver, SurfaceHolder.Callback, SurfaceTexture.OnFrameAvailableListener {
    private val TAG = KameraPreviewObserver::class.java.simpleName

    companion object {
        private var prevSurfaceHolder: SurfaceHolder? = null
    }

    private val rectDrawable = ScaledDrawable2d(Drawable2d.Prefab.RECTANGLE)
    private val rect = Sprite2d(rectDrawable)

    private var glExecutor: ExecutorService? = null
    private lateinit var eglCore: EglCore

    private var camera: Camera? = null
    private var cameraRotation: Int = -1

    private var windowSurface: WindowSurface? = null
    private var texProgram: ShaderProgram? = null

    private lateinit var cameraTexture: SurfaceTexture
    private lateinit var windowSurfaceDimens: Dimens
    private lateinit var cameraPreviewDimens: Dimens

    private lateinit var centerPos: XY

    @MainThread
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun setup() {
        Log.d(TAG, "setup@onCreate")
        surfaceView.holder.addCallback(this)
    }

    @MainThread
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun show() {
        Log.d(TAG, "show@onResume")
        glExecutor = Executors.newSingleThreadExecutor()

        glExecutor?.execute {
            Log.d(TAG, "Init")
            // Init
            eglCore = EglCore()
            CameraExtensions.openCamera(deviceRotation).let { (cam, rotation) ->
                cam.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)
                camera = cam
                cameraRotation = rotation
            }
        }

        prevSurfaceHolder?.let {
            Log.d(TAG, "Previous Surface Available")
            glExecutor?.execute {
                it.surfaceAvailable(false)
            }
        }
    }

    @MainThread
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun hide() {
        Log.d(TAG, "hide@onPause")
        glExecutor?.checkShutdown()?.execute {
            Log.d(TAG, "release execute")
            releaseCamera()
            releaseGL()
            eglCore.release()
            Log.d(TAG, "release done")
        }
        glExecutor?.shutdown()
    }

    @MainThread
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun destroy() {
        Log.d(TAG, "destroy@onDestroy")
    }

    @WorkerThread
    private fun releaseCamera() {
        camera?.let {
            it.stopPreview()
            it.release()
            camera = null
            Log.d(TAG, "Camera Release Done")
        }
    }

    @MainThread
    override fun surfaceCreated(holder: SurfaceHolder?) {
        Log.d(TAG, "surfaceCreated")

        prevSurfaceHolder?.let {
            throw RuntimeException("SurfaceHolder already created!")
        }

        prevSurfaceHolder = holder

        glExecutor?.checkShutdown()?.execute {
            holder?.surfaceAvailable(true)
        }

        // TODO : Log corner case(see grafika)
    }

    @MainThread
    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        Log.d(TAG, "surfaceChanged, width : $width, height : $height")
        glExecutor?.checkShutdown()?.execute {
            finishSurfaceSetup(Dimens(width, height))
        }
    }

    // Runs on arbitary thread(camera)
    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        glExecutor?.checkShutdown()?.execute {
            cameraTexture.updateTexImage()
            draw()
        }
    }

    @WorkerThread
    private fun draw() {
        GlUtil.checkGlError("Draw Start!")

        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        val (width, height) = windowSurfaceDimens

        val displayProjectionMatrix = FloatArray(16)
        Matrix.orthoM(displayProjectionMatrix, 0, 0F, width.toFloat(), 0F, height.toFloat(), -1F, 1F)

        GLES20.glViewport(0, 0, width, height)

        texProgram?.let { program ->
            rect.draw(program, displayProjectionMatrix)
        }

        checkNotNull(windowSurface, { Log.e(TAG, "WindowSurface is Null@draw") })
                .swapBuffers()

        GlUtil.checkGlError("Draw Done!")
    }

    @MainThread
    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        Log.d(TAG, "Surface Destroyed")
        glExecutor?.checkShutdown()?.execute {
            releaseGL()
        }
        prevSurfaceHolder = null
    }

    @WorkerThread
    private fun releaseGL() {
        GlUtil.checkGlError("releaseGl Start!")

        windowSurface?.let {
            it.release()
            windowSurface = null
        }

        texProgram?.let {
            it.release()
            texProgram = null
        }

        GlUtil.checkGlError("releaseGl Done!")

        eglCore.makeNothingCurrent()
    }

    @WorkerThread
    private fun finishSurfaceSetup(windowDimens: Dimens) {
        windowSurfaceDimens = windowDimens
        cameraPreviewDimens = checkNotNull(camera, { Log.e(TAG, "Camera is Null@finishSurfaceSetup") })
                .setCameraPreviewSize(windowSurfaceDimens, cameraRotation)

        Log.d(TAG, "finishSurfaceSetup size= $windowSurfaceDimens, Camera Preview Size : $cameraPreviewDimens")

        val (windowWidth, windowHeight) = windowSurfaceDimens

        centerPos = XY(windowWidth / 2F, windowHeight / 2F)

        updateGeometry()

        Log.d(TAG, "Start Camera Preview")

        checkNotNull(camera, { Log.e(TAG, "Camera is Null@finishSurfaceSetup") }).let {
            // Checked Exception Ignored
            it.setPreviewTexture(cameraTexture)
            it.startPreview()
        }
    }

    @WorkerThread
    private fun updateGeometry() {
        val (windowWidth, windowHeight) = windowSurfaceDimens

        val smallDim = Math.min(windowWidth, windowHeight).toFloat()
        val cameraAspect = cameraPreviewDimens.width.toFloat() / cameraPreviewDimens.height.toFloat()
        // Camera aspect is assumed smaller than window aspect
        val newWidth = Math.round(smallDim)
        val newHeight = Math.round(smallDim * cameraAspect)

        Log.d(TAG, "New Size : $newWidth x $newHeight")

        rect.run {
            fun defaultDimens() = setScale(newWidth.toFloat(), -newHeight.toFloat())
            fun deg90SetScale() = setScale(-newHeight.toFloat(), -newWidth.toFloat())

            when (cameraRotation) {
                0, 180 -> {
                    defaultDimens()
                }
                90, 270 -> {
                    deg90SetScale()
                }
                else -> {
                    Log.d(TAG, "Wrong rotation : $cameraRotation")
                    defaultDimens()
                }
            }
            setRotation(cameraRotation.toFloat())
            setPosition(centerPos.x, centerPos.y)
        }
        rectDrawable.setScale(1F)
    }

    @WorkerThread
    private fun SurfaceHolder.surfaceAvailable(newSurface: Boolean) {
        val surface: Surface = this.surface

        windowSurface = WindowSurface(eglCore, surface, false).apply {
            makeCurrent()
        }

        texProgram = ShaderProgram()

        val textureId = ShaderProgram.createTextureObject()
        textureId.let { id ->
            cameraTexture = SurfaceTexture(id)
            rect.setTexture(id)
        }

        if (!newSurface) {
            Log.d(TAG, "not a new surface")
            checkNotNull(windowSurface, { Log.e(TAG, "windowSurface is Null@surfaceAvailable") }).let {
                finishSurfaceSetup(Dimens(it.width, it.height))
            }
        }

        cameraTexture.setOnFrameAvailableListener(this@KameraPreviewObserver)
    }

    @MainThread
    private fun ExecutorService.checkShutdown(): ExecutorService? {
        if (this.isShutdown) {
            Log.e(TAG, "already shutdown!")
            return null
        }
        return this
    }
}
