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
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.android.apksig.ApkSigner
import java.io.File
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.spec.AlgorithmParameterSpec
import java.security.cert.X509Certificate
import java.util.Date
import javax.security.auth.x500.X500Principal
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

class MainActivity : Activity() {
    private lateinit var status: TextView
    private var selectedApk: Uri? = null
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

        page.addView(actionButton("Choose APK", "Select an APK to work with") { chooseApk() })
        page.addView(actionButton("Extract APK files", "Extract the APK's raw files into app storage") { extractApk() })
        page.addView(actionButton("Build / Compile", "Needs a decoded project; not available in this build") {
            show("Rebuilding is not available yet. A real APK rebuild needs decoded resources and smali, not just ZIP compression.")
        })
        page.addView(actionButton("Sign APK", "Sign the selected APK with a local app signing key") { chooseSignOutput() })
        page.addView(actionButton("APK Analyzer", "Show package details and APK contents") { analyzeApk() })

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
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, PICK_APK)
    }

    private fun chooseSignOutput() {
        if (selectedApk == null) {
            show("Choose an APK first.")
            return
        }
        val name = (queryName(selectedApk!!) ?: "app.apk").removeSuffix(".apk") + "-signed.apk"
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_TITLE, name)
        }
        startActivityForResult(intent, CREATE_SIGNED_APK)
    }

    @Deprecated("Deprecated by Android, retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        when (requestCode) {
            PICK_APK -> {
                selectedApk = uri
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: SecurityException) { }
                val name = queryName(uri) ?: "Selected APK"
                val size = querySize(uri)
                status.text = "Selected: $name\nSize: ${size?.let { formatBytes(it) } ?: "Unknown"}"
            }
            CREATE_SIGNED_APK -> signSelectedApk(uri)
        }
    }

    private fun copyUriToTemp(uri: Uri, suffix: String): File {
        val file = File(cacheDir, "input-${System.currentTimeMillis()}.$suffix")
        contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Could not open the selected APK." }
            FileOutputStream(file).use { output -> input.copyTo(output) }
        }
        return file
    }

    private fun analyzeApk() {
        val uri = selectedApk
        if (uri == null) {
            show("Choose an APK first.")
            return
        }
        show("Analyzing APK…")
        Thread {
            try {
                val file = copyUriToTemp(uri, "apk")
                val info = packageManager.getPackageArchiveInfo(file.absolutePath, 0)
                val details = buildString {
                    appendLine("APK: ${queryName(uri) ?: "Selected APK"}")
                    appendLine("Size: ${formatBytes(file.length())}")
                    if (info != null) {
                        appendLine("Package: ${info.packageName}")
                        appendLine("Version: ${info.versionName ?: "Unknown"}")
                        @Suppress("DEPRECATION")
                        appendLine("Version code: ${info.versionCode}")
                    } else {
                        appendLine("Package metadata could not be read.")
                    }
                    ZipFile(file).use { zip ->
                        val entries = zip.entries()
                        var count = 0
                        var total = 0L
                        while (entries.hasMoreElements()) {
                            val entry = entries.nextElement()
                            count++
                            if (entry.size > 0) total += entry.size
                        }
                        appendLine("Files inside APK: $count")
                        appendLine("Uncompressed size: ${formatBytes(total)}")
                    }
                }
                file.delete()
                runOnUiThread { status.text = details }
            } catch (e: Exception) {
                runOnUiThread { show("Analysis failed: ${e.message ?: e.javaClass.simpleName}") }
            }
        }.start()
    }

    private fun extractApk() {
        val uri = selectedApk
        if (uri == null) {
            show("Choose an APK first.")
            return
        }
        show("Extracting APK files…")
        Thread {
            try {
                val input = contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Could not open the selected APK.")
                val folder = File(getExternalFilesDir(null) ?: filesDir, "ExtractedAPKs/${System.currentTimeMillis()}")
                if (!folder.mkdirs() && !folder.isDirectory) throw IllegalStateException("Could not create output folder.")
                input.use { stream ->
                    ZipInputStream(stream).use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            val output = File(folder, entry.name).canonicalFile
                            if (!output.path.startsWith(folder.canonicalPath + File.separator)) {
                                throw SecurityException("APK contains an unsafe file path.")
                            }
                            if (entry.isDirectory) {
                                output.mkdirs()
                            } else {
                                output.parentFile?.mkdirs()
                                FileOutputStream(output).use { out -> zip.copyTo(out) }
                            }
                            zip.closeEntry()
                            entry = zip.nextEntry
                        }
                    }
                }
                runOnUiThread { status.text = "Extracted APK contents to:\n${folder.absolutePath}\n\nThis is raw ZIP extraction, not Apktool resource decoding." }
            } catch (e: Exception) {
                runOnUiThread { show("Extraction failed: ${e.message ?: e.javaClass.simpleName}") }
            }
        }.start()
    }

    private fun signSelectedApk(outputUri: Uri) {
        val inputUri = selectedApk ?: return
        show("Signing APK…")
        Thread {
            var input: File? = null
            var output: File? = null
            try {
                input = copyUriToTemp(inputUri, "apk")
                output = File(cacheDir, "signed-${System.currentTimeMillis()}.apk")
                val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                val alias = "PinkEasyToolSigningKey"
                if (!keyStore.containsAlias(alias)) {
                    val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
                    val spec: AlgorithmParameterSpec = KeyGenParameterSpec.Builder(
                        alias,
                        KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                    ).setKeySize(2048)
                        .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                        .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                        .setCertificateSubject(X500Principal("CN=Pink Easy Tool"))
                        .setCertificateSerialNumber(BigInteger.ONE)
                        .setCertificateNotBefore(Date(System.currentTimeMillis() - 86400000L))
                        .setCertificateNotAfter(Date(System.currentTimeMillis() + 315360000000L))
                        .build()
                    generator.initialize(spec)
                    generator.generateKeyPair()
                }
                val privateKey = keyStore.getKey(alias, null) as PrivateKey
                val certificate = keyStore.getCertificate(alias) as X509Certificate
                val signerConfig = ApkSigner.SignerConfig.Builder(
                    "PinkEasyTool",
                    privateKey,
                    listOf(certificate)
                ).build()
                ApkSigner.Builder(listOf(signerConfig))
                    .setInputApk(input)
                    .setOutputApk(output)
                    .setV1SigningEnabled(true)
                    .setV2SigningEnabled(true)
                    .setV3SigningEnabled(true)
                    .build()
                    .sign()
                contentResolver.openOutputStream(outputUri, "w").use { stream ->
                    requireNotNull(stream) { "Could not write the signed APK." }
                    output.inputStream().use { it.copyTo(stream) }
                }
                runOnUiThread { status.text = "Signed APK saved successfully.\n\nOutput: ${queryName(outputUri) ?: "signed.apk"}\nSigning key: Pink Easy Tool (stored on this device)." }
            } catch (e: Exception) {
                runOnUiThread { show("Signing failed: ${e.message ?: e.javaClass.simpleName}") }
            } finally {
                input?.delete()
                output?.delete()
            }
        }.start()
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

    companion object {
        private const val PICK_APK = 1001
        private const val CREATE_SIGNED_APK = 1002
    }
}
