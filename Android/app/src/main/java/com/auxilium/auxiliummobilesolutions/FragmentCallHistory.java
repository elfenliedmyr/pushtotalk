package com.auxilium.auxiliummobilesolutions;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.CallLog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.auxilium.auxiliummobilesolutions.adapters.CallHistoryAdapter;
import com.auxilium.auxiliummobilesolutions.dbhelpers.DBContactsHelper;
import com.auxilium.auxiliummobilesolutions.dbhelpers.DBHistoryHelper;
import com.auxilium.auxiliummobilesolutions.extras.RecyclerItemClickHelper;
import com.auxilium.auxiliummobilesolutions.extras.RecyclerItemTouchHelper;
import com.auxilium.auxiliummobilesolutions.models.CallHistory;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FragmentCallHistory extends Fragment implements RecyclerItemTouchHelper.RecyclerItemTouchHelperListener {

    private static String TAG = "FragmentCallHistory";
    private CallHistoryAdapter mAdapter;
    private List<CallHistory> mCallList;
    private RelativeLayout relativeLayout;

    DBHistoryHelper mDBHistoryHelper;
    DBContactsHelper mDBContactsHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDBHistoryHelper = new DBHistoryHelper(getContext());
        mDBContactsHelper = new DBContactsHelper(getContext());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_app_callhistory, container, false);
        RecyclerView recyclerView = v.findViewById(R.id.call_history_recyclerView);
        relativeLayout = v.findViewById(R.id.call_history_layout);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        getCallHistory();
        mAdapter = new CallHistoryAdapter(getContext(), mCallList);
        recyclerView.setAdapter(mAdapter);

        ItemTouchHelper.SimpleCallback itemTouchHelperCallback =
            new RecyclerItemTouchHelper(0, ItemTouchHelper.LEFT, this);

        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView);
        recyclerView.addOnItemTouchListener(
            new RecyclerItemClickHelper(getContext(), recyclerView ,new RecyclerItemClickHelper.OnItemClickListener() {
                @Override public void onItemClick(View view, int position) {
                    displayConfirmation(position);
                }

                @Override public void onLongItemClick(View view, int position) {
//                    Log.d(TAG, "Long pressed on: " + mCallList.get(position).getContact_name());
                }
            })
        );

        return v;
    }

    private void getCallHistory(){

        List<CallHistory> list = new ArrayList<>();

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat
                .requestPermissions(getActivity(), new String[]{Manifest.permission.READ_CALL_LOG}, 2);
        } else {

            String strOrder = android.provider.CallLog.Calls.DATE + " DESC";
            String fromDate = String.valueOf(System.currentTimeMillis()
                    - (20 * 24 * 60 * 60 * 1000)
                    - (20 * 24 * 60 * 60 * 1000)
                    - (20 * 24 * 60 * 60 * 1000)); // 60 Days
            String toDate = String.valueOf(System.currentTimeMillis() + (24 * 60 * 60 * 1000));

            String[] whereValue = {fromDate,toDate};

            @SuppressLint("MissingPermission")
            Cursor cursor = getContext().getContentResolver().query(CallLog.Calls.CONTENT_URI,
                    null,
                    android.provider.CallLog.Calls.DATE + " BETWEEN ? AND ?",
                    whereValue,
                    strOrder);

            assert cursor != null;

            if (cursor.moveToFirst()) {
                do {
                    int name = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
                    int number = cursor.getColumnIndex(CallLog.Calls.NUMBER);
                    int time = cursor.getColumnIndex(CallLog.Calls.DATE);
                    int duration = cursor.getColumnIndex(CallLog.Calls.DURATION);
                    int type = cursor.getColumnIndex(CallLog.Calls.TYPE);

                    // COMPARE WITH DATABASE VALUES, INSERT NEW VALUES INTO DATABASE
                    String safeName = (cursor.getString(name) != null) ?
                            cursor.getString(name).replaceAll("[^a-zA-Z0-9\\s+]", "")
                            : cursor.getString(name);
                    Cursor dbData = mDBHistoryHelper.getItemsID(
                            safeName,
                            cursor.getString(number),
                            cursor.getString(time),
                            cursor.getInt(type));
                    if (dbData == null || dbData.getCount() <= 0) {
                        if (cursor.getString(name) != null) mDBHistoryHelper.addData(
                                cursor.getString(name).replaceAll("[^a-zA-Z0-9\\s+]", ""),
                                cursor.getString(number),
                                cursor.getString(time),
                                cursor.getString(duration),
                                cursor.getInt(type),
                                "new"
                        );
                    }
                    if (dbData != null) dbData.close();
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        // GET DATABASE VALUES
        Cursor data = mDBHistoryHelper.getData();
        while(data.moveToNext()) {
            if (!data.getString(6).equals("hidden") && !data.getString(6).equals("billed"))
                list.add(new CallHistory(
                    data.getString(1).replaceAll("[^a-zA-Z0-9\\s+]", ""),
                    data.getString(2),
                    data.getString(3),
                    data.getString(4),
                    data.getInt(5)
                ));
        }
        mCallList = list;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof CallHistoryAdapter.ViewHolder) {
            // get the removed item name to display it in snack bar
            String name = mCallList.get(viewHolder.getAdapterPosition()).getContact_name();

            // backup of removed item for undo purpose
            final CallHistory deletedItem = mCallList.get(viewHolder.getAdapterPosition());
            final int deletedIndex = viewHolder.getAdapterPosition();

            // remove the item from recycler view
            mAdapter.deleteItem(viewHolder.getAdapterPosition());

            // showing snack bar with Undo option
            Snackbar snackbar = Snackbar
                .make(relativeLayout, name + " removed!", Snackbar.LENGTH_LONG);
            snackbar.setAction("UNDO", view -> {

                // undo is selected, restore the deleted item
                mAdapter.restoreItem(deletedItem, deletedIndex);
                mDBHistoryHelper.setStatus(deletedItem, "new");
            });
            snackbar.setActionTextColor(Color.YELLOW);
            snackbar.show();

            mDBHistoryHelper.setStatus(deletedItem, "hidden");
        }
    }

    private void displayConfirmation(int position) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_call_history, null);
        EditText noteField = view.findViewById(R.id.call_history_notes);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Submit Call");
        builder.setView(view.findViewById(R.id.dialog_call_history));
        builder.setNegativeButton("Cancel", (dialog, which) -> {

        });
        builder.setPositiveButton("Okay", (dialog, which) -> {
            sendCallToDatalynk(position, noteField.getText().toString());
        });
        builder.show();
    }

    private void sendCallToDatalynk(int position, String notes) {
        Api api = (Api) Utils.readObjectFromFile(getActivity(), "authUser");
        SharedPreferences preferences = this.getActivity().getSharedPreferences("AMS", 0);

        CallHistory record = mCallList.get(position);
        Cursor contactData = mDBContactsHelper
            .getItemsIdWithPhone(record.getContact_name(), record.getContact_number());

        if (contactData != null && contactData.getCount() > 0) {
            while (contactData.moveToNext()) {

                int contactId = contactData.getInt(contactData.getColumnIndex("ID"));
                String duration = record.getCall_duration();
                String contactPhone = record.getContact_number();

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/M/d  H:m:s");
                String date = dateFormat.format(new Date(Long.parseLong(record.getCall_time())));
                String request = "{'$/slice/xinsert':{" +
                        "slice:" + preferences.getInt("historySlice", 0) + ", " +
                        "rows: [{\"notes\": '" + notes.replaceAll("[^a-zA-Z0-9:,.!?\\s+]", "") +
                        "', \"duration\": '" + duration + "', \"phone\": '" + contactPhone +
                        "', \"date\": {\"$/tools/date\":\"" + date + "\"}, \"clientRef\": " + contactId + "}]}}";

                // Send info to Datalynk
                String syncRes = api.request(request);
                if (syncRes != null) {
                    try {
                        JSONObject res = new JSONObject(syncRes);
                        if (res.has("keys")) {
                            mDBHistoryHelper.setStatus(record, "billed");
                            mAdapter.deleteItem(position);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            contactData.close();
        } else {
            // log the contacts
            for(int i = 0; i < mCallList.size(); i++) {
                FirebaseCrashlytics.getInstance().setCustomKey("contact " + i, "name:" +
                        mCallList.get(i).getContact_name() + " number: " + mCallList.get(i).getContact_number());
            }
            // send crash
            FirebaseCrashlytics.getInstance().recordException(new Exception("sendCallToDatalynk name:" + record.getContact_name() +
                    " number:" + record.getContact_number()));
            Toast.makeText(getActivity(),
                    "Contact not found. Please sync your contacts with Datalynk.", Toast.LENGTH_LONG).show();
        }
    }
}