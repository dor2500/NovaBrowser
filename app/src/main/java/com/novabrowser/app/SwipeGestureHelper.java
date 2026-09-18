package com.novabrowser.app;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * SwipeGestureHelper - detects left/right swipes on a View
 * Used for back/forward navigation in the browser
 */
public class SwipeGestureHelper extends GestureDetector.SimpleOnGestureListener {

    private static final int SWIPE_THRESHOLD = 120;
    private static final int SWIPE_VELOCITY_THRESHOLD = 200;

    public interface SwipeListener {
        void onSwipeLeft();   // Forward
        void onSwipeRight();  // Back
        void onSwipeUp();     // Scroll to top / show toolbar
        void onSwipeDown();   // Pull to refresh hint
    }

    private final SwipeListener listener;
    private final GestureDetector gestureDetector;

    public SwipeGestureHelper(Context context, SwipeListener listener) {
        this.listener = listener;
        this.gestureDetector = new GestureDetector(context, this);
    }

    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (e1 == null || e2 == null) return false;

        float diffX = e2.getX() - e1.getX();
        float diffY = e2.getY() - e1.getY();

        if (Math.abs(diffX) > Math.abs(diffY)) {
            // Horizontal swipe
            if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffX > 0) {
                    listener.onSwipeRight(); // Back
                } else {
                    listener.onSwipeLeft();  // Forward
                }
                return true;
            }
        } else {
            // Vertical swipe
            if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffY > 0) {
                    listener.onSwipeDown();
                } else {
                    listener.onSwipeUp();
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Attach gesture detection to a View
     */
    public void attachTo(View view) {
        view.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false; // Don't consume - let WebView handle it too
        });
    }
}
