package com.auxilium.auxiliummobilesolutions;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private static SharedPreferences preferences;
    private static Api api;

    private TextInputLayout txtInputPassword, txtInputUsername;
    private EditText clientView, usernameView, passwordView;
    private TextView versionName;
    private Bundle extras;
    private ImageView imageView;
    private LinearLayout clientLayout;

    private String spoke;

    private int clickCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Get Shared Preferences
        preferences = this.getSharedPreferences("AMS", 0);

        // Get fields
        imageView = findViewById(R.id.imageView);
        clientLayout = findViewById(R.id.clientLayout);
        clientView = findViewById(R.id.client);
        clientView.setSingleLine();
        usernameView = findViewById(R.id.username);
        usernameView.setSingleLine();
        txtInputUsername = findViewById(R.id.txtInputUsername);
        passwordView = findViewById(R.id.password);
        txtInputPassword = findViewById(R.id.txtInputPassword);
        passwordView.setSingleLine();
        passwordView.setTransformationMethod(new PasswordTransformationMethod());
        versionName = findViewById(R.id.txtVersionName);
        versionName.setText("Version: " + BuildConfig.VERSION_NAME);

        // Load bundle
        extras = getIntent().getExtras();

        // Load data to make requests
        String username = preferences.getString("username", "");
        String password = preferences.getString("password", "");
        spoke = preferences.getString("spoke", "");

        if(!spoke.equals("")) {
            api = new Api("https://api.datalynk.ca");
            api.setSpoke(spoke.toLowerCase());
            if (!api.getSpoke().equals(""))
                clientLayout.setVisibility(View.GONE);

            if (preferences.getBoolean("autoLoginEnabled", true)) {
                if (!api.getSpoke().equals("") && api.isLoggedIn())
                    launchWebView();
                else if (!username.equals("") && !password.equals("") && login(username, password))
                    launchWebView();
            }

            // If we can't log you in, autocomplete what we can
            clientView.setText(spoke);
            usernameView.setText(username);
            passwordView.setText(password);
        }

        imageView.setOnClickListener(v -> {
            clickCounter++;
            if (clickCounter == 2) {
                clientLayout.setVisibility(View.VISIBLE);
                clickCounter = 0;
            }
        });
    }

    public boolean login(String username, String password) {
        // Attempt login and highlight any wrong information

        int result = api != null ? api.login(username.replace(" ", ""), password.replace(" ", "")) : 99;
        switch (result) {
            case 0:
                txtInputUsername.setError("This user does not exist");
                return false;
            case 2:
                clientView.setError("This client name does not exist");
                return false;
            case 3:
                AlertDialog.Builder builder;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    builder = new AlertDialog.Builder(LoginActivity.this, android.R.style.Theme_Material_Dialog_Alert);
                } else {
                    builder = new AlertDialog.Builder(LoginActivity.this);
                }
                builder.setTitle("Network Issue")
                        .setMessage("There is a problem connecting to Datalynk servers at this time.")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                return false;
            case -1:
                txtInputPassword.setError("Invalid login credentials");
                return false;
            case 99:
                api = new Api("https://api.datalynk.ca");
                api.setSpoke(spoke.toLowerCase().replace(" ", ""));
                this.login(username, password);
                return false;
        }
        return true;
    }

    public void login(View view) {
        // Check for empty fields
        if (clientView.getText().toString().isEmpty()) {
            clientView.setError("Please enter client name");
            return;
        }
        if (usernameView.getText().toString().isEmpty()) {
            txtInputUsername.setError("Please enter a username or email");
            return;
        }
        if (passwordView.getText().toString().isEmpty()) {
            txtInputPassword.setError("Please enter a password");
            return;
        }

        // Set spoke based on Client Name text field
        String spoke = clientView.getText().toString().replace(" ", "");
        if (spoke.equals(""))
            clientView.setError("Invalid client");
        else {
            preferences.edit().putString("spoke", spoke.toLowerCase()).apply();
            api = new Api("https://api.datalynk.ca");
            api.setSpoke(spoke.toLowerCase());
        }

        // Attempt login
        if (login(
                usernameView.getText().toString().replace(" ", ""),
                passwordView.getText().toString().replace(" ", "")
            )
        ) launchWebView();
    }

    private void launchWebView() {
        Utils.witeObjectToFile(this, api, "authUser");
        savePreferences();
        Intent mainActivity = new Intent(getBaseContext(), MainActivity.class);
        startActivity(mainActivity);
        finish();
    }

    private void savePreferences() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("username", api.getUsername().replace(" ", ""));
        editor.putString("spoke", api.getSpoke().replace(" ", ""));
        editor.putString("firstName", api.getFirstName());
        editor.putString("lastName", api.getLastName());
        editor.putBoolean("login", true);

        if (preferences.getBoolean("autoLoginEnabled", true))
            editor.putString("password", api.getPassword().replace(" ", ""));
        editor.apply();
    }
}
