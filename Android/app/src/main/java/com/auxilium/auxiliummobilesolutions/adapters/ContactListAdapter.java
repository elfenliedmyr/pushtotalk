package com.auxilium.auxiliummobilesolutions.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.auxilium.auxiliummobilesolutions.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ContactListAdapter extends RecyclerView.Adapter<ContactListAdapter.ViewHolder> {

    private Context mContext;
    private static final String TAG = "ContactListAdapter";
    private JSONArray mUserList;
    private LayoutInflater layoutInflater;

    public ContactListAdapter(Context context, JSONArray userlist) {
        mContext = context;
        mUserList = userlist;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        layoutInflater = LayoutInflater.from(mContext);
        View view = layoutInflater.inflate(R.layout.messages_listitem_contact, parent,false);
        ContactListAdapter.ViewHolder viewHolder = new ContactListAdapter.ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TextView contactName;
        ImageView contactImage;

        contactName = holder.contactName;
        contactImage = holder.contactImage;

        try {
            JSONObject curObj = mUserList.getJSONObject(position);
            if (curObj.has("display")) contactName.setText(curObj.getString("display"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return mUserList.length();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView contactImage;
        TextView contactName;
        LinearLayout contactRow;

        public ViewHolder(View itemView) {
            super(itemView);

            contactImage = itemView.findViewById(R.id.contactImage);
            contactName = itemView.findViewById(R.id.contactName);
            contactRow = itemView.findViewById(R.id.contactRow);
        }
    }
}
