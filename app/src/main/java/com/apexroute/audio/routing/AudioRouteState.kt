package com.apexroute.audio.routing

enum class TargetRoute {
    NONE,
    EARPIECE,
    SPEAKER
}

enum class RoutingStatus {
    STOPPED,
    REQUESTING,
    CONFIRMED,
    UNSUPPORTED,
    FAILED
}

data class DeviceHardwareInfo(
    val hasEarpiece: Boolean = false,
    val hasSpeaker: Boolean = false,
    val hasWiredHeadset: Boolean = false,
    val hasBluetoothAudio: Boolean = false,
    val availableDevicesSummary: String = ""
)

data class AudioRouteState(
    val targetRoute: TargetRoute = TargetRoute.NONE,
    val status: RoutingStatus = RoutingStatus.STOPPED,
    val activeDeviceName: String? = null,
    val activeDeviceType: Int? = null,
    val statusMessage: String = "Service stopped",
    val hardwareInfo: DeviceHardwareInfo = DeviceHardwareInfo(),
    val isMediaRoutingLikelySupported: Boolean = true
)
