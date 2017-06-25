package com.pritesh.ritintercom.activities;

import android.app.Activity;
import android.app.Application;

import com.pritesh.ritintercom.utils.ConnectionUtils;
import com.pritesh.ritintercom.datatransfer.ListenConnection;


public class AppController extends Application {

    private ListenConnection connListener;
    private int myPort;

    private boolean isConnectionListenerRunning = false;


    private Activity mCurrentActivity = null;
    public Activity getCurrentActivity(){
        return mCurrentActivity;
    }
    public void setCurrentActivity(Activity mCurrentActivity){
        this.mCurrentActivity = mCurrentActivity;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        myPort = ConnectionUtils.getPort(getApplicationContext());
        connListener = new ListenConnection(getApplicationContext(), myPort);
    }

    public void stopConnectionListener() {
        if (!isConnectionListenerRunning) {
            return;
        }
        if (connListener != null) {
            connListener.tearDown();
            connListener = null;
        }
        isConnectionListenerRunning = false;
    }

    public void startConnectionListener() {
        if (isConnectionListenerRunning) {
            return;
        }
        if (connListener == null) {
            connListener = new ListenConnection(getApplicationContext(), myPort);
        }
        if (!connListener.isAlive()) {
            connListener.interrupt();
            connListener.tearDown();
            connListener = null;
        }
        connListener = new ListenConnection(getApplicationContext(), myPort);
        connListener.start();
        isConnectionListenerRunning = true;
    }

    public void startConnectionListener(int port) {
        myPort = port;
        startConnectionListener();
    }

    public void restartConnectionListenerWith(int port) {
        stopConnectionListener();
        startConnectionListener(port);
    }

    public boolean isConnListenerRunning() {
        return isConnectionListenerRunning;
    }

    public int getPort(){
        return myPort;
    }
}
