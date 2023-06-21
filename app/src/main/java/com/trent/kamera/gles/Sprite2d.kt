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

import android.opengl.Matrix

import com.trent.kamera.gles.shader.ShaderProgram

/**
 * Base class for a 2d object.  Includes position, scale, rotation, and flat-shaded color.
 */
class Sprite2d(private val mDrawable: Drawable2d) {

    /**
     * Returns the color.
     *
     *
     * To avoid allocations, this returns internal state.  The caller must not modify it.
     */
    private val color: FloatArray = FloatArray(4).apply { this[3] = 1.0f }

    private var mTextureId: Int = -1
    private var mAngle: Float = 0.toFloat()
    private var mScaleX: Float = 0.toFloat()
    private var mScaleY: Float = 0.toFloat()
    private var mPosX: Float = 0.toFloat()
    private var mPosY: Float = 0.toFloat()

    private var mMatrixReady: Boolean = false

    private val mScratchMatrix = FloatArray(16)

    /**
     * Returns the model-view matrix.
     *
     *
     * To avoid allocations, this returns internal state.  The caller must not modify it.
     */
    private val backingMatrix: FloatArray = FloatArray(16)
    private val modelViewMatrix: FloatArray
        get() {
            if (!mMatrixReady) {
                recomputeMatrix()
            }
            return backingMatrix
        }

    /**
     * Re-computes backingMatrix, based on the current values for rotation, scale, and
     * translation.
     */
    private fun recomputeMatrix() {
        val modelView = backingMatrix

        Matrix.setIdentityM(modelView, 0)
        Matrix.translateM(modelView, 0, mPosX, mPosY, 0.0f)
        if (mAngle != 0.0f) {
            Matrix.rotateM(modelView, 0, mAngle, 0.0f, 0.0f, 1.0f)
        }
        Matrix.scaleM(modelView, 0, mScaleX, mScaleY, 1.0f)
        mMatrixReady = true
    }

    /**
     * Sets the sprite scale (size).
     */
    fun setScale(scaleX: Float, scaleY: Float) {
        mScaleX = scaleX
        mScaleY = scaleY
        mMatrixReady = false
    }

    /**
     * Sets the sprite rotation angle, in degrees.  Sprite will rotate counter-clockwise.
     */
    fun setRotation(angle: Float) {
        var value = angle
        // Normalize.  We're not expecting it to be way off, so just iterate.
        while (value >= 360.0f) {
            value -= 360.0f
        }
        while (value <= -360.0f) {
            value += 360.0f
        }
        mAngle = angle
        mMatrixReady = false
    }

    /**
     * Sets the sprite position.
     */
    fun setPosition(posX: Float, posY: Float) {
        mPosX = posX
        mPosY = posY
        mMatrixReady = false
    }

    /**
     * Sets texture to use for textured rendering.  Has no effect on flat-shaded rendering.
     */
    fun setTexture(textureId: Int) {
        mTextureId = textureId
    }

    /**
     * Draws the rectangle with the supplied program and projection matrix.
     */
    fun draw(program: ShaderProgram, projectionMatrix: FloatArray) {

        // Compute model/view/projection matrix.
        Matrix.multiplyMM(mScratchMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)

        program.draw(mScratchMatrix, mDrawable.vertexArray, 0,
                mDrawable.vertexCount, mDrawable.coordsPerVertex,
                mDrawable.vertexStride, GlUtil.IDENTITY_MATRIX, mDrawable.texCoordArray,
                mTextureId, mDrawable.texCoordStride)
    }

    override fun toString(): String {
        return "[Sprite2d pos=" + mPosX + "," + mPosY +
                " scale=" + mScaleX + "," + mScaleY + " angle=" + mAngle +
                " color={" + color[0] + "," + color[1] + "," + color[2] +
                "} drawable=" + mDrawable + "]"
    }
}
