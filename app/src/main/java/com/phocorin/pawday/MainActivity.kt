package com.phocorin.pawday

import android.Manifest
import android.app.*
import android.os.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.*
import android.widget.*
import androidx.core.content.FileProvider
import androidx.webkit.WebViewAssetLoader
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import org.json.JSONObject

class MainActivity : Activity() {
    lateinit var web: WebView
    private var chooser: ValueCallback<Array<Uri>>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Reminders.channel(this)
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = false
        web.settings.allowContentAccess = true
        web.settings.setSupportMultipleWindows(false)
        val loader = WebViewAssetLoader.Builder().addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this)).build()
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(v: WebView, r: WebResourceRequest): WebResourceResponse? =
                loader.shouldInterceptRequest(r.url) ?: WebResourceResponse("text/plain", "UTF-8", 403, "Blocked", emptyMap(), "".byteInputStream())
            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest) = true
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(v: WebView, callback: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
                chooser?.onReceiveValue(null); chooser = callback
                return try { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE) }, 41); true }
                catch (_: Exception) { chooser?.onReceiveValue(null); chooser = null; false }
            }
        }
        web.addJavascriptInterface(Bridge(), "Android")
        setContentView(web)
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html")
        Reminders.sync(this)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 41) { chooser?.onReceiveValue(if (resultCode == RESULT_OK && data?.data != null) arrayOf(data.data!!) else null); chooser = null }
    }
    @Deprecated("Uses in-page modal navigation")
    override fun onBackPressed() { web.evaluateJavascript("if(document.getElementById('md').className==='show'){closeM()}else if(S.tab!=='home'){go('home')}else{Android.exit()}", null) }
    override fun onDestroy() { chooser?.onReceiveValue(null); web.removeJavascriptInterface("Android"); web.destroy(); super.onDestroy() }
    inner class Bridge {
        @JavascriptInterface fun load(): String = getSharedPreferences("pawday", MODE_PRIVATE).getString("state", "") ?: ""
        @JavascriptInterface fun store(value: String): Boolean {
            return try {
                val s = JSONObject(value)
                for (key in listOf("pets", "recs", "rts", "evs")) s.getJSONArray(key)
                val ok = getSharedPreferences("pawday", MODE_PRIVATE).edit().putString("state", value).commit()
                if (ok) runOnUiThread { Reminders.sync(this@MainActivity) }
                ok
            } catch (_: Exception) { false }
        }
        @JavascriptInterface fun exit() { runOnUiThread { finish() } }
        @JavascriptInterface fun share(text: String) { runOnUiThread { startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "PawDay")) } }
        @JavascriptInterface fun notifications() { runOnUiThread {
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 42)
            else startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName))
        } }
        @JavascriptInterface fun testNotification() { runOnUiThread { Reminders.notify(this@MainActivity, "test", if (ja()) "通知テスト" else "Notification test") } }
        @JavascriptInterface fun qr(text: String) { runOnUiThread {
            try {
                require(text.toByteArray().size <= 1500)
                val bits = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 720, 720, mapOf(EncodeHintType.CHARACTER_SET to "UTF-8"))
                val bitmap = Bitmap.createBitmap(720, 720, Bitmap.Config.ARGB_8888)
                for (y in 0 until 720) for (x in 0 until 720) bitmap.setPixel(x, y, if (bits[x,y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                val dir = File(cacheDir, "qr").apply { mkdirs() }
                val file = File(dir, "profile.png")
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                val uri = FileProvider.getUriForFile(this@MainActivity, "$packageName.files", file)
                val image = ImageView(this@MainActivity).apply { setImageBitmap(bitmap); adjustViewBounds = true; contentDescription = "QR" }
                AlertDialog.Builder(this@MainActivity).setTitle(if (ja()) "プロフィールQR" else "Profile QR").setView(image)
                    .setNegativeButton(android.R.string.cancel, null).setPositiveButton(if (ja()) "共有" else "Share") { _, _ ->
                        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri); clipData = ClipData.newRawUri("QR", uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "PawDay"))
                    }.show()
            } catch (_: Exception) { Toast.makeText(this@MainActivity, if (ja()) "QRを作れません。メモを短くしてください。" else "Could not create QR. Shorten the notes.", Toast.LENGTH_LONG).show() }
        } }
        private fun ja() = try { JSONObject(load()).optString("lang") == "ja" } catch (_: Exception) { false }
    }
}
