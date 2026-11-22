package com.apexroute.audio.routing

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.sin

class AudioTestPlayer {
    private var audioTrack: AudioTrack? = null
    @Volatile
    private var isPlaying = false
    private var playbackThread: Thread? = null

    fun startTestTone() {
        if (isPlaying) return
        isPlaying = true

        playbackThread = Thread {
            try {
                val sampleRate = 16000 // Voice communication sample rate
                val channelConfig = AudioFormat.CHANNEL_OUT_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2

                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                val format = AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .setEncoding(audioFormat)
                    .build()

                val track = AudioTrack.Builder()
                    .setAudioAttributes(attributes)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack = track
                track.play()

                val freq = 440.0 // A4 tone
                val f = 2.0 * Math.PI * freq / sampleRate
                val buffer = ShortArray(bufferSize / 2)
                var angle = 0.0

                while (isPlaying) {
                    for (i in buffer.indices) {
                        // Generate a soft sine wave with intermittent beep pattern
                        val sample = (sin(angle) * 8000.0).toInt().toShort()
                        buffer[i] = sample
                        angle += f
                        if (angle > 2.0 * Math.PI) angle -= 2.0 * Math.PI
                    }
                    track.write(buffer, 0, buffer.size)
                }

                track.stop()
                track.release()
                audioTrack = null
            } catch (_: Exception) {
                isPlaying = false
                audioTrack = null
            }
        }.apply { start() }
    }

    fun stopTestTone() {
        isPlaying = false
        playbackThread?.interrupt()
        playbackThread = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {
        }
        audioTrack = null
    }
}
