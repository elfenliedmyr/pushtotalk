package com.auxilium.auxiliummobilesolutions.models;

import android.graphics.drawable.Drawable;

public class CallHistory {

    private String contact_name;
    private String contact_number;
    private String call_time;
    private String call_duration;
    private int call_type;
    private Drawable call_icon;

    public CallHistory(
            String contact_name,
            String contact_number,
            String call_time,
            String call_duration,
            int call_type) {
        this.contact_name = contact_name;
        this.contact_number = contact_number;
        this.call_time = call_time;
        this.call_duration = call_duration;
        this.call_type = call_type;
    }

    public String getContact_name() {
        return contact_name;
    }

    public void setContact_name(String contact_name) {
        this.contact_name = contact_name;
    }

    public String getContact_number() {
        return contact_number;
    }

    public void setContact_number(String contact_number) {
        this.contact_number = contact_number;
    }

    public String getCall_time() {
        return call_time;
    }

    public void setCall_time(String call_time) {
        this.call_time = call_time;
    }

    public String getCall_duration() {
        return call_duration;
    }

    public void setCall_duration(String call_duration) {
        this.call_duration = call_duration;
    }

    public Drawable getCall_icon() {
        return call_icon;
    }

    public void setCall_icon(Drawable call_icon) {
        this.call_icon = call_icon;
    }

    public int getCall_type() {
        return call_type;
    }

    public void setCall_type(int call_type) {
        this.call_type = call_type;
    }

}
