package com.novabrowser.app;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // UI Components
    private WebView webView;
    private EditText urlEditText;
    private ProgressBar progressBar;
    private ImageView securityIcon;
    private ImageView btnBack, btnForward, btnHome, btnTabs, btnMenu;
    private LinearLayout bottomNav, topBar;
    private LinearLayout webViewContainer;
    private ScrollView homeScreen;
    private LinearLayout findInPageBar;
    private EditText findEditText;
    private TextView findCount, tabCountText;
    private FrameLayout tabCountButton;

    // Split Screen & Tab Manager
    private View tabManagerContainer;
    private GridLayout tabsGrid;
    private View splitScreenDivider;
    private WebView webView2;
    private boolean isSplitScreen = false;
    private ImageView menuButton;
    private SwipeRefreshLayout swipeRefresh;
    private ListView urlSuggestions;

    // State
    private boolean isIncognito = false;
    private boolean isDesktopMode = false;
    private boolean isDarkMode = false;
    private String currentTheme = "default";
    private boolean adBlockerEnabled = true;
    private int tabCount = 1;
    private String currentSearchEngine = "google"; // google, bing, ddg
    private List<TabInfo> tabs = new ArrayList<>();
    private GithubUpdater githubUpdater;

    // Data
    private SharedPreferences prefs;
    private List<HistoryItem> historyItems = new ArrayList<>();
    private List<BookmarkItem> bookmarks = new ArrayList<>();
    private List<String> readingList = new ArrayList<>();

    // File chooser
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQUEST = 100;

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

    // Ad block list
    private final String[] AD_DOMAINS = {
        "doubleclick.net", "googlesyndication.com", "googletagservices.com",
        "adservice.google.com", "ads.yahoo.com", "advertising.com",
        "scorecardresearch.com", "quantserve.com", "adnxs.com",
        "outbrain.com", "taboola.com", "criteo.com", "adsystem.com",
        "tracking.com", "analytics.google.com", "facebook.com/tr"
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("NovaBrowser", Context.MODE_PRIVATE);
        isDarkMode = prefs.getBoolean("dark_mode", false);
        currentTheme = prefs.getString("theme_preference", "default");
        
        if ("cyberpunk".equals(currentTheme)) {
            setTheme(R.style.Theme_NovaBrowser_Cyberpunk);
        } else if ("glass".equals(currentTheme)) {
            setTheme(R.style.Theme_NovaBrowser_Glass);
        }

        super.onCreate(savedInstanceState);

        adBlockerEnabled = prefs.getBoolean("ad_blocker", true);
        currentSearchEngine = prefs.getString("search_engine", "google");

        // Apply dark mode
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        setContentView(R.layout.activity_main);
        githubUpdater = new GithubUpdater(this);

        initViews();
        setupWebView();
        setupUrlBar();
        setupBottomNav();
        setupHomeScreen();
        loadBookmarks();
        loadHistory();
        loadReadingList();

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
        swipeRefresh = findViewById(R.id.swipeRefresh);
        urlSuggestions = findViewById(R.id.urlSuggestions);

        // Split Screen & Tab Manager
        tabManagerContainer = findViewById(R.id.tabManagerContainer);
        tabsGrid = findViewById(R.id.tabsGrid);
        splitScreenDivider = findViewById(R.id.splitScreenDivider);
        webView2 = findViewById(R.id.webView2);

        if (findViewById(R.id.btnCloseTabs) != null) {
            findViewById(R.id.btnCloseTabs).setOnClickListener(v -> hideTabManager());
            findViewById(R.id.btnNewTab).setOnClickListener(v -> { hideTabManager(); loadUrl("about:blank"); });
            findViewById(R.id.btnSplitScreen).setOnClickListener(v -> toggleSplitScreen());
        }
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
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.addJavascriptInterface(new NovaJsInterface(), "NovaApp");
        webView.setWebViewClient(new NovaWebViewClient());
        webView.setWebChromeClient(new NovaWebChromeClient());
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) ->
            downloadFile(url, contentDisposition, mimeType));
            
        // Configure webView2 for Split Screen
        WebSettings settings2 = webView2.getSettings();
        settings2.setJavaScriptEnabled(true);
        settings2.setDomStorageEnabled(true);
        settings2.setLoadWithOverviewMode(true);
        settings2.setUseWideViewPort(true);
        settings2.setBuiltInZoomControls(true);
        settings2.setDisplayZoomControls(false);
        webView2.setWebViewClient(new NovaWebViewClient());
        webView2.setWebChromeClient(new NovaWebChromeClient());

        // SwipeRefresh setup
        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.secondary, R.color.accent);
        swipeRefresh.setOnRefreshListener(() -> {
            if (webView.getUrl() != null && !webView.getUrl().equals("about:blank")) {
                webView.reload();
            } else {
                swipeRefresh.setRefreshing(false);
            }
        });
    }

    private void setupUrlBar() {
        urlEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                loadUrl(urlEditText.getText().toString().trim());
                hideKeyboard();
                urlSuggestions.setVisibility(View.GONE);
                return true;
            }
            return false;
        });

        urlEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                urlEditText.selectAll();
                animateUrlBarExpand();
                showSuggestions(urlEditText.getText().toString());
            } else {
                animateUrlBarCollapse();
                urlSuggestions.setVisibility(View.GONE);
            }
        });

        // Autocomplete from history
        urlEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                showSuggestions(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        urlSuggestions.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            loadUrl(selected);
            urlSuggestions.setVisibility(View.GONE);
        });

        tabCountButton.setOnClickListener(v -> showTabManager());
        menuButton.setOnClickListener(v -> showMenu());

        // Find in page
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

    private void showSuggestions(String query) {
        if (query == null || query.length() < 2 || historyItems.isEmpty()) {
            urlSuggestions.setVisibility(View.GONE);
            return;
        }
        List<String> matches = new ArrayList<>();
        String q = query.toLowerCase();
        for (HistoryItem h : historyItems) {
            if ((h.url.toLowerCase().contains(q) || h.title.toLowerCase().contains(q))
                    && !matches.contains(h.url)) {
                matches.add(h.url);
                if (matches.size() >= 5) break;
            }
        }
        if (matches.isEmpty()) {
            urlSuggestions.setVisibility(View.GONE);
            return;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_list_item_1, matches);
        urlSuggestions.setAdapter(adapter);
        urlSuggestions.setVisibility(View.VISIBLE);
    }

    private void setupBottomNav() {
        btnBack.setOnClickListener(v -> { if (webView.canGoBack()) webView.goBack(); });
        btnBack.setOnLongClickListener(v -> { showHistoryDialog(); return true; });
        btnForward.setOnClickListener(v -> { if (webView.canGoForward()) webView.goForward(); });
        btnHome.setOnClickListener(v -> showHomeScreen());
        btnTabs.setOnClickListener(v -> showTabManager());
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

        // Search engine chips
        TextView chipGoogle = homeScreen.findViewById(R.id.chipGoogle);
        TextView chipBing = homeScreen.findViewById(R.id.chipBing);
        TextView chipDDG = homeScreen.findViewById(R.id.chipDDG);
        updateSearchEngineChips(chipGoogle, chipBing, chipDDG);

        if (chipGoogle != null) chipGoogle.setOnClickListener(v -> {
            currentSearchEngine = "google";
            prefs.edit().putString("search_engine", "google").apply();
            updateSearchEngineChips(chipGoogle, chipBing, chipDDG);
            Toast.makeText(this, "🔍 Google Search", Toast.LENGTH_SHORT).show();
        });
        if (chipBing != null) chipBing.setOnClickListener(v -> {
            currentSearchEngine = "bing";
            prefs.edit().putString("search_engine", "bing").apply();
            updateSearchEngineChips(chipGoogle, chipBing, chipDDG);
            Toast.makeText(this, "🔍 Bing Search", Toast.LENGTH_SHORT).show();
        });
        if (chipDDG != null) chipDDG.setOnClickListener(v -> {
            currentSearchEngine = "ddg";
            prefs.edit().putString("search_engine", "ddg").apply();
            updateSearchEngineChips(chipGoogle, chipBing, chipDDG);
            Toast.makeText(this, "🔍 DuckDuckGo Search", Toast.LENGTH_SHORT).show();
        });

        // Dark Mode & Incognito cards
        CardView cardDarkMode = homeScreen.findViewById(R.id.cardDarkMode);
        CardView cardIncognito = homeScreen.findViewById(R.id.cardIncognito);
        if (cardDarkMode != null) cardDarkMode.setOnClickListener(v -> toggleDarkMode());
        if (cardIncognito != null) cardIncognito.setOnClickListener(v -> toggleIncognito());

        setupQuickLinks();
    }

    private void updateSearchEngineChips(TextView google, TextView bing, TextView ddg) {
        int selectedBg = R.drawable.chip_selected;
        int normalBg = R.drawable.chip_background;
        if (google == null || bing == null || ddg == null) return;
        google.setBackground(ContextCompat.getDrawable(this, "google".equals(currentSearchEngine) ? selectedBg : normalBg));
        google.setTextColor("google".equals(currentSearchEngine) ? 0xFFFFFFFF : ContextCompat.getColor(this, R.color.text_secondary));
        bing.setBackground(ContextCompat.getDrawable(this, "bing".equals(currentSearchEngine) ? selectedBg : normalBg));
        bing.setTextColor("bing".equals(currentSearchEngine) ? 0xFFFFFFFF : ContextCompat.getColor(this, R.color.text_secondary));
        ddg.setBackground(ContextCompat.getDrawable(this, "ddg".equals(currentSearchEngine) ? selectedBg : normalBg));
        ddg.setTextColor("ddg".equals(currentSearchEngine) ? 0xFFFFFFFF : ContextCompat.getColor(this, R.color.text_secondary));
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
            card.setCardElevation(4 * getResources().getDisplayMetrics().density);

            try { card.setCardBackgroundColor(android.graphics.Color.parseColor(color)); }
            catch (Exception e) { card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.primary)); }

            TextView icon = new TextView(this);
            icon.setText(name.substring(0, 1));
            icon.setTextColor(android.graphics.Color.WHITE);
            icon.setTextSize(22);
            icon.setGravity(Gravity.CENTER);
            icon.setTypeface(null, android.graphics.Typeface.BOLD);
            icon.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            card.addView(icon);

            TextView label = new TextView(this);
            label.setText(name);
            label.setTextSize(12);
            label.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            label.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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

    private String buildSearchUrl(String query) {
        String encoded = Uri.encode(query);
        switch (currentSearchEngine) {
            case "bing": return "https://www.bing.com/search?q=" + encoded;
            case "ddg":  return "https://duckduckgo.com/?q=" + encoded;
            default:     return "https://www.google.com/search?q=" + encoded;
        }
    }

    private void loadUrl(String input) {
        if (input == null || input.isEmpty()) return;
        String url;
        if (URLUtil.isValidUrl(input)) url = input;
        else if (input.contains(".") && !input.contains(" ")) url = "https://" + input;
        else url = buildSearchUrl(input);

        homeScreen.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        webView.loadUrl(url);
        urlEditText.setText(url);
        urlEditText.clearFocus();
        hideKeyboard();
        urlSuggestions.setVisibility(View.GONE);
        addToHistory(url);
    }

    private void showHomeScreen() {
        webView.setVisibility(View.INVISIBLE);
        homeScreen.setVisibility(View.VISIBLE);
        urlEditText.setText("");
        urlEditText.setHint(getString(R.string.search_hint));
    }

    private void showErrorState(String failingUrl, String errorMsg) {
        LinearLayout errorLayout = findViewById(R.id.errorPage);
        if (errorLayout != null) {
            TextView txtUrl = findViewById(R.id.errorUrl);
            TextView txtMsg = findViewById(R.id.errorMessage);
            View btnRetry = findViewById(R.id.btnErrorRetry);
            View btnHome = findViewById(R.id.btnErrorHome);

            if (txtUrl != null) txtUrl.setText(failingUrl != null ? failingUrl : "");
            if (txtMsg != null && errorMsg != null) txtMsg.setText(errorMsg);

            if (btnRetry != null) btnRetry.setOnClickListener(v -> {
                errorLayout.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                if (failingUrl != null) webView.loadUrl(failingUrl);
                else webView.reload();
            });

            if (btnHome != null) btnHome.setOnClickListener(v -> {
                errorLayout.setVisibility(View.GONE);
                showHomeScreen();
            });

            webView.setVisibility(View.GONE);
            homeScreen.setVisibility(View.GONE);
            errorLayout.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(this, "⚠️ Network Error: " + errorMsg, Toast.LENGTH_SHORT).show();
        }
    }

    private void showMenu() {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet =
            new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.Theme_NovaBrowser);
        View menuView = LayoutInflater.from(this).inflate(R.layout.layout_menu, null);
        bottomSheet.setContentView(menuView);
        setupMenuItems(menuView, bottomSheet);
        bottomSheet.show();
    }

    private void setupMenuItems(View menuView, com.google.android.material.bottomsheet.BottomSheetDialog dialog) {
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
        TextView menuReadingList = menuView.findViewById(R.id.menuReadingList);
        TextView menuDarkMode = menuView.findViewById(R.id.menuDarkMode);
        TextView menuPrint = menuView.findViewById(R.id.menuPrint);
        TextView menuTheme = menuView.findViewById(R.id.menuTheme);
        TextView menuUpdate = menuView.findViewById(R.id.menuUpdate);

        if (menuBookmarks != null) menuBookmarks.setOnClickListener(v -> { dialog.dismiss(); showBookmarksDialog(); });
        if (menuHistory != null) menuHistory.setOnClickListener(v -> { dialog.dismiss(); showHistoryDialog(); });
        if (menuIncognito != null) {
            menuIncognito.setText(isIncognito ? "Exit Incognito" : "🕵️  Incognito Mode");
            menuIncognito.setOnClickListener(v -> { dialog.dismiss(); toggleIncognito(); });
        }
        if (menuDesktop != null) {
            menuDesktop.setText(isDesktopMode ? "📱  Mobile Mode" : "💻  Desktop Mode");
            menuDesktop.setOnClickListener(v -> { dialog.dismiss(); toggleDesktopMode(); });
        }
        if (menuDarkMode != null) {
            menuDarkMode.setText(isDarkMode ? "☀️  Light Mode" : "🌙  Dark Mode");
            menuDarkMode.setOnClickListener(v -> { dialog.dismiss(); toggleDarkMode(); });
        }
        if (menuTheme != null) menuTheme.setOnClickListener(v -> { dialog.dismiss(); showThemeDialog(); });
        if (menuUpdate != null) menuUpdate.setOnClickListener(v -> { dialog.dismiss(); githubUpdater.checkForUpdates(true); });
        if (menuFindInPage != null) menuFindInPage.setOnClickListener(v -> { dialog.dismiss(); openFindInPage(); });
        if (menuShare != null) menuShare.setOnClickListener(v -> { dialog.dismiss(); shareCurrentPage(); });
        if (menuAddBookmark != null) menuAddBookmark.setOnClickListener(v -> { dialog.dismiss(); addBookmark(); });
        if (menuReadingList != null) menuReadingList.setOnClickListener(v -> { dialog.dismiss(); addToReadingList(); });
        if (menuSettings != null) menuSettings.setOnClickListener(v -> { dialog.dismiss(); openSettings(); });
        if (menuAdBlock != null) {
            menuAdBlock.setText(adBlockerEnabled ? "🛡️  Ad Blocker: ON" : "Ad Blocker: OFF");
            menuAdBlock.setOnClickListener(v -> { dialog.dismiss(); toggleAdBlocker(); });
        }
        if (menuReader != null) menuReader.setOnClickListener(v -> { dialog.dismiss(); toggleReaderMode(); });
        if (menuScreenshot != null) menuScreenshot.setOnClickListener(v -> { dialog.dismiss(); takeScreenshot(); });
        if (menuRefresh != null) menuRefresh.setOnClickListener(v -> { dialog.dismiss(); webView.reload(); });
        if (menuPrint != null) menuPrint.setOnClickListener(v -> { dialog.dismiss(); printPage(); });
    }

    private void showThemeDialog() {
        String[] themes = {"Default", "Cyberpunk", "Glass"};
        int checkedItem = 0;
        if ("cyberpunk".equals(currentTheme)) checkedItem = 1;
        else if ("glass".equals(currentTheme)) checkedItem = 2;

        new AlertDialog.Builder(this)
            .setTitle("🎨 Select Theme")
            .setSingleChoiceItems(themes, checkedItem, (dialog, which) -> {
                String selected = "default";
                if (which == 1) selected = "cyberpunk";
                else if (which == 2) selected = "glass";
                
                prefs.edit().putString("theme_preference", selected).apply();
                dialog.dismiss();
                recreate();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void toggleDarkMode() {
        isDarkMode = !isDarkMode;
        prefs.edit().putBoolean("dark_mode", isDarkMode).apply();
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            Toast.makeText(this, "🌙 Dark Mode ON", Toast.LENGTH_SHORT).show();
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            Toast.makeText(this, "☀️ Light Mode", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleIncognito() {
        isIncognito = !isIncognito;
        if (isIncognito) {
            webView.getSettings().setDomStorageEnabled(false);
            CookieManager.getInstance().setAcceptCookie(false);
            Toast.makeText(this, "🕵️ Incognito Mode ON", Toast.LENGTH_SHORT).show();
        } else {
            webView.getSettings().setDomStorageEnabled(true);
            CookieManager.getInstance().setAcceptCookie(true);
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
            settings.setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
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
        Toast.makeText(this, adBlockerEnabled ? "🛡️ Ad Blocker ON" : "Ad Blocker OFF", Toast.LENGTH_SHORT).show();
        webView.reload();
    }

    private void toggleReaderMode() {
        String readerJs = "javascript:(function(){" +
            "var body = document.body;" +
            "var article = document.querySelector('article') || document.querySelector('main') || body;" +
            "var content = article.innerText;" +
            "var bg = '" + (isDarkMode ? "#1a1a2e" : "#FAFAFA") + "';" +
            "var fg = '" + (isDarkMode ? "#E0E0E0" : "#222222") + "';" +
            "var btnBg = '" + (isDarkMode ? "#6C63FF" : "#6C63FF") + "';" +
            "var btnHtml = '<div style=\"text-align:center;margin:20px 0;\"><button onclick=\"window.NovaApp.requestAiSummary(document.querySelector(\\'article\\') ? document.querySelector(\\'article\\').innerText : document.body.innerText)\" style=\"background:'+btnBg+';color:#fff;border:none;padding:12px 24px;border-radius:24px;font-size:16px;cursor:pointer;box-shadow:0 4px 6px rgba(0,0,0,0.1);font-weight:bold;\">✨ AI Summary</button></div>';" +
            "document.body.innerHTML = '<div style=\"max-width:680px;margin:40px auto;padding:24px;font-family:Georgia,serif;font-size:18px;line-height:1.8;color:'+fg+';background:'+bg+'\">' +" +
            "'<h1 style=\"font-size:28px;margin-bottom:20px;color:'+fg+'\">' + document.title + '</h1>' +" +
            "btnHtml +" +
            "'<p>' + content.replace(/\\n\\n/g,'</p><p>') + '</p></div>';" +
            "document.body.style.background=bg;" +
            "})()";
        webView.loadUrl(readerJs);
        Toast.makeText(this, "📖 Reader Mode", Toast.LENGTH_SHORT).show();
    }

    private void showAiSummary(String text) {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_ai_summary, null);
        dialog.setContentView(view);
        
        TextView aiSummaryText = view.findViewById(R.id.aiSummaryText);
        TextView aiLoadingIndicator = view.findViewById(R.id.aiLoadingIndicator);
        
        dialog.show();
        
        // Simulate AI Processing delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            aiLoadingIndicator.setText("Done");
            aiSummaryText.setText(generateMockSummary(text));
        }, 1500);
    }
    
    private String generateMockSummary(String text) {
        if (text == null || text.length() < 50) return "Not enough content to summarize.";
        String[] sentences = text.split("\\.");
        StringBuilder summary = new StringBuilder("Key Takeaways:\n\n");
        int count = 0;
        for (String s : sentences) {
            if (s.trim().length() > 20) {
                summary.append("• ").append(s.trim()).append(".\n\n");
                count++;
            }
            if (count >= 3) break;
        }
        return summary.toString();
    }

    private void printPage() {
        android.print.PrintManager printManager = (android.print.PrintManager) getSystemService(Context.PRINT_SERVICE);
        if (printManager != null) {
            String jobName = webView.getTitle() != null ? webView.getTitle() : "Nova Browser Print";
            android.print.PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter(jobName);
            android.print.PrintAttributes attributes = new android.print.PrintAttributes.Builder()
                .setMediaSize(android.print.PrintAttributes.MediaSize.ISO_A4)
                .build();
            printManager.print(jobName, printAdapter, attributes);
            Toast.makeText(this, "🖨️ Print dialog opened", Toast.LENGTH_SHORT).show();
        }
    }

    private void addToReadingList() {
        String url = webView.getUrl();
        if (url == null) { Toast.makeText(this, "No page loaded", Toast.LENGTH_SHORT).show(); return; }
        if (!readingList.contains(url)) {
            readingList.add(0, url);
            saveReadingList();
            Toast.makeText(this, "📚 Added to Reading List", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Already in Reading List", Toast.LENGTH_SHORT).show();
        }
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
        bookmarks.add(0, new BookmarkItem(title != null ? title : url, url, System.currentTimeMillis()));
        saveBookmarks();
        Toast.makeText(this, "⭐ Bookmarked!", Toast.LENGTH_SHORT).show();
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void showBookmarksDialog() {
        if (bookmarks.isEmpty()) { Toast.makeText(this, getString(R.string.no_bookmarks), Toast.LENGTH_SHORT).show(); return; }
        String[] items = new String[bookmarks.size()];
        for (int i = 0; i < bookmarks.size(); i++) items[i] = bookmarks.get(i).title;
        new AlertDialog.Builder(this).setTitle("⭐ Bookmarks")
            .setItems(items, (d, w) -> loadUrl(bookmarks.get(w).url))
            .setNegativeButton("Close", null).show();
    }

    private void showHistoryDialog() {
        if (historyItems.isEmpty()) { Toast.makeText(this, getString(R.string.no_history), Toast.LENGTH_SHORT).show(); return; }
        int cnt = Math.min(historyItems.size(), 30);
        String[] items = new String[cnt];
        for (int i = 0; i < cnt; i++) items[i] = historyItems.get(i).title;
        new AlertDialog.Builder(this).setTitle("🕐 History")
            .setItems(items, (d, w) -> loadUrl(historyItems.get(w).url))
            .setNeutralButton("Clear", (d, w) -> { historyItems.clear(); saveHistory(); Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show(); })
            .setNegativeButton("Close", null).show();
    }

    private void showTabsDialog() {
        String[] options = {"+ New Tab", "+ New Incognito Tab"};
        new AlertDialog.Builder(this).setTitle("Tabs (" + tabCount + ")")
            .setItems(options, (d, w) -> openNewTab(w == 1))
            .setNegativeButton("Close", null).show();
    }

    private void openNewTab(boolean incognito) {
        tabCount++;
        tabCountText.setText(String.valueOf(tabCount));
        if (incognito) { isIncognito = true; CookieManager.getInstance().setAcceptCookie(false); }
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
        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm != null) { dm.enqueue(request); Toast.makeText(this, "⬇️ Downloading: " + filename, Toast.LENGTH_SHORT).show(); }
    }

    private void takeScreenshot() {
        webView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(webView.getDrawingCache());
        webView.setDrawingCacheEnabled(false);
        if (bitmap != null) {
            File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "nova_" + System.currentTimeMillis() + ".png");
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
        ObjectAnimator sx = ObjectAnimator.ofFloat(urlEditText, "scaleX", 1f, 1.02f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(urlEditText, "scaleY", 1f, 1.02f);
        AnimatorSet s = new AnimatorSet(); s.playTogether(sx, sy);
        s.setDuration(200); s.setInterpolator(new DecelerateInterpolator()); s.start();
    }

    private void animateUrlBarCollapse() {
        ObjectAnimator sx = ObjectAnimator.ofFloat(urlEditText, "scaleX", 1.02f, 1f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(urlEditText, "scaleY", 1.02f, 1f);
        AnimatorSet s = new AnimatorSet(); s.playTogether(sx, sy); s.setDuration(200); s.start();
    }

    private void animateTabCount() {
        ObjectAnimator.ofFloat(tabCountText, "scaleX", 1f, 1.4f, 1f).setDuration(300);
    }

    private void animateItemClick(View v) {
        ObjectAnimator sx = ObjectAnimator.ofFloat(v, "scaleX", 1f, 0.92f, 1f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(v, "scaleY", 1f, 0.92f, 1f);
        AnimatorSet s = new AnimatorSet(); s.playTogether(sx, sy); s.setDuration(200); s.start();
    }

    // Persistence
    private void addToHistory(String url) {
        if (isIncognito) return;
        String title = webView.getTitle() != null ? webView.getTitle() : url;
        historyItems.add(0, new HistoryItem(title, url, System.currentTimeMillis()));
        if (historyItems.size() > 100) historyItems.remove(historyItems.size() - 1);
        saveHistory();
    }

    private void saveHistory() {
        try {
            JSONArray arr = new JSONArray();
            for (HistoryItem h : historyItems) {
                JSONObject o = new JSONObject(); o.put("title", h.title); o.put("url", h.url); o.put("time", h.timestamp); arr.put(o);
            }
            prefs.edit().putString("history", arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void loadHistory() {
        try {
            JSONArray arr = new JSONArray(prefs.getString("history", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                historyItems.add(new HistoryItem(o.getString("title"), o.getString("url"), o.getLong("time")));
            }
        } catch (Exception ignored) {}
    }

    private void saveBookmarks() {
        try {
            JSONArray arr = new JSONArray();
            for (BookmarkItem b : bookmarks) {
                JSONObject o = new JSONObject(); o.put("title", b.title); o.put("url", b.url); o.put("time", b.timestamp); arr.put(o);
            }
            prefs.edit().putString("bookmarks", arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void loadBookmarks() {
        try {
            JSONArray arr = new JSONArray(prefs.getString("bookmarks", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                bookmarks.add(new BookmarkItem(o.getString("title"), o.getString("url"), o.getLong("time")));
            }
        } catch (Exception ignored) {}
    }

    private void saveReadingList() {
        try {
            JSONArray arr = new JSONArray();
            for (String url : readingList) arr.put(url);
            prefs.edit().putString("reading_list", arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void loadReadingList() {
        try {
            JSONArray arr = new JSONArray(prefs.getString("reading_list", "[]"));
            for (int i = 0; i < arr.length(); i++) readingList.add(arr.getString(i));
        } catch (Exception ignored) {}
    }

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
        if (findInPageBar.getVisibility() == View.VISIBLE) closeFindInPage();
        else if (webView.canGoBack()) webView.goBack();
        else showHomeScreen();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && filePathCallback != null) {
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                String s = data.getDataString();
                if (s != null) results = new Uri[]{Uri.parse(s)};
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    // ===== WebViewClient =====
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
            securityIcon.setVisibility(View.VISIBLE);
            if (url.startsWith("https://"))
                securityIcon.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.secure_icon));
            else
                securityIcon.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.insecure_icon));
            btnBack.setAlpha(view.canGoBack() ? 1.0f : 0.4f);
            btnForward.setAlpha(view.canGoForward() ? 1.0f : 0.4f);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
            swipeRefresh.setRefreshing(false);
            urlEditText.setText(url);
            addToHistory(url);
            btnBack.setAlpha(view.canGoBack() ? 1.0f : 0.4f);
            btnForward.setAlpha(view.canGoForward() ? 1.0f : 0.4f);

            // Apply Arc-style Dynamic UI Tinting (Chameleon Theme)
            String tintJs = "javascript:(function(){" +
                "var col = '';" +
                "var meta = document.querySelector('meta[name=\"theme-color\"]');" +
                "if(meta && meta.content) { col = meta.content; }" +
                "if(!col) {" +
                "  var bg = window.getComputedStyle(document.body).backgroundColor;" +
                "  if(bg && bg !== 'rgba(0, 0, 0, 0)' && bg !== 'transparent') { col = bg; }" +
                "}" +
                "if(col) { window.NovaApp.setDominantColor(col); }" +
                "})()";
            view.loadUrl(tintJs);

        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            showErrorState(failingUrl, description);
        }

        private boolean isAdUrl(String url) {
            for (String d : AD_DOMAINS) if (url.contains(d)) return true;
            return false;
        }
    }

    // ===== WebChromeClient =====
    private class NovaWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            progressBar.setProgress(newProgress);
            progressBar.setVisibility(newProgress == 100 ? View.GONE : View.VISIBLE);
            if (newProgress == 100) swipeRefresh.setRefreshing(false);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) { setTitle(title); }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback cb) {
            cb.invoke(origin, true, false);
        }

        @Override
        public void onPermissionRequest(PermissionRequest request) {
            request.grant(request.getResources());
        }

        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> fileCb, FileChooserParams params) {
            filePathCallback = fileCb;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(Intent.createChooser(intent, "Choose File"), FILE_CHOOSER_REQUEST);
            return true;
        }
    }

    // ===== Javascript Interface =====
    private class NovaJsInterface {
        @android.webkit.JavascriptInterface
        public void setDominantColor(String colorString) {
            runOnUiThread(() -> {
                try {
                    int color = android.graphics.Color.parseColor(colorString);
                    // Dim the color slightly to ensure icons are visible, or just apply it
                    topBar.setBackgroundColor(color);
                    bottomNav.setBackgroundColor(color);
                    getWindow().setStatusBarColor(color);
                    getWindow().setNavigationBarColor(color);
                } catch (Exception e) {}
            });
        }

        @android.webkit.JavascriptInterface
        public void requestAiSummary(String text) {
            runOnUiThread(() -> showAiSummary(text));
        }
    }

    // ===== Tab Manager & Split Screen Logic =====
    private void showTabManager() {
        tabManagerContainer.setVisibility(View.VISIBLE);
        // Fade in animation
        tabManagerContainer.setAlpha(0f);
        tabManagerContainer.animate().alpha(1f).setDuration(300).start();
        
        // Populate Grid
        tabsGrid.removeAllViews();
        addTabCard("Main View", webView.getUrl() != null ? webView.getUrl() : "New Tab");
        if (isSplitScreen) {
            addTabCard("Split View", webView2.getUrl() != null ? webView2.getUrl() : "New Tab");
        }
    }

    private void hideTabManager() {
        tabManagerContainer.animate().alpha(0f).setDuration(200).withEndAction(() -> 
            tabManagerContainer.setVisibility(View.GONE)
        ).start();
    }

    private void addTabCard(String title, String subtitle) {
        CardView card = new CardView(this);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = 300;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(16, 16, 16, 16);
        card.setLayoutParams(params);
        card.setRadius(24f);
        card.setCardElevation(8f);
        card.setCardBackgroundColor(android.graphics.Color.parseColor(isDarkMode ? "#252547" : "#FFFFFF"));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);
        
        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(android.graphics.Color.parseColor(isDarkMode ? "#FFFFFF" : "#000000"));
        
        TextView tvSub = new TextView(this);
        tvSub.setText(subtitle);
        tvSub.setTextSize(12f);
        tvSub.setTextColor(android.graphics.Color.GRAY);
        tvSub.setMaxLines(2);

        layout.addView(tvTitle);
        layout.addView(tvSub);
        card.addView(layout);
        
        card.setOnClickListener(v -> hideTabManager());
        tabsGrid.addView(card);
    }

    private void toggleSplitScreen() {
        hideTabManager();
        isSplitScreen = !isSplitScreen;
        if (isSplitScreen) {
            webView2.setVisibility(View.VISIBLE);
            splitScreenDivider.setVisibility(View.VISIBLE);
            if (webView2.getUrl() == null) {
                webView2.loadUrl("https://google.com");
            }
            Toast.makeText(this, "◫ Split View Enabled", Toast.LENGTH_SHORT).show();
            tabCountText.setText("2");
        } else {
            webView2.setVisibility(View.GONE);
            splitScreenDivider.setVisibility(View.GONE);
            Toast.makeText(this, "🔲 Split View Disabled", Toast.LENGTH_SHORT).show();
            tabCountText.setText("1");
        }
    }

    // ===== Data Classes =====
    static class TabInfo { String title, url; TabInfo(String t, String u) { title=t; url=u; } }
    static class HistoryItem { String title, url; long timestamp; HistoryItem(String t, String u, long ts) { title=t; url=u; timestamp=ts; } }
    static class BookmarkItem { String title, url; long timestamp; BookmarkItem(String t, String u, long ts) { title=t; url=u; timestamp=ts; } }
}
