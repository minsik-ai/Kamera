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
package com.trent.kamera.gles.shader

import com.trent.kamera.gles.shader.GLSLVars.aPosition
import com.trent.kamera.gles.shader.GLSLVars.aTextureCoord
import com.trent.kamera.gles.shader.GLSLVars.sTexture
import com.trent.kamera.gles.shader.GLSLVars.uMVPMatrix
import com.trent.kamera.gles.shader.GLSLVars.uTexMatrix
import com.trent.kamera.gles.shader.GLSLVars.vTextureCoord

/**
 * Shaders for OpenGL drawing.
 * Uses Kotlin's String Templates for readability.
 *

 */
internal object GLSLVars {

    const val uMVPMatrix = "uMVPMatrix"
    const val uTexMatrix = "uTexMatrix"
    const val aPosition = "aPosition"
    const val aTextureCoord = "aTextureCoord"

    const val vTextureCoord = "vTextureCoord"

    const val sTexture = "sTexture"
}

internal enum class Shader(val code: String) {

    VERTEX("uniform mat4 $uMVPMatrix;\n" +
            "uniform mat4 $uTexMatrix;\n" +
            "attribute vec4 $aPosition;\n" +
            "attribute vec4 $aTextureCoord;\n" +
            "varying vec2 $vTextureCoord;\n" +
            "void main() {\n" +
            "    gl_Position = $uMVPMatrix * $aPosition;\n" +
            "    $vTextureCoord = ($uTexMatrix * $aTextureCoord).xy;\n" +
            "}\n"
    ),
    FRAGMENT_EXT(
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 $vTextureCoord;\n" +
                    "uniform samplerExternalOES $sTexture;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D($sTexture, $vTextureCoord);\n" +
                    "}\n"
    )
}