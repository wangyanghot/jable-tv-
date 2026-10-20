package com.example.jabletv

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var rootLayout: FrameLayout
    private lateinit var webView: WebView
    private lateinit var cursorView: View
    private var fullscreenView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    private var cursorX = 600f
    private var cursorY = 400f
    private val moveStep = 38f
    private val scrollStep = 260

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        rootLayout = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        webView = WebView(this).apply {
            isFocusable = true
            isFocusableInTouchMode = true
        }
        setupWebView()

        cursorView = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#FF2A55"))
                setStroke(4, Color.WHITE)
            }
            visibility = View.VISIBLE
        }

        rootLayout.addView(
            webView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        rootLayout.addView(cursorView, FrameLayout.LayoutParams(28, 28))
        setContentView(rootLayout)

        rootLayout.post {
            cursorX = rootLayout.width / 2f
            cursorY = rootLayout.height / 2f
            updateCursorPosition()
        }
        webView.loadUrl("https://jable.tv")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true

        settings.userAgentString = settings.userAgentString
            .replace("; wv", "")
            .replace("Version/4.0 ", "")

        settings.setSupportMultipleWindows(true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                return !(url.contains("jable.tv") || url.contains("cloudflare") || url.contains("challenges"))
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: android.os.Message?
            ): Boolean {
                return false
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (fullscreenView != null) {
                    callback?.onCustomViewHidden()
                    return
                }
                fullscreenView = view
                customViewCallback = callback
                rootLayout.addView(
                    fullscreenView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                webView.visibility = View.GONE
                cursorView.bringToFront()
            }

            override fun onHideCustomView() {
                rootLayout.removeView(fullscreenView)
                fullscreenView = null
                customViewCallback?.onCustomViewHidden()
                webView.visibility = View.VISIBLE
                cursorView.bringToFront()
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val maxW = rootLayout.width.toFloat().coerceAtLeast(1080f)
            val maxH = rootLayout.height.toFloat().coerceAtLeast(720f)

            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> {
                    cursorView.visibility = View.VISIBLE
                    if (cursorY < maxH * 0.18f) {
                        webView.scrollBy(0, -scrollStep)
                    } else {
                        cursorY = (cursorY - moveStep).coerceAtLeast(10f)
                    }
                    updateCursorPosition()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    cursorView.visibility = View.VISIBLE
                    if (cursorY > maxH * 0.82f) {
                        webView.scrollBy(0, scrollStep)
                    } else {
                        cursorY = (cursorY + moveStep).coerceAtMost(maxH - 20f)
                    }
                    updateCursorPosition()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    cursorView.visibility = View.VISIBLE
                    cursorX = (cursorX - moveStep).coerceAtLeast(10f)
                    updateCursorPosition()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    cursorView.visibility = View.VISIBLE
                    cursorX = (cursorX + moveStep).coerceAtMost(maxW - 20f)
                    updateCursorPosition()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    performRealTouchClick(cursorX, cursorY)
                    return true
                }
                KeyEvent.KEYCODE_BACK -> {
                    if (fullscreenView != null) {
                        webView.webChromeClient?.onHideCustomView()
                        return true
                    }
                    if (webView.canGoBack()) {
                        webView.goBack()
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun updateCursorPosition() {
        cursorView.x = cursorX - 14f
        cursorView.y = cursorY - 14f
        cursorView.bringToFront()
    }

    private fun performRealTouchClick(x: Float, y: Float) {
        val downTime = SystemClock.uptimeMillis()
        val downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
        val upEvent = MotionEvent.obtain(downTime, downTime + 60, MotionEvent.ACTION_UP, x, y, 0)

        if (fullscreenView != null) {
            fullscreenView?.dispatchTouchEvent(downEvent)
            fullscreenView?.dispatchTouchEvent(upEvent)
        } else {
            webView.requestFocus()
            webView.dispatchTouchEvent(downEvent)
            webView.dispatchTouchEvent(upEvent)
        }

        downEvent.recycle()
        upEvent.recycle()
    }
}
