package com.pritesh.ritintercom.datatransfer;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.pritesh.ritintercom.data.ChatData;
import com.pritesh.ritintercom.activities.ChatActivity;
import com.pritesh.ritintercom.activities.HomeWiFiDirect;
import com.pritesh.ritintercom.data.DatabaseAdapter;
import com.pritesh.ritintercom.data.DeviceData;


public class HandleData {

    public static final String DEVICE_LIST_CHANGED = "device_list_updated";

    public static final String CHAT_REQUEST_RECEIVED = "chat_request_received";
    public static final String CHAT_RESPONSE_RECEIVED = "chat_response_received";
    public static final String KEY_CHAT_REQUEST = "chat_requester_or_responder";
    public static final String KEY_IS_CHAT_REQUEST_ACCEPTED = "is_chat_request_Accespter";


    private ITransferable data;
    private Context mContext;
    private String senderIP;
    private LocalBroadcastManager broadcaster;
    private DatabaseAdapter databaseAdapter = null;

    HandleData(Context context, String senderIP, ITransferable data) {
        this.mContext = context;
        this.data = data;
        this.senderIP = senderIP;
        this.databaseAdapter = DatabaseAdapter.getInstance(mContext);
        this.broadcaster = LocalBroadcastManager.getInstance(mContext);
    }

    public void process() {
        if (data.getRequestType().equalsIgnoreCase(ConstantsTransfer.TYPE_REQUEST)) {
            processRequest();
        } else {
            processResponse();
        }
    }

    private void processRequest() {
        switch (data.getRequestCode()) {
            case ConstantsTransfer.CLIENT_DATA:
                processPeerDeviceInfo();
                DataSender.sendCurrentDeviceData(mContext, senderIP,
                        databaseAdapter.getDevice(senderIP).getPort(), false);
                break;
            case ConstantsTransfer.CLIENT_DATA_WD:
                processPeerDeviceInfo();
                Intent intent = new Intent(HomeWiFiDirect.FIRST_DEVICE_CONNECTED);
                intent.putExtra(HomeWiFiDirect.KEY_FIRST_DEVICE_IP, senderIP);
                broadcaster.sendBroadcast(intent);
                break;
            case ConstantsTransfer.CHAT_REQUEST_SENT:
                processChatRequestReceipt();
            default:
                break;
        }
    }

    private void processResponse() {
        switch (data.getRequestCode()) {
            case ConstantsTransfer.CLIENT_DATA:
            case ConstantsTransfer.CLIENT_DATA_WD:
                processPeerDeviceInfo();
                break;
            case ConstantsTransfer.CHAT_DATA:
                processChatData();
                break;
            case ConstantsTransfer.CHAT_REQUEST_ACCEPTED:
                processChatRequestResponse(true);
                break;
            case ConstantsTransfer.CHAT_REQUEST_REJECTED:
                processChatRequestResponse(false);
                break;
            default:
                break;
        }
    }

    private void processChatRequestReceipt() {
        String chatRequesterDeviceJSON = data.getData();
        DeviceData chatRequesterDevice = DeviceData.fromJSON(chatRequesterDeviceJSON);
        chatRequesterDevice.setIp(senderIP);

        Intent intent = new Intent(CHAT_REQUEST_RECEIVED);
        intent.putExtra(KEY_CHAT_REQUEST, chatRequesterDevice);
        broadcaster.sendBroadcast(intent);
    }

    private void processChatRequestResponse(boolean isRequestAccepted) {
        String chatResponderDeviceJSON = data.getData();
        DeviceData chatResponderDevice = DeviceData.fromJSON(chatResponderDeviceJSON);
        chatResponderDevice.setIp(senderIP);

        Intent intent = new Intent(CHAT_RESPONSE_RECEIVED);
        intent.putExtra(KEY_CHAT_REQUEST, chatResponderDevice);
        intent.putExtra(KEY_IS_CHAT_REQUEST_ACCEPTED, isRequestAccepted);
        broadcaster.sendBroadcast(intent);
    }

    private void processChatData() {
        String chatJSON = data.getData();
        ChatData chatObject = ChatData.fromJSON(chatJSON);
        chatObject.setFromIP(senderIP);
        //Save in db if needed here
        Intent chatReceivedIntent = new Intent(ChatActivity.ACTION_CHAT_RECEIVED);
        chatReceivedIntent.putExtra(ChatActivity.KEY_CHAT_DATA, chatObject);
        broadcaster.sendBroadcast(chatReceivedIntent);
    }

    private void processPeerDeviceInfo() {
        String deviceJSON = data.getData();
        DeviceData device = DeviceData.fromJSON(deviceJSON);
        device.setIp(senderIP);
        long rowid = databaseAdapter.addDevice(device);

        if (rowid > 0) {
            Log.d("DXDX", Build.MANUFACTURER + " received: " + deviceJSON);
        } else {
            Log.e("DXDX", Build.MANUFACTURER + " can't save: " + deviceJSON);
        }

        Intent intent = new Intent(DEVICE_LIST_CHANGED);
        broadcaster.sendBroadcast(intent);
    }

}
