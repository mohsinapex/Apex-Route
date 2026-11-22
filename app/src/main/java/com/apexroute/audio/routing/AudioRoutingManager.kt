package com.apexroute.audio.routing

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors

class AudioRoutingManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    private val _routeState = MutableStateFlow(AudioRouteState())
    val routeState: StateFlow<AudioRouteState> = _routeState.asStateFlow()

    private var originalMode: Int = AudioManager.MODE_NORMAL
    private var originalSpeakerphoneOn: Boolean = false
    private var originalBluetoothSco: Boolean = false
    private var originalCommunicationDevice: AudioDeviceInfo? = null
    private var isStateSaved = false

    private var audioDeviceCallback: AudioDeviceCallback? = null
    private var communicationDeviceChangeListener: Any? = null
    private var audioFocusRequest: Any? = null
    private var proximitySensorManager: ProximitySensorManager? = null
    private val keepAlivePlayer = EarpieceKeepAlivePlayer()

    init {
        updateHardwareInfo()
        registerCallbacks()
        setupProximitySensor()
    }

    private fun setupProximitySensor() {
        proximitySensorManager = ProximitySensorManager(context) { isNear ->
            if (_routeState.value.targetRoute == TargetRoute.EARPIECE && _routeState.value.status == RoutingStatus.CONFIRMED) {
                if (isNear) {
                    applyRoute(TargetRoute.EARPIECE)
                }
            }
        }
    }

    fun updateHardwareInfo() {
        val hwInfo = AudioDeviceHelper.getHardwareInfo(context)
        _routeState.value = _routeState.value.copy(hardwareInfo = hwInfo)
    }

    private fun saveOriginalStateIfNeeded() {
        if (!isStateSaved) {
            originalMode = audioManager.mode
            @Suppress("DEPRECATION")
            run {
                originalSpeakerphoneOn = audioManager.isSpeakerphoneOn
                originalBluetoothSco = audioManager.isBluetoothScoOn
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                originalCommunicationDevice = audioManager.communicationDevice
            }
            isStateSaved = true
        }
    }

    fun applyRoute(target: TargetRoute): AudioRouteState {
        if (target == TargetRoute.NONE) {
            return restoreOriginalState()
        }

        saveOriginalStateIfNeeded()
        updateHardwareInfo()

        val hw = _routeState.value.hardwareInfo
        if (target == TargetRoute.EARPIECE && !hw.hasEarpiece) {
            val newState = _routeState.value.copy(
                targetRoute = target,
                status = RoutingStatus.UNSUPPORTED,
                statusMessage = "Device has no built-in earpiece hardware.",
                activeDeviceName = "N/A"
            )
            _routeState.value = newState
            return newState
        }

        if (target == TargetRoute.SPEAKER && !hw.hasSpeaker) {
            val newState = _routeState.value.copy(
                targetRoute = target,
                status = RoutingStatus.UNSUPPORTED,
                statusMessage = "Device has no main speaker hardware.",
                activeDeviceName = "N/A"
            )
            _routeState.value = newState
            return newState
        }

        val success = try {
            requestAudioFocus()

            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

            @Suppress("DEPRECATION")
            if (target == TargetRoute.EARPIECE) {
                audioManager.isSpeakerphoneOn = false
                audioManager.isBluetoothScoOn = false
                keepAlivePlayer.start()
            } else {
                keepAlivePlayer.stop()
                audioManager.isSpeakerphoneOn = true
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val wantedType = if (target == TargetRoute.SPEAKER) {
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                } else {
                    AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
                }

                val targetDevice = AudioDeviceHelper.findAudioDeviceInfo(context, wantedType)
                if (targetDevice != null) {
                    audioManager.setCommunicationDevice(targetDevice)
                }
            }

            try {
                if (target == TargetRoute.EARPIECE) {
                    audioManager.setParameters("set_call_audio_output=0")
                    audioManager.setParameters("routing=1")
                    audioManager.setParameters("earpiece=on")
                    audioManager.setParameters("force_use=0")
                    audioManager.setParameters("voice_call_routing=1")
                } else {
                    audioManager.setParameters("set_call_audio_output=2")
                    audioManager.setParameters("routing=2")
                    audioManager.setParameters("earpiece=off")
                    audioManager.setParameters("force_use=1")
                }
            } catch (_: Exception) {
            }

            true
        } catch (_: Exception) {
            false
        }

        if (target == TargetRoute.EARPIECE) {
            proximitySensorManager?.startListening()
        } else {
            proximitySensorManager?.stopListening()
        }

        val targetDeviceName = if (target == TargetRoute.SPEAKER) "Main Speaker" else "Phone Earpiece"
        val activeDevice = getActualActiveCommunicationDevice()

        val isConfirmed = success
        val finalStatus = if (isConfirmed) RoutingStatus.CONFIRMED else RoutingStatus.FAILED
        val message = if (isConfirmed) {
            "Active: All media & voice routed to $targetDeviceName."
        } else {
            "Failed to set audio route to $targetDeviceName."
        }

        val newState = _routeState.value.copy(
            targetRoute = target,
            status = finalStatus,
            activeDeviceName = activeDevice?.let { AudioDeviceHelper.formatDeviceType(it.type) } ?: targetDeviceName,
            activeDeviceType = activeDevice?.type,
            statusMessage = message
        )
        _routeState.value = newState
        return newState
    }

    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusRequest = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .build()
                audioFocusRequest = focusRequest
                audioManager.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    { },
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
        } catch (_: Exception) {
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest as android.media.AudioFocusRequest)
                audioFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus { }
            }
        } catch (_: Exception) {
        }
    }

    fun restoreOriginalState(): AudioRouteState {
        keepAlivePlayer.stop()
        proximitySensorManager?.stopListening()
        if (isStateSaved) {
            try {
                abandonAudioFocus()
                try {
                    audioManager.setParameters("set_call_audio_output=2")
                    audioManager.setParameters("routing=2")
                    audioManager.setParameters("earpiece=off")
                    audioManager.setParameters("force_use=1")
                } catch (_: Exception) {
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    audioManager.clearCommunicationDevice()
                }
                @Suppress("DEPRECATION")
                run {
                    audioManager.isSpeakerphoneOn = originalSpeakerphoneOn
                    audioManager.isBluetoothScoOn = originalBluetoothSco
                }
                audioManager.mode = originalMode
            } catch (_: Exception) {
            }
            isStateSaved = false
        }

        updateHardwareInfo()
        val newState = AudioRouteState(
            targetRoute = TargetRoute.NONE,
            status = RoutingStatus.STOPPED,
            statusMessage = "Service stopped. System audio restored.",
            hardwareInfo = _routeState.value.hardwareInfo
        )
        _routeState.value = newState
        return newState
    }

    private fun getActualActiveCommunicationDevice(): AudioDeviceInfo? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return audioManager.communicationDevice
        }
        return null
    }

    private fun registerCallbacks() {
        audioDeviceCallback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                mainHandler.post {
                    updateHardwareInfo()
                    if (_routeState.value.status == RoutingStatus.CONFIRMED) {
                        applyRoute(_routeState.value.targetRoute)
                    }
                }
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                mainHandler.post {
                    updateHardwareInfo()
                    if (_routeState.value.status == RoutingStatus.CONFIRMED) {
                        applyRoute(_routeState.value.targetRoute)
                    }
                }
            }
        }
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, mainHandler)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val listener = AudioManager.OnCommunicationDeviceChangedListener { device ->
                mainHandler.post {
                    if (_routeState.value.targetRoute != TargetRoute.NONE) {
                        val activeName = device?.let { AudioDeviceHelper.formatDeviceType(it.type) } ?: "Default"
                        _routeState.value = _routeState.value.copy(
                            activeDeviceName = activeName,
                            activeDeviceType = device?.type
                        )
                    }
                }
            }
            communicationDeviceChangeListener = listener
            audioManager.addOnCommunicationDeviceChangedListener(executor, listener)
        }
    }

    fun unregisterCallbacks() {
        keepAlivePlayer.stop()
        proximitySensorManager?.stopListening()
        audioDeviceCallback?.let {
            audioManager.unregisterAudioDeviceCallback(it)
            audioDeviceCallback = null
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (communicationDeviceChangeListener as? AudioManager.OnCommunicationDeviceChangedListener)?.let {
                audioManager.removeOnCommunicationDeviceChangedListener(it)
                communicationDeviceChangeListener = null
            }
        }
        executor.shutdown()
    }
}
