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

import java.nio.FloatBuffer

/**
 * Base class for stuff we like to draw.
 */
open class Drawable2d(private val mPrefab: Prefab) {

    /**
     * Returns the array of vertices.
     *
     *
     * To avoid allocations, this returns internal state.  The caller must not modify it.
     */
    val vertexArray: FloatBuffer


    /**
     * Returns the array of texture coordinates.
     *
     *
     * To avoid allocations, this returns internal state.  The caller must not modify it.
     */
    protected val backingTexCoordArray: FloatBuffer
    open val texCoordArray: FloatBuffer
            get() = backingTexCoordArray

    /**
     * Returns the number of vertices stored in the vertex array.
     */
    val vertexCount: Int

    /**
     * Returns the number of position coordinates per vertex.  This will be 2 or 3.
     */
    val coordsPerVertex: Int

    /**
     * Returns the width, in bytes, of the data for each vertex.
     */
    val vertexStride: Int

    /**
     * Returns the width, in bytes, of the data for each texture coordinate.
     */
    val texCoordStride: Int

    /**
     * Enum values for constructor.
     */
    enum class Prefab {
        TRIANGLE, RECTANGLE, FULL_RECTANGLE
    }

    init {
        when (mPrefab) {
            Drawable2d.Prefab.TRIANGLE -> {
                vertexArray = TRIANGLE_BUF
                backingTexCoordArray = TRIANGLE_TEX_BUF
                coordsPerVertex = 2
                vertexStride = coordsPerVertex * SIZEOF_FLOAT
                vertexCount = TRIANGLE_COORDS.size / coordsPerVertex
            }
            Drawable2d.Prefab.RECTANGLE -> {
                vertexArray = RECTANGLE_BUF
                backingTexCoordArray = RECTANGLE_TEX_BUF
                coordsPerVertex = 2
                vertexStride = coordsPerVertex * SIZEOF_FLOAT
                vertexCount = RECTANGLE_COORDS.size / coordsPerVertex
            }
            Drawable2d.Prefab.FULL_RECTANGLE -> {
                vertexArray = FULL_RECTANGLE_BUF
                backingTexCoordArray = FULL_RECTANGLE_TEX_BUF
                coordsPerVertex = 2
                vertexStride = coordsPerVertex * SIZEOF_FLOAT
                vertexCount = FULL_RECTANGLE_COORDS.size / coordsPerVertex
            }
        }
        texCoordStride = 2 * SIZEOF_FLOAT
    }

    override fun toString(): String {
        return "[Drawable2d: $mPrefab]"
    }

    companion object {
        internal val SIZEOF_FLOAT = 4

        /**
         * Simple equilateral triangle (1.0 per side).  Centered on (0,0).
         */
        private val TRIANGLE_COORDS =
                floatArrayOf(
                        0.0f, 0.577350269f,     // 0 top
                        -0.5f, -0.288675135f,   // 1 bottom left
                        0.5f, -0.288675135f     // 2 bottom right
                )
        private val TRIANGLE_TEX_COORDS =
                floatArrayOf(
                        0.5f, 0.0f,             // 0 top center
                        0.0f, 1.0f,             // 1 bottom left
                        1.0f, 1.0f              // 2 bottom right
                )
        private val TRIANGLE_BUF = GlUtil.createFloatBuffer(TRIANGLE_COORDS)
        private val TRIANGLE_TEX_BUF = GlUtil.createFloatBuffer(TRIANGLE_TEX_COORDS)

        /**
         * Simple square, specified as a triangle strip.  The square is centered on (0,0) and has
         * a size of 1x1.
         *
         *
         * Triangles are 0-1-2 and 2-1-3 (counter-clockwise winding).
         */
        private val RECTANGLE_COORDS =
                floatArrayOf(
                        -0.5f, -0.5f,           // 0 bottom left
                        0.5f, -0.5f,            // 1 bottom right
                        -0.5f, 0.5f,            // 2 top left
                        0.5f, 0.5f              // 3 top right
                )
        private val RECTANGLE_TEX_COORDS =
                floatArrayOf(
                        0.0f, 1.0f,             // 0 bottom left
                        1.0f, 1.0f,             // 1 bottom right
                        0.0f, 0.0f,             // 2 top left
                        1.0f, 0.0f              // 3 top right
                )
        private val RECTANGLE_BUF = GlUtil.createFloatBuffer(RECTANGLE_COORDS)
        private val RECTANGLE_TEX_BUF = GlUtil.createFloatBuffer(RECTANGLE_TEX_COORDS)

        /**
         * A "full" square, extending from -1 to +1 in both dimensions.  When the model/view/projection
         * matrix is identity, this will exactly cover the viewport.
         *
         *
         * The texture coordinates are Y-inverted relative to RECTANGLE.  (This seems to work out
         * right with external textures from SurfaceTexture.)
         */
        private val FULL_RECTANGLE_COORDS =
                floatArrayOf(
                        -1.0f, -1.0f,           // 0 bottom left
                        1.0f, -1.0f,            // 1 bottom right
                        -1.0f, 1.0f,            // 2 top left
                        1.0f, 1.0f              // 3 top right
                )
        private val FULL_RECTANGLE_TEX_COORDS =
                floatArrayOf(
                        0.0f, 0.0f,             // 0 bottom left
                        1.0f, 0.0f,             // 1 bottom right
                        0.0f, 1.0f,             // 2 top left
                        1.0f, 1.0f              // 3 top right
                )
        private val FULL_RECTANGLE_BUF = GlUtil.createFloatBuffer(FULL_RECTANGLE_COORDS)
        private val FULL_RECTANGLE_TEX_BUF = GlUtil.createFloatBuffer(FULL_RECTANGLE_TEX_COORDS)
    }
}
