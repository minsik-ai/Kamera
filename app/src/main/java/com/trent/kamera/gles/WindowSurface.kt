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

import android.view.Surface

/**
 * Recordable EGL window surface.
 *
 *
 * It's good practice to explicitly release() the surface, preferably from a "finally" block.
 */
class WindowSurface(eglCore: EglCore, private var mSurface: Surface?, private val mReleaseSurface: Boolean) : EglSurfaceBase(eglCore) {

    init {
        mSurface?.let {
            createWindowSurface(it)
        }
    }

    /**
     * Releases any resources associated with the EGL surface (and, if configured to do so,
     * with the Surface as well).
     *
     *
     * Does not require that the surface's EGL context be current.
     */
    fun release() {
        releaseEglSurface()
        if (mReleaseSurface) {
            mSurface?.release()
        }
        mSurface = null
    }
}
