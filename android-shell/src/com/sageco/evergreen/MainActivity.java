package com.sageco.evergreen;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * SageCo Evergreen — Android App (v3.1.0)
 *
 * One company, one product. The app renders the live SageCo Evergreen
 * website inside a native shell. The site detects the "SagecoApp"
 * user-agent marker and serves its dedicated App Mode UI — identical
 * logo, screens and 100% of website functionality, always in sync.
 *
 * v3.1.0: native bottom menu + side drawer for fast navigation to any
 * section of the site. v3.0.1: loads the fast URL first with automatic
 * failover, loading progress bar, offline Retry screen.
 */
public class MainActivity extends Activity {

    // Primary first (fast, always up), fallback second
    private static final String[] SITE_URLS = {
        "https://sageco-evergreen-co.vercel.app",
        "https://sagecoevergreen.publicvm.com"
    };
    private static final String UA_MARKER = "SagecoApp";
    private static final int FILE_CHOOSER_REQUEST = 1001;
    private static final long LOAD_TIMEOUT_MS = 20000;

    private static final int GREEN = Color.parseColor("#0A3D1F");
    private static final int GREEN_LIGHT = Color.parseColor("#7CFC9A");
    private static final int WHITE = Color.parseColor("#FFFFFF");

    private WebView webView;
    private LinearLayout errorView;
    private ProgressBar progressBar;
    private LinearLayout drawerPanel;
    private View drawerScrim;
    private LinearLayout bottomBar;
    private ValueCallback<Uri[]> fileChooserCallback;
    private Handler handler = new Handler(Looper.getMainLooper());
    private int currentUrlIndex = 0;
    private boolean pageFinished = false;
    private String currentPath = "/";

