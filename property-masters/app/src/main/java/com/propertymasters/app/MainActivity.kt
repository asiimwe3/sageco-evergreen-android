package com.propertymasters.app

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/**
 * SAGECO EVERGREEN — Android App
 *
 * A native WebView shell that renders the live SAGECO Evergreen website.
 * The site detects the "SagecoApp" user-agent marker and serves its
 * dedicated App Mode UI — so the app shows the EXACT same logo, screens
 * and 100% of the website's functionality, always in sync.
 *
 * Features visible in-app (everything the website has):
 *   - Property listings and search
 *   - Broker directory and registration
 *   - Agent/MLM system with wallet & withdrawals
 *   - Chatbot
 *   - Book viewings, escrow, valuations
 *   - GPS land measurement
 *   - Title search, eco score, investments
 *   - Admin panel
 *   - All future website features appear automatically
 */
class MainActivity : AppCompatActivity() {

    companion object {
        const val SITE_URL = "https://sageco-evergreen-co.vercel.app/?app=true"
        const val SITE_HOST = "sageco-evergreen-co.vercel.app"
        const val UA_MARKER = "SagecoApp"
    }

    private lateinit var webView: WebView
    private lateinit var splashView: LinearLayout
    private lateinit var errorView: LinearLayout
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private var pageLoaded = false

    // File upload support (property images, documents, agent photos)
    private val fileChooser =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            fileChooserCallback?.onReceiveValue(uris ?: arrayOf())
            fileChooserCallback = null
        }

    // Location permission (GPS land measurement page)
    private val locationPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ask up-front so GPS pages work
        locationPermission.launch(arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ))

        // Splash screen
        splashView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 96)
            gravity = Gravity.CENTER
            setBackgroundColor(0xFF0A3D1F.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            addView(TextView(context).apply {
                text = "SAGECO"
                textSize = 42f
                setTextColor(0xFFD4A017.toInt())
                gravity = Gravity.CENTER
                letterSpacing = 0.15f
                setPadding(0, 0, 0, 4)
            })
            addView(TextView(context).apply {
                text = "EVERGREEN"
                textSize = 16f
                setTextColor(0xFFFFFFFF.toInt())
                gravity = Gravity.CENTER
                letterSpacing = 0.3f
            })
            addView(TextView(context).apply {
                text = "Real Estate Platform"
                textSize = 12f
                setTextColor(0xFF888888.toInt())
                gravity = Gravity.CENTER
                setPadding(0, 8, 0, 0)
            })
            addView(ProgressBar(context).apply {
                layoutParams = LinearLayout.LayoutParams(180, 3)
                setPadding(0, 48, 0, 0)
                isIndeterminate = true
                indeterminateDrawable.setColorFilter(0xFFD4A017.toInt(), android.graphics.PorterDuff.Mode.SRC_IN)
            })
            addView(TextView(context).apply {
                text = "Loading…"
                textSize = 11f
                setTextColor(0xFF666666.toInt())
                gravity = Gravity.CENTER
                setPadding(0, 12, 0, 0)
            })
        }

        // WebView
        webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
            visibility = View.GONE
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.userAgentString = settings.userAgentString + " " + UA_MARKER
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url
                if (url.host == SITE_HOST) return false
                // External links (PesaPal, WhatsApp, tel:, mail:, socials) open natively
                return try {
                    startActivity(Intent(Intent.ACTION_VIEW, url))
                    true
                } catch (_: Exception) { true }
            }

            override fun onPageFinished(view: WebView, url: String) {
                pageLoaded = true
                splashView.visibility = View.GONE
                webView.visibility = View.VISIBLE
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame && !pageLoaded) showError()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                params: FileChooserParams
            ): Boolean {
                fileChooserCallback?.onReceiveValue(null)
                fileChooserCallback = filePathCallback
                val intent = params.createIntent().apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, params.mode == FileChooserParams.MODE_OPEN_MULTIPLE)
                }
                return try { fileChooser.launch(intent); true }
                catch (_: Exception) { fileChooserCallback = null; false }
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String, callback: GeolocationPermissions.Callback
            ) {
                callback.invoke(origin, true, false)
            }
        }

        // Phone-style back handling
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })

        errorView = buildErrorView()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
        root.addView(webView)
        root.addView(splashView)
        root.addView(errorView)
        setContentView(root)

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            webView.loadUrl(SITE_URL)
        }
    }

    private fun buildErrorView(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 96)
            gravity = Gravity.CENTER
            setBackgroundColor(0xFF0A3D1F.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE

            addView(TextView(context).apply {
                text = "📡"
                textSize = 48f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 16)
            })
            addView(TextView(context).apply {
                text = "You're offline"
                textSize = 20f
                setTextColor(0xFFFFFFFF.toInt())
                gravity = Gravity.CENTER
            })
            addView(TextView(context).apply {
                text = "SAGECO EVERGREEN needs a connection.\nCheck your network and try again."
                textSize = 14f
                setTextColor(0FF888888.toInt())
                gravity = Gravity.CENTER
                setPadding(0, 16, 0, 32)
            })
            addView(Button(context).apply {
                text = "Retry"
                setBackgroundColor(0xFFD4A017.toInt())
                setTextColor(0xFFFFFFFF.toInt())
                setOnClickListener {
                    errorView.visibility = View.GONE
                    splashView.visibility = View.VISIBLE
                    webView.visibility = View.GONE
                    pageLoaded = false
                    webView.loadUrl(SITE_URL)
                }
            })
        }
    }

    private fun showError() {
        splashView.visibility = View.GONE
        webView.visibility = View.GONE
        errorView.visibility = View.VISIBLE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }
}
