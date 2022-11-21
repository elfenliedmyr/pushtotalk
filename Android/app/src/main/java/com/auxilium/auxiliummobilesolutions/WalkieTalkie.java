package com.auxilium.auxiliummobilesolutions;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class WalkieTalkie extends AppCompatActivity {

    public static final int RequestPermissionCode = 1;

    private ImageButton btnTalk;
    private Button btnRoom1, btnRoom2;
    private TextView txtStatus;
    private ListView lstUsers;

    private AudioRecord recorder;
    private AudioManager am = null;
    private AudioTrack track = null;
    private MediaPlayer mMediaPlayer;
    private int sampleRate = 8000;
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    //private int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
    private int minBufSize = 3000;
    private boolean status = true;

    private Context mContext;
    private Socket mSocket;
    private String username, spoke, currentlyTalking;
    ArrayList<String> mainRoomUsersList = new ArrayList<>();
    ArrayList<String> privateUserList = new ArrayList<>();
    ArrayAdapter<String> mainRoomUserListAdapter;
    private JSONObject userList;

    private NotificationManager mNotificationManager;
    private Integer NOTIFICATION_ID = 4561;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walkie_talkie);

        mContext = this;
        mainRoomUserListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mainRoomUsersList);

        // Set up action bar aesthetics
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setLogo(R.mipmap.ic_icon_small);
            actionBar.setDisplayUseLogoEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setTitle(R.string.app_title);
        }

        // Setup audio volume control to OS
        setVolumeControlStream(AudioManager.MODE_IN_COMMUNICATION);

        // Create Audio Manager and output sound to Speakerphone
        am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        am.setSpeakerphoneOn(true);

        // Component reference
        btnTalk = (ImageButton) findViewById(R.id.btnTalk);
        btnTalk.setOnTouchListener(talkListener);
        btnRoom1 = (Button) findViewById(R.id.btnRoom1);
        btnRoom2 = (Button) findViewById(R.id.btnRoom2);
        txtStatus = (TextView) findViewById(R.id.txtStatus);
        lstUsers = (ListView) findViewById(R.id.lstUsers);

        // Public room buttons clicked
        btnRoom1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoRoom(1);
            }
        });
        btnRoom2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoRoom(2);
            }
        });

        // MainRoomUserListClicked
        lstUsers.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {

                Object user = lstUsers.getItemAtPosition(position);
                View row = arg0.getChildAt(position);

                if(privateUserList.contains(user)){
                    privateUserList.remove(user.toString());
                    row.setBackgroundColor(Color.TRANSPARENT);
                    row.setPadding(50,0,0,0);
                } else {
                    privateUserList.add(user.toString());
                    row.setBackgroundColor(Color.GRAY);
                    row.setPadding(100,0,0,0);
                }

            }
        });

        // Connect to server
        SharedPreferences preferences = getSharedPreferences("AMS", 0);
        username = preferences.getString("lastName","") + ", " + preferences.getString("firstName", "");
        spoke = preferences.getString("spoke", null);
        socketConnect();

        // Check permissions, request permissions if needed
        if(!checkPermission()) {
            requestPermission();
        }

        // Create a persistent notification
        makeNotification();

        // Create a receiver to listen for CLOSE_ACTIVITY
        IntentFilter filter = new IntentFilter("android.intent.CLOSE_ACTIVITY");
        registerReceiver(mReceiver, filter);
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            disconnect();
            finish();
        }

    };

    private void gotoRoom(int roomNumber){

        mSocket.emit("leaveRoom",spoke);
        mSocket.disconnect();

        Intent intent = new Intent(mContext, WalkieTalkieRoom.class);
        intent.putExtra("roomName","Public Room " + roomNumber);
        intent.putExtra("roomNumber",roomNumber);
        mContext.startActivity(intent);

        finish();
    }

    private void makeNotification() {
        Intent intent = new Intent(mContext, WalkieTalkie.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentClose = new Intent("android.intent.CLOSE_ACTIVITY");
        PendingIntent pIntentClose = PendingIntent.getBroadcast(mContext, 0 , intentClose, 0);

        NotificationCompat.Builder builder;

        builder = new NotificationCompat.Builder(mContext)
            .setContentTitle("Auxilium Lynk")
            .setContentText("Auxilium Lynk push to talk service is still running.")
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_walkie));

        if (Build.VERSION.SDK_INT >= 20) {
            NotificationCompat.Action actionClose = new NotificationCompat.Action
                .Builder(R.mipmap.ic_launcher, "Close Lynk", pIntentClose)
                .build();
            builder.addAction(actionClose);

        } else {
            builder.addAction(R.drawable.ic_clear_black_24dp, "Close Lynk", pIntentClose);
        }

        Notification n;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            n = builder.build();
        } else {
            n = builder.getNotification();
        }

        n.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_ID, n);
    }

    // Disconnect from server and remove persistent notification
    public void disconnect() {
        if(mSocket!=null){
            mSocket.disconnect();
        }
        mNotificationManager.cancel(NOTIFICATION_ID);
        try {
            unregisterReceiver(mReceiver);
        } catch (IllegalArgumentException e) {

        }
    }

    @Override
    public void onDestroy(){
        disconnect();
        super.onDestroy();
    }

    // Stop BEEP
    private void stopBeep(){
        if(mMediaPlayer != null){
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    // Play BEEP
    private void playBeep(){
        mMediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.beep);
        if(mMediaPlayer!=null) {
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopBeep();
                }
            });

            mMediaPlayer.start();
        }
    }

    // Button touch listener
    // Action Down: start stream
    // Action Up: stop recording, stop stream.
    private final View.OnTouchListener talkListener = new View.OnTouchListener(){

        @Override
        public boolean onTouch(View v, MotionEvent event) {
        switch(event.getAction()){
            case MotionEvent.ACTION_UP:
                status = false;
                if(recorder != null) {
                    recorder.stop();
                    recorder.release();
                    currentlyTalking = null;
                    btnTalk.setBackgroundResource(R.drawable.round_button);
                    mSocket.emit("endServerStream");
                    //minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
                    minBufSize = 3000;
                }
                btnTalk.setImageResource(R.drawable.ic_mic_none_white_48dp);
                break;

            case MotionEvent.ACTION_DOWN:
                status = true;
                track = null;
                sendStream();
                btnTalk.setImageResource(R.drawable.ic_mic_white_48dp);
                break;
        }
        return true;
        }

    };

    // Stream to server
    public void sendStream() {

        Thread streamThread = new Thread(new Runnable() {

            @Override
            public void run() {
            byte[] buffer = new byte[minBufSize];

            // Start recording microphone
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,sampleRate,channelConfig,audioFormat,minBufSize);
            recorder.startRecording();



            while(status == true) {

                // Read data from microphone into buffer
                minBufSize = recorder.read(buffer, 0, buffer.length);

                // Put some stuff in to Jason
                // lots of stuff
                JSONObject data = new JSONObject();
                try {
                    data.put("buffer", buffer);
                    data.put("user", username);
                    data.put("spoke", spoke);

                    // Yet to implement selectable push to talk 1-1, or 1-many.
//                    if(privateUserList.size()>0){
//                        String[] socketIds = new String[privateUserList.size()];
//                        for(int i=0;i<privateUserList.size();i++){
//                            socketIds[i] = "x";
//                        }
//                        data.put("to",socketIds);
//                        Log.d("PTT",socketIds.toString());
//                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }


                // Emit object to server
                mSocket.emit("streamingToServer", data);

            }
            }

        });
        streamThread.start();
    }

    // Connect to server
    public void socketConnect(){

        try {
            if(mSocket==null) {
                mSocket = IO.socket("https://auxiliumgroup.com:8999/");
                //mSocket = IO.socket("http://198.27.64.223:9001/");
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {

            JSONObject data = new JSONObject();
            try {
                data.put("username",username);
                data.put("spoke",spoke);
                mSocket.emit("join", data);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            }
        });
        mSocket.on("streamingToClient", streaming);
        mSocket.on("serverReady", ready);
        mSocket.on("endClientStream", endStream);
        mSocket.on("userList", userListUpdated);
        mSocket.connect();

    }

    // ****************************************************
    // SOCKET.IO LISTENERS
    // ****************************************************

    // Receive updated user list from server
    private Emitter.Listener userListUpdated = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            if(args[0] != null){
                userList = (JSONObject) args[0];
                Iterator<String> keysIterator = userList.keys();
                mainRoomUsersList.clear();
                lstUsers.setAdapter(mainRoomUserListAdapter);
                while (keysIterator.hasNext()) {
                    String usernameIterator = keysIterator.next();
                    if(!usernameIterator.equals(username)){
                        mainRoomUsersList.add(usernameIterator);
                    }
                }
                mainRoomUserListAdapter.notifyDataSetChanged();
            }
            }
        });
        }
    };

    // Server ready listener
    // When a user has connected and about to start streaming
    private Emitter.Listener ready = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            if(args[0] != null && args[0].toString().equals(username)){
                txtStatus.setText("You are currently talking");
                btnTalk.setBackgroundResource(R.drawable.round_button_active);
            }
            playBeep();
            }
        });
        }
    };

    // End stream listener
    // When a user has stopped streaming this is called.
    private Emitter.Listener endStream = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                currentlyTalking = null;
                txtStatus.setText("");
                btnTalk.setBackgroundResource(R.drawable.round_button);
                if(track!=null) {
                    track.release();
                    track = null;
                }
            }
        });
        }
    };

    // Stream listener
    // When a user is streaming this is called
    private Emitter.Listener streaming = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

                    JSONObject data = (JSONObject) args[0];
                    byte[] stringData = (byte[])args[1];
                    String streamUsername;
                    try {
                        streamUsername = data.getString("username");

                        if(currentlyTalking == null){
                            currentlyTalking = streamUsername;
                            txtStatus.setText(currentlyTalking + " is talking");

                            // AUDIO CODE
                            int intSize = android.media.AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
                            track = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, intSize, AudioTrack.MODE_STREAM);
                            if (track != null) {
                                track.play();
                            }
                        }

                        if(track!=null) {
                            track.write(stringData, 0, stringData.length);
                        }

                    } catch (JSONException e) {
                        return;
                    }
                }
            });
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Create menu in action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.walkietalkie, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle menu item selected
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(this, WalkieTalkieSettings.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void requestPermission() {
        ActivityCompat.requestPermissions(WalkieTalkie.this, new
            String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO, INTERNET}, RequestPermissionCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case RequestPermissionCode:
                if (grantResults.length> 0) {
                    boolean StoragePermission = grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED;
                    boolean RecordPermission = grantResults[1] ==
                        PackageManager.PERMISSION_GRANTED;
                    boolean InternetPermission = grantResults[2] ==
                        PackageManager.PERMISSION_GRANTED;

                    if (StoragePermission && RecordPermission && InternetPermission) {
                        Toast.makeText(WalkieTalkie.this, "Permission Granted", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(WalkieTalkie.this,"Permission Denied", Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }

    public boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(),
            WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(),
            RECORD_AUDIO);
        int result2 = ContextCompat.checkSelfPermission(getApplicationContext(),
            INTERNET);
        return result == PackageManager.PERMISSION_GRANTED &&
            result1 == PackageManager.PERMISSION_GRANTED &&
            result2 == PackageManager.PERMISSION_GRANTED;

    }
}
