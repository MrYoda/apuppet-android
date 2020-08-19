package com.hmdm.control;

import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class SettingsActivity extends AppCompatActivity {

    private static final int[] bitrates = {128000, 256000, 512000, 768000, 1024000};
    private static final int[] frame_rates = {5, 10, 15, 20};

    private EditText editTextServerUrl;
    private CheckBox checkBoxTranslateAudio;
    private Spinner spinnerBitrate;
    private Spinner spinnerFrameRate;

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
        checkBoxTranslateAudio = findViewById(R.id.translate_audio);
        spinnerBitrate = findViewById(R.id.bitrate);
        spinnerFrameRate = findViewById(R.id.frame_rate);

        loadSettings();

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (saveSettings()) {
                    finish();
                }
            }
        });
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return false;
    }

    private void loadSettings() {
        editTextServerUrl.setText(settingsHelper.getString(SettingsHelper.KEY_SERVER_URL));
        checkBoxTranslateAudio.setChecked(settingsHelper.getBoolean(SettingsHelper.KEY_TRANSLATE_AUDIO));
        int bitrate = getIndex(bitrates, settingsHelper.getInt(SettingsHelper.KEY_BITRATE));
        if (bitrate >= 0) {
            spinnerBitrate.setSelection(bitrate);
        }
        int frameRate = getIndex(frame_rates, settingsHelper.getInt(SettingsHelper.KEY_FRAME_RATE));
        if (frameRate >= 0) {
            spinnerFrameRate.setSelection(frameRate);
        }
    }

    private boolean saveSettings() {
        String serverUrl = editTextServerUrl.getText().toString();
        if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
            Toast.makeText(this, R.string.enter_correct_url, Toast.LENGTH_LONG).show();
            return false;
        }
        settingsHelper.setString(SettingsHelper.KEY_SERVER_URL, serverUrl);
        settingsHelper.setBoolean(SettingsHelper.KEY_TRANSLATE_AUDIO, checkBoxTranslateAudio.isChecked());
        settingsHelper.setInt(SettingsHelper.KEY_BITRATE, bitrates[spinnerBitrate.getSelectedItemPosition()]);
        settingsHelper.setInt(SettingsHelper.KEY_FRAME_RATE, frame_rates[spinnerFrameRate.getSelectedItemPosition()]);
        return true;
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
