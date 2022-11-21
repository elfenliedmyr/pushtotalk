package com.auxilium.auxiliummobilesolutions;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.*;
import android.webkit.*;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class FragmentWebPortal extends Fragment {
    // App Required
    private Api api;
    private SharedPreferences preferences;
    private static final String TAG = "FragmentWebPortal";

    // Webview
//    String urlOverride = "http://caerulamar.datalynk:4200/";
    String urlOverride;
    private WebView webView;
    private boolean clearHistory = false;

    // Location Variables
    private boolean askGPSPermission = false;

    // Photo Variables
    private String mCM;
    private Uri mCameraUri;
    private ValueCallback<Uri> mUM;
    private ValueCallback<Uri[]> mUMA;
    private final static int FCR=1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_webfragment, menu);

        SharedPreferences preferences = this.getActivity().getSharedPreferences("AMS", 0);
        if (preferences.getBoolean("hideRefresh", false)) {
            menu.findItem(R.id.refresh).setVisible(false);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.refresh && webView != null) {
            if (webView.getUrl().equals(api.getLandingPage())) webView.reload();
            else {
                webView.loadUrl(api.getLandingPage());
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        getFragmentManager().addOnBackStackChangedListener(() -> {
            if (getFragmentManager() == null) return;
            Fragment fr = getFragmentManager().findFragmentById(R.id.content_frame);
            if (fr instanceof FragmentWebPortal) {
                setHasOptionsMenu(true);
            } else {
                setHasOptionsMenu(false);
            }
        });
        return inflater.inflate(R.layout.fragment_app_webview, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        preferences = getActivity().getSharedPreferences("AMS", 0);

        // Get Api
        this.api = (Api) Utils.readObjectFromFile(getActivity(), "authUser");

        // Check for mobile phone number
        phoneNumberCheck();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (Build.VERSION.SDK_INT >= 21) {
            Uri[] results = null;
            //Check if response is positive

            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == FCR) {
                    if (null == mUMA) return;
                    if (intent == null || intent.getData() == null) {
                        //Capture Photo if no image available
                        if (mCM != null) results = new Uri[]{Uri.parse(mCM)};
                        else if (mCameraUri != null) {
                            results = new Uri[]{mCameraUri};
                        }
                    } else {
                        String dataString = intent.getDataString();
                        if (dataString != null) results = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }
            mUMA.onReceiveValue(results);
            mUMA = null;
        } else {
            if (requestCode == FCR) {
                if (null == mUM) return;
                Uri result = intent == null || resultCode != -1 ? null : intent.getData();
                mUM.onReceiveValue(result);
                mUM = null;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        Api oldApi = api;
        api = (Api) Utils.readObjectFromFile(getActivity(), "authUser");

        // REFRESH THE WEBVIEW IF DIFFERENT USER SIGNING IN, FROM AN INITIAL LOGIN SESSION, OR RELOAD SETTING IS ENABLED
        if (oldApi != null && oldApi.getUsername() != null && !oldApi.getUsername().equals(api.getUsername()) ||
            preferences.getBoolean("login", false) ||
            preferences.getBoolean("refreshWebview", false)) {

            String tmpUrl = preferences.getString("tmpUrl", "");
            if (!tmpUrl.equals("")) urlOverride = tmpUrl;

            this.setupWebView();
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("login", false);
            editor.putBoolean("refreshWebview", preferences.getBoolean("refreshWebview", false));
            editor.remove("tmpUrl");
            urlOverride = null;
            editor.apply();
        }

        String barcodeData = preferences.getString("barcodeData", null);
        String transferCode = preferences.getString("transferCode", null);
        String punchFile = preferences.getString("punchFile", null);
        boolean cameraRepeat = preferences.getBoolean("repeat", false);

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("punchFile", null);
        editor.putBoolean("cameraRepeat", false);
        editor.apply();

        if (punchFile != null){

            // Load image from path
            File image = new File(punchFile);

            if (image.exists()) {
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                Bitmap imgToUpload = BitmapFactory.decodeFile(image.getAbsolutePath(), bmOptions);

                // Convert to byteArary
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                imgToUpload.compress(Bitmap.CompressFormat.JPEG, 75, bos);
                byte[] bArray = bos.toByteArray();
                String blobString = Base64.encodeToString(bArray, Base64.DEFAULT);

                webView.loadUrl("javascript:my.namespace.publicFunc('" + blobString + "', '"+cameraRepeat+"')");
            }
        }
        if (barcodeData != null) {
            // SEND DATA TO WEBVIEW
            String barcodes, transfer;
            try {
                barcodes = URLEncoder.encode(barcodeData,"UTF-8");
                if (transferCode != null) {
                    transfer = URLEncoder.encode(transferCode,"UTF-8");
                    webView.loadUrl("javascript:my.namespace.publicFunc('" + barcodes + "', '" + transfer + "');");
                }
                else webView.loadUrl("javascript:my.namespace.publicFunc('" + barcodes + "');");

                // CLEAR WHATEVER DATA WAS HERE BEFORE
                editor.putString("transferCode", null);
                editor.putString("barcodeData", null);
                editor.apply();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        // Theme the Toolbar based on User Account
        Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
        if (toolbar != null) toolbar.setTitle(R.string.app_title);
        try {
            String api_resp = this.api.request("{\"$/env/me\":{}}");
            if (api_resp != null && !api_resp.equals("")) {
                JSONObject response = new JSONObject(this.api.request(api_resp));
                if (response.has("mobileappname") && toolbar != null)
                    toolbar.setTitle(response.getString("mobileappname"));
                if (response.has("mobileappcolor")
                    && !response.getString("mobileappcolor").equals("")
                    && toolbar != null) {
                    String color = (response.getString("mobileappcolor").contains("#")) ?
                        response.getString("mobileappcolor") :
                        "#"+response.getString("mobileappcolor");
                    toolbar.setBackground(
                        new ColorDrawable(
                            Color.parseColor(color)
                        )
                    );
                }
                else if (toolbar != null) toolbar
                    .setBackground(new ColorDrawable(Color.parseColor("#212121")));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(
                new Exception("Error " + e.getLocalizedMessage() + " -> " + e.getMessage() + " -> " + e.toString())
            );
        }
    }

    public boolean canGoBack() {
        boolean goBack = webView != null && webView.canGoBack();
        if (goBack) webView.goBack();
        return goBack;
    }

    private boolean checkPermissions() {
        return  PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(getActivity(),
            Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private File createImageFile() throws IOException {
        @SuppressLint("SimpleDateFormat")
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "img_"+timeStamp+"_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName,".jpg",storageDir);
    }

    private void phoneNumberCheck() {
        String phone = preferences.getString("phone", null);

        // Request Phone Number
        if(phone == null || phone.equals("")){
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Attention");
            builder.setMessage("Please enter your mobile phone number");

            final EditText input = new EditText(getActivity());
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            builder.setView(input);

            builder.setPositiveButton("OK", (dialog, which) -> {
                // Store entered value
                SharedPreferences.Editor editor = preferences.edit();
                String phoneNumber = input.getText().toString();
                editor.putString("phone", phoneNumber);
                editor.apply();

                // Send phone number to server
                api.updatePhone(phoneNumber);
            });

            builder.show();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        // Retrieve WebView
        webView = Objects.requireNonNull(getView()).findViewById(R.id.webView);
        if (webView == null) return;

        webView.getSettings().setAppCacheMaxSize(1024*1024*8);
        webView.getSettings().setAppCachePath(getActivity().getFilesDir().getPath());
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setGeolocationEnabled(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setSupportZoom(true);

        webView.addJavascriptInterface(new WebInterface(getActivity()), "AMS");

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            String[] urlExp = url.split("&name=");
            String fileName = null;

            try { fileName = URLDecoder.decode(urlExp[1], "UTF-8"); }
            catch (UnsupportedEncodingException e) { e.printStackTrace(); }

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimeType);
//            String cookies = CookieManager.getInstance().getCookie(url);
//            request.addRequestHeader("cookie", cookies);
            request.addRequestHeader("User-Agent", userAgent);
            request.setDescription("Downloading " + fileName);
            request.setTitle(fileName);
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            DownloadManager dm = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
            assert dm != null;
            dm.enqueue(request);
            Toast.makeText(getActivity(), "Downloading " + fileName, Toast.LENGTH_LONG).show();
        });

        //noinspection unused
        webView.setWebChromeClient(new WebChromeClient(){
            //For Android 4.1+
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture){
                mUM = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                startActivityForResult(Intent.createChooser(i, "File Chooser"), FCR);
            }
            //For Android 5.0+
            public boolean onShowFileChooser(
                WebView webView, ValueCallback<Uri[]> filePathCallback,
                WebChromeClient.FileChooserParams fileChooserParams){

                if(mUMA != null){
                    mUMA.onReceiveValue(null);
                }
                mUMA = filePathCallback;
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if(takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {

                    File photoFile = null;
                    Uri photoUri = null;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Android Q compatibility
                        photoUri = createImageUri();
                        mCameraUri = photoUri;
                        if (photoUri != null) {
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                        }
                    } else {
                        try {
                            photoFile = createImageFile();
                            takePictureIntent.putExtra("PhotoPath", mCM);
                        } catch (IOException ex) {
                            Log.e(TAG, "Image file creation failed", ex);
                        }
                        if (photoFile != null) {
                            mCM = "file:" + photoFile.getAbsolutePath();
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                        } else {
                            takePictureIntent = null;
                        }
                    }
                }

                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("image/*");
                Intent[] intentArray;
                intentArray = (takePictureIntent != null) ? new Intent[]{takePictureIntent} : new Intent[0];

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                startActivityForResult(chooserIntent, FCR);
                return true;
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin,
                GeolocationPermissions.Callback callback) {
                if (!checkPermissions()) askGPSPermission = true;
                else callback.invoke(origin, true, false);
            }
        });

        // Get URL passed into activity
        String url;
        if (urlOverride == null || urlOverride.isEmpty()) url = api.getLandingPage() != null &&
                !api.getLandingPage().equals("") ?
                api.getLandingPage() : "file:///android_asset/www/index.html";
        else url = urlOverride;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        }

        // Tell WebView to load URL
        new android.os.Handler().postDelayed(() -> webView.loadUrl(url), 500);

        // Logic to decide whether url should be handled by webview or externally
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                String key = api.getDevelopement() ? "authorization-http://api/" :
                        "authorization-https://api.datalynk.ca";
                String val = api.getToken();
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT &&
                        webView != null)
                    webView.evaluateJavascript("localStorage.setItem('"+ key +"','"+ val +"');", null);
                else if (webView != null)
                    webView.loadUrl("javascript:localStorage.setItem('"+ key +"','"+ val +"');");
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (clearHistory && webView != null) {
                    clearHistory = false;
                    webView.clearHistory();
                }

                String key = api.getDevelopement() ? "authorization-http://api/" :
                        "authorization-https://api.datalynk.ca";
                String val = api.getToken();
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT &&
                        webView != null)
                    webView.evaluateJavascript("localStorage.setItem('"+ key +"','"+ val +"');", null);
                else if (webView != null)
                    webView.loadUrl("javascript:localStorage.setItem('"+ key +"','"+ val +"');");

                if (Build.VERSION.SDK_INT >= 21) {
                    if (ContextCompat.checkSelfPermission(view.getContext(),
                            Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                        ActivityCompat.requestPermissions(getActivity(), new String[] {
                                Manifest.permission.CAMERA,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE }, 0);
                    if (ContextCompat.checkSelfPermission(view.getContext(),
                            Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)
                        ActivityCompat.requestPermissions(getActivity(), new String[] {
                                Manifest.permission.INTERNET}, 0);
                    if (askGPSPermission && ContextCompat.checkSelfPermission(view.getContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                        ActivityCompat.requestPermissions(getActivity(), new String[] {
                                Manifest.permission.ACCESS_FINE_LOCATION}, 0);

                    if (webView == null) return;
                    webView.getSettings().setMixedContentMode(0);
                    webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                }
                else if (Build.VERSION.SDK_INT >= 19 && webView != null)
                    webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }

            // Takes care of all api versions greater than 23
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request,
                                            WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);

                // Attempt to revalidate the session on an invalidated session
                if (errorResponse.getStatusCode() == 401) {
                    preferences = getActivity().getSharedPreferences("AMS", 0);
                    String username = preferences.getString("username", "");
                    String password = preferences.getString("password", "");

                    int result = api != null ? api.login(
                            username.replace(" ", ""),
                            password.replace(" ", "")) : 99;

                    if (result == 1) webView.reload();
                }
            }

            // Takes care of versions less than api version 23
            @Override
            public void onReceivedError(WebView view, int errorCode, String description,
                                        String failingUrl) {

                // Attempt to revalidate the session on an invalidated session
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP & errorCode == 401) {
                    preferences = getActivity().getSharedPreferences("AMS", 0);
                    String username = preferences.getString("username", "");
                    String password = preferences.getString("password", "");

                    int result = api != null ? api.login(
                            username.replace(" ", ""),
                            password.replace(" ", "")) : 99;

                    if (result == 1) webView.reload();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading (WebView view, String url) {
                if (url.startsWith("tel:")) {
                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                    startActivity(intent);
                    view.reload();
                    return true;
                }
                if(url.startsWith("mailto:")){
                    Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
                    startActivity(i);
                    return true;
                }
                return false;
            }
        });
    }
    private Uri createImageUri() {
        String status = Environment.getExternalStorageState();
        if (getActivity() == null) return null;
        if (status.equals(Environment.MEDIA_MOUNTED)){
            return getActivity().getApplicationContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues());
        } else {
            return getActivity().getApplicationContext().getContentResolver().insert(MediaStore.Images.Media.INTERNAL_CONTENT_URI, new ContentValues());
        }
    }
}
