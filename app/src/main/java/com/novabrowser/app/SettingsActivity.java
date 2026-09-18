package com.novabrowser.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("NovaBrowser", Context.MODE_PRIVATE);
        setupUI();
    }

    private void setupUI() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 64, 32, 32);
        layout.setBackgroundColor(getResources().getColor(R.color.background, null));

        TextView title = new TextView(this);
        title.setText("⚙️ Settings");
        title.setTextSize(26);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(getResources().getColor(R.color.text_primary, null));
        title.setPadding(0, 0, 0, 32);
        layout.addView(title);

        // Ad Blocker
        addSwitch(layout, "🛡️ Ad Blocker",
            "Block ads and trackers automatically",
            prefs.getBoolean("ad_blocker", true), "ad_blocker");

        // JavaScript
        addSwitch(layout, "⚡ JavaScript",
            "Enable JavaScript on websites",
            prefs.getBoolean("javascript", true), "javascript");

        // Save History
        addSwitch(layout, "🕐 Save History",
            "Remember visited pages",
            prefs.getBoolean("save_history", true), "save_history");

        // Dark Mode
        addSwitch(layout, "🌙 Dark Reader",
            "Apply dark theme to all websites",
            prefs.getBoolean("dark_reader", false), "dark_reader");

        // Clear Data Button
        addButton(layout, "🗑️ Clear All Data", () -> {
            new AlertDialog.Builder(this)
                .setTitle("Clear Data")
                .setMessage("This will clear all browsing data, history and bookmarks.")
                .setPositiveButton("Clear", (d, w) -> {
                    prefs.edit().clear().apply();
                    Toast.makeText(this, "Data cleared!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        addButton(layout, "ℹ️ About Nova Browser", () -> {
            new AlertDialog.Builder(this)
                .setTitle("Nova Browser")
                .setMessage("Version 1.0.0\n\nA modern, fast, and beautiful browser for Android.\n\nFeatures:\n• Built-in Ad Blocker\n• Incognito Mode\n• Desktop Mode\n• Reader Mode\n• Find in Page\n• Screenshot Tool\n• Bookmarks & History\n• Multi-Tab Support")
                .setPositiveButton("OK", null)
                .show();
        });

        setContentView(layout);
    }

    private void addSwitch(LinearLayout parent, String title, String subtitle, boolean defaultVal, String key) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 24, 0, 24);

        LinearLayout textGroup = new LinearLayout(this);
        textGroup.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textGroup.setLayoutParams(textParams);

        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextSize(16);
        tv.setTextColor(getResources().getColor(R.color.text_primary, null));
        tv.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView sub = new TextView(this);
        sub.setText(subtitle);
        sub.setTextSize(13);
        sub.setTextColor(getResources().getColor(R.color.text_secondary, null));

        textGroup.addView(tv);
        textGroup.addView(sub);

        Switch sw = new Switch(this);
        sw.setChecked(prefs.getBoolean(key, defaultVal));
        sw.setOnCheckedChangeListener((btn, checked) -> prefs.edit().putBoolean(key, checked).apply());

        row.addView(textGroup);
        row.addView(sw);
        parent.addView(row);

        // Divider
        android.view.View divider = new android.view.View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(getResources().getColor(R.color.divider, null));
        parent.addView(divider);
    }

    private void addButton(LinearLayout parent, String text, Runnable onClick) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextSize(16);
        btn.setTextColor(getResources().getColor(R.color.primary, null));
        btn.setTypeface(null, android.graphics.Typeface.BOLD);
        btn.setPadding(0, 32, 0, 32);
        btn.setBackground(getResources().getDrawable(R.drawable.menu_item_background, null));
        btn.setOnClickListener(v -> onClick.run());
        parent.addView(btn);

        android.view.View divider = new android.view.View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(getResources().getColor(R.color.divider, null));
        parent.addView(divider);
    }
}
