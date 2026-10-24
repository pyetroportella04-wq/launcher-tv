package br.com.launchertv

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import android.view.KeyEvent
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class MainActivity : Activity() {

    private lateinit var web: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Serve a pasta assets/ por um endereço https interno (evita problemas de file://)
        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        web = WebView(this).apply {
            setBackgroundColor(0xFF171230.toInt())
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.mediaPlaybackRequiresUserGesture = false
            isFocusable = true
            isFocusableInTouchMode = true
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView, request: WebResourceRequest
                ): WebResourceResponse? = loader.shouldInterceptRequest(request.url)

                // Mantém a navegação dentro do launcher
                override fun shouldOverrideUrlLoading(
                    view: WebView, request: WebResourceRequest
                ): Boolean = request.url.host != "appassets.androidplatform.net"
            }
            addJavascriptInterface(Bridge(), "Android")
        }
        setContentView(web)
        web.loadUrl("https://appassets.androidplatform.net/assets/launcher.html")
    }

    override fun onResume() {
        super.onResume()
        web.onResume()
        web.requestFocus()
        hideSystemUi()
    }

    override fun onPause() {
        web.onPause()
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
    }

    // Botão HOME com o launcher já aberto: volta ao início da tela
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        web.evaluateJavascript("go(1,0)", null)
    }

    // Botão Voltar nunca fecha o launcher: só volta ao primeiro app
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            web.evaluateJavascript("go(1,0)", null)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    @Suppress("DEPRECATION")
    private fun hideSystemUi() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    private fun openSystemScreen(action: String) {
        runOnUiThread {
            try {
                startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e: Exception) {
                // Se a TV não tiver essa tela, abre as configurações gerais
                try {
                    startActivity(
                        Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun launchables(): List<ResolveInfo> {
        val found = mutableListOf<ResolveInfo>()
        for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
            found += packageManager.queryIntentActivities(intent, 0)
        }
        return found.distinctBy { it.activityInfo.packageName }
    }

    private fun toDataUri(drawable: Drawable): String {
        val size = 192
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(Canvas(bitmap))
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        return "data:image/png;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    /** Funções que a página (launcher.html) chama pelo objeto "Android". */
    inner class Bridge {

        /**
         * Recebe [{"n":"Netflix","pk":["com.netflix.ninja"]}, ...] e devolve, para cada app
         * instalado na TV, o pacote e o ícone oficial em PNG (data URI).
         */
        @JavascriptInterface
        fun resolveApps(json: String): String {
            val pm = packageManager
            val installed = launchables()
            val query = JSONArray(json)
            val result = JSONArray()
            for (i in 0 until query.length()) {
                val item = query.getJSONObject(i)
                val name = item.getString("n")
                val packages = item.getJSONArray("pk")
                var hit: ResolveInfo? = null
                for (j in 0 until packages.length()) {
                    val pkg = packages.getString(j)
                    hit = installed.firstOrNull { it.activityInfo.packageName == pkg }
                    if (hit != null) break
                }
                if (hit == null) {
                    hit = installed.firstOrNull { it.loadLabel(pm).toString().equals(name, true) }
                        ?: installed.firstOrNull { it.loadLabel(pm).toString().contains(name, true) }
                }
                val entry = JSONObject().put("n", name)
                if (hit != null) {
                    entry.put("pkg", hit.activityInfo.packageName)
                    entry.put("icon", toDataUri(hit.loadIcon(pm)))
                }
                result.put(entry)
            }
            return result.toString()
        }

        @JavascriptInterface
        fun launchApp(pkg: String): Boolean {
            val pm = packageManager
            val intent = pm.getLeanbackLaunchIntentForPackage(pkg)
                ?: pm.getLaunchIntentForPackage(pkg)
                ?: return false
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runOnUiThread { startActivity(intent) }
            return true
        }

        @JavascriptInterface
        fun openBluetoothSettings() = openSystemScreen(Settings.ACTION_BLUETOOTH_SETTINGS)

        @JavascriptInterface
        fun openSettings() = openSystemScreen(Settings.ACTION_SETTINGS)
    }
}
