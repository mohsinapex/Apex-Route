package com.apexroute.audio.routing

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

class EarpieceKeepAlivePlayer {
    private var audioTrack: AudioTrack? = null
    @Volatile
    private var isRunning = false
    private var thread: Thread? = null

    fun start() {
        if (isRunning) return
        isRunning = true

        thread = Thread {
            try {
                val sampleRate = 8000
                val channelConfig = AudioFormat.CHANNEL_OUT_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

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

                val silentBuffer = ByteArray(bufferSize)
                while (isRunning) {
                    track.write(silentBuffer, 0, silentBuffer.size)
                    Thread.sleep(100)
                }

                track.stop()
                track.release()
                audioTrack = null
            } catch (_: Exception) {
                isRunning = false
                audioTrack = null
            }
        }.apply { start() }
    }

    fun stop() {
        isRunning = false
        thread?.interrupt()
        thread = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {
        }
        audioTrack = null
    }
}
