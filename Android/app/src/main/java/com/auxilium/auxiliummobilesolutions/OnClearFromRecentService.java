package com.auxilium.auxiliummobilesolutions;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import java.util.concurrent.TimeUnit;


public class OnClearFromRecentService extends Service {
    private Disconnect serviceCallbacks;
    private final IBinder binder = new LocalBinder();

    // Class used for the client Binder.
    public class LocalBinder extends Binder {
        OnClearFromRecentService getService() {
            // Return this instance of MyService so clients can call public methods
            return OnClearFromRecentService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("ClearFromRecentService", "Service Started");
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        //Close Sockets to prevent them from hanging open
        serviceCallbacks.closeSocket();
        super.onDestroy();
        Log.d("ClearFromRecentService", "Service Destroyed");
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.e("ClearFromRecentService", "END");
        Log.e("ClearFromRecentService", "Disconnecting User from Voice Chat");
            serviceCallbacks.disconnectUser();
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        stopSelf();
    }
    public void setCallbacks(Disconnect callbacks) {
        serviceCallbacks = callbacks;
    }
}
