/*
 * Copyright 2013 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Ported to Kotlin & modified & extended for Kamera
 *
 * @author Trent Oh(devtrentoh@gmail.com)
 */
package com.trent.kamera.gles

import android.opengl.EGL14
import android.util.Log

/**
 * Common base class for EGL surfaces.
 *
 *
 * There can be multiple surfaces associated with a single context.
 */
open class EglSurfaceBase protected constructor(private var mEglCore: EglCore) {
    private val TAG ="EglSurfaceBase"

    private var mEGLSurface = EGL14.EGL_NO_SURFACE
    private var mWidth = -1
    private var mHeight = -1

    /**
     * Returns the surface's width, in pixels.
     *
     *
     * If this is called on a window surface, and the underlying surface is in the process
     * of changing size, we may not see the new size right away (e.g. in the "surfaceChanged"
     * callback).  The size should match after the next buffer swap.
     */
    val width: Int
        get() = if (mWidth < 0) {
            mEglCore.querySurface(mEGLSurface, EGL14.EGL_WIDTH)
        } else {
            mWidth
        }

    /**
     * Returns the surface's height, in pixels.
     */
    val height: Int
        get() = if (mHeight < 0) {
            mEglCore.querySurface(mEGLSurface, EGL14.EGL_HEIGHT)
        } else {
            mHeight
        }

    /**
     * Creates a window surface.
     *
     *
     * @param surface May be a Surface or SurfaceTexture.
     */
    fun createWindowSurface(surface: Any) {
        if (mEGLSurface != EGL14.EGL_NO_SURFACE) {
            throw IllegalStateException("surface already created")
        }
        mEGLSurface = mEglCore.createWindowSurface(surface)
    }

    /**
     * Release the EGL surface.
     */
    fun releaseEglSurface() {
        mEglCore.releaseSurface(mEGLSurface)
        mEGLSurface = EGL14.EGL_NO_SURFACE
        mHeight = -1
        mWidth = mHeight
    }

    /**
     * Makes our EGL context and surface current.
     */
    fun makeCurrent() {
        mEglCore.makeCurrent(mEGLSurface)
    }

    /**
     * Calls eglSwapBuffers.  Use this to "publish" the current frame.
     *
     * @return false on failure
     */
    fun swapBuffers(): Boolean {
        val result = mEglCore.swapBuffers(mEGLSurface)
        if (!result) {
            Log.d(TAG, "WARNING: swapBuffers() failed")
        }
        return result
    }

}
