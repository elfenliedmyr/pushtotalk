package com.auxilium.auxiliummobilesolutions;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;
import android.webkit.JavascriptInterface;


import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.io.Serializable;

@SuppressWarnings("unused")
class WebInterface {

    private static final String TAG = "WebInterface";
    private Context context;
    private Serializable api;

    WebInterface(Context context) {
        this.api = (Api) Utils.readObjectFromFile(context, "authUser");
        this.context = context;
    }

    @JavascriptInterface
    public void openWalkieTalkie() {
        FirebaseCrashlytics.getInstance().setCustomKey(TAG, "Opening Walkie Talkie app via web interface");
        Helpers.openWalkieTalkie(context);
    }

    @JavascriptInterface
    public void openFrontCamera(int recordId) {
        FirebaseCrashlytics.getInstance().setCustomKey(TAG, "Opening Front Facing Camera");
        Helpers.openCamera(context, recordId);
    }

    @JavascriptInterface
    public void updateToken(String newToken) {
        FirebaseCrashlytics.getInstance().setCustomKey(TAG, "Token has been updated");
        Helpers.updateToken(context, newToken);
    }


    @JavascriptInterface
    public void openCamera(int camera, int resize, boolean repeat, boolean auto, int timer) {
        FirebaseCrashlytics.getInstance().setCustomKey(TAG, "Opening Camera");
        Helpers.openCameraFull(context, camera, resize, repeat, auto, timer);
    }

    @JavascriptInterface
    public void openBarcode(String scanType) {
        FirebaseCrashlytics.getInstance().setCustomKey(TAG, "Opening Barcode Scanner via web interface");
        Helpers.openBarcodeScanner(context, scanType, api);
    }

    @JavascriptInterface
    public void createToast(String message) {
        FirebaseCrashlytics.getInstance().setCustomKey(TAG, "Creating toast via web interface");
        Helpers.createToast(context, message);
    }

    @JavascriptInterface
    public void createNotification(String title, String body, String url, boolean alarm, int gps) {
        FirebaseCrashlytics.getInstance().setCustomKey(TAG, "Creating notification via web interface");
        Intent intent = new Intent(context, LoginActivity.class);
        intent.putExtra("url", url);
        intent.putExtra("alarm", alarm);
        intent.putExtra("gps", gps);
        Helpers.createNotification(context, intent, title, body);
    }

    @JavascriptInterface
    public void startTracking(int minutes) {
        FirebaseCrashlytics.getInstance().setCustomKey(TAG, "Starting tracker via web interface");
        Activity activity = (Activity) context;
        ((MainActivity)activity).startTracking(minutes, true);
    }

    @JavascriptInterface
    public String currentLocation() {
        FirebaseCrashlytics.getInstance().setCustomKey(TAG, "Requesting current location from Android Device");
        Activity activity = (Activity) context;
        Location tmp = ((MainActivity)activity).currentLocation();
        return "{\"lat\": " + tmp.getLatitude() + ", \"lng\": " + tmp.getLongitude() + "}";
    }

    @JavascriptInterface
    public void stopTracking() {
        FirebaseCrashlytics.getInstance().setCustomKey(TAG, "Stopped tracking via web interface");
        Activity activity = (Activity) context;
        ((MainActivity)activity).stopTracking();
    }

    @JavascriptInterface
    public boolean isTracking() {
        FirebaseCrashlytics.getInstance().setCustomKey(TAG, "Web intreface checking if tracking");
        Activity activity = (Activity) context;
        return ((MainActivity)activity).isTracking();
    }

    @JavascriptInterface
    public void setVolume(float volume) {
        FirebaseCrashlytics.getInstance().setCustomKey(TAG, "Changing volume via web interface");
        Helpers.setVolume(context, volume);
    }

    @JavascriptInterface
    public void soundAlarm() {
        FirebaseCrashlytics.getInstance().setCustomKey(TAG, "Sounding alarm via web interface");
        Helpers.soundAlarm(context);
    }

    @JavascriptInterface
    public String userDetails() {
        SharedPreferences preferences = context.getSharedPreferences("AMS", 0);
        return "{\"username\": \"" + preferences.getString("username", "Settings") + "\", \"password\": \"" + preferences.getString("password", "Settings") + "\"}";
    }
}
