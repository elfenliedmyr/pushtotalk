package com.auxilium.auxiliummobilesolutions;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.auxilium.auxiliummobilesolutions.dbhelpers.DBContactsHelper;
import com.auxilium.auxiliummobilesolutions.models.AndroidContact;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.Gson;
import com.onesignal.OneSignal;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FragmentSettings extends Fragment {

    DBContactsHelper mDBContactsHelper;

    private static final String TAG = "Settings";
    private Switch notification, autoLogin, gps, refresh, hideRefresh, lockSettings, hideLogout;
    private Spinner spinnerDegrees;
    private NumberPicker np;
    private EditText contact_slice, history_slice;
    private int runCount = 0;
    private int spinnerPos = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDBContactsHelper = new DBContactsHelper(getContext());
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FirebaseCrashlytics.getInstance().setCustomKey(TAG, "Settings opened");
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        View appView = this.getView();

        assert appView != null;
        TextView txtVersion = appView.findViewById(R.id.txtSettingsVersionName);
        txtVersion.setText("Version: " + BuildConfig.VERSION_NAME);

        // Get views
//        Button btnSync = appView.findViewById(R.id.btnSyncContacts);
//        callHistory = appView.findViewById(R.id.switch5);
//        contact_slice = appView.findViewById(R.id.contact_slice);
//        history_slice = appView.findViewById(R.id.history_slice);
        autoLogin = appView.findViewById(R.id.switchAutologin);
        notification = appView.findViewById(R.id.switchNotification);
        gps = appView.findViewById(R.id.switchGPS);
        refresh = appView.findViewById(R.id.switchRefresh);
        hideRefresh = appView.findViewById(R.id.switchHideRefresh);
        hideLogout = appView.findViewById(R.id.switchHideLogout);
        lockSettings = appView.findViewById(R.id.switchLockSettings);
        np = appView.findViewById(R.id.numberPicker1);
        gps.setOnCheckedChangeListener((buttonView, isChecked) ->
            appView.findViewById(R.id.settings_gpscontainer).setVisibility(isChecked ? View.VISIBLE : View.GONE));

        spinnerDegrees = appView.findViewById(R.id.spinner_degrees);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(appView.getContext(),
                R.array.degrees, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDegrees.setAdapter(adapter);

//        callHistory.setOnCheckedChangeListener((buttonView, isChecked) -> save());
//        btnSync.setOnClickListener(v -> {
//            this.runCount = 0;
//            syncContacts();
//        });
//        callHistory.setOnCheckedChangeListener((buttonView, isChecked) ->
//            appView.findViewById(R.id.call_historyContainer).setVisibility(isChecked ? View.VISIBLE : View.GONE));

        SharedPreferences preferences = this.getActivity().getSharedPreferences("AMS", 0);

        boolean callHistoryBool = preferences.getBoolean("callHistory", false);
        boolean gpsTrackBool = preferences.getBoolean("gpsEnabled", false);
        notification.setChecked(preferences.getBoolean("alarmEnabled", true));
        autoLogin.setChecked(preferences.getBoolean("autoLoginEnabled", true));
        gps.setChecked(gpsTrackBool);
        refresh.setChecked(preferences.getBoolean("refreshWebview", false));
        hideRefresh.setChecked(preferences.getBoolean("hideRefresh", false));
        hideLogout.setChecked(preferences.getBoolean("hideLogout", false));
        lockSettings.setChecked(preferences.getBoolean("lockSettings", false));
        spinnerPos = adapter.getPosition(String.valueOf(preferences.getInt("rotateImage", 0)));
        spinnerDegrees.setSelection(spinnerPos);
        spinnerDegrees.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                spinnerPos = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        lockSettings.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Set Password");
                builder.setMessage("Lock access to the application settings with a password.");

                final EditText input = new EditText(getActivity());
                input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                input.setTransformationMethod(PasswordTransformationMethod.getInstance());

                FrameLayout container = new FrameLayout(getContext());
                FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.leftMargin = 40;
                params.rightMargin = 40;
                input.setLayoutParams(params);
                container.addView(input);

                builder.setView(container);

                builder.setNegativeButton("Cancel", (dialog, which) -> {
                    lockSettings.setChecked(false);
                });
                builder.setPositiveButton("Okay", (dialog, which) -> {
                    // Store entered value
                    SharedPreferences.Editor editor = preferences.edit();
                    String settingsPassword = input.getText().toString();
                    editor.putString("settingsLockPassword", settingsPassword);
                    editor.apply();
                });

                builder.show();
            }
        });

