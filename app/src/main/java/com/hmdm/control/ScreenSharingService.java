package com.hmdm.control;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import net.majorkernelpanic.streaming.rtp.AbstractPacketizer;
import net.majorkernelpanic.streaming.rtp.H264Packetizer;
import net.majorkernelpanic.streaming.rtp.MediaCodecInputStream;

import java.io.IOException;
import java.net.InetAddress;

public class ScreenSharingService extends Service {
    public static String CHANNEL_ID = "com.hmdm.control";
    private static final int NOTIFICATION_ID = 111;

    private static final String MIME_TYPE_VIDEO = "video/avc";

    private int mScreenDensity;
    private int mScreenWidth;
    private int mScreenHeight;

    private boolean mRecordAudio;
    private String mRtpHost;
    private int mRtpAudioPort;
    private int mRtpVideoPort;
    private int mVideoFrameRate;
    private int mVideoBitrate;

    private MediaProjectionManager mProjectionManager;
    private MediaProjection mMediaProjection;
    private MediaProjection.Callback mMediaProjectionCallback;
    private VirtualDisplay mVirtualDisplay;

    private MediaCodec mMediaCodec;
    private Surface mInputSurface;

    private AbstractPacketizer mPacketizer;

    public static final String ACTION_SET_METRICS = "metrics";
    public static final String ACTION_CONFIGURE = "configure";
    public static final String ACTION_REQUEST_SHARING = "request";
    public static final String ACTION_START_SHARING = "start";
    public static final String ACTION_STOP_SHARING = "stop";
    public static final String ATTR_SCREEN_WIDTH = "screenWidth";
    public static final String ATTR_SCREEN_HEIGHT = "screenHeight";
    public static final String ATTR_SCREEN_DENSITY = "screenDensity";
    public static final String ATTR_AUDIO = "audio";
    public static final String ATTR_FRAME_RATE = "frameRate";
    public static final String ATTR_BITRATE = "bitrate";
    public static final String ATTR_HOST = "host";
    public static final String ATTR_AUDIO_PORT = "audioPort";
    public static final String ATTR_VIDEO_PORT = "videoPort";
    public static final String ATTR_RESULT_CODE = "resultCode";
    public static final String ATTR_DATA = "data";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        try {
            mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE_VIDEO);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mPacketizer = new H264Packetizer();
        mProjectionManager = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startAsForeground();
    }

    private void startAsForeground() {
        NotificationCompat.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Notification Channel", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        } else {
            builder = new NotificationCompat.Builder(this);
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, Const.REQUEST_FROM_NOTIFICATION, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = builder
                .setContentTitle(getString(R.string.app_name))
                .setTicker(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_text))
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_notification).build();

        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return Service.START_STICKY;
        }
        String action = intent.getAction();
        if (action.equals(ACTION_SET_METRICS)) {
            mScreenWidth = intent.getIntExtra(ATTR_SCREEN_WIDTH, 0);
            mScreenHeight = intent.getIntExtra(ATTR_SCREEN_HEIGHT, 0);
            mScreenDensity = intent.getIntExtra(ATTR_SCREEN_DENSITY, 0);

        } else if (action.equals(ACTION_CONFIGURE)) {
            configure(intent.getBooleanExtra(ATTR_AUDIO, false),
                    intent.getIntExtra(ATTR_FRAME_RATE, 0),
                    intent.getIntExtra(ATTR_BITRATE, 0),
                    intent.getStringExtra(ATTR_HOST),
                    intent.getIntExtra(ATTR_AUDIO_PORT, 0),
                    intent.getIntExtra(ATTR_VIDEO_PORT, 0));

        } else if (action.equals(ACTION_REQUEST_SHARING)) {
            requestSharing();

        } else if (action.equals(ACTION_START_SHARING)) {
            int resultCode = intent.getIntExtra(ATTR_RESULT_CODE, 0);
            Intent data = intent.getParcelableExtra(ATTR_DATA);
            startSharing(resultCode, data);

        } else if (action.equals(ACTION_STOP_SHARING)) {
            stopSharing();
        }

        return Service.START_STICKY;
    }

    private void configure(boolean audio, int videoFrameRate, int videoBitRate, String host, int audioPort, int videoPort) {
        mVideoFrameRate = videoFrameRate;
        mVideoBitrate = videoBitRate;

        // This is executed in the background because the operation requires host resolution
        new AsyncTask<Void,Void,Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    // Here I set RTCP port to videoPort+1 (conventional), but RTCP is not used, and 0 or -1 cause errors in libstreaming
                    mPacketizer.setDestination(InetAddress.getByName(host), videoPort, videoPort + 1);
                    mPacketizer.setTimeToLive(64);

                } catch (Exception e) {
                    // We should not be here because configure() is called after successful connection to the host
                    e.printStackTrace();
                }
                return null;
            }

        }.execute();

    }

    private void requestSharing() {
        if (!initRecorder()) {
            // Some initialization error, report to activity
            Intent intent = new Intent(Const.ACTION_SCREEN_SHARING_FAILED);
            intent.putExtra(Const.EXTRA_MESSAGE, getString(R.string.sharing_error));
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            return;
        }
        tryShareScreen();
    }


    private void tryShareScreen() {
        if (mMediaProjection == null) {
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Const.ACTION_SCREEN_SHARING_PERMISSION_NEEDED));
            return;
        }
        mVirtualDisplay = createVirtualDisplay();
        mMediaCodec.start();
        startSending();
    }

    private void startSharing(int resultCode, Intent data) {
        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mMediaProjectionCallback = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                stopSharing();
                LocalBroadcastManager.getInstance(ScreenSharingService.this).sendBroadcast(new Intent(Const.ACTION_SCREEN_SHARING_STOP));
            }
        };
        mMediaProjection.registerCallback(mMediaProjectionCallback, null);
        mVirtualDisplay = createVirtualDisplay();
        mMediaCodec.start();
        startSending();

    }

    public void stopSharing() {
        try {
            mPacketizer.stop();
            mMediaCodec.stop();
            Log.v(Const.LOG_TAG, "Stopping Recording");
            stopScreenSharing();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopScreenSharing() {
        if (mVirtualDisplay == null) {
            return;
        }
        mPacketizer.stop();
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

    private void startSending() {
        MediaCodecInputStream mcis = new MediaCodecInputStream(mMediaCodec);
        mPacketizer.setInputStream(mcis);
        mcis.setH264Packetizer((H264Packetizer) mPacketizer);
        mPacketizer.start();
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Const.ACTION_SCREEN_SHARING_START));
    }

    private VirtualDisplay createVirtualDisplay() {
        return mMediaProjection.createVirtualDisplay("MainActivity",
                mScreenWidth, mScreenHeight, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mInputSurface, null /*Callbacks*/, null
                /*Handler*/);
    }

    private boolean initRecorder() {
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE_VIDEO, mScreenWidth, mScreenHeight);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mVideoBitrate);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mVideoFrameRate);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        // This method call may throw CodecException!
        try {
            mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (Exception e) {
            Log.e(Const.LOG_TAG, "Failed to configure codec with parameters: screenWidth=" + mScreenWidth +
                    ", screenHeight=" + mScreenHeight + ", bitrate=" + mVideoBitrate + ", frameRate=" + mVideoFrameRate +
                    ", colorFormat=" + MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface + ", frameInterval=1");
            e.printStackTrace();
            return false;
        }
        mInputSurface = mMediaCodec.createInputSurface();
        return true;
    }

}