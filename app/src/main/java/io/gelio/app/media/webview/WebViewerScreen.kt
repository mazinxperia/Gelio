@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.gelio.app.media.webview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebSettings
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.CookieManager
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.gelio.app.kiosk.appContainer
import io.gelio.app.core.ui.ShowcaseBackground
import io.gelio.app.core.ui.ViewerTopBar
import kotlin.math.roundToInt

@Composable
fun WebViewerScreen(
    title: String,
    url: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onClose: () -> Unit,
) {
    ShowcaseBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            ViewerTopBar(
                title = title,
                subtitle = subtitle,
                onBack = onBack,
                onHome = onHome,
                onClose = onClose,
            )
            ShowcaseWebFrame(
                url = url,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ShowcaseWebFrame(
    url: String,
    modifier: Modifier = Modifier,
) {
    var isLoading by remember(url) { mutableStateOf(true) }
    var frameSize by remember(url) { mutableStateOf(IntSize.Zero) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val canLoad = frameSize.width > 0 && frameSize.height > 0
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.let { webView ->
                runCatching {
                    webView.onPause()
                    webView.pauseTimers()
                    webView.stopLoading()
                    webView.webChromeClient = null
                    webView.webViewClient = WebViewClient()
                    webView.destroy()
                }
            }
            webViewRef = null
        }
    }

    DisposableEffect(lifecycleOwner, webViewRef) {
        val webView = webViewRef ?: return@DisposableEffect onDispose { }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    webView.onResume()
                    webView.resumeTimers()
                }
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    webView.onPause()
                    webView.pauseTimers()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = modifier.onSizeChanged { frameSize = it },
    ) {
        if (canLoad) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    FrameLayout(context).apply {
                        clipChildren = true
                        clipToPadding = true
                        isHapticFeedbackEnabled = false
                        setBackgroundColor(AndroidColor.BLACK)

                        addView(
                            WebView(context).apply {
                                isHapticFeedbackEnabled = false
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                settings.javaScriptCanOpenWindowsAutomatically = true
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                settings.builtInZoomControls = false
                                settings.displayZoomControls = false
                                settings.setSupportZoom(false)
                                settings.allowContentAccess = true
                                settings.allowFileAccess = true
                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                setBackgroundColor(AndroidColor.BLACK)
                                webChromeClient = FullscreenWebChromeClient(context)
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        isLoading = true
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isLoading = false
                                    }
                                }
                                webViewRef = this
                            },
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            ),
                        )
                    }
                },
                update = { host ->
                    val webView = host.getChildAt(0) as WebView
                    val loadKey = url
                    if (webView.tag != loadKey) {
                        isLoading = true
                        webView.tag = loadKey
                        webView.loadContentWhenReady(url)
                    }
                },
            )
        }

        if (isLoading || !canLoad) {
            AnimatedVisibility(
                visible = true,
                modifier = Modifier.align(Alignment.Center),
                enter = fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
                exit = fadeOut(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
            ) {
                ContainedLoadingIndicator()
            }
        }
    }
}

