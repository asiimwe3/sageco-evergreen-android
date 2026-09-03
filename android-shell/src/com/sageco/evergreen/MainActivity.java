package com.sageco.evergreen;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * SageCo Evergreen — Android App (v3.0.0)
 *
 * One company, one product. The app renders the live SageCo Evergreen
 * website (sagecoevergreen.publicvm.com) inside a native shell. The
 * site detects the "SagecoApp" user-agent marker and serves its
 * dedicated App Mode UI — so the app shows the EXACT same logo,
 * screens and 100% of the website functionality, always in sync.
 */
public class MainActivity extends Activity {

    private static final String SITE_URL =
            "https://sagecoevergreen.publicvm.com/?app=true";
    private static final String SITE_HOST = "sagecoevergreen.publicvm.com";
    private static final String FALLBACK_HOST = "sageco-evergreen-co.vercel.app";
    private static final String UA_MARKER = "SagecoApp";
    private static final int FILE_CHOOSER_REQUEST = 1001;

    private WebView webView;
    private LinearLayout errorView;
    private ValueCallback<Uri[]> fileChooserCallback;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        webView = new WebView(this);
        webView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        // The marker the website uses to activate App Mode
        s.setUserAgentString(s.getUserAgentString() + " " + UA_MARKER);
        // Keep the user signed in, exactly like the website
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri url = request.getUrl();
                String host = url.getHost();
                // In-site links stay inside the app
                if (SITE_HOST.equals(host) || FALLBACK_HOST.equals(host)) return false;
                // External links (PesaPal checkout, WhatsApp, tel:, mail:) open natively —
                // exactly how the website handles them
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, url));
                } catch (Exception ignored) { }
                return true;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) showError();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            // File uploads (property images, documents) — same as website
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (fileChooserCallback != null) fileChooserCallback.onReceiveValue(null);
                fileChooserCallback = callback;
                try {
                    Intent intent = params.createIntent();
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                    return true;
                } catch (Exception e) {
                    fileChooserCallback = null;
                    return false;
                }
            }

            // Geolocation for the GPS-measure page — same as website
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin,
                    GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
        });

        errorView = buildErrorView();

        root.addView(webView);
        root.addView(errorView);
        setContentView(root);

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(SITE_URL);
        }
    }

    private LinearLayout buildErrorView() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(48, 96, 48, 96);
        box.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        box.setVisibility(View.GONE);

        TextView title = new TextView(this);
        title.setText("You're offline");
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.parseColor("#0A3D1F"));
        box.addView(title);

        TextView msg = new TextView(this);
        msg.setText("SageCo Evergreen needs a connection. Check your network and try again.");
        msg.setTextSize(14);
        msg.setGravity(Gravity.CENTER);
        msg.setPadding(0, 16, 0, 32);
        box.addView(msg);

        Button retry = new Button(this);
        retry.setText("Retry");
        retry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                errorView.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                webView.loadUrl(SITE_URL);
            }
        });
        box.addView(retry);
        return box;
    }

    private void showError() {
        webView.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);
    }

    // Phone-style back: goes back through the site, like a browser
    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST && fileChooserCallback != null) {
            Uri[] uris = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                uris = new Uri[]{ data.getData() };
            }
            fileChooserCallback.onReceiveValue(uris);
            fileChooserCallback = null;
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) webView.saveState(outState);
    }

    @Override
    protected void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }
}
