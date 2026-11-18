package com.pink.easytool

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.graphics.Color
import android.graphics.Typeface

class MainActivity : Activity() {
    private lateinit var status: TextView
    private val pink = Color.rgb(255, 64, 160)
    private val dark = Color.rgb(28, 12, 35)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildHome()
    }

    private fun buildHome() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 24)
            setBackgroundColor(dark)
        }
        val scroll = ScrollView(this)
        page.addView(TextView(this).apply {
            text = "♡  PINK EASY TOOL"
            textSize = 27f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }, matchWrap())
        page.addView(TextView(this).apply {
            text = "Android APK toolkit"
            textSize = 14f
            setTextColor(Color.rgb(255, 175, 215))
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 28)
        }, matchWrap())

        page.addView(actionButton("Choose APK", "Select an APK to inspect") { chooseApk() })
        page.addView(actionButton("Decompile APK", "Coming in the next build") { show("Decompiling is not implemented yet.") })
        page.addView(actionButton("Build / Compile", "Coming in the next build") { show("APK rebuilding is not implemented yet.") })
        page.addView(actionButton("Sign APK", "Coming in the next build") { show("APK signing is not implemented yet.") })
        page.addView(actionButton("APK Analyzer", "Choose an APK to view its file details") { chooseApk() })

        status = TextView(this).apply {
            text = "Ready. Choose an APK to begin."
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(4, 24, 4, 8)
        }
        page.addView(status, matchWrap())
        scroll.addView(page)
        setContentView(scroll)
    }

    private fun actionButton(label: String, hint: String, action: () -> Unit): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 12)
            setBackgroundColor(Color.rgb(70, 28, 72))
        }
        val button = Button(this).apply {
            text = label
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(pink)
            setOnClickListener { action() }
        }
        box.addView(button, matchWrap())
        box.addView(TextView(this).apply {
            text = hint
            textSize = 12f
            setTextColor(Color.rgb(255, 205, 230))
            setPadding(4, 5, 4, 0)
        }, matchWrap())
        box.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 14 }
        return box
    }

    private fun chooseApk() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/vnd.android.package-archive"
        }
        startActivityForResult(intent, PICK_APK)
    }

    @Deprecated("Deprecated by Android, retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_APK && resultCode == RESULT_OK) {
            val uri: Uri = data?.data ?: return
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) { }
            val name = queryName(uri) ?: "Selected APK"
            val size = querySize(uri)
            status.text = "Selected: $name\nSize: ${size?.let { formatBytes(it) } ?: "Unknown"}\n\nFile access works. Full APK analysis, decompiling, rebuilding and signing are still being developed."
        }
    }

    private fun queryName(uri: Uri): String? {
        val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) return it.getString(index)
            }
        }
        return null
    }

    private fun querySize(uri: Uri): Long? {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0 && !it.isNull(index)) return it.getLong(index)
            }
        }
        return null
    }

    private fun formatBytes(bytes: Long): String =
        if (bytes < 1024) "$bytes B" else String.format("%.2f MB", bytes / 1024.0 / 1024.0)

    private fun show(message: String) { status.text = message }

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    companion object { private const val PICK_APK = 1001 }
}
