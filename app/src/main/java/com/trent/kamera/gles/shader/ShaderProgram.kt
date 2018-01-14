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
package com.trent.kamera.gles.shader

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.support.annotation.WorkerThread
import android.util.Log
import com.trent.kamera.gles.GlUtil
import com.trent.kamera.gles.shader.GLSLVars.aPosition
import com.trent.kamera.gles.shader.GLSLVars.aTextureCoord
import com.trent.kamera.gles.shader.GLSLVars.sTexture
import com.trent.kamera.gles.shader.GLSLVars.uMVPMatrix
import com.trent.kamera.gles.shader.GLSLVars.uTexMatrix
import java.nio.FloatBuffer

@WorkerThread
class ShaderProgram {
    private val TAG = ShaderProgram::class.java.simpleName

    // Handles to the GL program and various components of it.
    protected var programHandle: Int = 0

    private val vertexShader: Shader = Shader.VERTEX
    private val fragmentShader: Shader = Shader.FRAGMENT_EXT

    // Vertex Shader Variables
    // uniform
    private var uMVPMatrixLoc: Int = 0
    private var uTexMatrixLoc: Int = 0
    // attribute
    private var aPositionLoc: Int = 0
    private var aTextureCoordLoc: Int = 0

    // Fragment Shader Variables
    // texture / uniform
    private var sTextureLoc: Int = 0

    init {
        @Suppress("LeakingThis")
        programHandle = GlUtil.createProgram(vertexShader.code, fragmentShader.code)

        if (programHandle == 0) {
            throw RuntimeException("Unable to create program")
        }
        Log.d(TAG, "Created program ${this.javaClass.simpleName}")

        aPositionLoc = GLES20.glGetAttribLocation(programHandle, aPosition)
        GlUtil.checkLocation(aPositionLoc, aPosition)
        aTextureCoordLoc = GLES20.glGetAttribLocation(programHandle, aTextureCoord)
        GlUtil.checkLocation(aTextureCoordLoc, aTextureCoord)
        uMVPMatrixLoc = GLES20.glGetUniformLocation(programHandle, uMVPMatrix)
        GlUtil.checkLocation(uMVPMatrixLoc, uMVPMatrix)
        uTexMatrixLoc = GLES20.glGetUniformLocation(programHandle, uTexMatrix)
        GlUtil.checkLocation(uTexMatrixLoc, uTexMatrix)
        sTextureLoc = GLES20.glGetUniformLocation(programHandle, sTexture)
        GlUtil.checkLocation(sTextureLoc, sTexture)
    }

    /**
     * Issues the draw call.  Does the full setup on every call.
     *
     * @param mvpMatrix       The 4x4 projection matrix.
     * @param vertexBuffer    Buffer with vertex position data.
     * @param firstVertex     Index of first vertex to use in vertexBuffer.
     * @param vertexCount     Number of vertices in vertexBuffer.
     * @param coordsPerVertex The number of coordinates per vertex (e.g. x,y is 2).
     * @param vertexStride    Width, in bytes, of the position data for each vertex (often
     * vertexCount * sizeof(float)).
     * @param texMatrix       A 4x4 transformation matrix for texture coords.  (Primarily intended
     * for use with SurfaceTexture.)
     * @param texBuffer       Buffer with vertex texture data.
     * @param texStride       Width, in bytes, of the texture data for each vertex.
     */
    @WorkerThread
    fun draw(mvpMatrix: FloatArray, vertexBuffer: FloatBuffer, firstVertex: Int,
             vertexCount: Int, coordsPerVertex: Int, vertexStride: Int,
             texMatrix: FloatArray, texBuffer: FloatBuffer, textureId: Int, texStride: Int) {
        GlUtil.checkGlError("draw start")

        // Select the program.
        GLES20.glUseProgram(programHandle)
        GlUtil.checkGlError("glUseProgram")

        // Set the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glUniform1i(sTextureLoc, 0)

        // Copy the model / view / projection matrix over.
        GLES20.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, mvpMatrix, 0)
        GlUtil.checkGlError("glUniformMatrix4fv")

        // Copy the texture transformation matrix over.
        GLES20.glUniformMatrix4fv(uTexMatrixLoc, 1, false, texMatrix, 0)
        GlUtil.checkGlError("glUniformMatrix4fv")

        // Enable the "aPosition" vertex attribute.
        GLES20.glEnableVertexAttribArray(aPositionLoc)
        GlUtil.checkGlError("glEnableVertexAttribArray")

        // Connect vertexBuffer to "aPosition".
        GLES20.glVertexAttribPointer(aPositionLoc, coordsPerVertex,
                GLES20.GL_FLOAT, false, vertexStride, vertexBuffer)
        GlUtil.checkGlError("glVertexAttribPointer")

        // Enable the "aTextureCoord" vertex attribute.
        GLES20.glEnableVertexAttribArray(aTextureCoordLoc)
        GlUtil.checkGlError("glEnableVertexAttribArray")

        // Connect texBuffer to "aTextureCoord".
        GLES20.glVertexAttribPointer(aTextureCoordLoc, 2,
                GLES20.GL_FLOAT, false, texStride, texBuffer)
        GlUtil.checkGlError("glVertexAttribPointer")

        // Draw the rect.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, firstVertex, vertexCount)
        GlUtil.checkGlError("glDrawArrays")

        // Done -- disable vertex array, texture, and program.
        GLES20.glDisableVertexAttribArray(aPositionLoc)
        GLES20.glDisableVertexAttribArray(aTextureCoordLoc)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)

        GLES20.glUseProgram(0)
    }

    /**
     * Releases the program.
     *
     *
     * The appropriate EGL context must be current (i.e. the one that was used to create
     * the program).
     */
    @WorkerThread
    fun release() {
        Log.d(TAG, "deleting program " + programHandle)

        GLES20.glDeleteProgram(programHandle)
        programHandle = -1
    }

    companion object {
        /**
         * Creates a texture object suitable for use with this program.
         *
         *
         * On exit, the texture will be bound.
         */
        @WorkerThread
        fun createTextureObject(): Int {
            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            GlUtil.checkGlError("glGenTextures")

            val texId = textures[0]
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texId)
            GlUtil.checkGlError("glBindTexture " + texId)

            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_NEAREST.toFloat())
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR.toFloat())
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
                    GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
                    GLES20.GL_CLAMP_TO_EDGE)
            GlUtil.checkGlError("glTexParameter")

            return texId
        }
    }
}