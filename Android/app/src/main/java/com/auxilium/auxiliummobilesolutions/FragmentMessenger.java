package com.auxilium.auxiliummobilesolutions;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.*;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import io.reactivex.Observable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Objects;

public class FragmentMessenger extends Fragment {
    TabLayout tabLayout;
    ViewPager viewPager;
    PageAdapter pageAdapter;

    int slice_messenger = 0, slice_members = 0, slice_messages = 0;

    SharedPreferences preferences;

    public Observable<JSONArray> observableUserlist;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_app_messages, container, false);
        tabLayout = view.findViewById(R.id.tabs);
        viewPager = view.findViewById(R.id.viewPager);

        pageAdapter = new PageAdapter(getChildFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(pageAdapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        preferences = Objects.requireNonNull(getActivity()).getSharedPreferences("AMS", 0);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        getMessengerSlices();
        return view;
    }

    void getMessengerSlices() {
        Api api = (Api) Utils.readObjectFromFile(getActivity(), "authUser");
        String query = api.request("{'$/schema/simpleDescription':{}}");
        if (!query.isEmpty()) {
            try {
                JSONObject x = new JSONObject(query);
                Iterator<String> iterator = x.keys();

                while (iterator.hasNext()) {
                    String key = iterator.next();
                    if (key.contains("MEMBERS")) {
                        try {
                            JSONObject value = new JSONObject(x.get(key).toString());
                            this.slice_members = Integer.parseInt(value.get("slice").toString());
                        } catch (JSONException e) {
                            // Something went wrong!
                        }
                    }

                    else if (key.contains("MESSENGER")) {
                        try {
                            JSONObject value = new JSONObject(x.get(key).toString());
                            this.slice_messenger = Integer.parseInt(value.get("slice").toString());
                        } catch (JSONException e) {
                            // Something went wrong!
                        }
                    }

                    else if (key.contains("MESSAGES")) {
                        try {
                            JSONObject value = new JSONObject(x.get(key).toString());
                            this.slice_messages = Integer.parseInt(value.get("slice").toString());
                        } catch (JSONException e) {
                            // Something went wrong!
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (this.slice_messenger != 0 && this.slice_messages != 0 && this.slice_members != 0) {
            getAllUsers();
            getActiveChats();
        } else {
            if (createTables()) {
                getAllUsers();
                getActiveChats();
            }
        }
    }

    Boolean createTables() {
        // {'$/tools/action_chain': [

        // messenger table schema
        //{'$/schema/createTable':{table:'mobile_messenger1', columns:{date:'date', name:'text', socket_id:'text'}}},

        // members table schema
        //

        // messages table schema
        // {'$/schema/createTable':{table:'mobile_messenger1', columns:{date:'date', name:'text', socket_id:'text'}}},

        // ]}
        return true;
    }

    void getAllUsers() {
        Api api = (Api) Utils.readObjectFromFile(getActivity(), "authUser");
        String query = api.request("{'$/env/users/fetch':{}}");
        if (!query.isEmpty()) {
            try {
                JSONObject x = new JSONObject(query);
                if (x.has("parent")) {
                    String usersRes = api.request("{'$/slice/report':{slice:" + x.getInt("parent") + "}}");
                    JSONObject res = new JSONObject(usersRes);
                    if (res.has("rows")) {
                        JSONArray rows = res.getJSONArray("rows");
                        int myJsonArraySize = rows.length();
                        if (myJsonArraySize > 0) observableUserlist = Observable.just(rows);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    void getActiveChats() {
        Api api = (Api) Utils.readObjectFromFile(getActivity(), "authUser");
        String res = api.request("{'$/slice/report':{slice:" + this.slice_members + ", where: [\'$eq\', [\'$field\', \'MemberRef\'], "+api.getUserId()+"]}}");
        if (res != null && !res.isEmpty()) {
//            try {
//                JSONObject x = new JSONObject(query);
//                if (x.has("parent")) {
//                    String roomsRes = api.request("{'$/slice/report':{slice:" + x.getInt("parent") + "}}");
//
//                    Log.d("ANDRE", roomsRes);
////                    JSONObject res = new JSONObject(usersRes);
////                    if (res.has("rows")) {
////                        JSONArray rows = res.getJSONArray("rows");
////                        int myJsonArraySize = rows.length();
////                        if (myJsonArraySize > 0) observableUserlist = Observable.just(rows);
////                    }
//                }
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
        } else {
            Log.d("ANDRE", res);
        }
    }
}
