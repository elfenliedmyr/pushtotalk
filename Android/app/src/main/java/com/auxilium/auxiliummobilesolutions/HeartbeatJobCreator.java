package com.auxilium.auxiliummobilesolutions;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

class HeartbeatJobCreator implements JobCreator {
    @Nullable
    @Override
    public Job create(@NonNull String tag) {
        return new HeartbeatJob();
    }
}
