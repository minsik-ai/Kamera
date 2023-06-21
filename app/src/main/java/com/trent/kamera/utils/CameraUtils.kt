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
@file:Suppress("DEPRECATION")

package com.trent.kamera.utils

import android.hardware.Camera
import android.util.Log
import com.trent.kamera.customtypes.Dimens

/**
 * Camera-related utility functions.
 */
object CameraUtils {
    private val TAG = CameraUtils::class.java.simpleName

    /**
     * Attempts to find a preview size that matches the provided width and height (which
     * specify the dimensions of the encoded video).  If it fails to find a match it just
     * uses the default preview size for video.
     */
    fun choosePreviewSize(params: Camera.Parameters, cameraRotation: Int, windowDimens: Dimens) {
        // We should make sure that the requested MPEG size is less than the preferred
        // size, and has the same aspect ratio.
        val ppsfv = params.preferredPreviewSizeForVideo?.apply {
            Log.d(TAG, "Camera preferred preview size for video is " +
                    width + "x" + height)
        }

        params.supportedPreviewSizes.forEach { size -> Log.d(TAG, "supported: " + size.width + "x" + size.height) }

        for (size in params.supportedPreviewSizes) {

            val portraitDimens =
                    when (cameraRotation) {
                        90, 270 -> {
                            Dimens(size.height, size.width)
                        }
                        0, 180 -> {
                            Dimens(size.width, size.height)
                        }
                        else -> {
                            Dimens(size.width, size.height)
                        }
                    }

            if (portraitDimens.ratio > windowDimens.ratio) {
                // fit width - max preview
                if (portraitDimens.width <= windowDimens.width) {
                    Log.d(TAG, "Trying preview size : " + size.width + "x" + size.height)
                    params.setPreviewSize(size.width, size.height)
                    return
                }
            } else {
                // fit height - max preview
                if (portraitDimens.height <= windowDimens.height) {
                    Log.d(TAG, "Trying preview size : " + size.width + "x" + size.height)
                    params.setPreviewSize(size.width, size.height)
                    return
                }
            }
        }

        Log.w(TAG, "Unable to set preview size to " + windowDimens.width + "x" + windowDimens.height)
        ppsfv?.let {
            params.setPreviewSize(it.width, it.height)
        }
        // else use whatever the default size is
    }

    /**
     * Attempts to find a fixed preview frame rate that matches the desired frame rate.
     *
     * @return The expected frame rate, in thousands of frames per second.
     */
    fun chooseFixedPreviewFps(parms: Camera.Parameters, desiredThousandFps: Int): Int {
        val supported = parms.supportedPreviewFpsRange

        for (entry in supported) {
            //Log.d(TAG, "entry: " + entry[0] + " - " + entry[1]);
            if (entry[0] == entry[1] && entry[0] == desiredThousandFps) {
                parms.setPreviewFpsRange(entry[0], entry[1])
                return entry[0]
            }
        }

        val tmp = IntArray(2)
        parms.getPreviewFpsRange(tmp)
        val guess: Int
        if (tmp[0] == tmp[1]) {
            guess = tmp[0]
        } else {
            guess = tmp[1] / 2     // shrug
        }

        Log.d(TAG, "Couldn't find match for $desiredThousandFps, using $guess")
        return guess
    }
}
