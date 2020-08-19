package com.hmdm.control;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Surface;

import java.io.IOException;

public class ScreenSharer {
    private int mScreenDensity;
    private int mScreenWidth;
    private int mScreenHeight;

    private MediaProjectionManager mProjectionManager;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjection.Callback mMediaProjectionCallback;

    private MediaRecorder mMediaRecorder;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    public final int REQUEST_CODE_SCREEN_SHARE = 1000;

    public ScreenSharer(Activity activity) {
        Display display = activity.getWindowManager(). getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        mScreenWidth = metrics.widthPixels;
        mScreenHeight = metrics.heightPixels;

        mMediaRecorder = new MediaRecorder();

        mProjectionManager = (MediaProjectionManager)activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);

    }

    public void setMediaProjectionCallback(MediaProjection.Callback callback) {
        mMediaProjectionCallback = callback;
    }

    public void onSharePermissionGranted(Activity activity, int resultCode, Intent data) {
        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mMediaProjection.registerCallback(mMediaProjectionCallback, null);
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
    }

    public boolean startShare(Activity activity) {
        if (!initRecorder(activity)) {
            return false;
        }
        shareScreen(activity);
        return true;
    }

    public void stopShare(Activity activity) {
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        Log.v(Const.LOG_TAG, "Stopping Recording");
        stopScreenSharing();
    }

    public void destroy() {
        destroyMediaProjection();
    }

    private void shareScreen(Activity activity) {
        if (mMediaProjection == null) {
            activity.startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_SCREEN_SHARE);
            return;
        }
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
    }

    private VirtualDisplay createVirtualDisplay() {
        return mMediaProjection.createVirtualDisplay("MainActivity",
                mScreenWidth, mScreenHeight, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(), null /*Callbacks*/, null
                /*Handler*/);
    }

    private boolean initRecorder(Activity activity) {
        try {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mMediaRecorder.setOutputFile(Environment
                    .getExternalStoragePublicDirectory(Environment
                            .DIRECTORY_DOWNLOADS) + "/video.mp4");
            mMediaRecorder.setVideoSize(mScreenWidth, mScreenHeight);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mMediaRecorder.setVideoEncodingBitRate(256 * 1000);
            mMediaRecorder.setVideoFrameRate(10);
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            int orientation = ORIENTATIONS.get(rotation + 90);
            mMediaRecorder.setOrientationHint(orientation);
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void stopScreenSharing() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        //mMediaRecorder.release(); //If used: mMediaRecorder object cannot
        // be reused again
        destroyMediaProjection();
    }

    private void destroyMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.unregisterCallback(mMediaProjectionCallback);
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        Log.i(Const.LOG_TAG, "MediaProjection Stopped");
    }
}
