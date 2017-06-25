package com.pritesh.ritintercom.datatransfer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.pritesh.ritintercom.activities.AppController;
import com.pritesh.ritintercom.utils.Utility;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Properties;


public class ListenConnection extends Thread {

    private int mPort;
    private Context mContext;
    private ServerSocket mServer;

    private boolean acceptRequests = true;
    Activity activity;
    String extension;


    public ListenConnection(Context context, int port) {

        this.activity = activity;
        this.mContext = context;
        this.mPort = port;


    }

    @Override
    public void run() {
        try {
            Log.d("DXDX", Build.MANUFACTURER + ": conn listener: " + mPort);
            mServer = new ServerSocket(mPort);
            mServer.setReuseAddress(true);

            if (mServer != null && !mServer.isBound()) {
                mServer.bind(new InetSocketAddress(mPort));
            }

            Log.d("DDDD", "Inet4Address: " + Inet4Address.getLocalHost().getHostAddress());

            Socket socket = null;
            while (acceptRequests) {
                // this is a blocking operation
                socket = mServer.accept();
                handleData(socket.getInetAddress().getHostAddress(), socket.getInputStream());
            }
            Log.e("DXDX", Build.MANUFACTURER + ": Connection listener terminated. " +
                    "acceptRequests: " + acceptRequests);
            socket.close();
            socket = null;

        } catch (IOException e) {
            Log.e("DXDX", Build.MANUFACTURER + ": Connection listener EXCEPTION. " + e.toString());
            e.printStackTrace();
        }
    }

    private void handleData(String senderIP, InputStream inputStream) {
        try {
            final byte[] input = Utility.getInputStreamByteArray(inputStream);

            ObjectInput oin = null;
            try {
                oin = new ObjectInputStream(new ByteArrayInputStream(input));
                ITransferable transferObject = (ITransferable) oin.readObject();

                String data =  transferObject.getData();
                //processing incoming data

                extension = data.substring(data.lastIndexOf("."));


                (new HandleData(mContext, senderIP, transferObject)).process();

                oin.close();
                return;

            } catch (ClassNotFoundException cnfe) {
                Log.e("DDDD", cnfe.toString());
                cnfe.printStackTrace();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                if (oin != null) {
                    oin.close();
                }
            }

            ByteArrayInputStream bis = new ByteArrayInputStream(input);


//            String mimeType = MimeTypeUtils.detectMymeTypeCustom(bis);

// copy top 32 bytes and pass to the guessMimeType(byte[]) funciton
            byte[] topOfStream = new byte[32];
            System.arraycopy(input, 0, topOfStream, 0, topOfStream.length);
            String mimeGuess = guessMimeType(topOfStream);




// copy top 32 bytes and pass to the guessMimeType(byte[]) funciton
//            byte[] topOfStream = new byte[32];
//            System.arraycopy(input, 0, topOfStream, 0, topOfStream.length);
//            String mimeGuess = guessMimeType(topOfStream);
//
//            String ext = guessMimeType(topOfStream);

            final Activity currentActivity = ((AppController)mContext).getCurrentActivity();


//            currentActivity.runOnUiThread(new Runnable() {
//                public void run() {
//
//
//                    final CharSequence[] items = {"jpg", "pdf", "dox", "xls"};
//
//                    AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
//                    builder.setTitle("Choose file type");
//                    builder.setCancelable(false);
//                    builder.setItems(items, new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int item) {



                            //If control comes here that means the byte array sent is not the transfer object
                            // that was expected. Processing it as a file (JPEG)
                            final File f = new File(Environment.getExternalStorageDirectory() + "/"
                                    + "/RIT/" + System.currentTimeMillis() + extension);



                            File dirs = new File(f.getParent());
                            if (!dirs.exists()) {
                                boolean dirsSuccess = dirs.mkdirs();
                            }
                            try {
                                boolean fileCreationSuccess = f.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            try {
                                Utility.copyFile(new ByteArrayInputStream(input), new FileOutputStream(f),mContext);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }





                            //opening the received file. (if exists)
                            if (f.exists() && f.length() > 0) {
                                Intent intent = new Intent();
                                intent.setAction(Intent.ACTION_VIEW);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                extension = extension.substring(1);
                                String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);


                                intent.setDataAndType(Uri.parse("file://" + f.getAbsolutePath()), mime);
                                mContext.startActivity(intent);


                            }



//                        }
//                    });
//                    AlertDialog alert = builder.create();
//                    alert.show();







//                }
//            });






        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // url = file path or whatever suitable URL you want.
    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }
    public String getMimeType(Uri uri) {
        String mimeType = null;
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            ContentResolver cr = mContext.getContentResolver();
            mimeType = cr.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri
                    .toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    fileExtension.toLowerCase());
        }
        return mimeType;
    }

    private static String guessMimeType1(byte[] topOfStream) {

        String mimeType = null;
        Properties magicmimes = new Properties();
        FileInputStream in = null;

        // Read in the magicmimes.properties file (e.g. of file listed below)
        try {
            in = new FileInputStream( "magicmimes.properties" );
            magicmimes.load(in);
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // loop over each file signature, if a match is found, return mime type
        for ( Enumeration keys = magicmimes.keys(); keys.hasMoreElements(); ) {
            String key = (String) keys.nextElement();
            byte[] sample = new byte[key.length()];
            System.arraycopy(topOfStream, 0, sample, 0, sample.length);
            if( key.equals( new String(sample) )){
                mimeType = magicmimes.getProperty(key);
                System.out.println("Mime Found! "+ mimeType);
                break;
            } else {
                System.out.println("trying "+key+" == "+new String(sample));
            }
        }

        return mimeType;
    }


    void saveFile(){

    }

    public void tearDown() {
        acceptRequests = false;
    }

    private static String guessMimeType(byte[] topOfStream) {

        String mimeType = null;
        Properties magicmimes = new Properties();
        FileInputStream in = null;

        // Read in the magicmimes.properties file (e.g. of file listed below)
        try {
            in = new FileInputStream( "magicmimes.properties" );
            magicmimes.load(in);
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // loop over each file signature, if a match is found, return mime type
        for (Enumeration keys = magicmimes.keys(); keys.hasMoreElements(); ) {
            String key = (String) keys.nextElement();
            byte[] sample = new byte[key.length()];
            System.arraycopy(topOfStream, 0, sample, 0, sample.length);
            if( key.equals( new String(sample) )){
                mimeType = magicmimes.getProperty(key);
                System.out.println("Mime Found! "+ mimeType);
                break;
            } else {
                System.out.println("trying "+key+" == "+new String(sample));
            }
        }

        return mimeType;
    }

}


