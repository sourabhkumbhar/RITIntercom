package com.pritesh.ritintercom.datatransfer;

import com.pritesh.ritintercom.data.ChatData;
import com.pritesh.ritintercom.data.DeviceData;


public class GenerateTransferModel {

    public static ITransferable generateDeviceTransferModelRequest(DeviceData device) {
        TransferModel transferModel = new TransferModel(ConstantsTransfer.CLIENT_DATA, ConstantsTransfer.TYPE_REQUEST,
                device.toString());
        return transferModel;
    }

    public static ITransferable generateDeviceTransferModelResponse(DeviceData device) {
        TransferModel transferModel = new TransferModel(ConstantsTransfer.CLIENT_DATA, ConstantsTransfer.TYPE_RESPONSE,
                device.toString());
        return transferModel;
    }

    public static ITransferable generateDeviceTransferModelRequestWD(DeviceData device) {
        TransferModel transferModel = new TransferModel(ConstantsTransfer.CLIENT_DATA_WD, ConstantsTransfer.TYPE_REQUEST,
                device.toString());
        return transferModel;
    }

    public static ITransferable generateDeviceTransferModelResponseWD(DeviceData device) {
        TransferModel transferModel = new TransferModel(ConstantsTransfer.CLIENT_DATA_WD, ConstantsTransfer.TYPE_RESPONSE,
                device.toString());
        return transferModel;
    }

    public static ITransferable generateChatTransferModel(ChatData chat) {
        //All chats are type response as no further response is needed as of now
        TransferModel transferModel = new TransferModel(ConstantsTransfer.CHAT_DATA,
                ConstantsTransfer.TYPE_RESPONSE,
                chat.toString());
        return transferModel;
    }

    public static ITransferable generateChatRequestModel(DeviceData device) {
        TransferModel transferModel = new TransferModel(ConstantsTransfer.CHAT_REQUEST_SENT,
                ConstantsTransfer.TYPE_REQUEST, device.toString());
        return transferModel;
    }

    public static ITransferable generateChatResponseModel(DeviceData device, boolean
            isChatRequestAccepted) {
        int reqCode = isChatRequestAccepted ? ConstantsTransfer.CHAT_REQUEST_ACCEPTED :
                ConstantsTransfer.CHAT_REQUEST_REJECTED;
        TransferModel transferModel = new TransferModel(reqCode,
                ConstantsTransfer.TYPE_RESPONSE, device.toString());
        return transferModel;
    }

    public static ITransferable generateExtensionRequest(String ext) {
        //All chats are type response as no further response is needed as of now
        TransferModel transferModel = new TransferModel(ConstantsTransfer.EXT_DATA,
                ConstantsTransfer.TYPE_REQUEST,
                ext);
        return transferModel;
    }

    public static ITransferable generateExtensionResponce(String ext) {
        //All chats are type response as no further response is needed as of now
        TransferModel transferModel = new TransferModel(ConstantsTransfer.EXT_DATA,
                ConstantsTransfer.TYPE_REQUEST,
                ext);
        return transferModel;
    }


    static class TransferModel implements ITransferable {

        int reqCode;
        String reqType;
        String data;

        TransferModel(int reqCode, String reqType, String data) {
            this.reqCode = reqCode;
            this.reqType = reqType;
            this.data = data;
        }

        @Override
        public int getRequestCode() {
            return reqCode;
        }

        @Override
        public String getRequestType() {
            return reqType;
        }

        @Override
        public String getData() {
            return data;
        }
    }
}
