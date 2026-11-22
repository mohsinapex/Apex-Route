package com.apexroute.audio.routing

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

object AudioDeviceHelper {

    fun getHardwareInfo(context: Context): DeviceHardwareInfo {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return DeviceHardwareInfo()

        val outputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

        var hasEarpiece = false
        var hasSpeaker = false
        var hasWiredHeadset = false
        var hasBluetoothAudio = false

        val namesList = mutableListOf<String>()

        for (device in outputDevices) {
            val name = formatDeviceType(device.type)
            if (name.isNotEmpty() && !namesList.contains(name)) {
                namesList.add(name)
            }
            when (device.type) {
                AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> hasEarpiece = true
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> hasSpeaker = true
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_USB_HEADSET -> hasWiredHeadset = true
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> hasBluetoothAudio = true
            }
        }

        val summary = if (namesList.isNotEmpty()) {
            namesList.joinToString(", ")
        } else {
            "Default Audio System"
        }

        return DeviceHardwareInfo(
            hasEarpiece = hasEarpiece,
            hasSpeaker = hasSpeaker,
            hasWiredHeadset = hasWiredHeadset,
            hasBluetoothAudio = hasBluetoothAudio,
            availableDevicesSummary = summary
        )
    }

    fun findAudioDeviceInfo(context: Context, type: Int): AudioDeviceInfo? {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val commDevices = audioManager.availableCommunicationDevices
            val commMatch = commDevices.firstOrNull { it.type == type }
            if (commMatch != null) return commMatch
        }

        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return outputs.firstOrNull { it.type == type }
    }

    fun formatDeviceType(type: Int): String {
        return when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Earpiece"
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Main Speaker"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headset"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Headphones"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth Audio (A2DP)"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth Voice (SCO)"
            AudioDeviceInfo.TYPE_USB_DEVICE -> "USB Audio Device"
            AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset"
            else -> "Audio Device ($type)"
        }
    }
}
