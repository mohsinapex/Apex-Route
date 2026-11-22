package com.apexroute.audio.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import com.apexroute.audio.R
import com.apexroute.audio.routing.AudioRouteState
import com.apexroute.audio.routing.AudioRoutingManager
import com.apexroute.audio.routing.RoutingStatus
import com.apexroute.audio.routing.TargetRoute
import com.apexroute.audio.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AudioRouteService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): AudioRouteService = this@AudioRouteService
    }

    private val binder = LocalBinder()
    private lateinit var routingManager: AudioRoutingManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val routeState: StateFlow<AudioRouteState>
        get() = routingManager.routeState

    override fun onCreate() {
        super.onCreate()
        routingManager = AudioRoutingManager(this)
        createNotificationChannel()

        serviceScope.launch {
            routingManager.routeState.collect { state ->
                if (state.status != RoutingStatus.STOPPED) {
                    updateNotification(state)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopRoutingAndSelf()
                return START_NOT_STICKY
            }
            ACTION_SPEAKER -> applyRoute(TargetRoute.SPEAKER)
            ACTION_EARPIECE -> applyRoute(TargetRoute.EARPIECE)
            ACTION_START, null -> {
                val routeName = intent?.getStringExtra(EXTRA_ROUTE) ?: ROUTE_EARPIECE
                val target = if (routeName == ROUTE_SPEAKER) TargetRoute.SPEAKER else TargetRoute.EARPIECE
                applyRoute(target)
            }
        }
        return START_STICKY
    }

    private fun applyRoute(target: TargetRoute) {
        val state = routingManager.applyRoute(target)
        startOrUpdateForeground(state)
    }

    private fun stopRoutingAndSelf() {
        routingManager.restoreOriginalState()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startOrUpdateForeground(state: AudioRouteState) {
        val notification = buildNotification(state)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            startForeground(NOTIFICATION_ID, notification, fgsType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(state: AudioRouteState) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(state))
    }

    private fun buildNotification(state: AudioRouteState): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 10, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
        )
        val speakerIntent = servicePendingIntent(ACTION_SPEAKER, 11)
        val earpieceIntent = servicePendingIntent(ACTION_EARPIECE, 12)
        val stopIntent = servicePendingIntent(ACTION_STOP, 13)

        val targetName = when (state.targetRoute) {
            TargetRoute.SPEAKER -> "Speaker"
            TargetRoute.EARPIECE -> "Earpiece"
            TargetRoute.NONE -> "Stopped"
        }

        val statusText = when (state.status) {
            RoutingStatus.CONFIRMED -> "Active: ${state.activeDeviceName ?: targetName}"
            RoutingStatus.REQUESTING -> "Switching route to $targetName…"
            RoutingStatus.UNSUPPORTED -> "Unsupported on this device"
            RoutingStatus.FAILED -> "Route request failed"
            RoutingStatus.STOPPED -> "Service inactive"
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setSmallIcon(R.drawable.apex_route_logo)
            .setContentTitle("Apex Route • $targetName")
            .setContentText(statusText)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, "Speaker", speakerIntent)
            .addAction(0, "Earpiece", earpieceIntent)
            .addAction(0, "Stop", stopIntent)
            .build()
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, AudioRouteService::class.java).setAction(action)
        return PendingIntent.getService(
            this, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
        )
    }

    private fun immutableFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, getString(R.string.service_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Apex Route audio routing persistent notification"
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        routingManager.restoreOriginalState()
        routingManager.unregisterCallbacks()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    companion object {
        const val ACTION_START = "com.apexroute.audio.START"
        const val ACTION_STOP = "com.apexroute.audio.STOP"
        const val ACTION_SPEAKER = "com.apexroute.audio.SPEAKER"
        const val ACTION_EARPIECE = "com.apexroute.audio.EARPIECE"
        const val EXTRA_ROUTE = "route"
        const val ROUTE_EARPIECE = "earpiece"
        const val ROUTE_SPEAKER = "speaker"
        private const val CHANNEL_ID = "apex_route_audio"
        private const val NOTIFICATION_ID = 4102
    }
}
