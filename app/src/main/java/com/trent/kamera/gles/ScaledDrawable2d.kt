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

 */
package com.trent.kamera.gles

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Tweaked version of Drawable2d that rescales the texture coordinates to provide a
 * "zoom" effect.
 */
class ScaledDrawable2d(shape: Drawable2d.Prefab) : Drawable2d(shape) {

    private var mScale = 1.0f
    private var mRecalculate: Boolean = true

    private val mTweakedTexCoordArray: FloatBuffer by lazy {
        val bb = ByteBuffer.allocateDirect(backingTexCoordArray.capacity() * SIZEOF_FLOAT)
        bb.order(ByteOrder.nativeOrder())
        return@lazy bb.asFloatBuffer()
    }

    /**
     * Returns the array of texture coordinates.  The first time this is called, we generate
     * a modified version of the array from the parent class.
     *
     *
     * To avoid allocations, this returns internal state.  The caller must not modify it.
     */
    override val texCoordArray: FloatBuffer
        get() {
            if (mRecalculate) {
                val fb = mTweakedTexCoordArray
                val scale = mScale

                val parentBuf = backingTexCoordArray
                val count = parentBuf.capacity()

                for (i in 0 until count) {
                    var fl = parentBuf.get(i)
                    fl = (fl - 0.5f) * scale + 0.5f
                    fb.put(i, fl)
                }

                mRecalculate = false
            }

            return mTweakedTexCoordArray
        }

    /**
     * Set the scale factor.
     */
    fun setScale(scale: Float) {
        if (scale < 0.0f || scale > 1.0f) {
            throw RuntimeException("invalid scale " + scale)
        }
        mScale = scale
        mRecalculate = true
    }

    companion object {
        private val SIZEOF_FLOAT = 4
    }
}
