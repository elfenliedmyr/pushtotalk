package com.auxilium.auxiliummobilesolutions;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.auxilium.auxiliummobilesolutions.barcode.BarcodeScanner;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.io.Serializable;

class Helpers {

    private static final String TAG = "Helpers";

    static void createNotification(Context context, Intent intent, String title, String body) {
        FirebaseCrashlytics.getInstance().setCustomKey(TAG, "Notification created");

        NotificationCompat.Builder notification = new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setColor(context.getResources().getColor(R.color.colorAccent))
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(0, notification.build());
    }

    static void openWalkieTalkie(Context context) {
        FirebaseCrashlytics.getInstance().setCustomKey(TAG, "Walkie Talkie app opened");

        Intent intent = new Intent(context.getApplicationContext(), WalkieTalkie.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        context.startActivity(intent);
    }

    static void openCamera(Context context, int recordId) {
        FirebaseCrashlytics.getInstance().setCustomKey(TAG, "Camera app opened");
        Intent intent = new Intent(context.getApplicationContext(), CameraActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra("recordId", recordId);
        context.startActivity(intent);
    }

    static void updateToken(Context context, String newToken) {
        SharedPreferences sharedPreferences;
        sharedPreferences = context.getApplicationContext()
                .getSharedPreferences("AMS", 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("token", newToken);
        editor.apply();
    }

    static void openCameraFull(Context context, int camera, int resize, boolean repeat, boolean auto, int timer) {
        FirebaseCrashlytics.getInstance().setCustomKey(TAG, "Full Camera app opened");
        Intent intent = new Intent(context.getApplicationContext(), CameraActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra("camera", camera);
        intent.putExtra("resize", resize);
        intent.putExtra("repeat", repeat);
        intent.putExtra("auto", auto);
        intent.putExtra("timer", timer);
        context.startActivity(intent);
    }

    static void openBarcodeScanner(Context context, String scanType, Serializable api) {
        FirebaseCrashlytics.getInstance().setCustomKey(TAG, "Barcode scanner opened");
        Intent intent;

        if (scanType.equals("scancode")) {
            intent = new Intent(context.getApplicationContext(), BarcodeScanner.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            context.startActivity(intent);
        } else if (scanType.equals("transfercode")) {
            intent = new Intent(context.getApplicationContext(), TransferScanner.class);
            intent.putExtra("api", api);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            context.startActivity(intent);
        } else if (scanType.equals("scan-close")) {
            intent = new Intent(context.getApplicationContext(), BarcodeScanner.class);
            intent.putExtra("forceclose", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            context.startActivity(intent);
        }

    }


    static void createToast(Context context, String message) {
        FirebaseCrashlytics.getInstance().setCustomKey(TAG, "Toast created");

        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    static void setVolume(Context context, float volume) {
        FirebaseCrashlytics.getInstance().setCustomKey(TAG, "Volume adjusted to: " + volume);

        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int v = Math.round(am.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * volume);
        am.setStreamVolume(AudioManager.STREAM_MUSIC, v, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    }

    static void soundAlarm(Context context) {
        FirebaseCrashlytics.getInstance().setCustomKey(TAG, "Alarm sounded");

        if (!context.getSharedPreferences("AMS", 0).getBoolean("alarmEnabled", true)) return;
        MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.alarm);
        mediaPlayer.start();
    }
}
