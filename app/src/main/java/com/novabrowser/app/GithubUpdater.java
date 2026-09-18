package com.novabrowser.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class GithubUpdater {
    private static final String REPO_API_URL = "https://api.github.com/repos/dor2500/NovaBrowser/releases/latest";
    private Activity activity;

    public GithubUpdater(Activity activity) {
        this.activity = activity;
    }

    public void checkForUpdates(boolean showToastIfUpToDate) {
        new Thread(() -> {
            try {
                URL url = new URL(REPO_API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject release = new JSONObject(response.toString());
                String latestVersion = release.getString("tag_name");
                
                String currentVersionStr = "1.0";
                try {
                    currentVersionStr = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
                } catch (Exception e) {}
                String currentVersion = "v" + currentVersionStr;
                
                String body = release.getString("body");
                
                String downloadUrl = null;
                if (release.has("assets") && release.getJSONArray("assets").length() > 0) {
                    downloadUrl = release.getJSONArray("assets").getJSONObject(0).getString("browser_download_url");
                }

                if (!currentVersion.equals(latestVersion) && downloadUrl != null) {
                    final String finalDownloadUrl = downloadUrl;
                    new Handler(Looper.getMainLooper()).post(() -> showUpdateDialog(latestVersion, body, finalDownloadUrl));
                } else if (showToastIfUpToDate) {
                    new Handler(Looper.getMainLooper()).post(() -> 
                        Toast.makeText(activity, "You have the latest version", Toast.LENGTH_SHORT).show()
                    );
                }

            } catch (Exception e) {
                e.printStackTrace();
                if (showToastIfUpToDate) {
                    new Handler(Looper.getMainLooper()).post(() -> 
                        Toast.makeText(activity, "Failed to check for updates", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        }).start();
    }

    private void showUpdateDialog(String version, String changelog, String downloadUrl) {
        new AlertDialog.Builder(activity)
                .setTitle("Update Available: " + version)
                .setMessage("What's new:\n" + changelog)
                .setPositiveButton("Download & Install", (dialog, which) -> downloadUpdate(downloadUrl, version))
                .setNegativeButton("Later", null)
                .show();
    }

    private void downloadUpdate(String url, String version) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("NovaBrowser Update " + version);
        request.setDescription("Downloading update...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "NovaBrowser_" + version + ".apk");

        DownloadManager manager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager != null) {
            manager.enqueue(request);
            Toast.makeText(activity, "Downloading update...", Toast.LENGTH_LONG).show();
        }
    }
}
