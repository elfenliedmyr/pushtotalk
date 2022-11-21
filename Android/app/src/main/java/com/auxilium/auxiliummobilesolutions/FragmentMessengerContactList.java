package com.auxilium.auxiliummobilesolutions;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.auxilium.auxiliummobilesolutions.adapters.ContactListAdapter;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import org.json.JSONArray;

public class FragmentMessengerContactList extends Fragment {

    private ContactListAdapter mAdapter;
    private RecyclerView recyclerView;
    private Observer<JSONArray> observerUserlist;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages_contactlist, container, false);

        recyclerView = view.findViewById(R.id.recyclerMessengerContactList);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        observerUserlist = new Observer<JSONArray>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(JSONArray jsonArray) {
                mAdapter = new ContactListAdapter(getContext(), jsonArray);
                recyclerView.setAdapter(mAdapter);
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        };


        FragmentMessenger parent = ((FragmentMessenger)this.getParentFragment());
        assert parent != null && parent.observableUserlist != null && observerUserlist != null;
        parent.observableUserlist.subscribe(observerUserlist);

        return view;
    }
}
