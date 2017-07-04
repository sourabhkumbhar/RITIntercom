package com.pritesh.ritintercom.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.pritesh.ritintercom.datatransfer.ConstantsTransfer;
import com.pritesh.ritintercom.datatransfer.DataSender;
import com.pritesh.ritintercom.datatransfer.HandleData;
import com.pritesh.ritintercom.utils.ConnectionUtils;
import com.pritesh.ritintercom.utils.DialogUtils;
import com.pritesh.ritintercom.utils.NotificationToast;
import com.pritesh.ritintercom.utils.Utility;
import com.pritesh.ritintercom.utils.WiFiDirectBroadcastReceiver;

import com.pritesh.ritintercom.R;
import com.pritesh.ritintercom.data.DatabaseAdapter;
import com.pritesh.ritintercom.data.DeviceData;

import java.util.ArrayList;
import java.util.List;

public class HomeWiFiDirect extends AppCompatActivity implements PeerListFragment.OnListFragmentInteractionListener
        , WifiP2pManager.PeerListListener, WifiP2pManager.ConnectionInfoListener {

    public static final String FIRST_DEVICE_CONNECTED = "first_device_connected";
    public static final String KEY_FIRST_DEVICE_IP = "first_device_ip";

    private static final String WRITE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private static final int WRITE_PERM_REQ_CODE = 19;

    PeerListFragment deviceListFragment;
    View progressBar;

    WifiP2pManager wifiP2pManager;
    WifiP2pManager.Channel wifip2pChannel;
    WiFiDirectBroadcastReceiver wiFiDirectBroadcastReceiver;
    private boolean isWifiP2pEnabled = false;
    DeviceData chatDevice;

    private boolean isWDConnected = false;

    private AppController appController;
//    private ListenConnection connListener;

    /**
     * @param isWifiP2pEnabled the isWifiP2pEnabled to set
     */
    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wd);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initialize();
    }

    private void initialize() {

        progressBar = findViewById(R.id.progressBarLocalDash);

        String myIP = Utility.getWiFiIPAddress(HomeWiFiDirect.this);
        Utility.saveString(HomeWiFiDirect.this, ConstantsTransfer.KEY_MY_IP, myIP);

//        Starting connection listener with default for now
//        connListener = new ListenConnection(HomeWiFiDirect.this, ConstantsTransfer.INITIAL_DEFAULT_PORT);
//        connListener.start();

        setToolBarTitle(0);

        wifiP2pManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        wifip2pChannel = wifiP2pManager.initialize(this, getMainLooper(), null);

        // Starting connection listener with default port for now
        appController = (AppController) getApplicationContext();
        appController.startConnectionListener(ConstantsTransfer.INITIAL_DEFAULT_PORT);

        checkWritePermission();
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

    public void findPeers(View v) {

        if (!isWDConnected) {
            Snackbar.make(v, "Finding peers", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            wifiP2pManager.discoverPeers(wifip2pChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    NotificationToast.showToast(HomeWiFiDirect.this, "Peer discovery started");
                }

                @Override
                public void onFailure(int reasonCode) {
                    NotificationToast.showToast(HomeWiFiDirect.this, "Peer discovery failure: "
                            + reasonCode);
                }
            });
        }
    }

    @Override
    protected void onPause() {
//        if (mNsdHelper != null) {
//            mNsdHelper.stopDiscovery();
//        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(Receiver);
        unregisterReceiver(wiFiDirectBroadcastReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter localFilter = new IntentFilter();
        localFilter.addAction(HandleData.DEVICE_LIST_CHANGED);
        localFilter.addAction(FIRST_DEVICE_CONNECTED);
        localFilter.addAction(HandleData.CHAT_REQUEST_RECEIVED);
        localFilter.addAction(HandleData.CHAT_RESPONSE_RECEIVED);
        LocalBroadcastManager.getInstance(HomeWiFiDirect.this).registerReceiver(Receiver,
                localFilter);

        IntentFilter wifip2pFilter = new IntentFilter();
        wifip2pFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        wifip2pFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        wifip2pFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        wifip2pFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        wiFiDirectBroadcastReceiver = new WiFiDirectBroadcastReceiver(wifiP2pManager,
                wifip2pChannel, this);
        registerReceiver(wiFiDirectBroadcastReceiver, wifip2pFilter);

        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(HandleData.DEVICE_LIST_CHANGED));
    }

    @Override
    protected void onDestroy() {
//        mNsdHelper.tearDown();
//        connListener.tearDown();
        appController.stopConnectionListener();
        Utility.clearPreferences(HomeWiFiDirect.this);
        Utility.deletePersistentGroups(wifiP2pManager, wifip2pChannel);
        DatabaseAdapter.getInstance(HomeWiFiDirect.this).clearDatabase();
        wifiP2pManager.removeGroup(wifip2pChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int i) {

            }
        });

        super.onDestroy();
    }

    private BroadcastReceiver Receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case FIRST_DEVICE_CONNECTED:
