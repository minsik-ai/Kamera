/**
 * Copyright 2017 Trent Oh(devtrentoh@gmail.com)
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

package com.trent.kamera.extensions

import android.hardware.Camera
import android.util.Log
import android.view.Surface
import com.trent.kamera.customtypes.Dimens
import com.trent.kamera.utils.CameraUtils

/**
 * Extension methods for Camera class & static utility methods for opening Camera.
 *
 * @author Trent Oh(devtrentoh@gmail.com)
 */
object CameraExtensions {

    const val TAG = "CameraExtensions"

    /**
     * Returns Camera with Rotation in Degrees
     */
    fun openCamera(deviceRotation: Int): Pair<Camera, Int> {
        fun processCameraId(id: Int): Pair<Camera, Int> =
                Pair(Camera.open(id), calculateCameraRotation(id, deviceRotation))

        getCameraId()?.let { id ->
            return processCameraId(id)
        }

        throw RuntimeException("Unable to Open Camera")
    }

    private fun getCameraId(): Int? {
        val info = Camera.CameraInfo()
        val numCameras = Camera.getNumberOfCameras()
        for (i in 0 until numCameras) {
            Camera.getCameraInfo(i, info)
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                return i
            }
        }
        return null
    }

    private fun calculateCameraRotation(cameraId: Int, deviceRotation: Int): Int {
        val info = android.hardware.Camera.CameraInfo()
        android.hardware.Camera.getCameraInfo(cameraId, info)
        val degrees =
                when (deviceRotation) {
                    Surface.ROTATION_0 -> 0
                    Surface.ROTATION_90 -> 90
                    Surface.ROTATION_180 -> 180
                    Surface.ROTATION_270 -> 270
                    else -> {
                        Log.e(TAG, "Wrong deviceRotation value : " + deviceRotation)
                        0
                    }
                }

        val result: Int =
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    val orient = (info.orientation + degrees) % 360
                    (360 - orient) % 360  // compensate the mirror
                } else {  // back-facing
                    (info.orientation - degrees + 360) % 360
                }

        Log.d(TAG, "Rotation : " + result)

        return result
    }
}

fun Camera.setCameraPreviewSize(windowDimens: Dimens, cameraRotation: Int): Dimens {

    val params = parameters
    CameraUtils.choosePreviewSize(params, cameraRotation, windowDimens)
    // TODO : Consider target FPS Setting
    params.setRecordingHint(true)
    parameters = params


    return parameters.previewSize.let {
        Dimens(it.width, it.height)
    }
}

fun Camera.setFocusMode(mode: String) {
    val params = parameters
    params.focusMode = mode
    parameters = params
}
