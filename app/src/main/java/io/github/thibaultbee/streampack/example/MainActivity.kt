package io.github.thibaultbee.streampack.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.media.AudioFormat
import android.media.MediaFormat
import android.os.Bundle
import android.util.Log
import android.util.Size
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.github.thibaultbee.streampack.data.AudioConfig
import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.error.StreamPackError
import io.github.thibaultbee.streampack.example.databinding.ActivityMainBinding
import io.github.thibaultbee.streampack.example.utils.PermissionsManager
import io.github.thibaultbee.streampack.example.utils.showDialog
import io.github.thibaultbee.streampack.example.utils.toast
import io.github.thibaultbee.streampack.ext.rtmp.streamers.CameraRtmpLiveStreamer
import io.github.thibaultbee.streampack.ext.srt.streamers.CameraSrtLiveStreamer
import io.github.thibaultbee.streampack.listeners.OnConnectionListener
import io.github.thibaultbee.streampack.listeners.OnErrorListener
import io.github.thibaultbee.streampack.streamers.StreamerLifeCycleObserver
import io.github.thibaultbee.streampack.streamers.helpers.CameraStreamerConfigurationHelper
import io.github.thibaultbee.streampack.utils.TAG
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val streamerRequiredPermissions =
        listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

    @SuppressLint("MissingPermission")
    private val permissionsManager = PermissionsManager(this,
        streamerRequiredPermissions,
        onAllGranted = { inflateStreamer() },
        onShowPermissionRationale = { permissions, onRequiredPermissionLastTime ->
            // Explain why we need permissions
            showDialog(
                title = "Permissions denied",
                message = "Explain why you need to grant $permissions permissions to stream",
                positiveButtonText = R.string.accept,
                onPositiveButtonClick = { onRequiredPermissionLastTime() },
                negativeButtonText = R.string.denied
            )
        },
        onDenied = {
            showDialog(
                "Permissions denied",
                "You need to grant all permissions to stream",
                positiveButtonText = 0,
                negativeButtonText = 0
            )
        }
    )

    // Reports and manages error with [OnErrorListener]
    private val errorListener = object : OnErrorListener {
        override fun onError(error: StreamPackError) {
            toast("An error occurred: $error")
        }
    }

    // Reports and manages connection events with [OnConnectionListener]
    private val connectionListener = object : OnConnectionListener {
        override fun onFailed(message: String) {
            toast("Connection failed: $message")
        }

        override fun onLost(message: String) {
            toast("Connection lost: $message")
        }

        override fun onSuccess() {
            toast("Connected")
        }
    }

    /**
     * The streamer is the central object of StreamPack.
     * It is responsible for the capture audio and video and the streaming process.
     *
     * Use a [CameraRtmpLiveStreamer] For RTMP. Or a [CameraSrtLiveStreamer] for SRT.
     */
    private val streamer by lazy {
        CameraRtmpLiveStreamer(
            this,
            initialOnErrorListener = errorListener,
            initialOnConnectionListener = connectionListener
        )
    }

    /**
     * Listen to lifecycle events. So we don't have to stop the streamer manually in `onPause` and release in `onDestroy
     */
    private val streamerLifeCycleObserver by lazy { StreamerLifeCycleObserver(streamer) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.liveButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                /**
                 * Dispatch from main thread is forced to avoid making network call on main thread
                 * with coroutines.
                 */
                lifecycleScope.launch {
                    try {
                        /**
                         * Always lock the device orientation during a live streaming to avoid
                         * to recreate the Activity.
                         */
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
                        /**
                         * For SRT, use srt://my.server.url:9998?streamid=myStreamId&passphrase=myPassphrase
                         */
                        streamer.startStream("rtmp://my.server.url:1234/app/streamKey")
                    } catch (e: Exception) {
                        binding.liveButton.isChecked = false
                        Log.e(TAG, "Failed to connect", e)
                    }
                }
            } else {
                streamer.stopStream()
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    override fun onStart() {
        super.onStart()
        permissionsManager.requestPermissions()
    }

    @RequiresPermission(allOf = [Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO])
    private fun inflateStreamer() {
        lifecycle.addObserver(streamerLifeCycleObserver) // Register the lifecycle observer
        /**
         * Configure the streamer before calling view.streamer. Because it will start camera preview
         * which required a configuration
         */
        configureStreamer()
        binding.preview.streamer = streamer // Bind the streamer to the preview
    }

    private fun configureStreamer() {
        /**
         * To check the parameters supported by the device, you can check parameter against:
         * - [CameraStreamerConfigurationHelper.flvHelper] for RTMP live streaming or
         * - [CameraStreamerConfigurationHelper.tsHelper] for SRT communication.
         */

        /**
         * There are other parameters in the [VideoConfig] such as:
         * - bitrate
         * - profile
         * - level
         * - gopSize
         * They will be initialized with an appropriate default value.
         */
        val videoConfig = VideoConfig(
            mimeType = MediaFormat.MIMETYPE_VIDEO_AVC, resolution = Size(1280, 720), fps = 25
        )

        /**
         * There are other parameters in the [AudioConfig] such as:
         * - byteFormat
         * - enableEchoCanceler
         * - enableNoiseSuppressor
         * They will be initialized with an appropriate default value.
         */
        val audioConfig = AudioConfig(
            mimeType = MediaFormat.MIMETYPE_AUDIO_AAC,
            sampleRate = 44100,
            channelConfig = AudioFormat.CHANNEL_IN_STEREO
        )
        streamer.configure(audioConfig, videoConfig)
    }


    private fun toast(message: String) {
        runOnUiThread { applicationContext.toast(message) }
    }
}