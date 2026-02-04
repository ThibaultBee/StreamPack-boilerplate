package io.github.thibaultbee.streampack.app

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.thibaultbee.streampack.app.data.rotation.RotationRepository
import io.github.thibaultbee.streampack.core.streamers.dual.DualStreamer
import io.github.thibaultbee.streampack.core.streamers.single.SingleStreamer

/**
 * Factory for constructing [MainViewModelFactory] from the [Application].
 */
class MainViewModelFactory(private val application: Application) :
    ViewModelProvider.Factory {
    private val rotationRepository = RotationRepository.getInstance(application)

    @Suppress("UNCHECKED_CAST")

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            val streamer = createStreamer(application)
            return MainViewModel(rotationRepository, streamer) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

    companion object {
        /**
         * Creates a streamer instance.
         *
         * The streamer is the central object of StreamPack.
         * It is responsible for the capture audio and video and the streaming process.
         *
         * If you need only 1 output (live only or record only), use [SingleStreamer].
         * If you need 2 outputs (live and record), use [DualStreamer].
         */
        private fun createStreamer(application: Application): SingleStreamer {
            // 1 output
            return SingleStreamer(
                application, withAudio = true, withVideo = true
            )
            // 2 outputs: uncomment the line below
            /*
            DualStreamer(
                this,
                withAudio = true,
                withVideo = true
            )
            */
        }
    }
}