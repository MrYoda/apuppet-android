package com.hmdm.control;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

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
}