    private final Runnable loadTimeout = new Runnable() {
        @Override
        public void run() {
            if (!pageFinished && !isFinishing()) tryNextUrl();
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        FrameLayout content = new FrameLayout(this);
        content.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setUserAgentString(s.getUserAgentString() + " " + UA_MARKER);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri url = request.getUrl();
                String host = url.getHost();
                for (String siteUrl : SITE_URLS) {
                    if (Uri.parse(siteUrl).getHost().equals(host)) {
                        currentPath = url.getPath();
                        if (currentPath == null || currentPath.isEmpty()) currentPath = "/";
                        return false;
                    }
                }
                try { startActivity(new Intent(Intent.ACTION_VIEW, url)); }
                catch (Exception ignored) { }
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                pageFinished = false;
                progressBar.setVisibility(View.VISIBLE);
                handler.postDelayed(loadTimeout, LOAD_TIMEOUT_MS);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                pageFinished = true;
                handler.removeCallbacks(loadTimeout);
                progressBar.setVisibility(View.GONE);
                errorView.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                Uri u = Uri.parse(url);
                currentPath = u.getPath();
                if (currentPath == null || currentPath.isEmpty()) currentPath = "/";
                highlightTab();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) tryNextUrl();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (fileChooserCallback != null) fileChooserCallback.onReceiveValue(null);
                fileChooserCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), FILE_CHOOSER_REQUEST);
                    return true;
                } catch (Exception e) {
                    fileChooserCallback = null;
                    return false;
                }
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin,
                    GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setIndeterminate(newProgress < 10);
            }
        });

        // Loading progress bar
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.getProgressDrawable().setColorFilter(
                GREEN, android.graphics.PorterDuff.Mode.SRC_IN);
        FrameLayout.LayoutParams pbParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(4), Gravity.TOP);
        progressBar.setLayoutParams(pbParams);

        errorView = buildErrorView();

        // Side drawer (slide-in panel + dim scrim)
        drawerScrim = new View(this);
        drawerScrim.setBackgroundColor(Color.parseColor("#99000000"));
        drawerScrim.setVisibility(View.GONE);
        drawerScrim.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { closeDrawer(); }
        });

        drawerPanel = buildDrawer();

        content.addView(webView);
        content.addView(errorView);
        content.addView(progressBar);
        content.addView(drawerScrim);
        content.addView(drawerPanel);

        // Bottom navigation menu
        bottomBar = buildBottomBar();

        screen.addView(content);
        screen.addView(bottomBar);
        setContentView(screen);

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            currentUrlIndex = 0;
            webView.loadUrl(siteUrl("/"));
        }
    }

    private String siteUrl(String path) {
        String base = SITE_URLS[currentUrlIndex];
        if (!path.startsWith("/")) path = "/" + path;
        return base + path + "?app=true";
    }

    private void navigate(String path) {
        closeDrawer();
        currentPath = path;
        webView.loadUrl(siteUrl(path));
    }

    // ---------- Bottom menu ----------

    private LinearLayout buildBottomBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackgroundColor(GREEN);
        bar.setPadding(0, dp(6), 0, dp(6));
        bar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        bar.addView(tab("🏠", "Home", "/"));
        bar.addView(tab("🏘️", "Properties", "/properties"));
        bar.addView(tab("🛒", "Market", "/market"));
        bar.addView(tab("👤", "Account", "/account"));
        bar.addView(tab("☰", "Menu", null)); // opens the side drawer

        return bar;
    }

    private View tab(String icon, String label, final String path) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(4), 0, dp(4), 0);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        t.setTag(path);

        TextView ic = new TextView(this);
        ic.setText(icon);
        ic.setTextSize(18);
        ic.setGravity(Gravity.CENTER);
        ic.setTag("icon");
        t.addView(ic);

        TextView tx = new TextView(this);
        tx.setText(label);
        tx.setTextSize(11);
        tx.setGravity(Gravity.CENTER);
        tx.setTextColor(WHITE);
        tx.setTag("label");
        t.addView(tx);

        t.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (path == null) openDrawer();
                else navigate(path);
            }
        });
        return t;
    }

    private void highlightTab() {
        String p = currentPath == null ? "/" : currentPath;
        for (int i = 0; i < bottomBar.getChildCount(); i++) {
            View child = bottomBar.getChildAt(i);
            String tag = (String) child.getTag();
            boolean active = tag != null && (p.equals(tag)
                    || (tag.equals("/") && (p.equals("/index") || p.isEmpty())) || (tag.equals("/") && p.equals("/")));
            TextView ic = (TextView) ((LinearLayout) child).getChildAt(0);
            TextView tx = (TextView) ((LinearLayout) child).getChildAt(1);
            ic.setAlpha(active ? 1f : 0.75f);
            tx.setTextColor(active ? GREEN_LIGHT : WHITE);
            tx.setTypeface(active ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        }
    }

    // ---------- Side drawer ----------

    private LinearLayout buildDrawer() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundColor(WHITE);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                dp(280), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START);
        panel.setLayoutParams(lp);
        panel.setTranslationX(-dp(300));

        // Header
        TextView header = new TextView(this);
        header.setText("SageCo Evergreen");
        header.setTextColor(WHITE);
        header.setTextSize(18);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setBackgroundColor(GREEN);
        header.setPadding(dp(20), dp(28), dp(20), dp(28));
        panel.addView(header);

        TextView sub = new TextView(this);
        sub.setText("Navigate");
        sub.setTextColor(Color.GRAY);
        sub.setTextSize(12);
        sub.setTypeface(Typeface.DEFAULT_BOLD);
        sub.setPadding(dp(20), dp(14), dp(20), dp(4));
        panel.addView(sub);

        ScrollView scroller = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroller.addView(list);
        panel.addView(scroller, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        String[][] items = {
            {"🏠", "Home", "/"},
            {"🏘️", "Properties", "/properties"},
            {"🛒", "Market", "/market"},
            {"👥", "Brokers", "/brokers"},
            {"🤖", "AI Broker", "/ai-broker"},
            {"📏", "GPS Land Measure", "/gps-measure"},
            {"📅", "Book Viewing", "/book"},
            {"💎", "Valuation", "/valuation"},
            {"🔍", "Title Search", "/title-search"},
            {"💼", "Subscription Plans", "/plans"},
            {"🔐", "Escrow", "/escrow"},
            {"🌱", "Sustainability", "/eco"},
            {"❓", "FAQ", "/faq"},
            {"📞", "Contact", "/contact"},
            {"👤", "My Account", "/account"},
        };
        for (String[] item : items) {
            list.addView(drawerItem(item[0], item[1], item[2]));
        }

        return panel;
    }

    private View drawerItem(String icon, String label, final String path) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(20), dp(13), dp(20), dp(13));
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView ic = new TextView(this);
        ic.setText(icon);
        ic.setTextSize(16);
        row.addView(ic);

        TextView tx = new TextView(this);
        tx.setText(label);
        tx.setTextSize(15);
        tx.setTextColor(Color.parseColor("#222222"));
        tx.setPadding(dp(14), 0, 0, 0);
        row.addView(tx);

        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { navigate(path); }
        });
        return row;
    }

    private void openDrawer() {
        drawerScrim.setVisibility(View.VISIBLE);
        drawerScrim.setAlpha(0f);
        drawerScrim.animate().alpha(1f).setDuration(180).start();
        drawerPanel.animate().translationX(0f).setDuration(180).start();
    }

    private void closeDrawer() {
        if (drawerScrim.getVisibility() != View.VISIBLE) return;
        drawerScrim.animate().alpha(0f).setDuration(180)
                .withEndAction(new Runnable() {
                    @Override public void run() { drawerScrim.setVisibility(View.GONE); }
                }).start();
        drawerPanel.animate().translationX(-dp(300)).setDuration(180).start();
    }

    private boolean isDrawerOpen() {
        return drawerScrim.getVisibility() == View.VISIBLE;
    }

    // ---------- Offline screen ----------

    private LinearLayout buildErrorView() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(48, 96, 48, 96);
        box.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        box.setVisibility(View.GONE);
        box.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("Can't reach SageCo Evergreen");
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(GREEN);
        box.addView(title);

        TextView msg = new TextView(this);
        msg.setText("Check your internet connection and try again.");
        msg.setTextSize(14);
        msg.setGravity(Gravity.CENTER);
        msg.setPadding(0, 16, 0, 32);
        box.addView(msg);

        Button retry = new Button(this);
        retry.setText("Retry");
        retry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentUrlIndex = 0;
                errorView.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                webView.loadUrl(siteUrl("/"));
            }
        });
        box.addView(retry);
        return box;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void tryNextUrl() {
        if (currentUrlIndex + 1 < SITE_URLS.length) {
            currentUrlIndex++;
            webView.stopLoading();
            webView.loadUrl(siteUrl(currentPath));
        } else {
            showError();
        }
    }

    private void showError() {
        handler.removeCallbacks(loadTimeout);
        progressBar.setVisibility(View.GONE);
        webView.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);
    }

    // Back: close drawer → go back in site → exit
    @Override
    public void onBackPressed() {
        if (isDrawerOpen()) { closeDrawer(); return; }
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
