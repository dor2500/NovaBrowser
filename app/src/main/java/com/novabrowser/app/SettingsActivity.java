package com.novabrowser.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("NovaBrowser", Context.MODE_PRIVATE);

        // Apply dark mode
        boolean darkMode = prefs.getBoolean("dark_mode", false);
        if (darkMode) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        setupUI();
    }

    private void setupUI() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 64, 32, 64);
        layout.setBackgroundColor(getResources().getColor(R.color.background, null));

        // Header
        TextView title = new TextView(this);
        title.setText("⚙️ Nova Settings");
        title.setTextSize(28);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(getResources().getColor(R.color.text_primary, null));
        title.setPadding(0, 0, 0, 8);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Customize your browsing experience");
        subtitle.setTextSize(14);
        subtitle.setTextColor(getResources().getColor(R.color.text_secondary, null));
        subtitle.setPadding(0, 0, 0, 40);
        layout.addView(subtitle);

        // === Privacy & Security ===
        addSectionHeader(layout, "🔒 Privacy & Security");
        addSwitch(layout, "🛡️ Ad Blocker", "Block ads and trackers automatically", prefs.getBoolean("ad_blocker", true), "ad_blocker");
        addSwitch(layout, "🍪 Block Third-party Cookies", "Prevent cross-site tracking", prefs.getBoolean("block_cookies", false), "block_cookies");
        addSwitch(layout, "⚡ JavaScript", "Enable JavaScript on websites", prefs.getBoolean("javascript", true), "javascript");

        // === Appearance ===
        addSectionHeader(layout, "🎨 Appearance");
        addSwitch(layout, "🌙 Dark Mode", "Apply dark theme to app and websites", prefs.getBoolean("dark_mode", false), "dark_mode", true);
        addSwitch(layout, "🔤 Large Text", "Increase font size for readability", prefs.getBoolean("large_text", false), "large_text");

        // === Browsing ===
        addSectionHeader(layout, "🌐 Browsing");
        addSwitch(layout, "🕐 Save History", "Remember visited pages", prefs.getBoolean("save_history", true), "save_history");
        addSwitch(layout, "👆 Swipe Gestures", "Swipe left/right to navigate history", prefs.getBoolean("swipe_gestures", true), "swipe_gestures");
        addSwitch(layout, "🖼️ Load Images", "Load images on web pages", prefs.getBoolean("load_images", true), "load_images");

        // === Search ===
        addSectionHeader(layout, "🔍 Search");
        String engine = prefs.getString("search_engine", "google");
        String engineLabel = "google".equals(engine) ? "Google" : "bing".equals(engine) ? "Bing" : "DuckDuckGo";
        addButton(layout, "🔍 Search Engine: " + engineLabel, () -> showSearchEngineDialog());

        // === Data ===
        addSectionHeader(layout, "🗑️ Data");
        addButton(layout, "🗑️ Clear History", () -> {
            prefs.edit().remove("history").apply();
            Toast.makeText(this, "History cleared!", Toast.LENGTH_SHORT).show();
        });
        addButton(layout, "⭐ Clear Bookmarks", () -> {
            new AlertDialog.Builder(this)
                .setTitle("Clear Bookmarks")
                .setMessage("Delete all bookmarks?")
                .setPositiveButton("Clear", (d, w) -> {
                    prefs.edit().remove("bookmarks").apply();
                    Toast.makeText(this, "Bookmarks cleared!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null).show();
        });
        addButton(layout, "🔴 Clear All Data", () -> {
            new AlertDialog.Builder(this)
                .setTitle("⚠️ Clear All Data")
                .setMessage("This will erase all history, bookmarks, and settings.")
                .setPositiveButton("Clear Everything", (d, w) -> {
                    prefs.edit().clear().apply();
                    Toast.makeText(this, "All data cleared!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Cancel", null).show();
        });

        // === About ===
        addSectionHeader(layout, "ℹ️ About");
        addButton(layout, "🌌 About Nova Browser v1.1.0", () -> {
            new AlertDialog.Builder(this)
                .setTitle("🌌 Nova Browser")
                .setMessage("Version 1.1.0\n\n" +
                    "A modern, fast, and beautiful browser for Android.\n\n" +
                    "✨ Features:\n" +
                    "• Built-in Ad Blocker\n" +
                    "• Incognito Mode\n" +
                    "• Desktop Mode\n" +
                    "• Reader Mode\n" +
                    "• Dark Mode\n" +
                    "• Find in Page\n" +
                    "• Screenshot Tool\n" +
                    "• Print / Save PDF\n" +
                    "• Reading List\n" +
                    "• Pull-to-Refresh\n" +
                    "• Custom Search Engine\n" +
                    "• URL Autocomplete\n" +
                    "• Multi-Tab Support\n\n" +
                    "Built with ❤️ by dor2500")
                .setPositiveButton("OK", null).show();
        });

        // Wrap in scroll view
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        scrollView.addView(layout);
        setContentView(scrollView);
    }

    private void addSectionHeader(LinearLayout parent, String text) {
        // Spacing
        android.view.View space = new android.view.View(this);
        space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 24));
        parent.addView(space);

        TextView header = new TextView(this);
        header.setText(text);
        header.setTextSize(13);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setTextColor(getResources().getColor(R.color.primary, null));
        header.setPadding(0, 0, 0, 8);
        header.setAllCaps(false);
        parent.addView(header);

        android.view.View div = new android.view.View(this);
        div.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2));
        div.setBackgroundColor(getResources().getColor(R.color.divider, null));
        parent.addView(div);
    }

    private void addSwitch(LinearLayout parent, String title, String subtitle, boolean defaultVal, String key) {
        addSwitch(parent, title, subtitle, defaultVal, key, false);
    }

    private void addSwitch(LinearLayout parent, String title, String subtitle, boolean defaultVal, String key, boolean restartApp) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 18, 0, 18);

        LinearLayout textGroup = new LinearLayout(this);
        textGroup.setOrientation(LinearLayout.VERTICAL);
        textGroup.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextSize(15);
        tv.setTextColor(getResources().getColor(R.color.text_primary, null));
        tv.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView sub = new TextView(this);
        sub.setText(subtitle);
        sub.setTextSize(12);
        sub.setTextColor(getResources().getColor(R.color.text_secondary, null));

        textGroup.addView(tv);
        textGroup.addView(sub);

        Switch sw = new Switch(this);
        sw.setChecked(prefs.getBoolean(key, defaultVal));
        sw.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean(key, checked).apply();
            if (restartApp && "dark_mode".equals(key)) {
                AppCompatDelegate.setDefaultNightMode(checked ?
                    AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        row.addView(textGroup);
        row.addView(sw);
        parent.addView(row);

        android.view.View div = new android.view.View(this);
        div.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
        div.setBackgroundColor(getResources().getColor(R.color.divider, null));
        parent.addView(div);
    }

    private void addButton(LinearLayout parent, String text, Runnable onClick) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextSize(15);
        btn.setTextColor(getResources().getColor(R.color.text_primary, null));
        btn.setPadding(0, 20, 0, 20);
        btn.setBackground(getResources().getDrawable(R.drawable.menu_item_background, null));
        btn.setOnClickListener(v -> onClick.run());
        parent.addView(btn);

        android.view.View div = new android.view.View(this);
        div.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
        div.setBackgroundColor(getResources().getColor(R.color.divider, null));
        parent.addView(div);
    }

    private void showSearchEngineDialog() {
        String[] engines = {"Google", "Bing", "DuckDuckGo"};
        String[] keys = {"google", "bing", "ddg"};
        String current = prefs.getString("search_engine", "google");
        int selected = 0;
        for (int i = 0; i < keys.length; i++) { if (keys[i].equals(current)) { selected = i; break; } }
        final int[] sel = {selected};

        new AlertDialog.Builder(this)
            .setTitle("🔍 Choose Search Engine")
            .setSingleChoiceItems(engines, selected, (d, w) -> sel[0] = w)
            .setPositiveButton("Save", (d, w) -> {
                prefs.edit().putString("search_engine", keys[sel[0]]).apply();
                Toast.makeText(this, "Search: " + engines[sel[0]], Toast.LENGTH_SHORT).show();
                recreate();
            })
            .setNegativeButton("Cancel", null).show();
    }
}
