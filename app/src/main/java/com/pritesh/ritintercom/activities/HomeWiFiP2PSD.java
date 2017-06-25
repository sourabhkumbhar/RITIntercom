package com.pritesh.ritintercom.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
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

import com.pritesh.ritintercom.datatransfer.ConstantsTransfer;
import com.pritesh.ritintercom.datatransfer.DataSender;
import com.pritesh.ritintercom.p2psd.P2PReceiver;
import com.pritesh.ritintercom.utils.ConnectionUtils;
import com.pritesh.ritintercom.utils.NotificationToast;
import com.pritesh.ritintercom.utils.Utility;

import com.pritesh.ritintercom.R;
import com.pritesh.ritintercom.data.DatabaseAdapter;
import com.pritesh.ritintercom.data.DeviceData;
import com.pritesh.ritintercom.datatransfer.HandleData;
import com.pritesh.ritintercom.utils.DialogUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HomeWiFiP2PSD extends AppCompatActivity implements PeerListFragment.OnListFragmentInteractionListener
        , WifiP2pManager.ConnectionInfoListener {

    private static final int CODE_PICK_IMAGE = 21;

    private static final String TAG = "WiFIP2PSD";
    private static final String SERVICE_INSTANCE = "RITIntercom";
    private static final String SERVICE_TYPE = "_ritintercom._tcp";

    PeerListFragment deviceListFragment;

    View progressBar;

    WifiP2pManager wifiP2pManager;
    WifiP2pManager.Channel wifip2pChannel;
    P2PReceiver p2PReceiver;
    //private boolean isWifiP2pEnabled = false;

    private AppController appController;

    WifiP2pDnsSdServiceRequest serviceRequest = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wd);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initialize();
    }

    private void initialize() {

        appController = (AppController) getApplicationContext();

        progressBar = findViewById(R.id.progressBarLocalDash);

        appController.startConnectionListener();

        setToolBarTitle(0);

        wifiP2pManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        wifip2pChannel = wifiP2pManager.initialize(this, getMainLooper(), null);

//        startRegistrationAndDiscovery(ConnectionUtils.getPort(HomeWiFiP2PSD.this));
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
        Snackbar.make(v, "Replace this junk with yours", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }

    /**
     * Registers a local service and then initiates a service discovery
     */
    private void startRegistrationAndDiscovery(int port) {

        String player = Utility.getString(HomeWiFiP2PSD.this, ConstantsTransfer.KEY_USER_NAME);

        Map<String, String> record = new HashMap<String, String>();
        record.put(ConstantsTransfer.KEY_BUDDY_NAME, player == null ? Build.MANUFACTURER : player);
        record.put(ConstantsTransfer.KEY_PORT_NUMBER, String.valueOf(port));
        record.put(ConstantsTransfer.KEY_DEVICE_STATUS, "available");
        record.put(ConstantsTransfer.KEY_WIFI_IP, Utility.getWiFiIPAddress(HomeWiFiP2PSD.this));

        WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(
                SERVICE_INSTANCE, SERVICE_TYPE, record);
        wifiP2pManager.addLocalService(wifip2pChannel, service, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Added Local Service");
            }

            @Override
            public void onFailure(int error) {
                Log.e(TAG, "ERRORCEPTION: Failed to add a service");
            }
        });
        discoverService();
    }

    private void discoverService() {

        /*
         * Register listeners for DNS-SD services. These are callbacks invoked
         * by the system when a service is actually discovered.
         */

        wifiP2pManager.setDnsSdResponseListeners(wifip2pChannel,
                new WifiP2pManager.DnsSdServiceResponseListener() {

                    @Override
                    public void onDnsSdServiceAvailable(String instanceName,
                                                        String registrationType, WifiP2pDevice srcDevice) {
                        Log.d(TAG, instanceName + "####" + registrationType);
                        // A service has been discovered. Is this our app?
                        if (instanceName.equalsIgnoreCase(SERVICE_INSTANCE)) {
                            // yes it is
                            WiFiP2pServiceHolder serviceHolder = new WiFiP2pServiceHolder();
                            serviceHolder.device = srcDevice;
                            serviceHolder.registrationType = registrationType;
                            serviceHolder.instanceName = instanceName;
                            connectP2p(serviceHolder);
                        } else {
                            //no it isn't
                        }
                    }
                }, new WifiP2pManager.DnsSdTxtRecordListener() {

                    @Override
                    public void onDnsSdTxtRecordAvailable(
                            String fullDomainName, Map<String, String> record,
                            WifiP2pDevice device) {
                        boolean isGroupOwner = device.isGroupOwner();
                        peerPort = Integer.parseInt(record.get(ConstantsTransfer.KEY_PORT_NUMBER).toString());
                        Log.v(TAG, Build.MANUFACTURER + ". peer port received: " + peerPort);
                        if (peerIP != null && peerPort > 0 && !isConnectionInfoSent) {
                            String player = record.get(ConstantsTransfer.KEY_BUDDY_NAME).toString();

                            DataSender.sendCurrentDeviceData(HomeWiFiP2PSD.this,
                                    peerIP, peerPort, true);
                            isWDConnected = true;
                            isConnectionInfoSent = true;
                        }
                    }
                });

        // After attaching listeners, create a service request and initiate
        // discovery.
        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        wifiP2pManager.addServiceRequest(wifip2pChannel, serviceRequest,
                new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Added service discovery request");
                    }

                    @Override
                    public void onFailure(int arg0) {
                        Log.d(TAG, "ERRORCEPTION: Failed adding service discovery request");
                    }
                });
        wifiP2pManager.discoverServices(wifip2pChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Service discovery initiated");
            }

            @Override
            public void onFailure(int arg0) {
                Log.d(TAG, "Service discovery failed: " + arg0);
            }
        });
    }

    @Override
    protected void onPause() {
//        if (mNsdHelper != null) {
//            mNsdHelper.stopDiscovery();
//        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        unregisterReceiver(p2PReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter localFilter = new IntentFilter();
        localFilter.addAction(HandleData.DEVICE_LIST_CHANGED);
        localFilter.addAction(HandleData.CHAT_REQUEST_RECEIVED);
        localFilter.addAction(HandleData.CHAT_RESPONSE_RECEIVED);
        LocalBroadcastManager.getInstance(HomeWiFiP2PSD.this).registerReceiver(receiver,
                localFilter);

        IntentFilter wifip2pFilter = new IntentFilter();
        wifip2pFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        wifip2pFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        p2PReceiver = new P2PReceiver(wifiP2pManager,
                wifip2pChannel, this);
        registerReceiver(p2PReceiver, wifip2pFilter);

        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(HandleData.DEVICE_LIST_CHANGED));
    }

    @Override
    protected void onDestroy() {
//        mNsdHelper.tearDown();
//        connListener.tearDown();
        appController.stopConnectionListener();

        Utility.clearPreferences(HomeWiFiP2PSD.this);
        Utility.deletePersistentGroups(wifiP2pManager, wifip2pChannel);

        if (wifiP2pManager != null && wifip2pChannel != null) {
            wifiP2pManager.removeGroup(wifip2pChannel, new WifiP2pManager.ActionListener() {

                @Override
                public void onFailure(int reasonCode) {
                    Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);
                }

                @Override
                public void onSuccess() {
                }

            });
        }
        super.onDestroy();
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case HandleData.DEVICE_LIST_CHANGED:
                    ArrayList<DeviceData> devices = DatabaseAdapter.getInstance(HomeWiFiP2PSD.this)
                            .getDeviceList();
                    int peerCount = (devices == null) ? 0 : devices.size();
                    if (peerCount > 0) {
                        isWDConnected = true;
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
                    DialogUtils.getChatRequestDialog(HomeWiFiP2PSD.this,
                            chatRequesterDevice).show();
                    break;
                case HandleData.CHAT_RESPONSE_RECEIVED:
                    boolean isChatRequestAccepted = intent.getBooleanExtra(HandleData
                            .KEY_IS_CHAT_REQUEST_ACCEPTED, false);
                    if (!isChatRequestAccepted) {
                        NotificationToast.showToast(HomeWiFiP2PSD.this, "Chat request " +
                                "rejected");
                    } else {
                        DeviceData chatDevice = (DeviceData) intent.getSerializableExtra(HandleData
                                .KEY_CHAT_REQUEST);
                        DialogUtils.openChatActivity(HomeWiFiP2PSD.this, chatDevice);
                        NotificationToast.showToast(HomeWiFiP2PSD.this, chatDevice
                                .getPlayerName() + "Accepted Chat request");
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private DeviceData selectedDevice = new DeviceData();

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CODE_PICK_IMAGE:
                    Uri imageUri = data.getData();


                    DataSender.sendFile(HomeWiFiP2PSD.this, selectedDevice.getIp(),
                            selectedDevice.getPort(), imageUri);


                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
            NotificationToast.showToast(HomeWiFiP2PSD.this, "This permission is needed for " +
                    "file sharing. But Whatever, if that's what you want...!!!");
            finish();
        } else {
            startRegistrationAndDiscovery(ConnectionUtils.getPort(HomeWiFiP2PSD.this));
        }
    }

    private void checkWritePermission() {
        boolean isGranted = Utility.checkPermission(HomeScreen.WRITE_PERMISSION, this);
        if (!isGranted) {
            Utility.requestPermission(HomeScreen.WRITE_PERMISSION, HomeScreen.WRITE_PERM_REQ_CODE,
                    this);
        } else {
            startRegistrationAndDiscovery(ConnectionUtils.getPort(HomeWiFiP2PSD.this));
        }
    }

    private boolean isConnectionInfoSent = false;
    private String peerIP = null;
    private int peerPort = -1;

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {

        Log.v(TAG, Build.MANUFACTURER + ". Conn info available" + wifiP2pInfo);
        Log.v(TAG, Build.MANUFACTURER + ". peer port: " + peerPort);

        if (wifiP2pInfo.groupFormed) {
            peerIP = wifiP2pInfo.groupOwnerAddress.getHostAddress();
        }

        if (!isConnectionInfoSent && peerPort > 0 && wifiP2pInfo != null && wifiP2pInfo.groupFormed) {
            DataSender.sendCurrentDeviceData(HomeWiFiP2PSD.this, peerIP, peerPort, true);
            isConnectionInfoSent = true;
        }
    }


    private boolean isConnectP2pCalled = false;

    private void connectP2p(WiFiP2pServiceHolder serviceHolder) {
        if (!isConnectP2pCalled) {
            isConnectP2pCalled = true;
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = serviceHolder.device.deviceAddress;
            config.wps.setup = WpsInfo.PBC;
            if (serviceRequest != null)
                wifiP2pManager.removeServiceRequest(wifip2pChannel, serviceRequest,
                        new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess() {
                            }

                            @Override
                            public void onFailure(int arg0) {
                            }
                        });

            wifiP2pManager.connect(wifip2pChannel, config, new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    //("Connecting to service");
                }

                @Override
                public void onFailure(int errorCode) {
                    //("Failed connecting to service");
                }
            });
        }
    }

    private boolean isWDConnected = false;

    @Override
    public void onListFragmentInteraction(DeviceData deviceData) {
        if (!isWDConnected) {
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = deviceData.getIp();
            config.wps.setup = WpsInfo.PBC;
            wifiP2pManager.connect(wifip2pChannel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    // Connection request succeeded. No code needed here
                }

                @Override
                public void onFailure(int i) {
                    NotificationToast.showToast(HomeWiFiP2PSD.this, "Connection failed. try again");
                }
            });
        } else {
            NotificationToast.showToast(HomeWiFiP2PSD.this, deviceData.getDeviceName() + " clicked");
            selectedDevice = deviceData;
            DialogUtils.getServiceSelectionDialog(HomeWiFiP2PSD.this, deviceData).show();
        }
    }

    private class WiFiP2pServiceHolder {
        WifiP2pDevice device;
        String instanceName;
        String registrationType;
    }

    private void setToolBarTitle(int peerCount) {
        if (getSupportActionBar() != null) {
            String title = String.format(getString(R.string.p2psd_title_with_count), String
                    .valueOf(peerCount));
            getSupportActionBar().setTitle(title);

        }
    }
}
