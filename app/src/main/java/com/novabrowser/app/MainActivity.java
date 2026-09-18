package com.novabrowser.app;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // UI Components
    private WebView webView;
    private EditText urlEditText;
    private ProgressBar progressBar;
    private ImageView securityIcon;
    private ImageView btnBack, btnForward, btnHome, btnTabs, btnMenu;
    private LinearLayout bottomNav, topBar;
    private FrameLayout webViewContainer;
    private ScrollView homeScreen;
    private LinearLayout findInPageBar;
    private EditText findEditText;
    private TextView findCount, tabCountText;
    private FrameLayout tabCountButton;
    private ImageView menuButton;

    // State
    private boolean isIncognito = false;
    private boolean isDesktopMode = false;
    private boolean isReaderMode = false;
    private boolean adBlockerEnabled = true;
    private int tabCount = 1;
    private List<TabInfo> tabs = new ArrayList<>();
    private int currentTabIndex = 0;

    // Data
    private SharedPreferences prefs;
    private List<HistoryItem> historyItems = new ArrayList<>();
    private List<BookmarkItem> bookmarks = new ArrayList<>();

    // File chooser
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQUEST = 100;
    private static final int PERMISSION_REQUEST = 200;

    // Quick links
    private final String[][] QUICK_LINKS = {
        {"Google", "https://google.com", "#4285F4"},
        {"YouTube", "https://youtube.com", "#FF0000"},
        {"Twitter", "https://twitter.com", "#1DA1F2"},
        {"Reddit", "https://reddit.com", "#FF5722"},
        {"GitHub", "https://github.com", "#333333"},
        {"Wikipedia", "https://wikipedia.org", "#636466"},
        {"Netflix", "https://netflix.com", "#E50914"},
        {"Maps", "https://maps.google.com", "#34A853"}
    };

    // Ad block list (simple domain-based)
    private final String[] AD_DOMAINS = {
        "doubleclick.net", "googlesyndication.com", "googletagservices.com",
        "adservice.google.com", "ads.yahoo.com", "advertising.com",
        "scorecardresearch.com", "quantserve.com", "adnxs.com",
        "outbrain.com", "taboola.com", "criteo.com", "adsystem.com"
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("NovaBrowser", Context.MODE_PRIVATE);
        adBlockerEnabled = prefs.getBoolean("ad_blocker", true);

        initViews();
        setupWebView();
        setupUrlBar();
        setupBottomNav();
        setupHomeScreen();
        loadBookmarks();
        loadHistory();

        // Handle incoming URL from SplashActivity
        String url = getIntent().getStringExtra("url");
        if (url != null) {
            loadUrl(url);
        } else {
            showHomeScreen();
        }
    }

    private void initViews() {
        webView = findViewById(R.id.webView);
        urlEditText = findViewById(R.id.urlEditText);
        progressBar = findViewById(R.id.progressBar);
        securityIcon = findViewById(R.id.securityIcon);
        btnBack = findViewById(R.id.btnBack);
        btnForward = findViewById(R.id.btnForward);
        btnHome = findViewById(R.id.btnHome);
        btnTabs = findViewById(R.id.btnTabs);
        btnMenu = findViewById(R.id.btnMenu);
        bottomNav = findViewById(R.id.bottomNav);
        topBar = findViewById(R.id.topBar);
        webViewContainer = findViewById(R.id.webViewContainer);
        homeScreen = findViewById(R.id.homeScreen);
        findInPageBar = findViewById(R.id.findInPageBar);
        findEditText = findViewById(R.id.findEditText);
        findCount = findViewById(R.id.findCount);
        tabCountText = findViewById(R.id.tabCountText);
        tabCountButton = findViewById(R.id.tabCountButton);
        menuButton = findViewById(R.id.menuButton);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);
        settings.setDatabaseEnabled(true);
        settings.setGeolocationEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        // Cookie support
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new NovaWebViewClient());
        webView.setWebChromeClient(new NovaWebChromeClient());
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            downloadFile(url, contentDisposition, mimeType);
        });
    }

    private void setupUrlBar() {
        urlEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                loadUrl(urlEditText.getText().toString().trim());
                hideKeyboard();
                return true;
            }
            return false;
        });

        urlEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                urlEditText.selectAll();
                animateUrlBarExpand();
            } else {
                animateUrlBarCollapse();
            }
        });

        tabCountButton.setOnClickListener(v -> showTabsDialog());
        menuButton.setOnClickListener(v -> showMenu());

        // Find in page buttons
        ImageView findPrev = findViewById(R.id.findPrev);
        ImageView findNext = findViewById(R.id.findNext);
        ImageView findClose = findViewById(R.id.findClose);

        findPrev.setOnClickListener(v -> webView.findNext(false));
        findNext.setOnClickListener(v -> webView.findNext(true));
        findClose.setOnClickListener(v -> closeFindInPage());

        findEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                webView.findAllAsync(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupBottomNav() {
        btnBack.setOnClickListener(v -> { if (webView.canGoBack()) webView.goBack(); });
        btnBack.setOnLongClickListener(v -> { showHistoryDialog(); return true; });
        btnForward.setOnClickListener(v -> { if (webView.canGoForward()) webView.goForward(); });
        btnHome.setOnClickListener(v -> showHomeScreen());
        btnTabs.setOnClickListener(v -> showTabsDialog());
        btnMenu.setOnClickListener(v -> showMenu());
    }

    private void setupHomeScreen() {
        LinearLayout homeSearchBar = homeScreen.findViewById(R.id.homeSearchBar);
        if (homeSearchBar != null) {
            homeSearchBar.setOnClickListener(v -> {
                urlEditText.requestFocus();
                showKeyboard();
            });
        }
        setupQuickLinks();
    }

    private void setupQuickLinks() {
        GridLayout grid = homeScreen.findViewById(R.id.quickLinksGrid);
        if (grid == null) return;

        for (String[] link : QUICK_LINKS) {
            String name = link[0];
            String url = link[1];
            String color = link[2];

            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(8, 8, 8, 8);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            item.setLayoutParams(params);

            CardView card = new CardView(this);
            int sizeDp = (int) (60 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(sizeDp, sizeDp);
            card.setLayoutParams(cardParams);
            card.setRadius(16 * getResources().getDisplayMetrics().density);
            card.setCardElevation(3 * getResources().getDisplayMetrics().density);

            try {
                card.setCardBackgroundColor(android.graphics.Color.parseColor(color));
            } catch (Exception e) {
                card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.primary));
            }

            TextView icon = new TextView(this);
            icon.setText(name.substring(0, 1));
            icon.setTextColor(android.graphics.Color.WHITE);
            icon.setTextSize(22);
            icon.setGravity(Gravity.CENTER);
            icon.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            icon.setLayoutParams(iconParams);
            card.addView(icon);

            TextView label = new TextView(this);
            label.setText(name);
            label.setTextSize(12);
            label.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            label.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            labelParams.topMargin = (int) (4 * getResources().getDisplayMetrics().density);
            label.setLayoutParams(labelParams);

            item.addView(card);
            item.addView(label);

            final String finalUrl = url;
            item.setOnClickListener(v -> {
                animateItemClick(item);
                loadUrl(finalUrl);
            });

            item.setBackground(ContextCompat.getDrawable(this, R.drawable.menu_item_background));

            grid.addView(item);
        }
    }

    private void loadUrl(String input) {
        if (input == null || input.isEmpty()) return;

        String url;
        if (URLUtil.isValidUrl(input)) {
            url = input;
        } else if (input.contains(".") && !input.contains(" ")) {
            url = "https://" + input;
        } else {
            // Search
            url = "https://www.google.com/search?q=" + Uri.encode(input);
        }

        homeScreen.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        webView.loadUrl(url);
        urlEditText.setText(url);
        urlEditText.clearFocus();
        hideKeyboard();

        // Save to history
        addToHistory(url);
    }

    private void showHomeScreen() {
        webView.setVisibility(View.INVISIBLE);
        homeScreen.setVisibility(View.VISIBLE);
        urlEditText.setText("");
        urlEditText.setHint(getString(R.string.search_hint));
    }

    private void showMenu() {
        View menuView = LayoutInflater.from(this).inflate(R.layout.layout_menu, null);
        PopupWindow popup = new PopupWindow(menuView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setElevation(16);
        popup.setOutsideTouchable(true);

        setupMenuItems(menuView, popup);

        popup.showAtLocation(bottomNav, Gravity.BOTTOM | Gravity.END, 24, 56);
    }

    private void setupMenuItems(View menuView, PopupWindow popup) {
        TextView menuBookmarks = menuView.findViewById(R.id.menuBookmarks);
        TextView menuHistory = menuView.findViewById(R.id.menuHistory);
        TextView menuIncognito = menuView.findViewById(R.id.menuIncognito);
        TextView menuDesktop = menuView.findViewById(R.id.menuDesktop);
        TextView menuFindInPage = menuView.findViewById(R.id.menuFindInPage);
        TextView menuShare = menuView.findViewById(R.id.menuShare);
        TextView menuAddBookmark = menuView.findViewById(R.id.menuAddBookmark);
        TextView menuSettings = menuView.findViewById(R.id.menuSettings);
        TextView menuAdBlock = menuView.findViewById(R.id.menuAdBlock);
        TextView menuReader = menuView.findViewById(R.id.menuReader);
        TextView menuScreenshot = menuView.findViewById(R.id.menuScreenshot);
        TextView menuRefresh = menuView.findViewById(R.id.menuRefresh);

        if (menuBookmarks != null) menuBookmarks.setOnClickListener(v -> { popup.dismiss(); showBookmarksDialog(); });
        if (menuHistory != null) menuHistory.setOnClickListener(v -> { popup.dismiss(); showHistoryDialog(); });
        if (menuIncognito != null) {
            menuIncognito.setText(isIncognito ? "Exit Incognito" : "Incognito Mode");
            menuIncognito.setOnClickListener(v -> { popup.dismiss(); toggleIncognito(); });
        }
        if (menuDesktop != null) {
            menuDesktop.setText(isDesktopMode ? "Mobile Mode" : "Desktop Mode");
            menuDesktop.setOnClickListener(v -> { popup.dismiss(); toggleDesktopMode(); });
        }
        if (menuFindInPage != null) menuFindInPage.setOnClickListener(v -> { popup.dismiss(); openFindInPage(); });
        if (menuShare != null) menuShare.setOnClickListener(v -> { popup.dismiss(); shareCurrentPage(); });
        if (menuAddBookmark != null) menuAddBookmark.setOnClickListener(v -> { popup.dismiss(); addBookmark(); });
        if (menuSettings != null) menuSettings.setOnClickListener(v -> { popup.dismiss(); openSettings(); });
        if (menuAdBlock != null) {
            menuAdBlock.setText(adBlockerEnabled ? "Ad Blocker: ON" : "Ad Blocker: OFF");
            menuAdBlock.setOnClickListener(v -> { popup.dismiss(); toggleAdBlocker(); });
        }
        if (menuReader != null) menuReader.setOnClickListener(v -> { popup.dismiss(); toggleReaderMode(); });
        if (menuScreenshot != null) menuScreenshot.setOnClickListener(v -> { popup.dismiss(); takeScreenshot(); });
        if (menuRefresh != null) menuRefresh.setOnClickListener(v -> { popup.dismiss(); webView.reload(); });
    }

    private void toggleIncognito() {
        isIncognito = !isIncognito;
        if (isIncognito) {
            webView.getSettings().setDomStorageEnabled(false);
            CookieManager.getInstance().setAcceptCookie(false);
            getWindow().getDecorView().setBackgroundColor(
                ContextCompat.getColor(this, R.color.incognito_background));
            Toast.makeText(this, "🕵️ Incognito Mode ON", Toast.LENGTH_SHORT).show();
        } else {
            webView.getSettings().setDomStorageEnabled(true);
            CookieManager.getInstance().setAcceptCookie(true);
            getWindow().getDecorView().setBackgroundColor(
                ContextCompat.getColor(this, R.color.background));
            Toast.makeText(this, "Incognito Mode OFF", Toast.LENGTH_SHORT).show();
        }
        webView.clearCache(true);
        webView.clearHistory();
        showHomeScreen();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void toggleDesktopMode() {
        isDesktopMode = !isDesktopMode;
        WebSettings settings = webView.getSettings();
        if (isDesktopMode) {
            settings.setUserAgentString(
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            Toast.makeText(this, "💻 Desktop Mode ON", Toast.LENGTH_SHORT).show();
        } else {
            settings.setUserAgentString(null);
            Toast.makeText(this, "📱 Mobile Mode", Toast.LENGTH_SHORT).show();
        }
        webView.reload();
    }

    private void toggleAdBlocker() {
        adBlockerEnabled = !adBlockerEnabled;
        prefs.edit().putBoolean("ad_blocker", adBlockerEnabled).apply();
        String msg = adBlockerEnabled ? "🛡️ Ad Blocker ON" : "Ad Blocker OFF";
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        webView.reload();
    }

    private void toggleReaderMode() {
        String readerJs = "javascript:(function(){" +
            "var body = document.body;" +
            "var article = document.querySelector('article') || document.querySelector('main') || body;" +
            "var content = article.innerText;" +
            "document.body.innerHTML = '<div style=\"max-width:680px;margin:40px auto;padding:20px;font-family:Georgia,serif;font-size:18px;line-height:1.8;color:#222;background:#FAFAFA\">' +" +
            "'<h1 style=\"font-size:28px;margin-bottom:20px\">' + document.title + '</h1>' +" +
            "'<p>' + content.replace(/\\n\\n/g,'</p><p>') + '</p></div>';" +
            "})()";
        webView.loadUrl(readerJs);
        Toast.makeText(this, "📖 Reader Mode", Toast.LENGTH_SHORT).show();
    }

    private void openFindInPage() {
        findInPageBar.setVisibility(View.VISIBLE);
        findInPageBar.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in));
        findEditText.requestFocus();
        showKeyboard();
    }

    private void closeFindInPage() {
        webView.clearMatches();
        findInPageBar.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_down_fade_out));
        findInPageBar.setVisibility(View.GONE);
        hideKeyboard();
    }

    private void shareCurrentPage() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, webView.getUrl());
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, webView.getTitle());
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    private void addBookmark() {
        String url = webView.getUrl();
        String title = webView.getTitle();
        if (url == null) { Toast.makeText(this, "No page loaded", Toast.LENGTH_SHORT).show(); return; }

        BookmarkItem bookmark = new BookmarkItem(title != null ? title : url, url,
            System.currentTimeMillis());
        bookmarks.add(0, bookmark);
        saveBookmarks();
        Toast.makeText(this, "⭐ Bookmarked!", Toast.LENGTH_SHORT).show();
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void showBookmarksDialog() {
        if (bookmarks.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_bookmarks), Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("⭐ Bookmarks");
        String[] items = new String[bookmarks.size()];
        for (int i = 0; i < bookmarks.size(); i++) {
            items[i] = bookmarks.get(i).title;
        }
        builder.setItems(items, (dialog, which) -> loadUrl(bookmarks.get(which).url));
        builder.setNegativeButton("Close", null);
        builder.show();
    }

    private void showHistoryDialog() {
        if (historyItems.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_history), Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🕐 History");
        int displayCount = Math.min(historyItems.size(), 30);
        String[] items = new String[displayCount];
        for (int i = 0; i < displayCount; i++) {
            items[i] = historyItems.get(i).title;
        }
        builder.setItems(items, (dialog, which) -> loadUrl(historyItems.get(which).url));
        builder.setNeutralButton("Clear", (dialog, which) -> {
            historyItems.clear();
            saveHistory();
            Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Close", null);
        builder.show();
    }

    private void showTabsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tabs (" + tabCount + ")");
        String[] options = {"+ New Tab", "+ New Incognito Tab"};
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) openNewTab(false);
            else openNewTab(true);
        });
        builder.setNegativeButton("Close", null);
        builder.show();
    }

    private void openNewTab(boolean incognito) {
        tabCount++;
        tabCountText.setText(String.valueOf(tabCount));
        if (incognito) {
            isIncognito = true;
            webView.clearCache(true);
            CookieManager.getInstance().setAcceptCookie(false);
        }
        webView.loadUrl("about:blank");
        showHomeScreen();
        animateTabCount();
        Toast.makeText(this, incognito ? "🕵️ Incognito Tab" : "New Tab", Toast.LENGTH_SHORT).show();
    }

    private void downloadFile(String url, String contentDisposition, String mimeType) {
        String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle(filename);
        request.setDescription("Downloading via Nova Browser");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
        request.setMimeType(mimeType);

        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm != null) {
            dm.enqueue(request);
            Toast.makeText(this, "⬇️ Downloading: " + filename, Toast.LENGTH_SHORT).show();
        }
    }

    private void takeScreenshot() {
        webView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(webView.getDrawingCache());
        webView.setDrawingCacheEnabled(false);
        if (bitmap != null) {
            File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "nova_screenshot_" + System.currentTimeMillis() + ".png");
            try {
                java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
                Toast.makeText(this, "📸 Screenshot saved!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Screenshot failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Animations
    private void animateUrlBarExpand() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(urlEditText, "scaleX", 1f, 1.02f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(urlEditText, "scaleY", 1f, 1.02f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.setDuration(200);
        set.setInterpolator(new DecelerateInterpolator());
        set.start();
    }

    private void animateUrlBarCollapse() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(urlEditText, "scaleX", 1.02f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(urlEditText, "scaleY", 1.02f, 1f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.setDuration(200);
        set.start();
    }

    private void animateTabCount() {
        ObjectAnimator anim = ObjectAnimator.ofFloat(tabCountText, "scaleX", 1f, 1.4f, 1f);
        anim.setDuration(300);
        anim.start();
    }

    private void animateItemClick(View v) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(v, "scaleX", 1f, 0.92f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(v, "scaleY", 1f, 0.92f, 1f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.setDuration(200);
        set.start();
    }

    // History management
    private void addToHistory(String url) {
        if (isIncognito) return;
        String title = webView.getTitle() != null ? webView.getTitle() : url;
        HistoryItem item = new HistoryItem(title, url, System.currentTimeMillis());
        historyItems.add(0, item);
        if (historyItems.size() > 100) historyItems.remove(historyItems.size() - 1);
        saveHistory();
    }

    private void saveHistory() {
        try {
            JSONArray arr = new JSONArray();
            for (HistoryItem h : historyItems) {
                JSONObject obj = new JSONObject();
                obj.put("title", h.title);
                obj.put("url", h.url);
                obj.put("time", h.timestamp);
                arr.put(obj);
            }
            prefs.edit().putString("history", arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void loadHistory() {
        try {
            String json = prefs.getString("history", "[]");
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                historyItems.add(new HistoryItem(obj.getString("title"),
                    obj.getString("url"), obj.getLong("time")));
            }
        } catch (Exception ignored) {}
    }

    private void saveBookmarks() {
        try {
            JSONArray arr = new JSONArray();
            for (BookmarkItem b : bookmarks) {
                JSONObject obj = new JSONObject();
                obj.put("title", b.title);
                obj.put("url", b.url);
                obj.put("time", b.timestamp);
                arr.put(obj);
            }
            prefs.edit().putString("bookmarks", arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void loadBookmarks() {
        try {
            String json = prefs.getString("bookmarks", "[]");
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                bookmarks.add(new BookmarkItem(obj.getString("title"),
                    obj.getString("url"), obj.getLong("time")));
            }
        } catch (Exception ignored) {}
    }

    // Keyboard helpers
    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(urlEditText, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(urlEditText.getWindowToken(), 0);
    }

    @Override
    public void onBackPressed() {
        if (findInPageBar.getVisibility() == View.VISIBLE) {
            closeFindInPage();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            showHomeScreen();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && filePathCallback != null) {
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                String dataString = data.getDataString();
                if (dataString != null) results = new Uri[]{Uri.parse(dataString)};
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    // ==================== WebViewClient ====================
    private class NovaWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            if (adBlockerEnabled && isAdUrl(url)) return true;
            view.loadUrl(url);
            return false;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(0);
            urlEditText.setText(url);

            // Security icon
            if (url.startsWith("https://")) {
                securityIcon.setVisibility(View.VISIBLE);
                securityIcon.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.secure_icon));
            } else {
                securityIcon.setVisibility(View.VISIBLE);
                securityIcon.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.insecure_icon));
            }

            // Update navigation buttons
            btnBack.setAlpha(view.canGoBack() ? 1.0f : 0.4f);
            btnForward.setAlpha(view.canGoForward() ? 1.0f : 0.4f);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
            urlEditText.setText(url);
            addToHistory(url);
            btnBack.setAlpha(view.canGoBack() ? 1.0f : 0.4f);
            btnForward.setAlpha(view.canGoForward() ? 1.0f : 0.4f);

            // Inject dark mode CSS if needed
            if (isIncognito) {
                String darkJs = "javascript:(function(){" +
                    "var style = document.createElement('style');" +
                    "style.innerHTML = '* { background-color: #1a1a2e !important; color: #e0e0e0 !important; } " +
                    "a { color: #9c5fff !important; }';" +
                    "document.head.appendChild(style);})()";
                view.loadUrl(darkJs);
            }
        }

        private boolean isAdUrl(String url) {
            for (String domain : AD_DOMAINS) {
                if (url.contains(domain)) return true;
            }
            return false;
        }
    }

    // ==================== WebChromeClient ====================
    private class NovaWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            progressBar.setProgress(newProgress);
            if (newProgress == 100) progressBar.setVisibility(View.GONE);
            else progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            setTitle(title);
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            callback.invoke(origin, true, false);
        }

        @Override
        public void onPermissionRequest(PermissionRequest request) {
            request.grant(request.getResources());
        }

        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                          FileChooserParams fileChooserParams) {
            MainActivity.this.filePathCallback = filePathCallback;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(Intent.createChooser(intent, "Choose File"), FILE_CHOOSER_REQUEST);
            return true;
        }
    }

    // ==================== Data Classes ====================
    static class TabInfo {
        String title, url;
        TabInfo(String title, String url) { this.title = title; this.url = url; }
    }

    static class HistoryItem {
        String title, url;
        long timestamp;
        HistoryItem(String title, String url, long timestamp) {
            this.title = title; this.url = url; this.timestamp = timestamp;
        }
    }

    static class BookmarkItem {
        String title, url;
        long timestamp;
        BookmarkItem(String title, String url, long timestamp) {
            this.title = title; this.url = url; this.timestamp = timestamp;
        }
    }
}
