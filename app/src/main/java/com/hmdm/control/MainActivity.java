package com.hmdm.control;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.hmdm.control.janus.SharingEngineJanus;

public class MainActivity extends AppCompatActivity implements SharingEngineJanus.EventListener, SharingEngineJanus.StateListener {

    private ImageView imageViewConnStatus;
    private TextView textViewConnStatus;
    private EditText editTextSessionId;
    private EditText editTextPassword;
    private TextView textViewComment;
    private TextView textViewConnect;
    private TextView textViewExit;

    private SharingEngine sharingEngine;

    private SettingsHelper settingsHelper;

    private String sessionId;
    private String password;
    private String adminName;

    private boolean needReconnect = false;

    private ScreenSharer screenSharer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        settingsHelper = SettingsHelper.getInstance(this);
        sharingEngine = SharingEngineFactory.getSharingEngine();
        sharingEngine.setEventListener(this);
        sharingEngine.setStateListener(this);

        screenSharer = new ScreenSharer(this);

        initUI();
        setDefaultSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();

        startService(new Intent(MainActivity.this, GestureDispatchService.class));
        if (!Utils.isAccessibilityPermissionGranted(this)) {
            textViewConnect.setVisibility(View.INVISIBLE);
            new AlertDialog.Builder(this)
                    .setMessage(R.string.accessibility_hint)
                    .setPositiveButton(R.string.continue_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                            startActivityForResult(intent, 0);
                        }
                    })
                    .setCancelable(false)
                    .create()
                    .show();
        } else {
            if (needReconnect) {
                // Here we go after changing settings
                needReconnect = false;
                if (sharingEngine.getState() != Const.STATE_DISCONNECTED) {
                    sharingEngine.disconnect(MainActivity.this, (success, errorReason) -> connect());
                } else {
                    connect();
                }
            } else {
                if (sharingEngine.getState() == Const.STATE_DISCONNECTED && sharingEngine.getErrorReason() == null) {
                    connect();
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, R.string.back_pressed, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            if (adminName != null) {
                Toast.makeText(this, R.string.settings_unavailable, Toast.LENGTH_LONG).show();
                return true;
            }
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivityForResult(intent, Const.REQUEST_SETTINGS);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Const.REQUEST_SETTINGS && resultCode == Const.RESULT_DIRTY) {
            needReconnect = true;
        } else if (requestCode == Const.REQUEST_SCREEN_SHARE) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(this, R.string.screen_cast_denied, Toast.LENGTH_LONG).show();
                adminName = null;
                updateUI();
            } else {
                screenSharer.setMediaProjectionCallback(new MediaProjection.Callback() {
                    @Override
                    public void onStop() {
                        super.onStop();
                        adminName = null;
                        updateUI();
                    }
                });
                screenSharer.onSharePermissionGranted(this, resultCode, data);
            }
        }
    }

    private void initUI() {
        imageViewConnStatus = findViewById(R.id.image_conn_status);
        textViewConnStatus = findViewById(R.id.conn_status);
        editTextSessionId = findViewById(R.id.session_id_edit);
        editTextPassword = findViewById(R.id.password_edit);
        textViewComment = findViewById(R.id.comment);
        textViewConnect = findViewById(R.id.reconnect);
        textViewExit = findViewById(R.id.disconnect_exit);

        textViewConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connect();
            }
        });

        textViewExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (adminName != null) {
                    screenSharer.stopShare(MainActivity.this);
                }
                sharingEngine.disconnect(MainActivity.this, new SharingEngineJanus.CompletionHandler() {
                    @Override
                    public void onComplete(boolean success, String errorReason) {
                        finish();
                    }
                });
            }
        });
    }

    private void updateUI() {
        int[] stateLabels = {R.string.state_disconnected, R.string.state_connecting, R.string.state_connected, R.string.state_sharing, R.string.state_disconnecting};
        int[] stateImages = {R.drawable.ic_disconnected, R.drawable.ic_connecting, R.drawable.ic_connected, R.drawable.ic_sharing, R.drawable.ic_connecting};

        int state = sharingEngine.getState();
        if (state == Const.STATE_CONNECTED && adminName != null) {
            imageViewConnStatus.setImageDrawable(getDrawable(stateImages[Const.STATE_SHARING]));
            textViewConnStatus.setText(stateLabels[Const.STATE_SHARING]);
        } else {
            imageViewConnStatus.setImageDrawable(getDrawable(stateImages[state]));
            textViewConnStatus.setText(stateLabels[state]);
        }
        String serverUrl = Utils.prepareDisplayUrl(settingsHelper.getString(SettingsHelper.KEY_SERVER_URL));

        textViewConnect.setVisibility(state == Const.STATE_DISCONNECTED ? View.VISIBLE : View.INVISIBLE);
        switch (state) {
            case Const.STATE_DISCONNECTED:
                editTextSessionId.setText("");
                editTextPassword.setText("");
                if (sharingEngine.getErrorReason() != null) {
                    textViewComment.setText(getString(R.string.hint_connection_error, serverUrl));
                }
                break;
            case Const.STATE_CONNECTING:
                textViewComment.setText(getString(R.string.hint_connecting, serverUrl));
                break;
            case Const.STATE_DISCONNECTING:
                textViewComment.setText(getString(R.string.hint_disconnecting));
                break;
            case Const.STATE_CONNECTED:
                editTextSessionId.setText(sessionId);
                editTextPassword.setText(password);
                textViewComment.setText(adminName != null ?
                        getString(R.string.hint_sharing, adminName) :
                        getString(R.string.hint_connected, serverUrl)
                        );
                break;
        }
    }

    private void setDefaultSettings() {
        if (settingsHelper.getString(SettingsHelper.KEY_DEVICE_NAME) == null) {
            settingsHelper.setString(SettingsHelper.KEY_DEVICE_NAME, Build.MANUFACTURER + " " + Build.MODEL);
        }
        if (settingsHelper.getInt(SettingsHelper.KEY_BITRATE) == 0) {
            settingsHelper.setInt(SettingsHelper.KEY_BITRATE, Const.DEFAULT_BITRATE);
        }
        if (settingsHelper.getInt(SettingsHelper.KEY_FRAME_RATE) == 0) {
            settingsHelper.setInt(SettingsHelper.KEY_FRAME_RATE, Const.DEFAULT_FRAME_RATE);
        }
    }

    private void connect() {
        sessionId = Utils.randomString(8, true);
        password = Utils.randomString(4, true);
        sharingEngine.setUsername(settingsHelper.getString(SettingsHelper.KEY_DEVICE_NAME));
        sharingEngine.connect(this, sessionId, password, new SharingEngineJanus.CompletionHandler() {
            @Override
            public void onComplete(boolean success, String errorReason) {
                if (!success) {
                    String message = getString(R.string.connection_error, settingsHelper.getString(SettingsHelper.KEY_SERVER_URL), errorReason);
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                    editTextSessionId.setText(null);
                    editTextPassword.setText(null);
                }
            }
        });
    }

    @Override
    public void onStartSharing(String adminName) {
        // This event is raised when the admin joins the text room
        this.adminName = adminName;
        updateUI();
        screenSharer.startShare(this);
    }

    @Override
    public void onStopSharing() {
        // This event is raised when the admin leaves the text room
        adminName = null;
        updateUI();
        screenSharer.stopShare(this);
    }

    @Override
    public void onRemoteControlEvent(String event) {
        Intent intent = new Intent(MainActivity.this, GestureDispatchService.class);
        intent.putExtra(Const.EXTRA_EVENT, event);
        startService(intent);
    }

    @Override
    public void onSharingApiStateChanged(int state) {
        updateUI();
        if (state == Const.STATE_CONNECTED) {
            screenSharer.configure(settingsHelper.getBoolean(SettingsHelper.KEY_TRANSLATE_AUDIO),
                    settingsHelper.getInt(SettingsHelper.KEY_FRAME_RATE),
                    settingsHelper.getInt(SettingsHelper.KEY_BITRATE),
                    Utils.getRtpUrl(settingsHelper.getString(SettingsHelper.KEY_SERVER_URL)),
                    sharingEngine.getAudioPort(),
                    sharingEngine.getVideoPort()
                    );
        }
    }
}
