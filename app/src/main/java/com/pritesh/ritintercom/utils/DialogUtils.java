package com.pritesh.ritintercom.utils;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;

import com.pritesh.ritintercom.R;
import com.pritesh.ritintercom.datatransfer.DataSender;
import com.pritesh.ritintercom.activities.ChatActivity;
import com.pritesh.ritintercom.data.DeviceData;


public class DialogUtils {

    public static final int CODE_PICK_IMAGE = 21;

    public static AlertDialog getServiceSelectionDialog(final Activity activity, final DeviceData
            selectedDevice) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
        alertDialog.setTitle(selectedDevice.getDeviceName());
        String[] types = {"Share File", "Chat"};
        alertDialog.setItems(types, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
                switch (which) {
                    case 0:
                        Intent imagePicker = new Intent(Intent.ACTION_PICK);
//                        imagePicker.setType("image/*");
                        imagePicker.setType("*/*");
//                        imagePicker.setType("image/* | ");

                        activity.startActivityForResult(imagePicker, CODE_PICK_IMAGE);

//                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//                        intent.addCategory(Intent.CATEGORY_OPENABLE);
//                        intent.setType("*/*");
//                        String[] mimetypes = {"image/*", "pdf/*"};
//                        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
//                        activity.startActivityForResult(intent, CODE_PICK_IMAGE);


                        break;
                    case 1:
                        DataSender.sendChatRequest(activity, selectedDevice.getIp
                                (), selectedDevice.getPort());
                        NotificationToast.showToast(activity, "chat request " +
                                "sent");
                        break;
                }
            }

        });

        return (alertDialog.create());
    }

    public static AlertDialog getChatRequestDialog(final Activity activity, final DeviceData requesterDevice) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);

        String chatRequestTitle = activity.getString(R.string.chat_request_title);
        chatRequestTitle = String.format(chatRequestTitle, requesterDevice.getPlayerName() + "(" +
                requesterDevice.getDeviceName() + ")");
        alertDialog.setTitle(chatRequestTitle);
        String[] types = {"Accept", "Reject"};
        alertDialog.setItems(types, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
                switch (which) {
                    //Request accepted
                    case 0:
                        openChatActivity(activity, requesterDevice);
                        NotificationToast.showToast(activity, "Chat request " +
                                "accepted");
                        DataSender.sendChatResponse(activity, requesterDevice.getIp(),
                                requesterDevice.getPort(), true);
                        break;
                    // Request rejected
                    case 1:
                        DataSender.sendChatResponse(activity, requesterDevice.getIp(),
                                requesterDevice.getPort(), false);
                        NotificationToast.showToast(activity, "Chat request " +
                                "rejected");
                        break;
                }
            }

        });

        return (alertDialog.create());
    }

    public static void openChatActivity(Activity activity, DeviceData device) {
        Intent chatIntent = new Intent(activity, ChatActivity.class);
        chatIntent.putExtra(ChatActivity.KEY_CHAT_IP, device.getIp());
        chatIntent.putExtra(ChatActivity.KEY_CHAT_PORT, device.getPort());
        chatIntent.putExtra(ChatActivity.KEY_CHATTING_WITH, device.getPlayerName());
        activity.startActivity(chatIntent);
    }
}