//        callHistory.setChecked(callHistoryBool);
        np.setMinValue(1);
        np.setMaxValue(60);
        np.setValue(preferences.getInt("maxTrackLength", 15));
        int contactSlice = preferences.getInt("contactSlice", 0);
        int historySlice = preferences.getInt("historySlice", 0);
        if (contactSlice != 0) contact_slice.setText(String.valueOf(contactSlice));
        if (historySlice != 0) history_slice.setText(String.valueOf(historySlice));

//        appView.findViewById(R.id.call_historyContainer).setVisibility(callHistoryBool ? View.VISIBLE : View.GONE);
        appView.findViewById(R.id.settings_gpscontainer).setVisibility(gpsTrackBool ? View.VISIBLE : View.GONE);

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.save();
    }

    private void save() {
        SharedPreferences preferences = this.getActivity().getSharedPreferences("AMS", 0);
        NavigationView navigationView = this.getActivity().findViewById(R.id.nav_view);

        navigationView.getMenu().findItem(R.id.mnu_app_gps).setVisible(gps.isChecked());
        navigationView.getMenu().findItem(R.id.mnu_logout).setVisible(!hideLogout.isChecked());
//        navigationView.getMenu().findItem(R.id.mnu_app_callhistory).setVisible(callHistory.isChecked());

        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("alarmEnabled", notification.isChecked());
        OneSignal.setSubscription(notification.isChecked());
        editor.putBoolean("autoLoginEnabled", autoLogin.isChecked());
        editor.putBoolean("gpsEnabled", gps.isChecked());
        editor.putBoolean("refreshWebview", refresh.isChecked());
        editor.putBoolean("hideRefresh", hideRefresh.isChecked());
        editor.putBoolean("hideLogout", hideLogout.isChecked());
        editor.putBoolean("lockSettings", lockSettings.isChecked());
        editor.putInt("rotateImage", Integer.parseInt((String) spinnerDegrees.getItemAtPosition(spinnerPos)));

//        editor.putBoolean("callHistory", callHistory.isChecked());
//        if (contact_slice.getText() != null && !contact_slice.getText().toString().equals(""))
//            editor.putInt("contactSlice", Integer.parseInt(contact_slice.getText().toString().replace(" ", "")));
//        if (history_slice.getText() != null && !history_slice.getText().toString().equals(""))
//            editor.putInt("historySlice", Integer.parseInt(history_slice.getText().toString().replace(" ", "")));
        editor.putInt("maxTrackLength", np.getValue());
        if (!autoLogin.isChecked()) {
            editor.putString("password", null);
            editor.putString("cookie", null);
        }
        editor.apply();
        FirebaseCrashlytics.getInstance().setCustomKey(TAG, "Settings saved:\nnotification - " + notification.isChecked() +
                "\nauto login - " + autoLogin.isChecked() + "\nGPS - " + gps.isChecked() +
                "\nGPS Max - " + np.getValue());
    }

