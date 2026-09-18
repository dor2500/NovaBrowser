package com.novabrowser.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private LinearLayout listContainer;
    private List<HistoryItem> historyItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("NovaBrowser", Context.MODE_PRIVATE);
        loadHistory();
        setupUI();
    }

    private void setupUI() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(ContextCompat.getColor(this, R.color.background));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 48, 24, 48);

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, 24);

        ImageView backBtn = new ImageView(this);
        backBtn.setImageDrawable(ContextCompat.getDrawable(this, android.R.drawable.ic_menu_revert));
        backBtn.setLayoutParams(new LinearLayout.LayoutParams(48, 48));
        backBtn.setOnClickListener(v -> finish());

        TextView title = new TextView(this);
        title.setText("🕐 History (" + historyItems.size() + ")");
        title.setTextSize(24);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tp.setMarginStart(16);
        title.setLayoutParams(tp);

        TextView clearAll = new TextView(this);
        clearAll.setText("Clear All");
        clearAll.setTextSize(14);
        clearAll.setTextColor(ContextCompat.getColor(this, R.color.accent));
        clearAll.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Clear History")
                .setMessage("Delete all browsing history?")
                .setPositiveButton("Clear", (d, w) -> {
                    historyItems.clear();
                    prefs.edit().remove("history").apply();
                    listContainer.removeAllViews();
                    title.setText("🕐 History (0)");
                    Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null).show();
        });

        header.addView(backBtn);
        header.addView(title);
        header.addView(clearAll);
        root.addView(header);

        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(listContainer);

        if (historyItems.isEmpty()) {
            showEmpty(root);
        } else {
            renderHistory();
        }

        scrollView.addView(root);
        setContentView(scrollView);
    }

    private void renderHistory() {
        listContainer.removeAllViews();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault());

        String lastDate = "";
        for (int i = 0; i < Math.min(historyItems.size(), 50); i++) {
            HistoryItem h = historyItems.get(i);
            String dateKey = new SimpleDateFormat("MMM d", Locale.getDefault()).format(new Date(h.timestamp));

            // Date section header
            if (!dateKey.equals(lastDate)) {
                lastDate = dateKey;
                TextView dateSep = new TextView(this);
                dateSep.setText(dateKey);
                dateSep.setTextSize(12);
                dateSep.setTypeface(null, android.graphics.Typeface.BOLD);
                dateSep.setTextColor(ContextCompat.getColor(this, R.color.primary));
                LinearLayout.LayoutParams sep = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                sep.setMargins(0, 16, 0, 8);
                dateSep.setLayoutParams(sep);
                listContainer.addView(dateSep);
            }

            CardView card = new CardView(this);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cp.setMargins(0, 0, 0, 8);
            card.setLayoutParams(cp);
            card.setRadius(12 * getResources().getDisplayMetrics().density);
            card.setCardElevation(1 * getResources().getDisplayMetrics().density);
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.surface));

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(16, 12, 16, 12);
            row.setGravity(Gravity.CENTER_VERTICAL);

            TextView icon = new TextView(this);
            icon.setText(h.url.startsWith("https://") ? "🔒" : "🌐");
            icon.setTextSize(18);
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(36, 36);
            ip.setMarginEnd(12);
            icon.setLayoutParams(ip);
            icon.setGravity(Gravity.CENTER);

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView hTitle = new TextView(this);
            hTitle.setText(h.title);
            hTitle.setTextSize(14);
            hTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            hTitle.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            hTitle.setMaxLines(1);
            hTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);

            TextView hUrl = new TextView(this);
            hUrl.setText(h.url);
            hUrl.setTextSize(11);
            hUrl.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            hUrl.setMaxLines(1);
            hUrl.setEllipsize(android.text.TextUtils.TruncateAt.END);

            info.addView(hTitle);
            info.addView(hUrl);

            TextView time = new TextView(this);
            time.setText(sdf.format(new Date(h.timestamp)).split(",")[1].trim());
            time.setTextSize(11);
            time.setTextColor(ContextCompat.getColor(this, R.color.text_hint));

            row.addView(icon);
            row.addView(info);
            row.addView(time);
            card.addView(row);

            final int idx = i;
            card.setOnClickListener(v -> {
                Intent result = new Intent();
                result.putExtra("url", h.url);
                setResult(RESULT_OK, result);
                finish();
            });
            card.setOnLongClickListener(v -> {
                historyItems.remove(idx);
                saveHistory();
                renderHistory();
                return true;
            });

            listContainer.addView(card);
        }
    }

    private void showEmpty(LinearLayout parent) {
        LinearLayout e = new LinearLayout(this);
        e.setOrientation(LinearLayout.VERTICAL);
        e.setGravity(Gravity.CENTER);
        e.setPadding(0, 80, 0, 80);
        TextView emoji = new TextView(this); emoji.setText("📭"); emoji.setTextSize(56); emoji.setGravity(Gravity.CENTER);
        TextView msg = new TextView(this); msg.setText("No browsing history"); msg.setTextSize(18); msg.setTypeface(null, android.graphics.Typeface.BOLD); msg.setTextColor(ContextCompat.getColor(this, R.color.text_primary)); msg.setGravity(Gravity.CENTER);
        e.addView(emoji); e.addView(msg);
        parent.addView(e);
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

    private void saveHistory() {
        try {
            JSONArray arr = new JSONArray();
            for (HistoryItem h : historyItems) {
                JSONObject o = new JSONObject(); o.put("title", h.title); o.put("url", h.url); o.put("time", h.timestamp); arr.put(o);
            }
            prefs.edit().putString("history", arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    static class HistoryItem { String title, url; long timestamp; HistoryItem(String t, String u, long ts) { title=t; url=u; timestamp=ts; } }
}
