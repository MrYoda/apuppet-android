package com.hmdm.control;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.WindowManager;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import static android.content.Context.WIFI_SERVICE;

public class Utils {

    public static boolean isAccessibilityPermissionGranted(Context context) {
        int accessibilityEnabled = 0;
        final String service = context.getPackageName() + "/com.hmdm.control.GestureDispatchService";

        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    context.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(
                    context.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                TextUtils.SimpleStringSplitter splitter = mStringColonSplitter;
                splitter.setString(settingValue);
                while (splitter.hasNext()) {
                    String accessibilityService = splitter.next();
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static String randomString(int length, boolean digitsOnly) {
        String charSource = "0123456789";
        if (!digitsOnly) {
            charSource += "abcdefghijklmnopqrstuvxyz";
        }
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = (int)(charSource.length()* Math.random());
            result.append(charSource.charAt(index));
        }
        return result.toString();
    }

    public static String generateTransactionId() {
        return randomString(12, true);
    }

    public static ByteBuffer stringToByteBuffer(String msg) {
        return ByteBuffer.wrap(msg.getBytes(Charset.defaultCharset()));
    }

    public static String byteBufferToString(ByteBuffer buffer) {
        byte[] bytes;
        if (buffer.hasArray()) {
            bytes = buffer.array();
        } else {
            bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
        }
        return new String(bytes, Charset.defaultCharset());
    }

    public static String prepareDisplayUrl(String url) {
        String result = url;
        if (url == null) {
            return "";
        }
        // Cut off the port (skipping : at the end of scheme)
        int pos = url.indexOf(':', 6);
        if (pos != -1) {
            result = result.substring(0, pos);
        }
        return result;
    }

    public static String getRtpUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            return url.getHost();
        } catch (MalformedURLException e) {
            // We must not be here because RTP URL is setup after successful connection
            e.printStackTrace();
        }
        return null;
    }

    public static String getLocalIpAddress(Context context) {
        WifiManager wm = (WifiManager) context.getSystemService(WIFI_SERVICE);
        return Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
    }

    public static int OverlayWindowType() {
        // https://stackoverflow.com/questions/45867533/system-alert-window-permission-on-api-26-not-working-as-expected-permission-den
        if (  Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            return WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    public static void lockDeviceRotation(Activity activity, boolean value) {
        if (value) {
            int currentOrientation = activity.getResources().getConfiguration().orientation;
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            } else {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            }
        } else {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
            } else {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
            }
        }
    }

    public static void promptOverlayPermissions(Activity activity, boolean canCancel) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setMessage(R.string.overlay_hint)
                .setPositiveButton(R.string.continue_button, (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + activity.getPackageName()));
                    activity.startActivityForResult(intent, Const.REQUEST_PERMISSION_OVERLAY);
                })
                .setCancelable(false);
        if (canCancel) {
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        }
        builder.create().show();
    }
}
