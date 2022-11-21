package com.auxilium.auxiliummobilesolutions;
/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

public class CameraActivity extends AppCompatActivity {
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        if (null == savedInstanceState) {

            SharedPreferences.Editor editor = getSharedPreferences("AMS", 0).edit();
            Intent intent = getIntent();
            Bundle args = new Bundle();
            args.putInt("camera", intent.getIntExtra("camera", 1));
            args.putInt("resize", intent.getIntExtra("resize", 0));
            args.putInt("timer", intent.getIntExtra("timer", 1));
            args.putBoolean("auto", intent.getBooleanExtra("auto", true));
            editor.putBoolean("repeat", intent.getBooleanExtra("repeat", false));
            editor.apply();

            Camera2BasicFragment camera2BasicFragment = Camera2BasicFragment.newInstance();
            camera2BasicFragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, camera2BasicFragment)
                    .commit();
        }
    }

    public void cameraClose() {
        finish();
    }

}