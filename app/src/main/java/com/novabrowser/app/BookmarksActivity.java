package com.novabrowser.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
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

/**
 * BookmarksActivity - Full-featured bookmarks manager
 * Supports view, open, delete, share bookmarks
 */
public class BookmarksActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private LinearLayout listContainer;
    private List<BookmarkItem> bookmarks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("NovaBrowser", Context.MODE_PRIVATE);
        loadBookmarks();
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
        backBtn.setPadding(4, 4, 4, 4);
        backBtn.setOnClickListener(v -> finish());

        TextView title = new TextView(this);
        title.setText("⭐ Bookmarks (" + bookmarks.size() + ")");
        title.setTextSize(24);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.setMarginStart(16);
        title.setLayoutParams(titleParams);

        TextView clearAll = new TextView(this);
        clearAll.setText("Clear All");
        clearAll.setTextSize(14);
        clearAll.setTextColor(ContextCompat.getColor(this, R.color.accent));
        clearAll.setOnClickListener(v -> confirmClearAll(title));

        header.addView(backBtn);
        header.addView(title);
        header.addView(clearAll);

        root.addView(header);

        // List container
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(listContainer);

        if (bookmarks.isEmpty()) {
            showEmptyState(root);
        } else {
            renderBookmarks();
        }

        scrollView.addView(root);
        setContentView(scrollView);
    }

    private void renderBookmarks() {
        listContainer.removeAllViews();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

        for (int i = 0; i < bookmarks.size(); i++) {
            BookmarkItem bm = bookmarks.get(i);
            final int idx = i;

            CardView card = new CardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, 12);
            card.setLayoutParams(cardParams);
            card.setRadius(16 * getResources().getDisplayMetrics().density);
            card.setCardElevation(2 * getResources().getDisplayMetrics().density);
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.surface));

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(16, 16, 16, 16);
            row.setGravity(Gravity.CENTER_VERTICAL);

            // Favicon placeholder
            TextView favicon = new TextView(this);
            favicon.setText("⭐");
            favicon.setTextSize(22);
            favicon.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams fvParams = new LinearLayout.LayoutParams(44, 44);
            fvParams.setMarginEnd(16);
            favicon.setLayoutParams(fvParams);

            // Text info
            LinearLayout textGroup = new LinearLayout(this);
            textGroup.setOrientation(LinearLayout.VERTICAL);
            textGroup.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView bmTitle = new TextView(this);
            bmTitle.setText(bm.title);
            bmTitle.setTextSize(15);
            bmTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            bmTitle.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            bmTitle.setMaxLines(1);
            bmTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);

            TextView bmUrl = new TextView(this);
            bmUrl.setText(bm.url);
            bmUrl.setTextSize(12);
            bmUrl.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            bmUrl.setMaxLines(1);
            bmUrl.setEllipsize(android.text.TextUtils.TruncateAt.END);

            TextView bmDate = new TextView(this);
            bmDate.setText(sdf.format(new Date(bm.timestamp)));
            bmDate.setTextSize(11);
            bmDate.setTextColor(ContextCompat.getColor(this, R.color.text_hint));

            textGroup.addView(bmTitle);
            textGroup.addView(bmUrl);
            textGroup.addView(bmDate);

            // Delete button
            TextView deleteBtn = new TextView(this);
            deleteBtn.setText("🗑️");
            deleteBtn.setTextSize(20);
            deleteBtn.setPadding(8, 8, 8, 8);
            deleteBtn.setOnClickListener(v -> deleteBookmark(idx));

            row.addView(favicon);
            row.addView(textGroup);
            row.addView(deleteBtn);
            card.addView(row);

            // Click to open
            card.setOnClickListener(v -> {
                Intent result = new Intent();
                result.putExtra("url", bm.url);
                setResult(RESULT_OK, result);
                finish();
            });

            // Long press to share
            card.setOnLongClickListener(v -> {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, bm.url);
                startActivity(Intent.createChooser(shareIntent, "Share bookmark"));
                return true;
            });

            listContainer.addView(card);
        }
    }

    private void showEmptyState(LinearLayout parent) {
        LinearLayout empty = new LinearLayout(this);
        empty.setOrientation(LinearLayout.VERTICAL);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(0, 80, 0, 80);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        empty.setLayoutParams(p);

        TextView emoji = new TextView(this);
        emoji.setText("📭");
        emoji.setTextSize(56);
        emoji.setGravity(Gravity.CENTER);

        TextView msg = new TextView(this);
        msg.setText("No bookmarks yet");
        msg.setTextSize(18);
        msg.setTypeface(null, android.graphics.Typeface.BOLD);
        msg.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        msg.setGravity(Gravity.CENTER);

        TextView sub = new TextView(this);
        sub.setText("Tap ⭐ Add Bookmark in the menu\nto save your favorite pages");
        sub.setTextSize(14);
        sub.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        sub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subParams.topMargin = 12;
        sub.setLayoutParams(subParams);

        empty.addView(emoji);
        empty.addView(msg);
        empty.addView(sub);
        parent.addView(empty);
    }

    private void deleteBookmark(int idx) {
        bookmarks.remove(idx);
        saveBookmarks();
        renderBookmarks();
        Toast.makeText(this, "Bookmark removed", Toast.LENGTH_SHORT).show();
    }

    private void confirmClearAll(TextView titleView) {
        new AlertDialog.Builder(this)
            .setTitle("Clear All Bookmarks")
            .setMessage("Delete all " + bookmarks.size() + " bookmarks?")
            .setPositiveButton("Clear", (d, w) -> {
                bookmarks.clear();
                saveBookmarks();
                listContainer.removeAllViews();
                titleView.setText("⭐ Bookmarks (0)");
                Toast.makeText(this, "All bookmarks cleared", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null).show();
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

    private void saveBookmarks() {
        try {
            JSONArray arr = new JSONArray();
            for (BookmarkItem b : bookmarks) {
                JSONObject o = new JSONObject(); o.put("title", b.title); o.put("url", b.url); o.put("time", b.timestamp); arr.put(o);
            }
            prefs.edit().putString("bookmarks", arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    static class BookmarkItem {
        String title, url; long timestamp;
        BookmarkItem(String t, String u, long ts) { title=t; url=u; timestamp=ts; }
    }
}
