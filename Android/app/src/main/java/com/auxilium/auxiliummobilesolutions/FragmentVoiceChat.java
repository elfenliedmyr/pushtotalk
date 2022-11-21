/**
 * This class handles the VoiceChat we are implementing.  Calls on the methods from MainActivity for sending and receiving
 * voice transmission.  Handles connecting and disconnecting from the server, joining a room, and checking and requesting
 * permissions from the user.
 */

package com.auxilium.auxiliummobilesolutions;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.auxilium.auxiliummobilesolutions.dbhelpers.DBAllChatHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.socket.emitter.Emitter;

import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.auxilium.auxiliummobilesolutions.WalkieTalkie.RequestPermissionCode;

public class FragmentVoiceChat extends Fragment implements View.OnClickListener {

    private AudioManager am = null;
    private final String LOGTAG = "ANDRETEST-VOICESOCKET";
    private static ArrayList<Integer> privateChatSelectedUsers = new ArrayList<>();

    Api api;
    Activity mActivity;
    ImageButton btnTalk;

    TextView txtStatus, joinedRoomNotification;
    Spinner spinnerRooms;
    String currentlyTalking;
    Button btnLeave, btn_allContacts;
    ListView lstUsers, lstAllUsers;
    Boolean showAll = false;
    DBAllChatHelper dbAllChatHelper;
    VoiceHelper voiceHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getActivity() == null) return;

        // Setup audio volume control to OS
        getActivity().setVolumeControlStream(AudioManager.MODE_IN_COMMUNICATION);

        // Create Audio Manager and output sound to Speakerphone
        am = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return;
        am.setSpeakerphoneOn(true);

        dbAllChatHelper = new DBAllChatHelper(getContext());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof Activity){
            mActivity = (Activity) context;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_app_voicechat, container, false);

        api = (Api) Utils.readObjectFromFile(getActivity(), "authUser");
        btnTalk = view.findViewById(R.id.btnTalk);

        if (voiceHelper.getListening()) {
            setButtonColor(R.color.lightGray);
        } else {
            setButtonColor(R.color.colorAccent);
        }

        btnTalk.setOnTouchListener(talkListener);
        btn_allContacts = view.findViewById(R.id.btn_allContacts);
        txtStatus = view.findViewById(R.id.txtStatus);
        txtStatus.setText(R.string.not_connected);
        joinedRoomNotification = view.findViewById(R.id.joinedRoomNotification);
        lstUsers = view.findViewById(R.id.lstUsers);
        lstAllUsers = view.findViewById(R.id.lstAllUsers);
        lstAllUsers = view.findViewById(R.id.lstAllUsers);

        String[] arraySpinner = new String[] {
                "Main Room", "Room 1", "Room 2", "Room 3", "Room 4", "Room 5"
        };
        spinnerRooms = view.findViewById(R.id.spinner_rooms);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, arraySpinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRooms.setAdapter(adapter);

        if (voiceHelper.getSocket() == null) {
            socketConnect();
        }

        spinnerRooms.setSelection(voiceHelper.getRoom());
        spinnerRooms.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (voiceHelper.getSocket() == null) {
                    socketConnect();
                }
                voiceHelper.setRoom(position);
                spinnerRooms.setSelection(voiceHelper.getRoom());

                String room = api.getSpoke() + "#" + spinnerRooms.getItemAtPosition(position)
                        .toString().toLowerCase().replaceAll(" ", "");
                    joinRoom(room);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        if(!checkPermission()) {
            requestPermission();
        }

        btnLeave = view.findViewById(R.id.btnLeave);
        btnLeave.setOnClickListener(this);
        btn_allContacts.setOnClickListener(this);

        lstAllUsers.setOnItemClickListener(allUserSelection);


        return view;
    }

    // Join Room
    public void joinRoom(String room) {
        JSONObject data = new JSONObject();
        try {
            data.put("username", api.getDisplay());
            data.put("spoke", api.getSpoke());
            data.put("room", room);

            SharedPreferences preferences = requireActivity()
                    .getSharedPreferences("AMS", 0);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("voiceRoomName", room);
            editor.apply();

            if (voiceHelper.getSocket() != null) {
                socketConnect();
            }

            voiceHelper.getSocket().emit("join", data);
            if (getActivity() != null)
                getActivity().runOnUiThread(() -> {
                    txtStatus.setText(R.string.connected);
                    btnLeave.setText(R.string.leave);
                });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Connect to server
    public void socketConnect(){
        txtStatus.setText(R.string.connecting);
        if (getActivity() == null) return;

        if (voiceHelper.getSocket() == null) {
            voiceHelper.connectSocket(this);

            if (spinnerRooms != null)
                spinnerRooms.setEnabled(true);

            ((MainActivity) getActivity()).fragmentVoiceChatConnected();
        }
    }
    public final Emitter.Listener onConnectError = args -> mActivity.runOnUiThread((Runnable) () -> {
        txtStatus.setText(R.string.not_connected);
        Toast.makeText(mActivity.getApplicationContext(), "Unable to connect to voice chat server", Toast.LENGTH_LONG).show();
    });
    public final Emitter.Listener onConnected = args -> mActivity.runOnUiThread((Runnable) () -> {
        txtStatus.setText(R.string.connected);
        Toast.makeText(mActivity.getApplicationContext(), "Connected to voice chat server", Toast.LENGTH_LONG).show();
    });

    private final AdapterView.OnItemClickListener allUserSelection = (parent, view, position, id) -> {

        if (privateChatSelectedUsers != null && privateChatSelectedUsers.size() > 0) {
            int remId = -1;
            for (int i=0; i<privateChatSelectedUsers.size(); i++) {
                int selId = privateChatSelectedUsers.get(i);
                if (selId == position) {
                    parent.getChildAt(selId).setBackgroundColor(Color.TRANSPARENT);
                    privateChatSelectedUsers.remove(i);
                    remId = position;
                }
            }
            if (remId != position) {
                parent.getChildAt(position).setBackgroundColor(Color.DKGRAY);
                privateChatSelectedUsers.add(position);
            }
        } else {
            parent.getChildAt(position).setBackgroundColor(Color.DKGRAY);
            privateChatSelectedUsers.add(position);

            for (int i = 0; i<privateChatSelectedUsers.size(); i++){
                Log.i(LOGTAG, "privateChatSelectedUsers Pos: " + i + " Member: " + privateChatSelectedUsers.get(i));
            }
        }
    };

    // Button touch listener
    // Action Down: start stream
    // Action Up: stop recording, stop stream.
    private final View.OnTouchListener talkListener = new View.OnTouchListener(){

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            // Disable button if socket is not connected or the main activity is null

            /*
              When disconnecting and attempting to use the transmit button the app crashes.  Receive an error
              Attempt to invoke virtual method 'boolean io.socket.client.Socket.connected()' on a null object reference
              Error caused because the voiceHelper.getSocket() when disconnected is changed to null, and it cannot be evaluated as true or false.
             */

            // Disconnect bug fixed
            if (voiceHelper.getSocket() == null || ((MainActivity) getActivity()) == null) return false;

            // TODO: CREATE ANOTHER METHOD OF DISABLING WHO IS TALKING,
            //  WITHOUT STOPPING THE CURRENTLY TALKING USER
            // Disable button if someone else is currently talking
//            if (txtStatus.getText().toString().contains("is talking")) return false;

            if (showAll) {

                // Private chat push talk functionality
                switch(event.getAction()){
                    case MotionEvent.ACTION_CANCEL :
                    case MotionEvent.ACTION_UP:
                        if (voiceHelper.getFailedTalk()) {
                            voiceHelper.setFailedTalk(false);
                            break;
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            setButtonColor(R.color.colorAccent);
                        }
                        if (privateChatSelectedUsers.size() > 0)
                            stopPrivateStream("private_" + api.getUserId());
                        break;

                    case MotionEvent.ACTION_DOWN:
                        if (voiceHelper.getListening()) {
                            if (privateChatSelectedUsers.size() > 0) {
                                ArrayList<String> users = new ArrayList<>();
                                for (int i = 0; i < privateChatSelectedUsers.size(); i++) {
                                    String user = lstAllUsers.getItemAtPosition(privateChatSelectedUsers.get(i)).toString();
                                    users.add(user);
                                }
                                JSONObject data = new JSONObject();
                                JSONArray jsonUsers = new JSONArray(users);

                                try {
                                    data.put("room", "private_" + api.getUserId());
                                    data.put("users", jsonUsers);
                                    voiceHelper.initPrivate(data);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                voiceHelper.sendPrivateStream("private_" + api.getUserId());
                                txtStatus.setText("You are talking");
                                btnTalk.setImageResource(R.drawable.ic_mic_white_48dp);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    setButtonColor(R.color.darkgreen);
                                }
                            }
                        } else {
                            voiceHelper.setFailedTalk(true);
                        }
                        break;
                }
            } else {

                // In room push talk functionality
                switch(event.getAction()){
                    case MotionEvent.ACTION_CANCEL :
                    case MotionEvent.ACTION_UP:
                        if (voiceHelper.getFailedTalk()) {
                            voiceHelper.setFailedTalk(false);
                            break;
                        }

                        if(!checkPermission()){
                            boolean showAppInfo = requestPermission();
                            if(!showAppInfo) {
                                Toast.makeText(getActivity(), "Enable 'Microphone' & 'Storage' in 'Permissions' settings in order to speak in chat room", Toast.LENGTH_LONG).show();
                                //Open app info so user can enable permissions to use voice chat
                                Intent i = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                i.addCategory(Intent.CATEGORY_DEFAULT);
                                i.setData(Uri.parse("package:" + "com.auxilium.auxiliummobilesolutions"));
                                startActivity(i);
                            }

                        } else {
                            txtStatus.setText("You stopped talking");
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                setButtonColor(R.color.colorAccent);
                            }
                            stopStream();
                        }
                        break;

                    case MotionEvent.ACTION_DOWN:
                        if(!checkPermission()){
                            txtStatus.setText("Need permission to access \n 'Storage' & 'Microphone'");
                            //do nothing until the button is released
                        } else {
                            if (voiceHelper.getListening()) {
                                voiceHelper.setFailedTalk(true);
                            } else {
                                voiceHelper.sendStream(getActivity().getSharedPreferences("AMS", 0));
                                txtStatus.setText("You are talking");
                                btnTalk.setImageResource(R.drawable.ic_mic_white_48dp);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    setButtonColor(R.color.darkgreen);
                                }
                            }
                        }
                        break;
                }
            }
            return true;
        }

    };

    public void stopStream() {
        currentlyTalking = null;
        voiceHelper.endStream();
        txtStatus.setText(voiceHelper.getSocket().connected() ? "Connected" : "Disconnected");
        btnTalk.setBackgroundResource(R.drawable.round_button);
        btnTalk.setImageResource(R.drawable.ic_mic_none_white_48dp);
    }

    public void stopPrivateStream(String room) {
        currentlyTalking = null;
        voiceHelper.endPrivateStream(room);
        txtStatus.setText(voiceHelper.getSocket().connected() ? "Connected" : "Disconnected");
        btnTalk.setBackgroundResource(R.drawable.round_button);
        btnTalk.setImageResource(R.drawable.ic_mic_none_white_48dp);
    }




    // PERMISSIONS

    private boolean requestPermission() {
        if (getActivity() == null) return true;
        ActivityCompat.requestPermissions(getActivity(), new
                String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO, INTERNET}, RequestPermissionCode);
        //Evaluates if the user checks the 'Don't ask again' checkbox
        boolean denyRecord = ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), RECORD_AUDIO);
        boolean denyStorage = ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), WRITE_EXTERNAL_STORAGE);
        return denyRecord || denyStorage;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode > 0 && RequestPermissionCode > 0) {
            if (grantResults.length> 0) {
                boolean StoragePermission = grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED;
                boolean RecordPermission = grantResults[1] ==
                        PackageManager.PERMISSION_GRANTED;
                boolean InternetPermission = grantResults[2] ==
                        PackageManager.PERMISSION_GRANTED;

                if (StoragePermission && RecordPermission && InternetPermission) {
                    Toast.makeText(getActivity(), "Permission Granted", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getActivity(),"Permission Denied", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(requireActivity()
                        .getApplicationContext(),
                WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),
                RECORD_AUDIO);
        int result2 = ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),
                INTERNET);
        return result == PackageManager.PERMISSION_GRANTED &&
                result1 == PackageManager.PERMISSION_GRANTED &&
                result2 == PackageManager.PERMISSION_GRANTED;
    }

    void setStatus(String txtStatus) {
        if (this.txtStatus != null) {
            this.txtStatus.setText(txtStatus);
        }
    }

    public void updateList(List<String> data) {

        Objects.requireNonNull(mActivity).runOnUiThread(() -> {
            // Stuff that updates the UI
            ArrayAdapter adapter = new ArrayAdapter(mActivity,
                    android.R.layout.simple_list_item_1, data);

            lstUsers.setAdapter(adapter);
            Animation animation = AnimationUtils.loadAnimation(mActivity, R.anim.listview_animation_left_to_right);
            lstUsers.startAnimation(animation);
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_allContacts:
                showAll = !showAll;
                if (showAll) {
                    btn_allContacts.setText(R.string.room_contacts);
                    lstAllUsers.setVisibility(View.VISIBLE);
                    lstUsers.setVisibility(View.GONE);
                    spinnerRooms.setVisibility(View.INVISIBLE);
                    btnLeave.setVisibility(View.INVISIBLE);

                    Cursor dbData = dbAllChatHelper.getData();
                    if (dbData != null && dbData.getCount() > 0) {

                        List<String> users = new ArrayList<>();
                        while (dbData.moveToNext()) {
                            @SuppressLint("Range") String communicationid = dbData.getString(dbData.getColumnIndex("communicationid"));
                            @SuppressLint("Range") String display = dbData.getString(dbData.getColumnIndex("display"));
                            if (communicationid != null && !communicationid.equals("null")) {
                                users.add(display);
                            }
                        }

                        ArrayAdapter adapter2 = new ArrayAdapter(mActivity,
                                android.R.layout.simple_list_item_1, users);
                        lstAllUsers.setAdapter(adapter2);
                        privateChatSelectedUsers.clear();

                        dbData.close();
                        return;
                    } else {
                        String query = api.request("{\"$/env/users/report\":{}}");
                        if (!query.isEmpty()) {
                            try {
                                JSONObject res = new JSONObject(query);
                                if (res.has("rows")) {
                                    JSONArray rows = res.getJSONArray("rows");
                                    int myJsonArraySize = rows.length();
                                    dbAllChatHelper.deleteAll();
                                    for(int i =0; i< myJsonArraySize; i++) {
                                        JSONObject user = rows.getJSONObject(i);
                                        dbAllChatHelper.addUser(
                                                user.getInt("id"),
                                                user.getString("sysadmin"),
                                                user.getString("communicationid"),
                                                user.getString("display"),
                                                user.getString("Department")
                                        );
                                    }

                                    dbData = dbAllChatHelper.getData();
                                    List<String> users = new ArrayList<>();
                                    while (dbData.moveToNext()) {
                                        @SuppressLint("Range") String communicationid = dbData.getString(dbData.getColumnIndex("communicationid"));
                                        @SuppressLint("Range") String display = dbData.getString(dbData.getColumnIndex("display"));
                                        if (communicationid != null && !communicationid.equals("null")) {
                                            users.add(display);
                                        }
                                    }

                                    ArrayAdapter adapter2 = new ArrayAdapter(mActivity,
                                            android.R.layout.simple_list_item_1, users);
                                    lstAllUsers.setAdapter(adapter2);
                                    dbData.close();
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else {
                    btn_allContacts.setText(R.string.all_contacts);
                    lstUsers.setVisibility(View.VISIBLE);
                    lstAllUsers.setVisibility(View.INVISIBLE);
                    spinnerRooms.setVisibility(View.VISIBLE);
                    btnLeave.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.btnLeave:
                    voiceHelper.setRoom(0);
                    disconnectVoiceChat();
                break;
        }
    }

    /**
     * Returns the voiceHelper for FragmentVoiceChat
     * @return VoiceHelper object
     */
    public VoiceHelper getVoiceHelper() {
        return voiceHelper;
    }

    /**
     * Sets the voiceHelper for FragmentVoiceChat
     * @param voiceHelper Voicehelper object to set
     */
    public void setVoiceHelper(VoiceHelper voiceHelper) {
        this.voiceHelper = voiceHelper;
        voiceHelper.setFragmentVoiceChat(this);
    }

    /**
     *  Sets the color of the button in FragmentVoiceChat
     * @param color The color to set the button.
     */
    public void setButtonColor (int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            btnTalk.setBackgroundTintList(getResources().getColorStateList(color));
        }
    }

    public void setListening (boolean listening) {
        voiceHelper.setListening(listening);
    }

    /**
     * Disconnects the user from the chat room when they click the 'Disconnect' button
     */
    public void disconnectVoiceChat(){
        JSONObject data = new JSONObject();

        try {
            data.put("username", api.getDisplay());
            data.put("spoke", api.getSpoke());
            String room = api.getSpoke() + "#" + spinnerRooms.getItemAtPosition(spinnerRooms.getSelectedItemPosition())
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

            try {
                TimeUnit.MILLISECONDS.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            voiceHelper.disconnectSocket();
            voiceHelper.resetSocket();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ((MainActivity) getActivity()).getSupportFragmentManager().popBackStack();
    }
}
