package io.github.thibaultbee.streampack.app

import android.Manifest
import android.media.AudioFormat
import android.media.MediaFormat
import android.util.Log
import android.util.Size
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import io.github.thibaultbee.streampack.app.data.rotation.RotationRepository
import io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord.MicrophoneSourceFactory
import io.github.thibaultbee.streampack.core.interfaces.releaseBlocking
import io.github.thibaultbee.streampack.core.interfaces.setCameraId
import io.github.thibaultbee.streampack.core.interfaces.startStream
import io.github.thibaultbee.streampack.core.streamers.single.AudioConfig
import io.github.thibaultbee.streampack.core.streamers.single.SingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.VideoConfig
import io.github.thibaultbee.streampack.core.utils.extensions.isClosedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class MainViewModel(
    private val rotationRepository: RotationRepository,
    val streamer: SingleStreamer
) : ViewModel() {
    private val defaultDispatcher = Dispatchers.Default

    /**
     * A LiveData to observe the stream state.
     */
    val isStreamingLiveData: LiveData<Boolean>
        get() = streamer.isStreamingFlow.asLiveData()

    /**
     * A LiveData to observe the pending connection state.
     */
    private val _isTryingConnectionLiveData = MutableLiveData<Boolean>()
    val isTryingConnectionLiveData: LiveData<Boolean> = _isTryingConnectionLiveData

    /**
     * A LiveData to observe async disconnection errors.
     */
    val closedThrowableLiveData: LiveData<Throwable> =
        streamer.throwableFlow.filterNotNull().filter { it.isClosedException }.asLiveData()

    /**
     * A LiveData to observe streamer errors.
     */
    val throwableLiveData: LiveData<Throwable> =
        streamer.throwableFlow.filterNotNull().filter { !it.isClosedException }.asLiveData()

    /** The connection failed to established */
    private val _pendingConnectionFailedFlow = MutableStateFlow<Throwable?>(null)
    val pendingConnectionFailedLiveData: LiveData<Throwable> =
        _pendingConnectionFailedFlow.filterNotNull().asLiveData()

    init {
        /**
         * Listens to device rotation.
         */
        viewModelScope.launch(defaultDispatcher) {
            rotationRepository.rotationFlow.collect {
                streamer.setTargetRotation(it)
            }
        }
    }

    /**
     * Starts the stream.
     *
     * Replace with a valid URL.
     */
    fun startStream() {
        viewModelScope.launch {
            _isTryingConnectionLiveData.postValue(true)
            try {
                /**
                 * For SRT, use srt://my.server.url:9998?streamid=myStreamId&passphrase=myPassphrase
                 */
                streamer.startStream("rtmp://my.server.url:1935/app/streamKey")
            } catch (t: Throwable) {
                _pendingConnectionFailedFlow.emit(t)
            } finally {
                _isTryingConnectionLiveData.postValue(false)
            }
        }
    }

    /**
     * Stops the stream.
     */
    fun stopStream() {
        viewModelScope.launch {
            streamer.stopStream()
        }
    }

    /**
     * Sets the audio configuration.
     *
     * You can verify the device supported configuration with [SingleStreamer.getInfo].
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun setAudioConfig() {
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

        viewModelScope.launch {
            streamer.setAudioConfig(audioConfig)
        }
    }

    /**
     * Sets the video configuration.
     *
     * You can verify the device supported configuration with [SingleStreamer.getInfo].
     */
    fun setVideoConfig() {
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

        viewModelScope.launch {
            streamer.setVideoConfig(videoConfig)
        }
    }

    /**
     * Sets the microphone as the audio source.
     */
    fun setAudioSource() {
        viewModelScope.launch {
            streamer.setAudioSource(MicrophoneSourceFactory())
        }
    }

    /**
     * Sets the camera with the given id as the video source.
     *
     * @param cameraId The camera id.
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    fun setCameraId(cameraId: String) {
        viewModelScope.launch {
            streamer.setCameraId(cameraId)
        }
    }

    override fun onCleared() {
        streamer.releaseBlocking()
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}