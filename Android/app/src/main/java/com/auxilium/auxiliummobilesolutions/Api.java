package com.auxilium.auxiliummobilesolutions;

import android.os.StrictMode;
import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;

class Api implements Serializable {

    private static final String TAG = "API";
    private static final String server = "live";

    private String url, spoke, landingPage, username, password, token, firstName, lastName, tmpUrl;
    private String display = null;
    private int userId = -1;

    private int reqCount = (int)(Math.random() * 1000);

    public Boolean developement = false;

    Api(String url) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        this.url = url;
    }
    Api(String url, String username, String password, String spoke, int userId, String token, String landingPage){
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        this.url = url;
        this.spoke = spoke;
        this.username = username;
        this.password = password;
        this.userId = userId;
        this.token = token;
        this.landingPage = landingPage;
    }

    Boolean getDevelopement() {return this.developement;}

    void setDevelopement(Boolean developement) {this.developement = developement;}

    String getSpoke() { return this.spoke != null ? this.spoke.toLowerCase() : null; }

    int getUserId() {
        return this.userId;
    }

    void setUserId(int userId){
        this.userId = userId;
    }

    String getLandingPage() { return this.landingPage; }

    String getUsername() { return this.username; }

    String getPassword() { return this.password; }

    String getToken() { return this.token; }

    void setToken(String token) { this.token = token; }

    String getFirstName() { return this.firstName; }

    String getDisplay() { return this.display; }

    String getLastName() { return this.lastName; }

    String getServer() { return this.server; }

    public String getTmpUrl() {
        return tmpUrl;
    }

    public void setTmpUrl(String tmpUrl) {
        this.tmpUrl = tmpUrl;
    }

    boolean isLoggedIn() {
        try {
            // Send request
            String api_resp = request("{\"$/env/me\":{}}");
            if (api_resp != null && !api_resp.equals("")) {
                JSONObject response = new JSONObject(api_resp);

                // If the response email is guest@datalynk.ca, its a guest account and not logged in
                if (response.has("email") && !response.getString("email").equals("guest@datalynk.ca")) {
                    this.userId = response.getInt("id");
                    this.firstName = response.getString("first_name");
                    this.lastName = response.getString("last_name");
                    if (response.has("mobileappname")) {
                        this.display = response.getString("mobileappname");
                    }
                    if (response.has("landingpage")) {
                        this.landingPage = response.getString("landingpage");
                    }
                    return true;
                }
            }
        } catch (JSONException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
        this.userId = -1;
        return false;
    }

    void setSpoke(String spoke) {
        this.spoke = spoke;
    }

    int login(String username, String password) {
        if (username == null || username.equals("")) {
            return 0;
        } else if (password == null || password.equals("")) {
            return -1;
        } else if (!this.isOnline()) {
            return 3;
        }

        reqCount++;
        try {
            String url = this.url + "/login";
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");
            con.setRequestMethod("POST");

            JSONObject data = new JSONObject();
            data.put("login", username);
            data.put("password", password);
            data.put("realm", spoke);

            OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
            wr.write(data.toString());
            wr.flush();
            wr.close();
            con.connect();

            StringBuilder sb = new StringBuilder();
            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                br.close();
                FirebaseCrashlytics.getInstance().setCustomKey(TAG, "LoginResponse (" + reqCount + "): " + sb.toString());

                JSONObject request = new JSONObject(sb.toString());
                if (!request.has("error")) {
                    this.username = username;
                    this.password = password;
                    this.token = request.get("token").toString();
                    isLoggedIn();
                    return 1;
                    // If we get an unknown user send back 0 for not found
                } else if (request.get("error").toString().contains("No connections defined for")) {
                    return 2;
                } else if (request.get("error").toString().contains("Unknown user")) {
                    return 0;
                } else if (request.get("error").toString().contains("connection-error")) {
                    return 3;
                }
            } else if (con.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                return -1;
            } else {
                FirebaseCrashlytics.getInstance().setCustomKey(TAG, "Error (" + reqCount + "): " + con.getResponseCode() + " -> " + con.getResponseMessage());
//                return "Status: " + con.getResponseCode();
            }
        } catch (ConnectTimeoutException bug) {
            FirebaseCrashlytics.getInstance().recordException(bug);
//            return "{\"error\":\"connection-error\"}";
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(new Exception("Error (" + reqCount + "): " + e.getLocalizedMessage() + " -> " + e.getMessage() + " -> " + e.toString()));
            e.printStackTrace();
//            return null;
        }
        return -1;
    }

    public boolean isOnline() {
        try {
            InetAddress ipAddr = InetAddress.getByName("69.90.202.100");
            return !ipAddr.equals("");
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(new Exception("Error " + e.getLocalizedMessage() + " -> " + e.getMessage() + " -> " + e.toString()));
            return false;
        }
    }

    void updateFCMToken(String token) {
        if(token == null) request("{\"$/env/users/xupdate\":{\"rows\":[{\"communicationid\":\"null\",\"phonetype\":\"Android\",\"id\":\"" + userId + "\"}]}}");
        else request("{\"$/env/users/xupdate\":{\"rows\":[{\"communicationid\":\"" + token + "\",\"phonetype\":\"Android\",\"id\":\"" + userId + "\"}]}}");
    }

    void updatePhone(String phone) {
        if(phone == null || phone == "") request("{\"$/env/users/xupdate\":\"rows\":[{\"mobile\":\"null\",\"phonetype\":\"Android\",\"id\":\"" + userId + "\"}]}}");
        else request("{\"$/env/users/xupdate\":{\"rows\":[{\"mobile\":\"" + phone + "\",\"phonetype\":\"Android\",\"id\":\"" + userId + "\"}]}}");
    }

    void logout() {
        request("{\"$/auth/logout\":{}}", true);
    }

    String request(String request) {
        return request(request, false);
    }

    private String request(String request, boolean cookie) {
        reqCount++;
        try {
            FirebaseCrashlytics.getInstance().setCustomKey(TAG, "Request (" + reqCount + "): " + request + " url: " + this.url);
            HttpURLConnection con = (HttpURLConnection) new URL(this.url).openConnection();
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("Authorization", "Bearer " + this.token);
            con.setRequestMethod("POST");
            OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
            wr.write(request);
            wr.flush();
            wr.close();

            StringBuilder sb = new StringBuilder();
            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                br.close();
                FirebaseCrashlytics.getInstance().setCustomKey(TAG, "Response (" + reqCount + "): " + sb.toString());
                return sb.toString();
            } else if (con.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                FirebaseCrashlytics.getInstance().setCustomKey(TAG, "Unauthorized (" + reqCount + ")");
                return "Unauthorized";
            } else {
                FirebaseCrashlytics.getInstance().setCustomKey(TAG, "Error (" + reqCount + "): " + con.getResponseCode() + " -> " + con.getResponseMessage());
                return "Status: " + con.getResponseCode();
            }
        } catch (ConnectTimeoutException bug) {
            FirebaseCrashlytics.getInstance().recordException(bug);
            return "{\"error\":\"connection-error\"}";
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(new Exception("Error (" + reqCount + "): " + e.getLocalizedMessage() + " -> " + e.getMessage() + " -> " + e.toString()));
            e.printStackTrace();
            return null;
        }
    }
}