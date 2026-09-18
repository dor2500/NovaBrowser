package com.novabrowser.app;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Get URL from intent
        String url = null;
        Intent intent = getIntent();
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri data = intent.getData();
            if (data != null) url = data.toString();
        }
        final String targetUrl = url;

        // Animate splash elements
        animateSplash();

        // Navigate after delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
            if (targetUrl != null) mainIntent.putExtra("url", targetUrl);
            startActivity(mainIntent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 2000);
    }

    private void animateSplash() {
        ImageView logo = findViewById(R.id.splashLogo);
        if (logo == null) return;

        // Logo bounce in
        logo.setScaleX(0f);
        logo.setScaleY(0f);
        logo.setAlpha(0f);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(logo, "scaleX", 0f, 1.1f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(logo, "scaleY", 0f, 1.1f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f);

        AnimatorSet logoAnim = new AnimatorSet();
        logoAnim.playTogether(scaleX, scaleY, alpha);
        logoAnim.setDuration(700);
        logoAnim.setInterpolator(new OvershootInterpolator(1.5f));
        logoAnim.start();

        // Animate parent views from bottom
        View rootView = getWindow().getDecorView().getRootView();
        rootView.setTranslationY(80f);
        rootView.setAlpha(0f);
        ObjectAnimator translateY = ObjectAnimator.ofFloat(rootView, "translationY", 80f, 0f);
        ObjectAnimator rootAlpha = ObjectAnimator.ofFloat(rootView, "alpha", 0f, 1f);
        AnimatorSet rootAnim = new AnimatorSet();
        rootAnim.playTogether(translateY, rootAlpha);
        rootAnim.setDuration(600);
        rootAnim.setInterpolator(new DecelerateInterpolator());
        rootAnim.start();
    }
}