//    void syncContacts() {
//        Api api = (Api) Utils.readObjectFromFile(getActivity(), "authUser");
//        List<AndroidContact> arrayList = new ArrayList<>();
//
//        // Check if we have permission to access contacts
//        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CONTACTS)
//            != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(getActivity(),
//                new String[]{Manifest.permission.READ_CONTACTS}, 1);
//        } else {
//            LayoutInflater inflater = getActivity().getLayoutInflater();
//            View view = inflater.inflate(R.layout.dialog_sync_contacts, null);
//
//            // CLEAR DATABASE OF ALL CONTACTS
//            mDBContactsHelper.deleteAll();
//
//            // Get and loop through contacts
//            Cursor cursor = getContext().getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
//                    null, null, null, null);
//
//            if (cursor != null && cursor.getCount() > 0) {
//                while (cursor.moveToNext()) {
//
//                    String phone = "";
//                    String email = "";
//                    String contact_id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
//                    String contact_name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
//                    if (contact_name != null) contact_name.replaceAll("[^a-zA-Z0-9\\s+]", "");
//
//                    // Lookup contact number
//                    int hasPhoneNumber = Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)));
//                    if (hasPhoneNumber > 0) {
//                        Cursor phoneCur = getContext().getContentResolver().query(
//                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
//                                null,
//                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ? ",
//                                new String[]{contact_id},
//                                null
//                        );
//                        if (phoneCur != null && phoneCur.getCount() > 0)
//                            while (phoneCur.moveToNext()) {
//                                phone = phone + phoneCur.getString(phoneCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
//                                if (phoneCur.getPosition() < phoneCur.getCount() - 1) phone = phone + ", ";
//                            }
//
//                       if (phoneCur != null) phoneCur.close();
//                    }
//
//                    // Lookup contact email
//                    Cursor emailCur = getContext().getContentResolver().query(
//                            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
//                            null,
//                            ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
//                            new String[]{contact_id},
//                            null);
//                    if (emailCur != null && emailCur.getCount() > 0) while (emailCur.moveToNext()) {
//                        email = emailCur.getString(emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
//                    }
//                    if (emailCur != null) emailCur.close();
//                    AndroidContact contact = new AndroidContact(
//                            contact_name,
//                            phone.replaceAll("[^0-9,\\s+]", ""),
//                            email
//                    );
//
//                    // Add to array of contacts only if contact has a number
//                    if (!phone.equals("")) arrayList.add(contact);
//                }
//                cursor.close();
//            }
//
//            // OLDER OS Progress Dialog Support
//            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
//                ProgressDialog progressDialog;
//                progressDialog = new ProgressDialog(getActivity());
//                progressDialog.setMessage("2/" + arrayList.size());
//                progressDialog.setTitle(getResources().getString(R.string.sync_contacts));
//                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//                progressDialog.setMax(arrayList.size() * 2);
//                progressDialog.show();
//                progressDialog.setCancelable(false);
//
//                new Thread(() -> {
//                    try {
//                        FirebaseCrashlytics.getInstance().setCustomKey(TAG, "syncing contacts for " + api.getUsername());
//                        progressDialog.incrementProgressBy(arrayList.size());
//                        // Send info to Datalynk
//                        String syncRes = api.request("{'$/datalynk/mobile/syncContacts':{" +
//                                "slice:" + contact_slice.getText() + ", " +
//                                "id:" + api.getUserId() + ", " +
//                                "contacts:" + new Gson().toJson(arrayList) + "}}");
//                        try {
//                            JSONObject res = new JSONObject(syncRes);
//                            if (res.has("rows")) {
//                                JSONArray rows = res.getJSONArray("rows");
//                                int myJsonArraySize = rows.length();
//
//                                FirebaseCrashlytics.getInstance().setCustomKey(TAG, "response array length " + myJsonArraySize);
//                                for (int i = 0; i < myJsonArraySize; i++) {
//                                    JSONObject myJsonObject = (JSONObject) rows.get(i);
//
//                                    // COMPARE WITH LOCAL DATABASE VALUES, INSERT NEW VALUES INTO LOCAL DATABASE
//                                    Cursor dbData = mDBContactsHelper.getItem(myJsonObject.getInt("id"));
//                                    if (dbData == null || dbData.getCount() <= 0) {
//                                        mDBContactsHelper.addData(
//                                                myJsonObject.getInt("id"),
//                                                myJsonObject.getString("name"),
//                                                myJsonObject.getString("phone").replaceAll("[^0-9,\\s+]", ""),
//                                                myJsonObject.getString("email")
//                                        );
//                                        progressDialog.incrementProgressBy(1);
//                                        progressDialog.setMessage(progressDialog.getProgress() + "/" + arrayList.size());
//
//                                        FirebaseCrashlytics.getInstance().setCustomKey(TAG, "adding contact " + myJsonObject.getString("name") +
//                                                " : " + myJsonObject.getString("phone").replaceAll("[^0-9,\\s+]", ""));
//                                    }
//
//                                    if (dbData != null) dbData.close();
//                                }
//                                getActivity().runOnUiThread(() -> {
//                                    int count = mDBContactsHelper.getData().getCount();
//                                    Toast.makeText(getActivity(),
//                                            "Your " + count + " contacts have been synced successfully!", Toast.LENGTH_LONG).show();
//                                    progressDialog.dismiss();
//                                });
//                            }
//                        } catch (JSONException e) {
//                            FirebaseCrashlytics.getInstance().recordException(e);
//                            getActivity().runOnUiThread(() -> Toast.makeText(getActivity(),
//                                    "There was an error syncing your contacts. Please, try again.", Toast.LENGTH_LONG).show());
//                            e.printStackTrace();
//                        }
//                    } catch (Exception e) {
//                        FirebaseCrashlytics.getInstance().recordException(e);
//                        e.printStackTrace();
//                    }
//
//                    FirebaseCrashlytics.getInstance().recordException(new Exception("Sync with DB complete"));
//                }).start();
//            }
//
//            // NEWER OS Progress Dialog Support
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                ProgressBar progressBar = view.findViewById(R.id.progress_sync_contacts);
//                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
//                builder.setTitle(getResources().getString(R.string.sync_contacts));
//                builder.setView(view.findViewById(R.id.dialog_sync_contacts));
//                builder.setNegativeButton("Cancel", (dialog, which) -> {
//                    mDBContactsHelper.deleteAll();
//                });
//                builder.setCancelable(false);
//                final AlertDialog dialog = builder.show();
//                progressBar.setMin(0);
//                progressBar.setMax(arrayList.size() * 2);
//
//                new Thread(() -> {
//                    try {
//
//                        FirebaseCrashlytics.getInstance().setCustomKey(TAG, "syncing contacts for " + api.getUsername());
//                        // Send info to Datalynk
//                        progressBar.incrementProgressBy(arrayList.size());
//                        String syncRes = api.request("{'$/datalynk/mobile/syncContacts':{" +
//                                "slice:" + contact_slice.getText() + ", " +
//                                "id:" + api.getUserId() + ", " +
//                                "contacts:" + new Gson().toJson(arrayList) + "}}");
//                        try {
//                            JSONObject res = new JSONObject(syncRes);
//                            if (res.has("rows")) {
//                                JSONArray rows = res.getJSONArray("rows");
//                                int myJsonArraySize = rows.length();
//
//                                FirebaseCrashlytics.getInstance().setCustomKey(TAG, "response array length " + myJsonArraySize);
//                                for (int i = 0; i < myJsonArraySize; i++) {
//                                    JSONObject myJsonObject = (JSONObject) rows.get(i);
//
//                                    // COMPARE WITH LOCAL DATABASE VALUES, INSERT NEW VALUES INTO LOCAL DATABASE
//                                    Cursor dbData = mDBContactsHelper.getItem(myJsonObject.getInt("id"));
//                                    if (dbData == null || dbData.getCount() <= 0) {
//                                        mDBContactsHelper.addData(
//                                                myJsonObject.getInt("id"),
//                                                myJsonObject.getString("name"),
//                                                myJsonObject.getString("phone").replaceAll("[^0-9,\\s+]", ""),
//                                                myJsonObject.getString("email")
//                                        );
//                                        progressBar.incrementProgressBy(1);
//
//                                        FirebaseCrashlytics.getInstance().setCustomKey(TAG, "adding contact " + myJsonObject.getString("name") +
//                                                " : " + myJsonObject.getString("phone").replaceAll("[^0-9,\\s+]", ""));
//                                    }
//                                    if (dbData != null) dbData.close();
//                                }
//                                getActivity().runOnUiThread(() -> {
//                                    int count = mDBContactsHelper.getData().getCount();
//                                    Toast.makeText(getActivity(),
//                                    "Your " + count + " contacts have been synced successfully!", Toast.LENGTH_LONG).show();
//                                    dialog.dismiss();
//                                });
//                            } else if (res.has("error")) {
//                                FirebaseCrashlytics.getInstance().recordException(new Exception("ERR: " + res.getString("error")));
//                            }
//                        } catch (JSONException e) {
//                            FirebaseCrashlytics.getInstance().recordException(e);
//                            getActivity().runOnUiThread(() -> Toast.makeText(getActivity(),
//                            "There was an error syncing your contacts. Please, try again.", Toast.LENGTH_LONG).show());
//                            e.printStackTrace();
//                        }
//                    } catch (Exception e) {
//                        FirebaseCrashlytics.getInstance().recordException(e);
//                        e.printStackTrace();
//                    }
//                }).start();
//            }
//        }
////            // EXAMPLE ON HOW TO FETCH THE CONTACTS IN THE DATABASE
////
////            Cursor dbData = mDBContactsHelper.getData();
////            if (dbData.getCount() > 0)
////                while (dbData.moveToNext()) {
////                    Log.d(TAG, "Id: " + dbData.getString(dbData.getColumnIndex("ID")) +
////                        " Name: " + dbData.getString(dbData.getColumnIndex("NAME")) +
////                        " Number: " + dbData.getString(dbData.getColumnIndex("PHONE")));
////                }
////            dbData.close();
//    }
}
