package com.apexroute.audio.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.apexroute.audio.R
import com.apexroute.audio.routing.AudioDeviceHelper

class SplashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(0xFF090810.toInt())
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.apex_route_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        root.addView(logo, LinearLayout.LayoutParams(176, 176))

        val title = TextView(this).apply {
            text = getString(R.string.title_apex_route)
            textSize = 30f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 18, 0, 4)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "Control your audio output"
            textSize = 14f
            setTextColor(0xFFB7B2C5.toInt())
            gravity = Gravity.CENTER
        }
        root.addView(subtitle)

        val hwInfo = AudioDeviceHelper.getHardwareInfo(this)
        val statusInfo = if (hwInfo.hasEarpiece) {
            "Audio system initialized"
        } else {
            "No earpiece detected on this device"
        }

        val loading = TextView(this).apply {
            text = statusInfo
            textSize = 13f
            setTextColor(0xFF9B68FF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 52, 0, 0)
        }
        root.addView(loading)
        setContentView(root)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 900)
    }
}
