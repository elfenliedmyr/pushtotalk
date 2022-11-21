package com.auxilium.auxiliummobilesolutions;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class VoiceHelper {

    private final Api api;
    private AudioRecord recorder;
    private AudioTrack track = null;
    private Socket mSocket;
    private boolean status = true;
    private String currentlyTalking;
    private FragmentVoiceChat fragmentVoiceChat;
    private final MainActivity main;
    private int room;
    private boolean talkInterrupted = true;

    private int minBufSize = 3000;
    private final int sampleRate = 8000;
    private final int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private boolean listening = false;
    private boolean failedTalk = false;

    @SuppressLint("MissingPermission")
    public VoiceHelper(MainActivity main, Api api) {
        this.main = main;
        this.api = api;
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, minBufSize);
        this.room = 0;
    }

    /**
     * Establishes a socket object to connect to the server.
     * @return The socket connection object.
     */
    public Socket connectSocket(FragmentVoiceChat voiceFragment) {
        try {
            //mSocket =  IO.socket("https://www.auxiliumgroup.com:8999/");
            mSocket =  IO.socket("https://will-it-https.duckdns.org");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        mSocket.on("streamingToClient", streaming);
        mSocket.on("serverReady", ready);
        mSocket.on("endClientStream", endStream);
        mSocket.on("roomMembers", roomMembers);
        mSocket.on("joinedRoom", joinedRoom);
        mSocket.on("leftRoom", leftRoom);
        mSocket.on("invitedPrivate", invitedPrivate);
        mSocket.on("privateStreaming", privateStreaming);
        mSocket.on("privateStreamingClient", streaming);
        mSocket.on("endPrivateClientStream", privateEnded);
        mSocket.on("talkNotGranted", talkNotGranted);

        mSocket.on(Socket.EVENT_CONNECT_ERROR, voiceFragment.onConnectError);
        mSocket.on(Socket.EVENT_CONNECT, voiceFragment.onConnected);

        mSocket.connect();
        return mSocket;
    }

    /**
     * Disconnect the socket for voice.
     */
    public void disconnectSocket() {
        mSocket.disconnect();
    }

    /**
     * Returns the socket object for voice.
     * @return Socket for voice.
     */
    public Socket getSocket() {
        return mSocket;
    }

    /**
     * Resets the voice socket.
     */
    public void resetSocket() {
        mSocket = null;
    }

    public void emit(String event, JSONObject data) {
        mSocket.emit(event, data);
    }

    public void endPrivateStream(String room) {
        status = false;

        if(recorder != null && recorder.getState() == AudioRecord.STATE_INITIALIZED) {
            recorder.stop();
            recorder.release();
            minBufSize = 3000;
        }

        JSONObject data = new JSONObject();
        try {
            data.put("username", api.getDisplay());
            data.put("room", room);
            data.put("spoke", api.getSpoke());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        getSocket().emit("privateEndStream", data);
    }

    /**
     * Opens a private stream to the server.
     * @param room The room that the stream is in.
     */
    public void sendPrivateStream(String room) {
        status = true;

        @SuppressLint("MissingPermission") Thread streamThread = new Thread(() -> {
            byte[] buffer = new byte[minBufSize];

            // Start recording microphone
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,sampleRate,channelConfig,audioFormat,minBufSize);
            recorder.startRecording();

            while(status) {

                // Read data from microphone into buffer
                minBufSize = recorder.read(buffer, 0, buffer.length);

                JSONObject data = new JSONObject();
                try {
                    data.put("buffer", buffer);
                    data.put("username", api.getDisplay());
                    data.put("room", room);
                    data.put("spoke", api.getSpoke());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // Emit object to server
                getSocket().emit("privateStreaming", data);
            }
        });
        streamThread.start();
    }

    /**
     * Opens a stream to the server.
     * @param preferences SharePreferences object
     */
    public void sendStream(SharedPreferences preferences) {
        String room = preferences.getString("voiceRoomName", "");

        status = true;

        @SuppressLint("MissingPermission") Thread streamThread = new Thread(() -> {
            byte[] buffer = new byte[minBufSize];

            // Start recording microphone
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,sampleRate,channelConfig,audioFormat,minBufSize);
            recorder.startRecording();

            while(status) {

                // Read data from microphone into buffer
                minBufSize = recorder.read(buffer, 0, buffer.length);

                JSONObject data = new JSONObject();
                try {
                    data.put("buffer", buffer);
                    data.put("username", api.getDisplay());
                    data.put("room", room);
                    data.put("spoke", api.getSpoke());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // Emit object to server
                getSocket().emit("streamingToServer", data);
            }
        });
        streamThread.start();
    }

    /**
     * Ends stream to the server.
     */
    public void endStream() {
        SharedPreferences preferences = main.getSharedPreferences("AMS", 0);
        status = false;

        if(recorder != null && recorder.getState() == AudioRecord.STATE_INITIALIZED) {
            recorder.stop();
            recorder.release();
            minBufSize = 3000;

            String room = preferences.getString("voiceRoomName", "");
            JSONObject data = new JSONObject();
            try {
                data.put("username", api.getDisplay());
                data.put("room", room);
                data.put("spoke", api.getSpoke());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            getSocket().emit("endServerStream", data);
        }
    }

    /**
     * Returns the string for the currently talking user.
     * @return The string for the currently talking user.
     */
    public String getCurrentlyTalking() {
        return currentlyTalking;
    }

    /**
     * Sets the string for the currently talking user.
     * @param currentlyTalking The string for the currently talking user.
     */
    public void setCurrentlyTalking(String currentlyTalking) {
        this.currentlyTalking = currentlyTalking;
    }

    /**
     * Sets the fragmentVoiceChat that will work with voiceHelper. Needed for listeners.
     * @param fragmentVoiceChat The fragmentVoiceChat to use.
     */
    public void setFragmentVoiceChat(FragmentVoiceChat fragmentVoiceChat) {
        this.fragmentVoiceChat = fragmentVoiceChat;
    }

    /**
     * Private chat listener.
     */
    public Emitter.Listener privateStreaming = args -> {
        JSONObject data = (JSONObject) args[0];
    };

    /**
     * Private chat ended
     */
    public Emitter.Listener privateEnded = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            JSONObject data = (JSONObject) args[0];
            getSocket().emit("leavePrivate", data);

            currentlyTalking = null;
            if (fragmentVoiceChat != null) fragmentVoiceChat.setStatus("Connected");
            resetTrack();
        }
    };

    /**
     * Invited to private chat listener
     */
    public Emitter.Listener invitedPrivate = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            JSONObject data = (JSONObject) args[0];

            // We get back a room, and a list of users invited to private room.
            // If the current user is in the list of users invited to private room, join the room
            try {
                JSONArray users = data.getJSONArray("users");
                for (int i=0;i<users.length(); i++){
                    if (users.get(i).toString().equalsIgnoreCase(api.getDisplay())) {

                        JSONObject joinData = new JSONObject();
                        try {
                            joinData.put("username", api.getDisplay());
                            joinData.put("room", data.getString("room"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        getSocket().emit("joinPrivate", joinData);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            main.playBeep();
        }
    };

    /**
     * Server ready listener
     * When a user has connected and about to start streaming
     */
    public Emitter.Listener ready = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            main.runOnUiThread(() -> {
                main.playBeep();
            });
        }
    };

    /**
     * Joined a room listener
     * When a user has joined a room
     */
    public Emitter.Listener joinedRoom = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            if(!api.getDisplay().equals(args[0].toString())) {
                if(fragmentVoiceChat == null){
                    main.runOnUiThread(() -> Toast.makeText(main, args[0] + " joined the room", Toast.LENGTH_LONG).show());
                } else {
                    Animation animation = AnimationUtils.loadAnimation(fragmentVoiceChat.getContext(), R.anim.listview_animation_right_to_left);
                    animation.setStartOffset(4000);
                    main.runOnUiThread(() -> {
                        fragmentVoiceChat.joinedRoomNotification.setText(args[0] + " joined the room");
                        fragmentVoiceChat.joinedRoomNotification.setVisibility(View.VISIBLE);
                        fragmentVoiceChat.joinedRoomNotification.startAnimation(animation);
                        fragmentVoiceChat.joinedRoomNotification.setVisibility(View.INVISIBLE);
                    });
                }
            }
            SharedPreferences preferences = main.getSharedPreferences("AMS", 0);
            String room = preferences.getString("voiceRoomName", "");

            JSONObject data = new JSONObject();
            try {
                data.put("spoke", api.getSpoke());
                data.put("room", room);
            } catch(JSONException e) {
                e.printStackTrace();
            }
            getSocket().emit("getRoomInfo", data);
        }
    };

    /**
     * Stream listener
     * When a user is streaming, this is called.
     */
    public Emitter.Listener streaming = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

            JSONObject data = (JSONObject) args[0];
            byte[] stringData = (byte[])args[1];
            String streamUsername;
            try {
                streamUsername = data.getString("username");

                if(getCurrentlyTalking() == null && !api.getDisplay().equals(streamUsername)){
                    setCurrentlyTalking(streamUsername);
                    setListening(true);

                    if (fragmentVoiceChat != null) {
                        fragmentVoiceChat.setStatus(getCurrentlyTalking() + " is talking");
                        fragmentVoiceChat.setButtonColor(R.color.lightGray);
                    } else {
                        main.runOnUiThread(() -> Toast.makeText(main, getCurrentlyTalking() + " is talking", Toast.LENGTH_LONG).show());
                        main.setButtonColor(R.color.lightGray);
                    }

                    // AUDIO CODE
                    int intSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
                    setTrack(new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, intSize, AudioTrack.MODE_STREAM));
                    if (getTrack() != null) {
                        getTrack().play();
                    }
                }

                if(getTrack() != null) {
                    getTrack().write(stringData, 0, stringData.length);
                }

            } catch (JSONException e) {
                main.log("error: " + e.getLocalizedMessage());
            }
        }
    };

    /**
     * End stream listener
     * When a user has stopped streaming
     */
    public Emitter.Listener endStream = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            setCurrentlyTalking(null);

            setListening(false);

            if (fragmentVoiceChat != null) {
                fragmentVoiceChat.setStatus("Connected");
                fragmentVoiceChat.setButtonColor(R.color.colorAccent);
            } else {
                main.setButtonColor(R.color.colorAccent);
            }

            resetTrack();
        }
    };

    /**
     * Left room listener
     * When a user has left a room
     */
    public Emitter.Listener leftRoom = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getSocket().emit("getRoomInfo", args[0]);
        }
    };

    /**
     * Room members listener
     * When the list of users for a room is provided
     */
    public Emitter.Listener roomMembers = args -> {
        JSONArray data = (JSONArray) args[0];

        List<String> items = new ArrayList<>();
        try {
            for(int i = 0; i < data.length(); i++){
                JSONObject object = data.getJSONObject(i);
                if(!object.getString("username").equals("null")){
                    items.add(object.getString("username"));
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (fragmentVoiceChat != null) {
            fragmentVoiceChat.updateList(items);
        }
    };

    /**
     * Listener for when the client sends a stream to the server and they are
     * not allowed to talk.
     */
    public Emitter.Listener talkNotGranted = args -> {
        failedTalk = true;
        endStream();
    };

    /**
     * Sets the status for whether the user is listening.
     * @param listening Boolean value for whether the user is listening.
     */
    public void setListening(boolean listening) {
        this.listening = listening;
    }

    /**
     * Returns the listening status of the user.
     * @return The boolean value for listening status.
     */
    public boolean getListening() {
        return listening;
    }

    public void setFailedTalk(boolean failedTalk) {
        this.failedTalk = failedTalk;
    }

    public boolean getFailedTalk() {
        return failedTalk;
    }

    /**
     * Sets the room number (based on the room spinner position number).
     * @param room Sets the room for the user.
     */
    public void setRoom(int room) {
        this.room = room;
    }

    /**
     * Gets the room number (based on the room spinner position number).
     * @return Returns the room that the user is in.
     */
    public int getRoom() {
        return room;
    }

    /**
     * Sends a private chat init to the server.
     * @param data Data for the init.
     */
    public void initPrivate(JSONObject data) {
        getSocket().emit("privateInit", data);
    }

    /**
     * Resets the track to null.
     */
    public void resetTrack() {
        if(track!=null) {
            track.release();
            track = null;
        }
    }

    /**
     * Sets the track.
     * @param track The AudioTrack to set track to.
     */
    public void setTrack(AudioTrack track) {
        this.track = track;
    }

    /**
     * Returns track.
     * @return The AudioTrack for track.
     */
    public AudioTrack getTrack() {
        return track;
    }
}
