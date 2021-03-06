package com.pritesh.ritintercom.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.pritesh.ritintercom.R;
import com.pritesh.ritintercom.data.DatabaseAdapter;
import com.pritesh.ritintercom.utils.NotificationToast;
import com.pritesh.ritintercom.datatransfer.ConstantsTransfer;
import com.pritesh.ritintercom.utils.ConnectionUtils;
import com.pritesh.ritintercom.utils.Utility;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;

public class HomeScreen extends AppCompatActivity {

    public static final String WRITE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    public static final int WRITE_PERM_REQ_CODE = 19;

    EditText etUsername;
    TextView tvPort;
    Button me;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);
        etUsername = (EditText) findViewById(R.id.et_home_player_name);
        tvPort = (TextView) findViewById(R.id.tv_port_info);
        me = (Button) findViewById(R.id.me);


        me.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(HomeScreen.this,ProfileActivity.class));
            }
        });



        String userNameHint = getString(R.string.enter_name_hint) + "(default = " + Build
                .MANUFACTURER + ")";
        etUsername.setHint(userNameHint);

        checkWritePermission();
        printInterfaces();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
            NotificationToast.showToast(HomeScreen.this, "This permission is needed for " +
                    "file sharing. But Whatever, if that's what you want...!!!");
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        DatabaseAdapter.getInstance(HomeScreen.this).clearDatabase();
        tvPort.setText(String.format(getString(R.string.port_info), ConnectionUtils.getPort
                (HomeScreen.this)));
    }

    private void printInterfaces() {
        try {
            Enumeration<NetworkInterface> x = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface ni : Collections.list(x)) {
                Log.v("NetWorkInterfaces", "display name: " + ni.getDisplayName());
                Log.v("NetWorkInterfaces", "name: " + ni.getName());
                Log.v("NetWorkInterfaces", "is up and running ? : " + String.valueOf(ni.isUp()));
                Log.v("NetWorkInterfaces", "Loopback?: " + String.valueOf(ni.isLoopback()));
                Log.v("NetWorkInterfaces", "Supports multicast: " + String.valueOf(ni
                        .supportsMulticast()));
                Log.v("NetWorkInterfaces", "is virtual: " + String.valueOf(ni.isVirtual()));
                Log.v("NetWorkInterfaces", "Hardware address: " + Arrays.toString(ni
                        .getHardwareAddress()));
                Log.v("NetWorkInterfaces", "Sub interfaces.....");
                Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
                for (InetAddress singleNI : Collections.list(inetAddresses)) {
                    Log.v("NetWorkInterfaces", "sub ni inetaddress: " + singleNI.getHostAddress());
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private void saveUsername() {
        String userName = etUsername.getText().toString();
        if (userName != null && userName.trim().length() > 0) {
            Utility.saveString(HomeScreen.this, ConstantsTransfer.KEY_USER_NAME, userName);
        }
    }

    private void checkWritePermission() {
        boolean isGranted = Utility.checkPermission(WRITE_PERMISSION, this);
        if (!isGranted) {
            Utility.requestPermission(WRITE_PERMISSION, WRITE_PERM_REQ_CODE, this);
        }
    }

    public void startNSD(View v) {
        if (Utility.isWifiConnected(HomeScreen.this)) {
            saveUsername();
            Intent nsdIntent = new Intent(HomeScreen.this, HomeActivity.class);
            startActivity(nsdIntent);
            finish();
        } else {
            NotificationToast.showToast(HomeScreen.this, getString(R.string
                    .wifi_not_connected_error));
        }
    }

    public void startWiFiDirect(View v) {
        if (Utility.isWiFiEnabled(HomeScreen.this)) {
            saveUsername();
            Intent wifiDirectIntent = new Intent(HomeScreen.this, HomeWiFiDirect.class);
            startActivity(wifiDirectIntent);
            finish();
        } else {
            NotificationToast.showToast(HomeScreen.this, getString(R.string
                    .wifi_not_enabled_error));
        }
    }

    public void startWiFiDirectServiceDiscovery(View v) {
        if (Utility.isWiFiEnabled(HomeScreen.this)) {
            saveUsername();
            Intent wifiDirectServiceDiscoveryIntent = new Intent(HomeScreen.this, HomeWiFiP2PSD.class);
            startActivity(wifiDirectServiceDiscoveryIntent);
            finish();
        } else {
            NotificationToast.showToast(HomeScreen.this, getString(R.string
                    .wifi_not_enabled_error));
        }
    }
}
