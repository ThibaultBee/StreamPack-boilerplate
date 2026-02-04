package io.github.thibaultbee.streampack.app

import android.content.pm.ActivityInfo

/**
 * Application configuration.
 */
object ApplicationConstants {
    /**
     * Default application orientation.
     * Also set in `AndroidManifest.xml` `android:screenOrientation` attribute.
     */
    const val supportedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
}