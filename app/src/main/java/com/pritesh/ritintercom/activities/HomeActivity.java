package com.pritesh.ritintercom.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.pritesh.ritintercom.R;
import com.pritesh.ritintercom.data.DatabaseAdapter;
import com.pritesh.ritintercom.data.DeviceData;
import com.pritesh.ritintercom.utils.NotificationToast;
import com.pritesh.ritintercom.utils.NsdHelper;
import com.pritesh.ritintercom.datatransfer.HandleData;
import com.pritesh.ritintercom.datatransfer.DataSender;
import com.pritesh.ritintercom.datatransfer.ConstantsTransfer;
import com.pritesh.ritintercom.utils.ConnectionUtils;
import com.pritesh.ritintercom.utils.DialogUtils;
import com.pritesh.ritintercom.utils.Utility;

import java.util.ArrayList;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class HomeActivity extends AppCompatActivity implements PeerListFragment.OnListFragmentInteractionListener {

    PeerListFragment deviceListFragment;

    NsdHelper mNsdHelper;

    View progressBar;

    AppController appController = null;

    private DeviceData selectedDevice;
    SweetAlertDialog pDialog;

    public String currentExtension = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nsd);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initialize();
        mNsdHelper = new NsdHelper(this);
        mNsdHelper.initializeNsd();

        mNsdHelper.registerService(ConnectionUtils.getPort(HomeActivity.this));

        mNsdHelper.discoverServices();


        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("file_send"));


    }


    @Override
    public void onBackPressed() {

        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        HomeActivity.this.finish();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }


    // Our handler for received Intents. This will be called whenever an Intent
