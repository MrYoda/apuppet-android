package com.hmdm.control;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.Service;
import android.content.Intent;
import android.graphics.Path;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class GestureDispatchService extends AccessibilityService {
    // Sharing state
    private boolean isSharing = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
    }

    @Override
    public void onInterrupt() {
    }

    // This is called from the main activity when it gets a message from the Janus socket
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return Service.START_STICKY;
        }
        if (intent.getAction().equals(Const.ACTION_GESTURE)) {
            String event = intent.getStringExtra(Const.EXTRA_EVENT);
            if (event != null) {
                processMessage(event);
            }
        }
        return Service.START_STICKY;
    }

    private void processMessage(String message) {
        float scale = SettingsHelper.getInstance(this).getFloat(SettingsHelper.KEY_VIDEO_SCALE);
        if (scale == 0) {
            scale = 1;
        }
        String[] parts = message.split(",");
        if (parts.length == 0) {
            // Empty message?
            return;
        }
        if (parts[0].equalsIgnoreCase("tap"))  {
            if (parts.length != 4) {
                Log.w(Const.LOG_TAG, "Wrong gesture event format: '" + message + "' Should be tap,X,Y,duration");
                return;
            }
            try {
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                if (scale != 1) {
                    x = (int)(x / scale);
                    y = (int)(y / scale);
                }
                int duration = Integer.parseInt(parts[3]);
                simulateGesture(x, y, null, null, duration);
            } catch (Exception e) {
                Log.w(Const.LOG_TAG, "Wrong gesture event format: '" + message + "': " + e);
            }
        } else if (parts[0].equalsIgnoreCase("swipe")) {
            if (parts.length != 6) {
                Log.w(Const.LOG_TAG, "Wrong message format: '" + message + "' Should be swipe,X1,Y1,X2,Y2");
                return;
            }
            try {
                int x1 = Integer.parseInt(parts[1]);
                int y1 = Integer.parseInt(parts[2]);
                int x2 = Integer.parseInt(parts[3]);
                int y2 = Integer.parseInt(parts[4]);
                if (scale != 1) {
                    x1 = (int)(x1 / scale);
                    y1 = (int)(y1 / scale);
                    x2 = (int)(x2 / scale);
                    y2 = (int)(y2 / scale);                }
                int duration = Integer.parseInt(parts[5]);
                simulateGesture(x1, y1, x2, y2, duration);
            } catch (Exception e) {
                Log.w(Const.LOG_TAG, "Wrong gesture event format: '" + message + "': " + e);
            }
        } else if (parts[0].equalsIgnoreCase("back")) {
            performGlobalAction(GLOBAL_ACTION_BACK);
        } else if (parts[0].equalsIgnoreCase("home")) {
            performGlobalAction(GLOBAL_ACTION_HOME);
        } else if (parts[0].equalsIgnoreCase("notifications")) {
            performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
        } else if (parts[0].equalsIgnoreCase("recents")) {
            performGlobalAction(GLOBAL_ACTION_RECENTS);
        } else {
            Log.w(Const.LOG_TAG, "Ignoring wrong gesture event: '" + message + "'");
        }
    }

    private void simulateGesture(Integer x1, Integer y1, Integer x2, Integer y2, int duration) {
        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();

        if (x2 == null || y2 == null) {
            // Tap
            Path clickPath = new Path();
            clickPath.moveTo(x1, y1);
            GestureDescription.StrokeDescription clickStroke =
                    new GestureDescription.StrokeDescription(clickPath, 0, duration);
            gestureBuilder.addStroke(clickStroke);
            Log.d(Const.LOG_TAG, "Simulating a gesture: tap, x1=" + x1 + ", y1=" + y1 + ", duration=" + duration);
        } else {
            // Swipe
            Path clickPath = new Path();
            clickPath.moveTo(x1, y1);
            clickPath.lineTo(x2, y2);
            GestureDescription.StrokeDescription clickStroke =
                    new GestureDescription.StrokeDescription(clickPath, 0, duration);
            gestureBuilder.addStroke(clickStroke);
            Log.d(Const.LOG_TAG, "Simulating a gesture: swipe, x1=" + x1 + ", y1=" + y1 + ", x2=" + x2 + ", y2=" + y2 + ", duration=" + duration);
        }

        boolean result = dispatchGesture(gestureBuilder.build(), null, null);
        Log.d(Const.LOG_TAG, "Gesture dispatched, result=" + result);
    }
}
