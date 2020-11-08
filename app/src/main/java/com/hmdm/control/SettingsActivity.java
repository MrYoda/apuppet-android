package com.hmdm.control;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

public class SettingsActivity extends AppCompatActivity {

    private static final int[] bitrates = {128000, 256000, 512000, 768000, 1024000};
    private static final int[] frame_rates = {5, 10, 15, 20};
    private static final int[] idle_timeouts = {60, 120, 300, 600, 1800, 0};

    private EditText editTextServerUrl;
    private EditText editTextSecret;
    private CheckBox checkBoxUseDefault;
    private CheckBox checkBoxTranslateAudio;
    private Spinner spinnerBitrate;
    private Spinner spinnerFrameRate;
    private EditText editTextDeviceName;
    private CheckBox checkBoxNotifySharing;
    private Spinner spinnerIdleTimeout;

    // For test purposes!
    private EditText editTextTestSrcIp;
    private EditText editTextTestDstIp;

    private SettingsHelper settingsHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        settingsHelper = SettingsHelper.getInstance(this);

        editTextServerUrl = findViewById(R.id.server_url);
        editTextSecret = findViewById(R.id.secret);
        checkBoxUseDefault = findViewById(R.id.use_default);
        checkBoxTranslateAudio = findViewById(R.id.translate_audio);
        spinnerBitrate = findViewById(R.id.bitrate);
        spinnerFrameRate = findViewById(R.id.frame_rate);
        editTextDeviceName = findViewById(R.id.device_name);
        checkBoxNotifySharing = findViewById(R.id.notify_sharing);
        spinnerIdleTimeout = findViewById(R.id.exit_on_idle);

        editTextTestSrcIp = findViewById(R.id.test_src_ip);
        editTextTestDstIp = findViewById(R.id.test_dst_ip);
        editTextTestSrcIp.setText(Utils.getLocalIpAddress(this));

