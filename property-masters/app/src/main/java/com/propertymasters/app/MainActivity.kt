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
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/**
 * SageCo Evergreen — Android App
 *
 * One company, one product: the app renders the live SageCo Evergreen
 * website inside a native shell. The site detects the "SagecoApp"
 * user-agent marker and serves its dedicated App Mode UI — so the app
 * shows the EXACT same logo, screens and 100% of the website's
 * functionality, always in sync.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        const val SITE_URL = "https://sagecoevergreen.publicvm.com/?app=true"
        const val SITE_HOST = "sagecoevergreen.publicvm.com"
        const val FALLBACK_HOST = "sageco-evergreen-co.vercel.app"
        const val UA_MARKER = "SagecoApp"
    }

    private lateinit var webView: WebView
    private lateinit var errorView: LinearLayout
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null

    // File upload support (property images, documents — same as website)
    private val fileChooser =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            fileChooserCallback?.onReceiveValue(uris ?: arrayOf())
            fileChooserCallback = null
        }

    // Location permission (GPS land measurement page — same as website)
    private val locationPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ask up-front so the GPS-measure page works like on the website
        locationPermission.launch(arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ))

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            // The marker the website uses to activate App Mode
            settings.userAgentString = settings.userAgentString + " " + UA_MARKER
            // Keep the user signed in like the website does
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url
                // In-site links stay in the app
                if (url.host == SITE_HOST || url.host == FALLBACK_HOST) return false
                // Everything else (PesaPal checkout, WhatsApp, tel:, mail:, socials)
                // opens natively — exactly how the website behaves for external links
                return try {
                    startActivity(Intent(Intent.ACTION_VIEW, url))
                    true
                } catch (_: Exception) { true }
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) showError()
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

            // Geolocation for the GPS-measure page (same as website)
            override fun onGeolocationPermissionsShowPrompt(
                origin: String, callback: GeolocationPermissions.Callback
            ) {
                callback.invoke(origin, true, false)
            }
        }

        // Phone-style back handling: goes back in the site like a browser
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })

        errorView = buildErrorView()

        root.addView(webView)
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
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE

            addView(TextView(context).apply {
                text = "You're offline"
                textSize = 20f
                gravity = Gravity.CENTER
            })
            addView(TextView(context).apply {
                text = "SageCo Evergreen needs a connection. Check your network and try again."
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, 16, 0, 32)
            })
            addView(Button(context).apply {
                text = "Retry"
                setOnClickListener {
                    errorView.visibility = View.GONE
                    webView.visibility = View.VISIBLE
                    webView.loadUrl(SITE_URL)
                }
            })
        }
    }

    private fun showError() {
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
