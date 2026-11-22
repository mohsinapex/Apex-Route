package com.apexroute.audio.ui

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.apexroute.audio.R
import com.apexroute.audio.routing.AudioDeviceHelper
import com.apexroute.audio.routing.AudioRouteState
import com.apexroute.audio.routing.AudioTestPlayer
import com.apexroute.audio.routing.RoutingStatus
import com.apexroute.audio.routing.TargetRoute
import com.apexroute.audio.service.AudioRouteService
import com.apexroute.audio.util.GitHubUpdateChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : Activity() {

    private var audioService: AudioRouteService? = null
    private var isBound = false
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var collectJob: Job? = null
    private val audioTestPlayer = AudioTestPlayer()
    private var isTestPlaying = false

    private lateinit var statusDotText: TextView
    private lateinit var statusMessageText: TextView
    private lateinit var activeDeviceDetailText: TextView

    private lateinit var earpieceBtn: Button
    private lateinit var speakerBtn: Button
    private lateinit var stopBtn: Button
    private lateinit var testAudioBtn: Button
    private lateinit var updateBtn: Button

    private lateinit var hwEarpieceStatus: TextView
    private lateinit var hwSpeakerStatus: TextView
    private lateinit var hwHeadsetStatus: TextView
    private lateinit var hwSummaryText: TextView

    private var spaceGrotesk: Typeface? = null
    private var inter: Typeface? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? AudioRouteService.LocalBinder
            audioService = binder?.getService()
            isBound = true

            collectJob?.cancel()
            collectJob = scope.launch {
                audioService?.routeState?.collectLatest { state ->
                    updateUiState(state)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            audioService = null
            isBound = false
            updateUiState(AudioRouteState())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        loadFonts()
        buildUi()
    }

    private fun loadFonts() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                spaceGrotesk = resources.getFont(R.font.space_grotesk)
                inter = resources.getFont(R.font.inter)
            } catch (_: Exception) {
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, AudioRouteService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        updateHardwareInfoDisplay()
    }

    override fun onStop() {
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        collectJob?.cancel()
        audioTestPlayer.stopTestTone()
        super.onStop()
    }

    private fun buildUi() {
        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(0xFF07060D.toInt())
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 36, 28, 48)
        }

        // App Title Header (Space Grotesk)
        val header = TextView(this).apply {
            text = "Apex Route"
            textSize = 30f
            setTextColor(0xFFFFFFFF.toInt())
            typeface = if (spaceGrotesk != null) Typeface.create(spaceGrotesk, Typeface.BOLD) else Typeface.DEFAULT_BOLD
        }
        root.addView(header)

        val subtitle = TextView(this).apply {
            text = "SYSTEM AUDIO ROUTING ENGINE"
            textSize = 11f
            letterSpacing = 0.14f
            setTextColor(0xFF9B68FF.toInt())
            setPadding(0, 4, 0, 24)
            typeface = if (spaceGrotesk != null) spaceGrotesk else Typeface.DEFAULT
        }
        root.addView(subtitle)

        // Live Status Card
        val statusCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 22, 24, 22)
            setBackgroundColor(0xFF14111F.toInt())
        }

        statusDotText = TextView(this).apply {
            text = "● Service Stopped"
            textSize = 16f
            setTextColor(0xFF787286.toInt())
            typeface = if (inter != null) Typeface.create(inter, Typeface.NORMAL) else Typeface.DEFAULT
        }
        statusCard.addView(statusDotText)

        statusMessageText = TextView(this).apply {
            text = "Select an audio route below to begin."
            textSize = 13.5f
            setTextColor(0xFFBDB8C9.toInt())
            setPadding(0, 6, 0, 4)
            typeface = if (inter != null) inter else Typeface.DEFAULT
        }
        statusCard.addView(statusMessageText)

        activeDeviceDetailText = TextView(this).apply {
            text = "Active OS Device: None"
            textSize = 12f
            setTextColor(0xFF7E788F.toInt())
            typeface = if (inter != null) inter else Typeface.DEFAULT
        }
        statusCard.addView(activeDeviceDetailText)
        root.addView(statusCard)

        // Section Title: Audio Output (Space Grotesk)
        val sectionTitle = TextView(this).apply {
            text = "Audio Output Selection"
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            typeface = if (spaceGrotesk != null) Typeface.create(spaceGrotesk, Typeface.BOLD) else Typeface.DEFAULT_BOLD
            setPadding(0, 28, 0, 12)
        }
        root.addView(sectionTitle)

        earpieceBtn = makeActionCardButton("☎   Phone Earpiece / Receiver")
        earpieceBtn.setOnClickListener { triggerRoute(TargetRoute.EARPIECE) }
        root.addView(earpieceBtn)

        speakerBtn = makeActionCardButton("◖))  Main Loudspeaker")
        speakerBtn.setOnClickListener { triggerRoute(TargetRoute.SPEAKER) }
        root.addView(speakerBtn)

        testAudioBtn = Button(this).apply {
            text = "🔊 Play Test Tone in Earpiece"
            isAllCaps = false
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF2C2442.toInt())
            typeface = if (inter != null) Typeface.create(inter, Typeface.NORMAL) else Typeface.DEFAULT
            gravity = Gravity.CENTER
            setPadding(24, 0, 24, 0)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                112
            )
            params.setMargins(0, 8, 0, 8)
            layoutParams = params
            setOnClickListener { toggleTestAudio() }
        }
        root.addView(testAudioBtn)

        stopBtn = Button(this).apply {
            text = "Stop Audio Routing Service"
            isAllCaps = false
            textSize = 14f
            setTextColor(0xFFFFDADA.toInt())
            setBackgroundColor(0xFF421E24.toInt())
            typeface = if (inter != null) Typeface.create(inter, Typeface.NORMAL) else Typeface.DEFAULT
            gravity = Gravity.CENTER
            setPadding(24, 0, 24, 0)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                112
            )
            params.setMargins(0, 8, 0, 24)
            layoutParams = params
            setOnClickListener { stopAudioService() }
        }
        root.addView(stopBtn)

        // Hardware Diagnostics Card (Space Grotesk Heading)
        val hwCardTitle = TextView(this).apply {
            text = "Hardware Diagnostics"
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            typeface = if (spaceGrotesk != null) Typeface.create(spaceGrotesk, Typeface.BOLD) else Typeface.DEFAULT_BOLD
            setPadding(0, 4, 0, 10)
        }
        root.addView(hwCardTitle)

        val hwCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 18, 22, 18)
            setBackgroundColor(0xFF100E17.toInt())
        }

        hwEarpieceStatus = TextView(this).apply {
            text = "Earpiece Hardware: Checking…"
            textSize = 13f
            setTextColor(0xFFBDB8C9.toInt())
            setPadding(0, 3, 0, 3)
            typeface = if (inter != null) inter else Typeface.DEFAULT
        }
        hwCard.addView(hwEarpieceStatus)

        hwSpeakerStatus = TextView(this).apply {
            text = "Speaker Hardware: Checking…"
            textSize = 13f
            setTextColor(0xFFBDB8C9.toInt())
            setPadding(0, 3, 0, 3)
            typeface = if (inter != null) inter else Typeface.DEFAULT
        }
        hwCard.addView(hwSpeakerStatus)

        hwHeadsetStatus = TextView(this).apply {
            text = "Headset / Bluetooth: None"
            textSize = 13f
            setTextColor(0xFFBDB8C9.toInt())
            setPadding(0, 3, 0, 3)
            typeface = if (inter != null) inter else Typeface.DEFAULT
        }
        hwCard.addView(hwHeadsetStatus)

        hwSummaryText = TextView(this).apply {
            text = "Outputs: Default"
            textSize = 12f
            setTextColor(0xFF7E788F.toInt())
            setPadding(0, 6, 0, 2)
            typeface = if (inter != null) inter else Typeface.DEFAULT
        }
        hwCard.addView(hwSummaryText)
        root.addView(hwCard)

        // Developer Credit Card (Space Grotesk Heading)
        val devCardTitle = TextView(this).apply {
            text = "Developer"
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            typeface = if (spaceGrotesk != null) Typeface.create(spaceGrotesk, Typeface.BOLD) else Typeface.DEFAULT_BOLD
            setPadding(0, 28, 0, 10)
        }
        root.addView(devCardTitle)

        val devCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 20, 22, 20)
            setBackgroundColor(0xFF14111F.toInt())
        }

        val devName = TextView(this).apply {
            text = "Mohsin Raza"
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            typeface = if (spaceGrotesk != null) Typeface.create(spaceGrotesk, Typeface.BOLD) else Typeface.DEFAULT_BOLD
        }
        devCard.addView(devName)

        val devBio = TextView(this).apply {
            text = "Software Engineer | Frontend Developer | Python Programmer | AI Enthusiast | Open-Source Developer"
            textSize = 13f
            setTextColor(0xFFBDB8C9.toInt())
            setPadding(0, 4, 0, 14)
            typeface = if (inter != null) inter else Typeface.DEFAULT
        }
        devCard.addView(devBio)

        // Social Links Layout
        val socialLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val githubBtn = makeSocialButton("GitHub") {
            GitHubUpdateChecker.openUrl(this@MainActivity, "https://github.com/mohsinapex")
        }
        val telegramBtn = makeSocialButton("Telegram") {
            GitHubUpdateChecker.openUrl(this@MainActivity, "https://t.me/mohsinapex")
        }
        val linkedinBtn = makeSocialButton("LinkedIn") {
            GitHubUpdateChecker.openUrl(this@MainActivity, "https://linkedin.com/in/mohsinapex")
        }

        socialLayout.addView(githubBtn, LinearLayout.LayoutParams(0, 108, 1f).apply { setMargins(0, 0, 6, 0) })
        socialLayout.addView(telegramBtn, LinearLayout.LayoutParams(0, 108, 1f).apply { setMargins(6, 0, 6, 0) })
        socialLayout.addView(linkedinBtn, LinearLayout.LayoutParams(0, 108, 1f).apply { setMargins(6, 0, 0, 0) })
        devCard.addView(socialLayout)
        root.addView(devCard)

        // Check for Updates Button
        updateBtn = Button(this).apply {
            text = "Check for Updates (v1.0)"
            isAllCaps = false
            textSize = 14f
            setTextColor(0xFF9B68FF.toInt())
            setBackgroundColor(0xFF14111F.toInt())
            typeface = if (inter != null) Typeface.create(inter, Typeface.NORMAL) else Typeface.DEFAULT
            gravity = Gravity.CENTER
            setPadding(24, 0, 24, 0)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                112
            )
            params.setMargins(0, 20, 0, 8)
            layoutParams = params
            setOnClickListener { checkForUpdatesOnline() }
        }
        root.addView(updateBtn)

        scrollView.addView(root)
        setContentView(scrollView)
    }

    private fun makeActionCardButton(label: String): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF1F1B2E.toInt())
            typeface = if (inter != null) Typeface.create(inter, Typeface.NORMAL) else Typeface.DEFAULT
            gravity = Gravity.CENTER_VERTICAL or Gravity.START
            setPadding(32, 0, 32, 0)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                112
            )
            params.setMargins(0, 6, 0, 6)
            layoutParams = params
        }
    }

    private fun makeSocialButton(label: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 12f
            setPadding(2, 0, 2, 0)
            setTextColor(0xFF9B68FF.toInt())
            setBackgroundColor(0xFF1F1B2E.toInt())
            typeface = if (inter != null) Typeface.create(inter, Typeface.NORMAL) else Typeface.DEFAULT
            gravity = Gravity.CENTER
            setOnClickListener { onClick() }
        }
    }

    private fun triggerRoute(target: TargetRoute) {
        requestNotificationPermissionIfNeeded()

        val intent = Intent(this, AudioRouteService::class.java).apply {
            action = AudioRouteService.ACTION_START
            putExtra(AudioRouteService.EXTRA_ROUTE, if (target == TargetRoute.SPEAKER) AudioRouteService.ROUTE_SPEAKER else AudioRouteService.ROUTE_EARPIECE)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            Toast.makeText(this, "Requested ${if (target == TargetRoute.SPEAKER) "Speaker" else "Earpiece"} routing", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not start audio service: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun toggleTestAudio() {
        if (!isTestPlaying) {
            triggerRoute(TargetRoute.EARPIECE)
            audioTestPlayer.startTestTone()
            isTestPlaying = true
            testAudioBtn.text = "⏹ Stop Test Tone"
            testAudioBtn.setBackgroundColor(0xFF421E24.toInt())
            Toast.makeText(this, "Playing test tone in earpiece", Toast.LENGTH_SHORT).show()
        } else {
            audioTestPlayer.stopTestTone()
            isTestPlaying = false
            testAudioBtn.text = "🔊 Play Test Tone in Earpiece"
            testAudioBtn.setBackgroundColor(0xFF2C2442.toInt())
            Toast.makeText(this, "Stopped test tone", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopAudioService() {
        if (isTestPlaying) {
            audioTestPlayer.stopTestTone()
            isTestPlaying = false
            testAudioBtn.text = "🔊 Play Test Tone in Earpiece"
            testAudioBtn.setBackgroundColor(0xFF2C2442.toInt())
        }

        val intent = Intent(this, AudioRouteService::class.java).apply {
            action = AudioRouteService.ACTION_STOP
        }
        startService(intent)
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        updateUiState(AudioRouteState())
        Toast.makeText(this, "Service stopped. Normal audio restored.", Toast.LENGTH_SHORT).show()
    }

    private fun checkForUpdatesOnline() {
        updateBtn.text = "Checking for updates…"
        scope.launch {
            GitHubUpdateChecker.checkForUpdates(this@MainActivity, "1.0") { hasUpdate, releaseUrl ->
                if (hasUpdate && releaseUrl != null) {
                    updateBtn.text = "New Update Available! (Tap to Download)"
                    Toast.makeText(this@MainActivity, "New version found on GitHub!", Toast.LENGTH_LONG).show()
                    GitHubUpdateChecker.openUrl(this@MainActivity, releaseUrl)
                } else {
                    updateBtn.text = "You are on the latest version (v1.0)"
                    Toast.makeText(this@MainActivity, "Apex Route is up to date", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateUiState(state: AudioRouteState) {
        val (color, dotPrefix, statusLabel) = when (state.status) {
            RoutingStatus.CONFIRMED -> Triple(0xFF4CAF50.toInt(), "● ", "Active: ${if (state.targetRoute == TargetRoute.SPEAKER) "Speaker" else "Earpiece"}")
            RoutingStatus.REQUESTING -> Triple(0xFFFF9800.toInt(), "● ", "Switching Route…")
            RoutingStatus.UNSUPPORTED -> Triple(0xFFEF5350.toInt(), "● ", "Unsupported Route")
            RoutingStatus.FAILED -> Triple(0xFFEF5350.toInt(), "● ", "Routing Failed")
            RoutingStatus.STOPPED -> Triple(0xFF787286.toInt(), "● ", "Service Stopped")
        }

        statusDotText.text = "$dotPrefix$statusLabel"
        statusDotText.setTextColor(color)

        statusMessageText.text = state.statusMessage
        activeDeviceDetailText.text = "Active OS Output: ${state.activeDeviceName ?: "Default Audio System"}"

        val isEarpieceActive = state.targetRoute == TargetRoute.EARPIECE && state.status == RoutingStatus.CONFIRMED
        val isSpeakerActive = state.targetRoute == TargetRoute.SPEAKER && state.status == RoutingStatus.CONFIRMED

        earpieceBtn.setBackgroundColor(if (isEarpieceActive) 0xFF6E32E3.toInt() else 0xFF1F1B2E.toInt())
        speakerBtn.setBackgroundColor(if (isSpeakerActive) 0xFF6E32E3.toInt() else 0xFF1F1B2E.toInt())
    }

    private fun updateHardwareInfoDisplay() {
        val hwInfo = AudioDeviceHelper.getHardwareInfo(this)
        hwEarpieceStatus.text = if (hwInfo.hasEarpiece) "✓ Built-in Earpiece: Available" else "✗ Built-in Earpiece: Not Available"
        hwEarpieceStatus.setTextColor(if (hwInfo.hasEarpiece) 0xFF4CAF50.toInt() else 0xFFEF5350.toInt())

        hwSpeakerStatus.text = if (hwInfo.hasSpeaker) "✓ Built-in Speaker: Available" else "✗ Built-in Speaker: Not Available"
        hwSpeakerStatus.setTextColor(if (hwInfo.hasSpeaker) 0xFF4CAF50.toInt() else 0xFFEF5350.toInt())

        val headsetInfo = when {
            hwInfo.hasBluetoothAudio -> "Connected (Bluetooth)"
            hwInfo.hasWiredHeadset -> "Connected (Wired)"
            else -> "Disconnected"
        }
        hwHeadsetStatus.text = "External Headset/Bluetooth: $headsetInfo"

        hwSummaryText.text = "Detected Outputs: ${hwInfo.availableDevicesSummary}"
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 700)
        }
    }
}