        checkBoxUseDefault.setVisibility(BuildConfig.DEFAULT_SECRET.equals("") ? View.GONE : View.VISIBLE);
        checkBoxUseDefault.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateControls();
            if (isChecked && (!editTextServerUrl.getText().toString().equals(BuildConfig.DEFAULT_SERVER_URL) ||
                    !editTextSecret.getText().toString().equals(BuildConfig.DEFAULT_SECRET))) {
                new AlertDialog.Builder(SettingsActivity.this)
                        .setMessage(R.string.use_default_hint)
                        .setPositiveButton(R.string.button_use_default, (dialog, which) -> {
                            editTextServerUrl.setText(BuildConfig.DEFAULT_SERVER_URL);
                            editTextSecret.setText(BuildConfig.DEFAULT_SECRET);
                            updateControls();
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                checkBoxUseDefault.setChecked(false);
                                updateControls();
                            }
                        })
                        .create()
                        .show();
            }
        });

        checkBoxTranslateAudio.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (ContextCompat.checkSelfPermission(SettingsActivity.this, Manifest.permission.RECORD_AUDIO) !=
                        PackageManager.PERMISSION_GRANTED) {
                    checkBoxTranslateAudio.setChecked(false);
                    if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                        new AlertDialog.Builder(SettingsActivity.this)
                                .setMessage(R.string.audio_permission_request)
                                .setPositiveButton(R.string.continue_button, (dialog, which) -> requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},
                                        Const.REQUEST_PERMISSION_AUDIO))
                                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                                })
                                .setCancelable(false)
                                .create()
                                .show();
                    } else {
                        // You can directly ask for the permission.
                        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},
                                Const.REQUEST_PERMISSION_AUDIO);
                    }
                }
            }
        });

        checkBoxNotifySharing.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!Settings.canDrawOverlays(this)) {
                    checkBoxNotifySharing.setChecked(false);
                    Utils.promptOverlayPermissions(this, true);
                }
            }
        });

        loadSettings();

        if (!checkBoxNotifySharing.isChecked()) {
            Toast.makeText(this, R.string.notify_sharing_hint, Toast.LENGTH_LONG).show();
        }

        toolbar.setNavigationOnClickListener(view -> {
            if (saveSettings()) {
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (saveSettings()) {
            finish();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Const.REQUEST_PERMISSION_AUDIO) {
            checkBoxTranslateAudio.setChecked(ContextCompat.checkSelfPermission(SettingsActivity.this, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED);
        } else if (requestCode == Const.REQUEST_PERMISSION_OVERLAY) {
            checkBoxNotifySharing.setChecked(Settings.canDrawOverlays(this));
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return false;
    }

    private void loadSettings() {
        editTextServerUrl.setText(settingsHelper.getString(SettingsHelper.KEY_SERVER_URL));
        editTextSecret.setText(settingsHelper.getString(SettingsHelper.KEY_SECRET));
        checkBoxUseDefault.setChecked(settingsHelper.getBoolean(SettingsHelper.KEY_USE_DEFAULT));
        checkBoxTranslateAudio.setChecked(settingsHelper.getBoolean(SettingsHelper.KEY_TRANSLATE_AUDIO));
        int bitrate = getIndex(bitrates, settingsHelper.getInt(SettingsHelper.KEY_BITRATE));
        if (bitrate >= 0) {
            spinnerBitrate.setSelection(bitrate);
        }
        int frameRate = getIndex(frame_rates, settingsHelper.getInt(SettingsHelper.KEY_FRAME_RATE));
        if (frameRate >= 0) {
            spinnerFrameRate.setSelection(frameRate);
        }
        editTextDeviceName.setText(settingsHelper.getString(SettingsHelper.KEY_DEVICE_NAME));
        editTextTestDstIp.setText(settingsHelper.getString(SettingsHelper.KEY_TEST_DST_IP));
        checkBoxNotifySharing.setChecked(settingsHelper.getBoolean(SettingsHelper.KEY_NOTIFY_SHARING));
        int idleTimeout = getIndex(idle_timeouts, settingsHelper.getInt(SettingsHelper.KEY_IDLE_TIMEOUT));
        if (idleTimeout >= 0) {
            spinnerIdleTimeout.setSelection(idleTimeout);
        }
        updateControls();
    }

    private boolean saveSettings() {
        String serverUrl = editTextServerUrl.getText().toString();
        if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
            Toast.makeText(this, R.string.enter_correct_url, Toast.LENGTH_LONG).show();
            return false;
        }
        String secret = editTextSecret.getText().toString();
        if (secret.trim().equals("")) {
            Toast.makeText(this, R.string.enter_secret, Toast.LENGTH_LONG).show();
            return false;
        }
        String deviceName = editTextDeviceName.getText().toString();
        if (deviceName.trim().equals("")) {
            Toast.makeText(this, R.string.enter_device_name, Toast.LENGTH_LONG).show();
            return false;
        }

        if (areSettingsChanged()) {
            setResult(Const.RESULT_DIRTY);
        } else {
            setResult(RESULT_OK);
        }

        settingsHelper.setString(SettingsHelper.KEY_SERVER_URL, serverUrl);
        settingsHelper.setString(SettingsHelper.KEY_SECRET, secret);
        settingsHelper.setBoolean(SettingsHelper.KEY_USE_DEFAULT, checkBoxUseDefault.isChecked());
        settingsHelper.setBoolean(SettingsHelper.KEY_TRANSLATE_AUDIO, checkBoxTranslateAudio.isChecked());
        settingsHelper.setInt(SettingsHelper.KEY_BITRATE, bitrates[spinnerBitrate.getSelectedItemPosition()]);
        settingsHelper.setInt(SettingsHelper.KEY_FRAME_RATE, frame_rates[spinnerFrameRate.getSelectedItemPosition()]);
        settingsHelper.setString(SettingsHelper.KEY_DEVICE_NAME, deviceName);
        settingsHelper.setString(SettingsHelper.KEY_TEST_DST_IP, editTextTestDstIp.getText().toString());
        settingsHelper.setBoolean(SettingsHelper.KEY_NOTIFY_SHARING, checkBoxNotifySharing.isChecked());
        settingsHelper.setInt(SettingsHelper.KEY_IDLE_TIMEOUT, idle_timeouts[spinnerIdleTimeout.getSelectedItemPosition()]);
        return true;
    }

    private void updateControls() {
        boolean enabled = !checkBoxUseDefault.isChecked();
        editTextServerUrl.setEnabled(enabled);
        editTextSecret.setEnabled(enabled);
    }

    private boolean areSettingsChanged() {
        if (!editTextServerUrl.getText().toString().equals(settingsHelper.getString(SettingsHelper.KEY_SERVER_URL))) {
            return true;
        }
        if (!editTextSecret.getText().toString().equals(settingsHelper.getString(SettingsHelper.KEY_SECRET))) {
            return true;
        }
        if (checkBoxTranslateAudio.isChecked() != settingsHelper.getBoolean(SettingsHelper.KEY_TRANSLATE_AUDIO)) {
            return true;
        }
        if (bitrates[spinnerBitrate.getSelectedItemPosition()] != settingsHelper.getInt(SettingsHelper.KEY_BITRATE)) {
            return true;
        }
        if (frame_rates[spinnerFrameRate.getSelectedItemPosition()] != settingsHelper.getInt(SettingsHelper.KEY_FRAME_RATE)) {
            return true;
        }
        if (!editTextDeviceName.getText().toString().equals(settingsHelper.getString(SettingsHelper.KEY_DEVICE_NAME))) {
            return true;
        }
        if (!editTextTestDstIp.getText().toString().equals(settingsHelper.getString(SettingsHelper.KEY_TEST_DST_IP))) {
            return true;
        }
        // No need to reconnect when KEY_NOTIFY_SHARING or KEY_IDLE_TIMEOUT are changed
        return false;
    }

    private int getIndex(int[] array, int value) {
        for (int n = 0; n < array.length; n++) {
            if (array[n] == value) {
                return n;
            }
        }
        return -1;
    }
}
