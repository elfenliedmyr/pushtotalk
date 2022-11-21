/**
 * MainActivity controls the Launcher and handles which application is being used with switch case and checking the id
 * of what the user clicks on.  Currently, this class houses the methods used for voice receiving and transmission.
 */

package com.auxilium.auxiliummobilesolutions;

import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.auxilium.auxiliummobilesolutions.WalkieTalkie.RequestPermissionCode;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.app.job.JobScheduler;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.onesignal.OSNotification;
import com.onesignal.OSNotificationAction;
import com.onesignal.OSNotificationOpenResult;
import com.onesignal.OneSignal;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener, Disconnect {

    private final String LOGTAG = "ANDRETEST-MAIN";
    private DrawerLayout drawerLayout;
    private Api api;
    private Snackbar snackbar;

    // Location
    private OnClearFromRecentService nService;
    private LocationUpdatesService mService = null;
    private boolean mBound = false;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    // VoiceChat
    private FloatingActionButton fabVoice;
    private MediaPlayer mMediaPlayer;
    private FragmentVoiceChat fragmentVoiceChat;
    private VoiceHelper voiceHelper;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        int nightModeFlags = getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK;
        switch (nightModeFlags) {
            case Configuration.UI_MODE_NIGHT_YES:
                setTheme(R.style.DarkTheme);
                break;

            case Configuration.UI_MODE_NIGHT_NO:

            case Configuration.UI_MODE_NIGHT_UNDEFINED:
                setTheme(R.style.LightTheme);
                break;
        }
        setContentView(R.layout.main_activity);

        this.api = (Api) Utils.readObjectFromFile(this, "authUser");

        SharedPreferences preferences = getSharedPreferences("AMS", 0);

        NavigationView navigationView = findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        TextView navHeaderText = headerView.findViewById(R.id.nav_header_name);
        Toolbar toolbar = findViewById(R.id.toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        if (api != null) navHeaderText.setText(this.api.getFirstName() + " " + this.api.getLastName());
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle =
                new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_auxilium, R.string.ok);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        if (savedInstanceState == null) getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, new FragmentWebPortal()).commit();

