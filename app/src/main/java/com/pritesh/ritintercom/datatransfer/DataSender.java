package com.pritesh.ritintercom.datatransfer;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.pritesh.ritintercom.data.ChatData;
import com.pritesh.ritintercom.utils.ConnectionUtils;
import com.pritesh.ritintercom.utils.Utility;
import com.pritesh.ritintercom.data.DeviceData;


public class DataSender {

    public static void sendData(Context context, String destIP, int destPort, ITransferable data) {
        Intent serviceIntent = new Intent(context,
                DataTransferService.class);
        serviceIntent.setAction(DataTransferService.ACTION_SEND_DATA);
        serviceIntent.putExtra(
                DataTransferService.DEST_IP_ADDRESS, destIP);
        serviceIntent.putExtra(
                DataTransferService.DEST_PORT_NUMBER, destPort);

        serviceIntent.putExtra(DataTransferService.EXTRAS_SHARE_DATA, data);
        context.startService(serviceIntent);
    }

    public static void sendFile(Context context, String destIP, int destPort, Uri fileUri) {
        Intent serviceIntent = new Intent(context,
                DataTransferService.class);
        serviceIntent.setAction(DataTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(
                DataTransferService.DEST_IP_ADDRESS, destIP);
        serviceIntent.putExtra(
                DataTransferService.DEST_PORT_NUMBER, destPort);
        serviceIntent.putExtra(
                DataTransferService.EXTRAS_FILE_PATH, fileUri.toString());

        context.startService(serviceIntent);
    }

    public static void sendCurrentDeviceData(Context context, String destIP, int destPort,
                                             boolean isRequest) {
        DeviceData currentDevice = new DeviceData();
        currentDevice.setPort(ConnectionUtils.getPort(context));
        String playerName = Utility.getString(context, ConstantsTransfer.KEY_USER_NAME);
        if (playerName != null) {
            currentDevice.setPlayerName(playerName);
        }
        currentDevice.setIp(Utility.getString(context, ConstantsTransfer.KEY_MY_IP));

        ITransferable transferData = null;
        if (!isRequest) {
            transferData = GenerateTransferModel.generateDeviceTransferModelResponse
                    (currentDevice);
        } else {
            transferData = GenerateTransferModel.generateDeviceTransferModelRequest
                    (currentDevice);
        }

        sendData(context, destIP, destPort, transferData);
    }

    public static void sendCurrentDeviceDataWD(Context context, String destIP, int destPort,
                                               boolean isRequest) {
        DeviceData currentDevice = new DeviceData();
        currentDevice.setPort(ConnectionUtils.getPort(context));
        String playerName = Utility.getString(context, ConstantsTransfer.KEY_USER_NAME);
        if (playerName != null) {
            currentDevice.setPlayerName(playerName);
        }
        currentDevice.setIp(Utility.getString(context, ConstantsTransfer.KEY_MY_IP));

        ITransferable transferData = null;
        if (!isRequest) {
            transferData = GenerateTransferModel.generateDeviceTransferModelResponseWD
                    (currentDevice);
        } else {
            transferData = GenerateTransferModel.generateDeviceTransferModelRequestWD
                    (currentDevice);
        }

        sendData(context, destIP, destPort, transferData);
    }

    public static void sendChatRequest(Context context, String destIP, int destPort) {
        DeviceData currentDevice = new DeviceData();
        currentDevice.setPort(ConnectionUtils.getPort(context));
        String playerName = Utility.getString(context, ConstantsTransfer.KEY_USER_NAME);
        if (playerName != null) {
            currentDevice.setPlayerName(playerName);
        }
        currentDevice.setIp(Utility.getString(context, ConstantsTransfer.KEY_MY_IP));
        ITransferable transferData = GenerateTransferModel.generateChatRequestModel(currentDevice);
        sendData(context, destIP, destPort, transferData);
    }

    public static void sendChatResponse(Context context, String destIP, int destPort, boolean
            isAccepted) {
        DeviceData currentDevice = new DeviceData();
        currentDevice.setPort(ConnectionUtils.getPort(context));
        String playerName = Utility.getString(context, ConstantsTransfer.KEY_USER_NAME);
        if (playerName != null) {
            currentDevice.setPlayerName(playerName);
        }
        currentDevice.setIp(Utility.getString(context, ConstantsTransfer.KEY_MY_IP));
        ITransferable transferData = GenerateTransferModel.generateChatResponseModel
                (currentDevice, isAccepted);
        sendData(context, destIP, destPort, transferData);
    }

    public static void sendChatInfo(Context context, String destIP, int destPort, ChatData chat) {
        ITransferable transferableData = GenerateTransferModel.generateChatTransferModel(chat);
        sendData(context, destIP, destPort, transferableData);
    }

    public static void sendExtension(Context context, String destIP, int destPort, String ext) {
        ITransferable transferableData = GenerateTransferModel.generateExtensionRequest(ext);
        sendData(context, destIP, destPort, transferableData);
    }

}