private class FullscreenWebChromeClient(
    private val context: Context,
) : WebChromeClient() {
    private var fullscreenView: View? = null
    private var fullscreenCallback: CustomViewCallback? = null

    @Suppress("DEPRECATION")
    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        val activity = context.findActivity()
        val decor = activity?.window?.decorView as? FrameLayout
        if (view == null || activity == null || decor == null) {
            callback?.onCustomViewHidden()
            return
        }

        if (fullscreenView != null) {
            callback?.onCustomViewHidden()
            return
        }

        fullscreenView = view
        fullscreenCallback = callback

        (view.parent as? ViewGroup)?.removeView(view)
        view.setBackgroundColor(AndroidColor.BLACK)
        view.isHapticFeedbackEnabled = false
        view.keepScreenOn = true
        decor.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        view.requestFocus()
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        WindowCompat.getInsetsController(activity.window, decor).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onShowCustomView(
        view: View?,
        requestedOrientation: Int,
        callback: CustomViewCallback?,
    ) {
        onShowCustomView(view, callback)
    }

    @Suppress("DEPRECATION")
    override fun onHideCustomView() {
        val activity = context.findActivity() ?: return
        val decor = activity.window.decorView as? FrameLayout ?: return
        val view = fullscreenView ?: return

        decor.removeView(view)
        view.keepScreenOn = false
        fullscreenCallback?.onCustomViewHidden()
        fullscreenView = null
        fullscreenCallback = null
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        WindowCompat.getInsetsController(activity.window, decor).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            show(WindowInsetsCompat.Type.statusBars())
            hide(WindowInsetsCompat.Type.navigationBars())
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun WebView.loadContentWhenReady(source: String) {
    post {
        if (width > 0 && height > 0) {
            loadContent(source, IntSize(width, height))
        } else {
            postDelayed({ loadContentWhenReady(source) }, 100)
        }
    }
}

private fun WebView.loadContent(
    source: String,
    viewportSize: IntSize,
) {
    if (source.isYouTubeUrl()) {
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        settings.loadWithOverviewMode = false
        settings.useWideViewPort = false
        loadUrl(
            source.toYouTubePlayerUrl(),
            mapOf("Referer" to DEFAULT_WEB_REFERER),
        )
    } else if (source.isIframeHtml()) {
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        val iframeSrc = extractIframeSrc(source)?.toEmbeddedViewerUrl()
        if (iframeSrc != null) {
            settings.loadWithOverviewMode = false
            settings.useWideViewPort = false
            loadDataWithBaseURL(
                iframeSrc.toKuulaBaseUrl(),
                iframeSrc.toKuulaEmbedDocument(
                    viewportSize = viewportSize,
                    density = resources.displayMetrics.density,
                ),
                "text/html",
                "utf-8",
                null,
            )
        } else {
            loadDataWithBaseURL(
                extractIframeBaseUrl(source),
                source.toEmbeddedHtmlDocument(),
                "text/html",
                "utf-8",
                null,
            )
        }
    } else {
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        loadUrl(source)
    }
}

private fun String.isYouTubeUrl(): Boolean =
    contains("youtube.com/embed/", ignoreCase = true) ||
        contains("youtube-nocookie.com/embed/", ignoreCase = true) ||
        contains("youtube.com/watch", ignoreCase = true) ||
        contains("youtu.be/", ignoreCase = true)

private fun String.toYouTubePlayerUrl(): String {
    val cleaned = replace("https://www.youtube-nocookie.com/", "https://www.youtube.com/", ignoreCase = true)
    val separator = if (cleaned.contains("?")) "&" else "?"
    return "$cleaned${separator}enablejsapi=1&origin=https%3A%2F%2Fgelio.app&widget_referrer=https%3A%2F%2Fgelio.app"
}

private fun String.toKuulaEmbedDocument(
    viewportSize: IntSize,
    density: Float,
): String {
    val scale = density.takeIf { it > 0f } ?: 1f
    val width = (viewportSize.width / scale).roundToInt().coerceAtLeast(1)
    val height = (viewportSize.height / scale).roundToInt().coerceAtLeast(1)
    val embedUrl = toKuulaShareUrl().toHtmlAttribute()

    return """
        <html>
        <head>
            <meta name="viewport" content="width=$width, height=$height, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <style>
                html, body {
                    margin: 0;
                    padding: 0;
                    width: ${width}px;
                    height: ${height}px;
                    overflow: hidden;
                    background: #000000;
                }
                iframe, body > div {
                    width: ${width}px !important;
                    height: ${height}px !important;
                    max-width: ${width}px !important;
                    max-height: ${height}px !important;
                    border: 0 !important;
                    overflow: hidden !important;
                    background: #000000 !important;
                }
            </style>
        </head>
        <body>
            <script
                src="https://static.kuula.io/embed.js"
                data-kuula="$embedUrl"
                data-width="${width}px"
                data-height="${height}px">
            </script>
        </body>
        </html>
    """.trimIndent()
}

private fun String.toKuulaShareUrl(): String {
    val parsed = runCatching { android.net.Uri.parse(this) }.getOrNull() ?: return this
    val host = parsed.host.orEmpty()
    if (host.startsWith("360.") || host.equals("kuula.co", ignoreCase = true) || host.endsWith(".kuula.co", ignoreCase = true)) {
        val path = parsed.encodedPath.orEmpty().trimStart('/')
        return "https://kuula.co/$path"
    }
    return this
}

private fun String.toKuulaBaseUrl(): String =
    substringBefore("/share/", missingDelimiterValue = this).let { "$it/" }

private fun String.toHtmlAttribute(): String =
    replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

private fun String.isIframeHtml(): Boolean =
    contains("<iframe", ignoreCase = true) || contains("<html", ignoreCase = true)

private fun String.toEmbeddedHtmlDocument(): String {
    if (contains("<html", ignoreCase = true)) return this
    return """
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <style>
                html, body {
                    margin: 0;
                    padding: 0;
                    width: 100%;
                    height: 100%;
                    background: #000000;
                    overflow: hidden;
                }
                iframe {
                    border: 0;
                    width: 100%;
                    height: 100%;
                }
            </style>
        </head>
        <body>
            $this
        </body>
        </html>
    """.trimIndent()
}

private const val DEFAULT_WEB_REFERER = "https://gelio.app/"

private fun extractIframeBaseUrl(source: String): String? {
    val srcValue = extractIframeSrc(source) ?: source.takeIf {
        it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true)
    } ?: return null
    return srcValue.substringBeforeLast("/", missingDelimiterValue = srcValue)
}

private fun extractIframeSrc(source: String): String? =
    Regex("""src\s*=\s*"([^"]+)"""", RegexOption.IGNORE_CASE)
        .find(source)
        ?.groupValues
        ?.getOrNull(1)

private fun String.toEmbeddedViewerUrl(): String {
    if (!contains("initload=", ignoreCase = true)) return this
    return replace("initload=0", "initload=1", ignoreCase = true)
}
