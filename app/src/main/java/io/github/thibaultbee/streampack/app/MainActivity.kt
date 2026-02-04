package io.github.thibaultbee.streampack.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.github.thibaultbee.streampack.app.databinding.ActivityMainBinding
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.defaultCameraId
import io.github.thibaultbee.streampack.core.streamers.lifecycle.StreamerActivityLifeCycleObserver
import io.github.thibaultbee.streampack.app.utils.PermissionsManager
import io.github.thibaultbee.streampack.app.utils.showDialog
import io.github.thibaultbee.streampack.app.utils.toast
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(this.application)
    }

    private val streamerRequiredPermissions =
        listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

    /**
     * A minimalist permission manager
     */
    @SuppressLint("MissingPermission")
    private val permissionsManager = PermissionsManager(
        this,
        streamerRequiredPermissions,
        onAllGranted = { onPermissionsGranted() },
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
        })

    /**
     * Listen to lifecycle events. So we don't have to stop the streamer manually in `onPause` and release in `onDestroy
     */
    private val streamerLifeCycleObserver by lazy { StreamerActivityLifeCycleObserver(viewModel.streamer) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bindProperties()
    }

    private fun bindProperties() {
        binding.liveButton.setOnCheckedChangeListener { view, isChecked ->
            if (view.isPressed) {
                if (isChecked) {
                    /**
                     * Dispatch from main thread is forced to avoid making network call on main thread
                     * with coroutines.
                     */
                    lifecycleScope.launch {
                        try {
                            viewModel.startStream()
                        } catch (e: Exception) {
                            binding.liveButton.isChecked = false
                            Log.e(TAG, "Failed to connect", e)
                            toast("Connection failed: ${e.message}")
                        }
                    }
                } else {
                    lifecycleScope.launch {
                        viewModel.stopStream()
                    }
                }
            }
        }

        // Register the lifecycle observer
        lifecycle.addObserver(streamerLifeCycleObserver)

        // Configure the streamer
        configureStreamer()

        // Bind events
        viewModel.closedThrowableLiveData.observe(this) {
            toast("Connection error: ${it.message}")
        }

        viewModel.throwableLiveData.observe(this) {
            toast("Error: ${it.message}")
        }

        viewModel.isStreamingLiveData.observe(this) { isStreaming ->
            if (isStreaming) {
                lockOrientation()
            } else {
                unlockOrientation()
            }
            if (isStreaming) {
                binding.liveButton.isChecked = true
            } else if (viewModel.isTryingConnectionLiveData.value == true) {
                binding.liveButton.isChecked = true
            } else {
                binding.liveButton.isChecked = false
            }
        }

        viewModel.isTryingConnectionLiveData.observe(this) { isWaitingForConnection ->
            if (isWaitingForConnection) {
                binding.liveButton.isChecked = true
            } else if (viewModel.isStreamingLiveData.value == true) {
                binding.liveButton.isChecked = true
            } else {
                binding.liveButton.isChecked = false
            }
        }
    }

    private fun lockOrientation() {
        /**
         * Lock orientation while stream is running to avoid stream interruption if
         * user turns the device.
         * For landscape only mode, set [requireActivity().requestedOrientation] to
         * [ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE].
         */
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
    }

    private fun unlockOrientation() {
        requestedOrientation = ApplicationConstants.supportedOrientation
    }

    override fun onStart() {
        super.onStart()
        permissionsManager.requestPermissions()
    }

    @RequiresPermission(allOf = [Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO])
    private fun onPermissionsGranted() {
        setAVSource()
        setStreamerView()
    }

    @RequiresPermission(allOf = [Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO])
    private fun setAVSource() {
        // Set audio and video sources.
        lifecycleScope.launch {
            viewModel.setAudioSource()
            viewModel.setCameraId(this@MainActivity.defaultCameraId)
        }
    }

    private fun setStreamerView() {
        lifecycleScope.launch {
            binding.preview.setVideoSourceProvider(viewModel.streamer) // Bind the streamer to the preview
        }
    }

    @SuppressLint("MissingPermission")
    private fun configureStreamer() {
        lifecycleScope.launch {
            viewModel.setAudioConfig()
            viewModel.setVideoConfig()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Unregister the lifecycle observer
        lifecycle.removeObserver(streamerLifeCycleObserver)
    }

    private fun toast(message: String) {
        runOnUiThread { applicationContext.toast(message) }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}