//                    connListener.tearDown();
//                    int newPort = ConnectionUtils.getPort(HomeWiFiDirect.this);
//                    connListener = new ListenConnection(HomeWiFiDirect.this,
//                            newPort);
//                    connListener.start();
//                    appController.stopConnectionListener();
//                    appController.startConnectionListener(ConnectionUtils.getPort(HomeWiFiDirect.this));
                    appController.restartConnectionListenerWith(ConnectionUtils.getPort(HomeWiFiDirect.this));

                    String senderIP = intent.getStringExtra(KEY_FIRST_DEVICE_IP);
                    int port = DatabaseAdapter.getInstance(HomeWiFiDirect.this).getDevice
                            (senderIP).getPort();
                    DataSender.sendCurrentDeviceData(HomeWiFiDirect.this, senderIP, port, true);
                    isWDConnected = true;
                    break;
                case HandleData.DEVICE_LIST_CHANGED:
                    ArrayList<DeviceData> devices = DatabaseAdapter.getInstance(HomeWiFiDirect.this)
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
//                    DialogUtils.getChatRequestDialog(HomeWiFiDirect.this,
//                            chatRequesterDevice).show();
                    DialogUtils.silentRequest(HomeWiFiDirect.this,
                            chatRequesterDevice);

                    break;
                case HandleData.CHAT_RESPONSE_RECEIVED:

                    boolean isChatRequestAccepted = intent.getBooleanExtra(HandleData
                            .KEY_IS_CHAT_REQUEST_ACCEPTED, false);
//                    if (!isChatRequestAccepted) {
//                        NotificationToast.showToast(HomeWiFiDirect.this, "Chat request " +
//                                "rejected");
//                    } else {
                        chatDevice = (DeviceData) intent.getSerializableExtra(HandleData
                                .KEY_CHAT_REQUEST);
//                        DialogUtils.openChatActivity(HomeWiFiDirect.this, chatDevice);
                        NotificationToast.showToast(HomeWiFiDirect.this, chatDevice
                                .getPlayerName() + "Accepted Chat request");
//                    }
                    break;
                default:
                    break;
            }
        }
    };

    private DeviceData selectedDevice;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case DialogUtils.CODE_PICK_IMAGE:
                if (resultCode == RESULT_OK) {
                    Uri imageUri = data.getData();
                    DataSender.sendFile(HomeWiFiDirect.this, selectedDevice.getIp(),
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
        boolean isGranted = Utility.checkPermission(WRITE_PERMISSION, this);
        if (!isGranted) {
            Utility.requestPermission(WRITE_PERMISSION, WRITE_PERM_REQ_CODE, this);
        }
    }

    boolean isConnectionInfoSent = false;

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {

        if (wifiP2pInfo.groupFormed && !wifiP2pInfo.isGroupOwner && !isConnectionInfoSent) {

            isWDConnected = true;

//            connListener.tearDown();
//            connListener = new ListenConnection(HomeWiFiDirect.this, ConnectionUtils.getPort
//                    (HomeWiFiDirect.this));
//            connListener.start();
//            appController.stopConnectionListener();
//            appController.startConnectionListener(ConnectionUtils.getPort(HomeWiFiDirect.this));
            appController.restartConnectionListenerWith(ConnectionUtils.getPort(HomeWiFiDirect.this));

            String groupOwnerAddress = wifiP2pInfo.groupOwnerAddress.getHostAddress();
            DataSender.sendCurrentDeviceDataWD(HomeWiFiDirect.this, groupOwnerAddress, ConstantsTransfer
                    .INITIAL_DEFAULT_PORT, true);
            isConnectionInfoSent = true;
        }
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {

        ArrayList<DeviceData> deviceDatas = new ArrayList<>();

        List<WifiP2pDevice> devices = (new ArrayList<>());
        devices.addAll(peerList.getDeviceList());
        for (WifiP2pDevice device : devices) {
            DeviceData deviceData = new DeviceData();
            deviceData.setIp(device.deviceAddress);
            deviceData.setPlayerName(device.deviceName);
            deviceData.setDeviceName(new String());
            deviceData.setOsVersion(new String());
            deviceData.setPort(-1);
            deviceDatas.add(deviceData);
        }


        progressBar.setVisibility(View.GONE);
        deviceListFragment = new PeerListFragment();
        Bundle args = new Bundle();
        args.putSerializable(PeerListFragment.ARG_DEVICE_LIST, deviceDatas);
        deviceListFragment.setArguments(args);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.deviceListHolder, deviceListFragment);
        ft.setTransition(FragmentTransaction.TRANSIT_NONE);
        ft.commit();
    }

    @Override
    public void onListFragmentInteraction(DeviceData deviceData) {
        if (!isWDConnected) {
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = deviceData.getIp();
            config.wps.setup = WpsInfo.PBC;
            config.groupOwnerIntent = 4;
            wifiP2pManager.connect(wifip2pChannel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    // Connection request succeeded. No code needed here
                }

                @Override
                public void onFailure(int reasonCode) {
                    NotificationToast.showToast(HomeWiFiDirect.this, "Connection failed. try" +
                            " again: reason: " + reasonCode);
                }
            });
        } else {
            selectedDevice = deviceData;
//            showServiceSelectionDialog();
            DialogUtils.getServiceSelectionDialog(HomeWiFiDirect.this, deviceData).show();
        }
    }

    private void setToolBarTitle(int peerCount) {
        if (getSupportActionBar() != null) {
            String title = String.format(getString(R.string.wd_title_with_count), String
                    .valueOf(peerCount));
            getSupportActionBar().setTitle(title);

        }
    }
}

