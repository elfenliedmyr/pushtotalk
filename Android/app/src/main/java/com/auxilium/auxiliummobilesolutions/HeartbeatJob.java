package com.auxilium.auxiliummobilesolutions;

import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.webkit.CookieManager;

import android.webkit.CookieSyncManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.evernote.android.job.Job;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

class HeartbeatJob extends Job {

    static final String IMMEDIATE_JOB_TAG = "IMMEDIATE_HEARTBEAT_JOB";
    static final String JOB_TAG = "HEARTBEAT_JOB";

    @Nullable
    @Override
    protected Result onRunJob(@NonNull Params params) {
        Api api = (Api) Utils.readObjectFromFile(getContext(), "authUser");

        try {
            CookieSyncManager.createInstance(this.getContext());
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            cookieManager.removeAllCookie();
            SystemClock.sleep(1000);
            cookieManager.setCookie(api.getSpoke() + ".auxiliumgroup.com", "SESSION-" +
                api.getSpoke() + "=" + api.getToken() + "; path=/");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) CookieManager.getInstance().flush();
            else CookieSyncManager.getInstance().sync();
            FirebaseCrashlytics.getInstance().setCustomKey(JOB_TAG, "Heartbeat sent");
            api.request("{\"$/env/users/xupdate\":{\"rows\":[{\"lastreported\":{\"$/tools/date\":\"now\"},\"id\":" +
                api.getUserId() + "}]}}");
            return Result.SUCCESS;
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            return Result.FAILURE;
        }
    }
}
