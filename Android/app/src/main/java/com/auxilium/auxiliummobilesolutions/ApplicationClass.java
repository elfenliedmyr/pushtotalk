package com.auxilium.auxiliummobilesolutions;

import android.app.Application;

import com.onesignal.OneSignal;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

public class ApplicationClass extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // OneSignal Initialization
        OneSignal.startInit(this)
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .init();
    }
}