//    private void showChatRequestedDialog(final DeviceData device) {
//        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
//
//        String chatRequestTitle = getString(R.string.chat_request_title);
//        chatRequestTitle = String.format(chatRequestTitle, device.getPlayerName() + "(" + device
//                .getDeviceName() + ")");
//        alertDialog.setTitle(chatRequestTitle);
//        String[] types = {"Accept", "Reject"};
//        alertDialog.setItems(types, new DialogInterface.OnClickListener() {
//
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//
//                dialog.dismiss();
//                switch (which) {
//                    //Request accepted
//                    case 0:
//                        DialogUtils.openChatActivity(HomeWiFiDirect.this, device);
//                        NotificationToast.showToast(HomeWiFiDirect.this, "Chat request " +
//                                "accepted");
//                        DataSender.sendChatResponse(HomeWiFiDirect.this, device.getIp(),
//                                device.getPort(), true);
//                        break;
//                    // Request rejected
//                    case 1:
//                        DataSender.sendChatResponse(HomeWiFiDirect.this, device.getIp(),
//                                device.getPort(), false);
//                        NotificationToast.showToast(HomeWiFiDirect.this, "Chat request " +
//                                "rejected");
//                        break;
//                }
//            }
//
//        });
//
//        alertDialog.show();
//    }

//    private void openChatActivity(DeviceData chatDevice) {
//        Intent chatIntent = new Intent(HomeWiFiDirect.this, ChatActivity
//                .class);
//        chatIntent.putExtra(ChatActivity.KEY_CHAT_IP, chatDevice.getIp());
//        chatIntent.putExtra(ChatActivity.KEY_CHAT_PORT, chatDevice.getPort());
//        chatIntent.putExtra(ChatActivity.KEY_CHATTING_WITH, chatDevice.getPlayerName());
//        startActivity(chatIntent);
//    }

//    private void showServiceSelectionDialog() {
//        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
//        alertDialog.setTitle(selectedDevice.getDeviceName());
//        String[] types = {"Share image", "Chat"};
//        alertDialog.setItems(types, new DialogInterface.OnClickListener() {
//
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//
//                dialog.dismiss();
//                switch (which) {
//                    case 0:
//                        Intent imagePicker = new Intent(Intent.ACTION_PICK);
//                        imagePicker.setType("image/*");
//                        startActivityForResult(imagePicker, DialogUtils.CODE_PICK_IMAGE);
//                        break;
//                    case 1:
//                        DataSender.sendChatRequest(HomeWiFiDirect.this, selectedDevice.getIp
//                                (), selectedDevice.getPort());
//                        NotificationToast.showToast(HomeWiFiDirect.this, "chat request " +
//                                "sent");
//                        break;
//                }
//            }
//
//        });
//
//        alertDialog.show();
//    }