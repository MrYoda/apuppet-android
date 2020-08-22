package com.hmdm.control;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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
    private Button buttonSharing;
    private TextView textViewExit;

    private SharingEngine sharingEngine;

    private SettingsHelper settingsHelper;

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

        initUI();
        setDefaultSettings();

        connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();

        startService(new Intent(MainActivity.this, GestureDispatchService.class));

 /*       if (!Utils.isAccessibilityPermissionGranted(this)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.accessibility_hint)
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
        }*/
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initUI() {
        imageViewConnStatus = findViewById(R.id.image_conn_status);
        textViewConnStatus = findViewById(R.id.conn_status);
        editTextSessionId = findViewById(R.id.session_id_edit);
        editTextPassword = findViewById(R.id.password_edit);
        buttonSharing = findViewById(R.id.sharing_button);
        textViewExit = findViewById(R.id.disconnect_exit);

        buttonSharing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int state = sharingEngine.getState();
                switch (state) {
                    case Const.STATE_DISCONNECTED:
                        connect();
                        break;
                    case Const.STATE_CONNECTED:
                        startSharing();
                        break;
                    case Const.STATE_SHARING:
                        stopSharing();
                        break;
                }
            }
        });

        textViewExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
        imageViewConnStatus.setImageDrawable(getDrawable(stateImages[state]));
        textViewConnStatus.setText(stateLabels[state]);

        buttonSharing.setEnabled(state != Const.STATE_CONNECTING && state != Const.STATE_DISCONNECTING);
        switch (state) {
            case Const.STATE_DISCONNECTED:
                editTextSessionId.setText("");
                editTextPassword.setText("");
                buttonSharing.setEnabled(true);
                buttonSharing.setText(R.string.connect);
                break;
            case Const.STATE_CONNECTING:
            case Const.STATE_DISCONNECTING:
                buttonSharing.setEnabled(false);
                break;
            case Const.STATE_CONNECTED:
                buttonSharing.setEnabled(true);
                buttonSharing.setText(R.string.start_sharing);
                break;
            case Const.STATE_SHARING:
                buttonSharing.setText(R.string.stop_sharing);
                break;
        }
    }

    private void setDefaultSettings() {
        if (settingsHelper.getInt(SettingsHelper.KEY_BITRATE) == 0) {
            settingsHelper.setInt(SettingsHelper.KEY_BITRATE, Const.DEFAULT_BITRATE);
        }
        if (settingsHelper.getInt(SettingsHelper.KEY_FRAME_RATE) == 0) {
            settingsHelper.setInt(SettingsHelper.KEY_FRAME_RATE, Const.DEFAULT_FRAME_RATE);
        }
    }

    private void connect() {
        String sessionId = Utils.randomString(8, true);
        final String password = Utils.randomString(4, true);
        editTextSessionId.setText(sessionId);
        editTextPassword.setText(password);
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

    private void startSharing() {
        // TODO: run screenSharer and provide audio and video port to it
    }

    private void stopSharing() {
        // TODO
    }

    @Override
    public void onStartSharing(String username) {
        // This event is raised when the admin joins the text room
    }

    @Override
    public void onStopSharing() {
        // This event is raised when the admin leaves the text room
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
    }
}
