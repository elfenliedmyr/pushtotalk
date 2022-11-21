package com.auxilium.auxiliummobilesolutions.adapters;

import android.content.Context;
import android.provider.CallLog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.auxilium.auxiliummobilesolutions.R;
import com.auxilium.auxiliummobilesolutions.models.CallHistory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CallHistoryAdapter extends RecyclerView.Adapter<CallHistoryAdapter.ViewHolder> {
    private static final String TAG = "CallHistoryAdapter";

    private LayoutInflater layoutInflater;
    private Context mContext;

    private List<CallHistory> mListCalls;

    public CallHistoryAdapter(Context context, List<CallHistory> listCalls) {
        mContext = context;
        mListCalls = listCalls;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        layoutInflater = LayoutInflater.from(mContext);

        View view = layoutInflater.inflate(R.layout.callhistory_row, parent,false);

        ViewHolder viewHolder = new ViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        TextView contactName, contactNumber, callTime, callDuration;
        ImageView callIcon;

        contactName = holder.contactName;
        contactNumber = holder.contactNumber;
        callTime = holder.callTime;
        callDuration = holder.callDuration;
        callIcon = holder.callIcon;

        contactName.setText(mListCalls.get(position).getContact_name());
        contactNumber.setText(mListCalls.get(position).getContact_number());

        Date d = new Date(Long.parseLong(mListCalls.get(position).getCall_time()));
        String itemDateStr = new SimpleDateFormat("dd MMM yyyy - hh:mm a").format(d);
        callTime.setText(itemDateStr);

        if (mListCalls.get(position).getCall_duration() != null) {
            int min = (Integer.parseInt(mListCalls.get(position).getCall_duration()) / 60);
            int sec = (Integer.parseInt(mListCalls.get(position).getCall_duration()) % 60);
            if (min > 0) callDuration.setText(min + " min");
            else if (min == 0) callDuration.setText(sec + " sec");
        }
        if (mListCalls.get(position).getCall_type() == CallLog.Calls.INCOMING_TYPE)
            callIcon.setImageResource(R.drawable.ic_call_incoming);
        else if (mListCalls.get(position).getCall_type() == CallLog.Calls.OUTGOING_TYPE)
            callIcon.setImageResource(R.drawable.ic_call_outgoing);
    }

    public void deleteItem(int position) {
        mListCalls.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public int getItemCount() {
        return mListCalls.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView contactName, contactNumber, callTime, callDuration;
        ImageView callIcon;
        public RelativeLayout call_history_background;
        public LinearLayout call_history_foreground;

        public ViewHolder(View itemView) {
            super(itemView);

            contactName = itemView.findViewById(R.id.contact_name);
            contactNumber = itemView.findViewById(R.id.contact_number);
            callTime = itemView.findViewById(R.id.call_time);
            callDuration = itemView.findViewById(R.id.call_duration);
            callIcon = itemView.findViewById(R.id.call_icon);
            call_history_background = itemView.findViewById(R.id.call_history_background);
            call_history_foreground = itemView.findViewById(R.id.call_history_foreground);
        }
    }

    public void restoreItem(CallHistory item, int position) {
        mListCalls.add(position, item);
        notifyItemInserted(position);
    }
}