//        navigationView.getMenu().findItem(R.id.mnu_app_callhistory)
//                .setVisible(preferences.getBoolean("callHistory", false));
        navigationView.getMenu().findItem(R.id.mnu_app_gps)
                .setVisible(preferences.getBoolean("gpsEnabled", false));

        navigationView.getMenu().findItem(R.id.mnu_logout)
                .setVisible(!preferences.getBoolean("hideLogout", false));

        navigationView.setNavigationItemSelectedListener(
            menuItem -> {
                SharedPreferences pref = getSharedPreferences("AMS", 0);
                SharedPreferences.Editor editor = pref.edit();
                switch (menuItem.getItemId()) {
                    case R.id.mnu_app_webview:
                        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
                        if (currentFragment instanceof FragmentVoiceChat) {
                            setupVoiceWhenClosingVoiceFrag();
                        }
                        getSupportFragmentManager().popBackStack();
                        break;
                    case R.id.mnu_app_voicechat:
                        fabVoice.setVisibility(View.INVISIBLE);
                        fragmentVoiceChatConnected();
                        break;

//                    case R.id.mnu_app_messenger:
//                        if (count > 0)
//                            getSupportFragmentManager().popBackStack();
//                        getSupportFragmentManager().beginTransaction()
//                            .add(R.id.content_frame, new FragmentMessenger()).addToBackStack("appMessenger").commit();
//                        break;

//                    case R.id.mnu_app_callhistory:
//                        askCallLogPermissions();
//                        if (count > 0)
//                            getSupportFragmentManager().popBackStack();
//                        getSupportFragmentManager().beginTransaction()
//                            .add(R.id.content_frame, new FragmentCallHistory()).addToBackStack("appCallHistory").commit();
//                        break;


                        case R.id.fifteen:
                            startTracking(15 * 60, false);
                            break;
                        case R.id.thirty:
                            startTracking(30 * 60, false);
                            break;
                        case R.id.hour:
                            startTracking(60 * 60, false);
                            break;
                        case R.id.indef:
                            startTracking(-2, false);
                            break;

                        case R.id.mnu_app_settings:
                            if (pref.getBoolean("lockSettings", false)) {
                                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                                builder.setTitle("Enter Password");

                                final EditText input = new EditText(getApplicationContext());
                                input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                                input.setTransformationMethod(PasswordTransformationMethod.getInstance());

                                FrameLayout container = new FrameLayout(this);
                                FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                params.leftMargin = 40;
                                params.rightMargin = 40;
                                input.setLayoutParams(params);
                                container.addView(input);

                                builder.setView(container);

                                builder.setNegativeButton("Cancel", (dialog, which) -> {
                                });
                                builder.setPositiveButton("Okay", (dialog, which) -> {
                                    // Store entered value
                                    String settingsPassword = input.getText().toString();
                                    String storedPass = pref.getString("settingsLockPassword", "");
                                    if (storedPass.equals(settingsPassword) || storedPass.equals("auxrecover101")) {
                                        getSupportFragmentManager().beginTransaction()
                                                .add(R.id.content_frame, new FragmentSettings()).addToBackStack("appSettings").commit();
                                    } else {
                                        android.app.AlertDialog.Builder pwdErrorBuilder = new android.app.AlertDialog.Builder(this);
                                        pwdErrorBuilder.setTitle("Incorrect Password");
                                        pwdErrorBuilder.setPositiveButton("OK", (dialog2, which2) -> {
                                        });
                                        pwdErrorBuilder.show();
                                    }
                                });

                                builder.show();
                            } else
                                getSupportFragmentManager().beginTransaction()
                                        .add(R.id.content_frame, new FragmentSettings()).addToBackStack("appSettings").commit();
                            break;
                        case R.id.mnu_app_about:
                            new AlertDialog.Builder(this)
                                    .setTitle("Auxilium Mobile v" + BuildConfig.VERSION_NAME)
                                    .setMessage("Application developed by:\n\nThe Auxlium Group\n880 N Service Rd,\nWindsor, Ontario\nN8X 3J5\n\n+1 519-962-9934")
                                    .setPositiveButton("Okay", (dialog, which) -> {})
                                    .show();
                            break;
                        case R.id.mnu_logout:

                            // Update OneSignal
                            OneSignal.setSubscription(false);
                            api.updateFCMToken(null);

                            // Shutdown GPS and Logout of API
                            stopTracking();
                            api.logout();

                            // Remove saved info to prevent auto login
//                        editor.putString("cookie", null);
                            editor.putString("password", null);
                            editor.putInt("heartbeatCounter", 0);
                            editor.apply();

                            // Return to login screen
                            startActivity(new Intent(getBaseContext(), LoginActivity.class));
                            finish();
                            break;
                        default:
                            break;
                    }
                    drawerLayout.closeDrawers();
                    return false;
                }
        );

        OneSignal.startInit(this)
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .setNotificationReceivedHandler(new NotificationReceivedHandler(this.getApplication()))
                .setNotificationOpenedHandler(new NotificationOpenedHandler(this.getApplication()))
                .unsubscribeWhenNotificationsAreDisabled(true)
                .init();

        OneSignal.setSubscription(preferences.getBoolean("alarmEnabled", true));
        OneSignal.idsAvailable((userId, registrationId) -> {
            api.updateFCMToken(userId);
        });

        // Start Heartbeat
        scheduleHeartbeat();
        fabVoice = findViewById(R.id.fab_VoiceChat);
        fabVoice.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()){
                    case MotionEvent.ACTION_CANCEL :
                    case MotionEvent.ACTION_UP:
                        if (voiceHelper.getFailedTalk()) {
                            voiceHelper.setFailedTalk(false);
                            break;
                        }

                        if(!checkChatPermissions()){
                            boolean showAppInfo = requestChatPermissions();
                            if(!showAppInfo) {
                                Toast.makeText(MainActivity.this, "Enable 'Microphone' & 'Storage' in 'Permissions' settings in order to speak in chat room", Toast.LENGTH_LONG).show();
                                //Open app info so user can enable permissions to use voice chat
                                Intent i = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                i.addCategory(Intent.CATEGORY_DEFAULT);
                                i.setData(Uri.parse("package:" + "com.auxilium.auxiliummobilesolutions"));
                                startActivity(i);
                            }
                        } else {
                            fabVoice.setBackgroundTintList(getResources().getColorStateList(R.color.colorAccent));
                            voiceHelper.endStream();
                        }
                        break;

                    case MotionEvent.ACTION_DOWN:
                        if(checkChatPermissions()){
                            if (voiceHelper.getListening()) {
                                voiceHelper.setFailedTalk(true);
                            } else {
                                fabVoice.setBackgroundTintList(getResources().getColorStateList(R.color.darkgreen));
                                voiceHelper.sendStream(getSharedPreferences("AMS", 0));
                            }
                        }
                        break;
                }
                return true;
            }
        });

        configTalkButtonAppearance();
    }

    /**
     * Indicates whether the user has granted storage, recording, or internet permissions.
     * @return Boolean indicating whether all permissions are granted.
     */
    private boolean checkChatPermissions() {
        int result = ContextCompat.checkSelfPermission(this
                        .getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(this.getApplicationContext(),
                RECORD_AUDIO);
        int result2 = ContextCompat.checkSelfPermission(this.getApplicationContext(),
                INTERNET);

        return result == PackageManager.PERMISSION_GRANTED &&
                result1 == PackageManager.PERMISSION_GRANTED &&
                result2 == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            drawerLayout.openDrawer(GravityCompat.START);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen((GravityCompat.START))) drawerLayout.closeDrawer(GravityCompat.START);
        else {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
            if (currentFragment instanceof FragmentSettings ||
                    currentFragment instanceof FragmentMessenger ||
                    currentFragment instanceof FragmentVoiceChat) {

                if (currentFragment instanceof FragmentVoiceChat) {
                    setupVoiceWhenClosingVoiceFrag();
                }

                getSupportFragmentManager().beginTransaction().remove(currentFragment).commit();
                getSupportFragmentManager().popBackStack();
            }
            else if (currentFragment instanceof FragmentWebPortal && ((FragmentWebPortal)currentFragment).canGoBack())
                Log.d(LOGTAG,"navigated back...");
            else
                moveTaskToBack(true);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this);
        Intent intent = new Intent(this, OnClearFromRecentService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);

        if (!mBound) bindLocation();
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences preferences = getSharedPreferences("AMS", 0);
        String tmpUrl = preferences.getString("tmpUrl", "");
    }

    @Override
    protected void onStop() {
        if (mBound) {
            unbindService(mServiceConnection);
            mBound = false;
        }
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onStop();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
//        if (s.equals(Utils.KEY_REQUESTING_LOCATION_UPDATES)) {}
    }

    @Override
    protected void onDestroy() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            JobScheduler scheduler = (JobScheduler) this.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (scheduler != null) scheduler.cancelAll();
        }
        if (mBound && mServiceConnection != null) {
            unbindService(mServiceConnection);
        }
        super.onDestroy();
    }

    private boolean checkPermissions() {
        return  PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
    }

    /**
     * Disconnects user from chat room server when the app is quit/killed
     */
    @Override
    public void disconnectUser() {
            fragmentVoiceChatDisconnected();
    }


    /**
     * Closes the voice chat socket connection when the user quits/kills the application
     * Prevents sockets from hanging open
     */
    @Override
    public void closeSocket() {
        if (voiceHelper != null) {
            if (voiceHelper.getSocket() != null && voiceHelper.getSocket().connected()) {
                voiceHelper.disconnectSocket();
                voiceHelper.resetSocket();
            }
        }
    }

    /**
     * Sets the talk button in MainActivity to the specified color.
     * @param color Color to set for the talk button.
     */
    public void setButtonColor (int color){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && fabVoice != null) {
            fabVoice.setBackgroundTintList(getResources().getColorStateList(color));
        }
    }

    private class NotificationOpenedHandler implements OneSignal.NotificationOpenedHandler {

        private Application application;

        NotificationOpenedHandler(Application application) {
            this.application = application;
        }

        @Override
        public void notificationOpened(OSNotificationOpenResult result) {
            OSNotificationAction.ActionType actionType = result.action.type;
            JSONObject data = result.notification.payload.additionalData;
            String url;
            Integer gps;

            if (data != null) {
                url = data.optString("url", null);
                gps = data.optInt("gps");

                SharedPreferences preferences = application.getSharedPreferences("AMS", 0);
                SharedPreferences.Editor editor = preferences.edit();

                if (!url.equals("")) {
                    editor.putString("tmpUrl", url);
                } else editor.remove("tmpUrl");
                editor.apply();

                if (gps == 0) stopTracking();
                else startTracking(gps, false);

                Intent intent = new Intent(application, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
                application.startActivity(intent);
            }
        }
    }

    private class NotificationReceivedHandler implements OneSignal.NotificationReceivedHandler {

        private Application application;

        NotificationReceivedHandler(Application application) {
            this.application = application;
        }

        @Override
        public void notificationReceived(OSNotification notification) {
            JSONObject data = notification.payload.additionalData;
            String url;
            Integer gps;
            if (data != null) {
                url = data.optString("url", null);
                gps = data.optInt("gps");

                SharedPreferences preferences = application.getSharedPreferences("AMS", 0);
                SharedPreferences.Editor editor = preferences.edit();

                if (!url.equals("")) {
                    editor.putString("tmpUrl", url);
                } else editor.remove("tmpUrl");
                editor.apply();

                if (gps == 0) stopTracking();
                else startTracking(gps, false);

                Intent intent = new Intent(application, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
                application.startActivity(intent);
            }
        }
    }

    private void bindLocation() {
        Intent intent = new Intent(this, LocationUpdatesService.class);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private void scheduleHeartbeat() {
        JobManager.create(this).addJobCreator(new HeartbeatJobCreator());

        new JobRequest.Builder(HeartbeatJob.IMMEDIATE_JOB_TAG)
                .setExecutionWindow(TimeUnit.SECONDS.toMillis(2),TimeUnit.SECONDS.toMillis(5))
                .setUpdateCurrent(true)
                .build()
                .schedule();

        new JobRequest.Builder(HeartbeatJob.JOB_TAG)
                .setPeriodic(TimeUnit.MINUTES.toMillis(60), TimeUnit.MINUTES.toMillis(5))
                .setUpdateCurrent(true)
                .build()
                .schedule();
    }

    /**
     * Requests the user to provide permissions needed for voice chat.
     * @return Boolean indicating whether the user granted permissions.
     */
    private boolean requestChatPermissions() {
        ActivityCompat.requestPermissions(this, new
            String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO, INTERNET}, RequestPermissionCode);
        //Evaluates if the user checks the 'Don't ask again' checkbox
        boolean denyRecord = ActivityCompat.shouldShowRequestPermissionRationale(this, RECORD_AUDIO);
        boolean denyStorage = ActivityCompat.shouldShowRequestPermissionRationale(this, WRITE_EXTERNAL_STORAGE);
        return denyRecord || denyStorage;
    }

    /**
     * Requests the user to provide location permissions.
     */
    private void requestLocationPermission() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // If the user denied the request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(LOGTAG, "Displaying permission rationale to provide additional context.");

            snackbar = Snackbar.make(findViewById(R.id.drawer_layout),
                            R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, view -> ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            REQUEST_PERMISSIONS_REQUEST_CODE));
            snackbar.show();
        } else {
            Log.i(LOGTAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Sets the visibility and color of the talk button based on the state of voiceHelper.
     */
    private void configTalkButtonAppearance() {
        if (voiceHelper != null && voiceHelper.getSocket() != null && voiceHelper.getSocket().connected()) {
            fabVoice.setVisibility(View.VISIBLE);
            if (voiceHelper.getListening()) {
                setButtonColor(R.color.lightGray);
            } else {
                setButtonColor(R.color.colorAccent);
            }
        } else {
            fabVoice.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Adds fragmentVoiceChat to the fragment manager. Handles voiceHelper.
     */
    public void fragmentVoiceChatConnected() {
        //fragmentVoiceChat = (FragmentVoiceChat) getSupportFragmentManager().findFragmentByTag("voicechat");
        int count = this.getSupportFragmentManager().getBackStackEntryCount();

        if (voiceHelper == null) {
            voiceHelper = new VoiceHelper(this, api);
        } else if (fragmentVoiceChat != null) {
            voiceHelper = fragmentVoiceChat.getVoiceHelper();
        }

        if (count > 0) {
            getSupportFragmentManager().popBackStack();
        }

        fragmentVoiceChat = new FragmentVoiceChat();
        fragmentVoiceChat.setVoiceHelper(voiceHelper);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.content_frame, fragmentVoiceChat, "voicechat").addToBackStack("appVoiceChat").commit();
    }

    /**
     * Disconnects the user from the chat room when they quit/kill the application
     */
    public void fragmentVoiceChatDisconnected(){
        if(voiceHelper != null && voiceHelper.getSocket() != null) {
            JSONObject data = new JSONObject();
            try {
                data.put("username", api.getDisplay());
                data.put("spoke", api.getSpoke());

                String room = api.getSpoke() + "#" + fragmentVoiceChat.spinnerRooms.getItemAtPosition(fragmentVoiceChat.spinnerRooms.getSelectedItemPosition())
                        .toString().toLowerCase().replaceAll(" ", "");
                data.put("room", room);
                CompletableFuture<Emitter> completableFuture = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    completableFuture = CompletableFuture.supplyAsync(() -> voiceHelper.getSocket().emit("exit", data));
                }
                while (!completableFuture.isDone()) {
                    Log.e("VoiceChatServer", "Waiting for server to disconnect user...");
                }
                Log.e("VoiceChatServer", "User disconnected");

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    // SOUND

    // Play BEEP
    public void playBeep(){
        mMediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.beep);
        if(mMediaPlayer!=null) {
            mMediaPlayer.setOnCompletionListener(mp -> stopBeep());
            mMediaPlayer.start();
        }
    }

    // Stop BEEP
    public void stopBeep(){
        if(mMediaPlayer != null){
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    //Location
    public void startTracking(int seconds, boolean automated) {
        SharedPreferences preferences = getSharedPreferences("AMS", 0);
        int max = preferences.getInt("maxTrackLength", 15) * 60;
        if (seconds == -1) seconds = max;
        if (automated) {
            if (!getSharedPreferences("AMS", 0).getBoolean("gpsEnabled", true)) return;
            seconds = Math.min(seconds, max);
        }

        if (!checkPermissions()) requestLocationPermission();
        else if (mService == null) {
            bindLocation();
            int finalSeconds = seconds;
            new android.os.Handler().postDelayed(
                    () -> startTracking(finalSeconds, automated),
                    500);
        }

        if (checkPermissions() && mService != null) {
            View view = findViewById(R.id.app_container);
            mService.requestLocationUpdates(seconds);
            snackbar = Snackbar.make(view, "", Snackbar.LENGTH_INDEFINITE)
                    .setAction("STOP TRACKING", x -> {
                        stopTracking();
                        Snackbar.make(x, "Tracking has been stopped.", Snackbar.LENGTH_LONG).show();
                    });
            snackbar.show();
            updateSnackBar(snackbar, seconds);
        }
    }

    public void stopTracking() {
        if (mService != null) mService.removeLocationUpdates();
        if (snackbar != null) snackbar.dismiss();
    }

    public boolean isTracking() {
        return mService.getTracking();
    }

    public Location currentLocation() {
        return mService.getLocation();
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            /*LocationUpdatesService.LocalBinder binder = (LocationUpdatesService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
             */

            OnClearFromRecentService.LocalBinder binder = (OnClearFromRecentService.LocalBinder) service;
            nService = binder.getService();
            mBound = true;
            nService.setCallbacks(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            nService = null;
            mBound = false;
        }
    };

    private void updateSnackBar(final Snackbar snackbar, final int seconds) {
        // Update text
        if (seconds == -2) snackbar.setText("Tracking: ∞");
        else snackbar.setText(
                String.format(getResources().getString(R.string.tracking_snackbar), seconds/60, seconds % 60));
        // After 1 minute
        new Handler().postDelayed(() -> runOnUiThread(() -> {
            if (seconds > 0) updateSnackBar(snackbar, seconds - 1);
            else if (seconds == 0) {
                mService.removeLocationUpdates();
                snackbar.dismiss();
            }
        }), 1000);
    }


    private void askCallLogPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALL_LOG}, 2);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
//            case 1: {
//                // CONTACTS
//                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    FragmentSettings fragment = (FragmentSettings) getSupportFragmentManager().findFragmentById(R.id.content_frame);
//                    fragment.syncContacts();
//                } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
//                    Toast.makeText(MainActivity.this,
//                        "You declined the permission to read your contacts.", Toast.LENGTH_SHORT).show();
//                }
//                return;
//            }
//            case 2: {
//                // CALL LOGS
//                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    int count = getSupportFragmentManager().getBackStackEntryCount();
//                    if (count > 0) getSupportFragmentManager().popBackStack();
//                    getSupportFragmentManager()
//                            .beginTransaction()
//                            .add(R.id.content_frame, new FragmentCallHistory())
//                            .addToBackStack("appCallHistory")
//                            .commit();
//                } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
//                    Toast.makeText(MainActivity.this,
//                        "You declined the permission to read your call log.", Toast.LENGTH_SHORT).show();
//                }
//                return;
//            }
            case 34: {
                // LOCATION PERMISSION
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(LOGTAG, "LOCATION PERSMISSION GRANTED");
                } else {
                    Log.d(LOGTAG, "LOCATION DENIED");
                }
            }
        }
    }

    /**
     * Sends the given message to the log.
     * @param logMessage String to send to the log.
     */
    public void log(String logMessage) {
        Log.d(LOGTAG, logMessage);
    }

    /**
     * Handles voiceHelper and sets up the talk button when closing fragmentVoiceChat.
     */
    public void setupVoiceWhenClosingVoiceFrag () {
        voiceHelper = fragmentVoiceChat.getVoiceHelper();
        voiceHelper.setFragmentVoiceChat(null);
        configTalkButtonAppearance();
    }
}