// with an action named "custom-event-name" is broadcasted.
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("message");
            Log.d("receiver", "Got message: " + message);

            if(pDialog != null){
                pDialog.dismiss();

            }


            new SweetAlertDialog(HomeActivity.this, SweetAlertDialog.SUCCESS_TYPE)
                    .setTitleText("Done!")
                    .setContentText("File transfer successfully..!")
                    .show();
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void initialize() {

        progressBar = findViewById(R.id.progressBarLocalDash);

        String myIP = Utility.getWiFiIPAddress(HomeActivity.this);
        Utility.saveString(HomeActivity.this, ConstantsTransfer.KEY_MY_IP, myIP);

//        connListener = new ListenConnection(HomeActivity.this, ConnectionUtils.getPort
//                (HomeActivity.this));
//        connListener.start();
        appController = (AppController) this.getApplicationContext();
        appController.startConnectionListener();

        checkWritePermission();

        setToolBarTitle(0);
    }



    private void clearReferences(){
        Activity currActivity = appController.getCurrentActivity();
        if (this.equals(currActivity))
            appController.setCurrentActivity(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_local_dash, menu);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void advertiseService(View v) {
//        mNsdHelper.registerService(ConnectionUtils.getPort(HomeActivity.this));
        mNsdHelper.discoverServices();

        Log.d("DXDXDX", Build.MANUFACTURER + " IP: " + Utility.getWiFiIPAddress(this));
        Snackbar.make(v, "Advertising service", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }

    @Override
    protected void onPause() {
        if (mNsdHelper != null) {
            mNsdHelper.stopDiscovery();
        }

        clearReferences();



        LocalBroadcastManager.getInstance(this).unregisterReceiver(Receiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(NsdHelper.BROADCAST_TAG);
        filter.addAction(HandleData.DEVICE_LIST_CHANGED);
        filter.addAction(HandleData.CHAT_REQUEST_RECEIVED);
        filter.addAction(HandleData.CHAT_RESPONSE_RECEIVED);
        LocalBroadcastManager.getInstance(HomeActivity.this).registerReceiver(Receiver,
                filter);
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(HandleData
                .DEVICE_LIST_CHANGED));

        appController.setCurrentActivity(this);


//        appController.startConnectionListener();
//        mNsdHelper.registerService(ConnectionUtils.getPort(HomeActivity.this));
    }

    @Override
    protected void onDestroy() {
        //mNsdHelper.tearDown();
        Utility.clearPreferences(HomeActivity.this);
        appController.stopConnectionListener();
        mNsdHelper.tearDown();
        mNsdHelper = null;
        DatabaseAdapter.getInstance(HomeActivity.this).clearDatabase();

        clearReferences();

        super.onDestroy();
    }

    @Override
    protected void onStop() {
        Log.d("DXDX", Build.MANUFACTURER + ": local dash NSD Stopped");
        super.onStop();
    }

    private BroadcastReceiver Receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case NsdHelper.BROADCAST_TAG:
//                    NsdServiceInfo serviceInfo = intent.getParcelableExtra(NsdHelper
//                            .KEY_SERVICE_INFO);
//                    String[] serviceSplit = serviceInfo.getServiceName().split(":");
//                    String ip = serviceSplit[1];
//                    int port = Integer.parseInt(serviceSplit[2]);
//                    DataSender.sendCurrentDeviceData(HomeActivity.this, ip, port, true);
                    NsdServiceInfo serviceInfo = mNsdHelper.getChosenServiceInfo();
                    String ipAddress = serviceInfo.getHost().getHostAddress();
                    int port = serviceInfo.getPort();
                    DataSender.sendCurrentDeviceData(HomeActivity.this, ipAddress, port, true);
                    break;
                case HandleData.DEVICE_LIST_CHANGED:
                    ArrayList<DeviceData> devices = DatabaseAdapter.getInstance(HomeActivity.this)
                            .getDeviceList();
                    int peerCount = (devices == null) ? 0 : devices.size();
                    if (peerCount > 0) {
                        progressBar.setVisibility(View.GONE);
                        deviceListFragment = new PeerListFragment();
                        Bundle args = new Bundle();
                        args.putSerializable(PeerListFragment.ARG_DEVICE_LIST, devices);
                        deviceListFragment.setArguments(args);

                        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                        ft.replace(R.id.deviceListHolder, deviceListFragment);
                        ft.setTransition(FragmentTransaction.TRANSIT_NONE);
                        ft.commit();
                    }
                    setToolBarTitle(peerCount);
                    break;
                case HandleData.CHAT_REQUEST_RECEIVED:
                    DeviceData chatRequesterDevice = (DeviceData) intent.getSerializableExtra(HandleData
                            .KEY_CHAT_REQUEST);
                    //showChatRequestedDialog(chatRequesterDevice);
                    DialogUtils.getChatRequestDialog(HomeActivity.this, chatRequesterDevice).show();
                    break;
                case HandleData.CHAT_RESPONSE_RECEIVED:
                    boolean isChatRequestAccepted = intent.getBooleanExtra(HandleData
                            .KEY_IS_CHAT_REQUEST_ACCEPTED, false);
                    if (!isChatRequestAccepted) {
                        NotificationToast.showToast(HomeActivity.this, "Chat request " +
                                "rejected");
                    } else {
                        DeviceData chatDevice = (DeviceData) intent.getSerializableExtra(HandleData
                                .KEY_CHAT_REQUEST);
                        DialogUtils.openChatActivity(HomeActivity.this, chatDevice);
                        NotificationToast.showToast(HomeActivity.this, chatDevice
                                .getPlayerName() + "Accepted Chat request");
                    }
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case DialogUtils.CODE_PICK_IMAGE:
                if (resultCode == RESULT_OK) {
                    Uri imageUri = data.getData();

                    String uri = imageUri.toString();
                    currentExtension =  uri.substring(uri.lastIndexOf(".") + 1);

                    pDialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
                    pDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
                    pDialog.setTitleText("Sending file, Please wait.");
                    pDialog.setCancelable(false);
                    pDialog.show();

                    DataSender.sendExtension(HomeActivity.this, selectedDevice.getIp(),
                            selectedDevice.getPort(), imageUri.toString());

                    DataSender.sendFile(HomeActivity.this, selectedDevice.getIp(),
                            selectedDevice.getPort(), imageUri);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
            finish();
        }
    }

    private void checkWritePermission() {
        boolean isGranted = Utility.checkPermission(HomeScreen.WRITE_PERMISSION, this);
        if (!isGranted) {
            Utility.requestPermission(HomeScreen.WRITE_PERMISSION, HomeScreen
                    .WRITE_PERM_REQ_CODE, this);
        }
    }

    @Override
    public void onListFragmentInteraction(DeviceData deviceData) {
        NotificationToast.showToast(HomeActivity.this, deviceData.getDeviceName() + " clicked");
        selectedDevice = deviceData;
//        showServiceSelectionDialog();
        DialogUtils.getServiceSelectionDialog(HomeActivity.this, deviceData).show();
    }

    private void setToolBarTitle(int peerCount) {
        if (getSupportActionBar() != null) {
            String title = String.format(getString(R.string.nsd_title_with_count), String
                    .valueOf(peerCount));
            getSupportActionBar().setTitle(title);

        }
    }
}