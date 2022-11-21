package com.auxilium.auxiliummobilesolutions.barcode;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.auxilium.auxiliummobilesolutions.R;
import com.google.gson.Gson;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.BeepManager;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Andre on 5/4/2017.
 */

public class BarcodeScanner extends Activity {

    private static final String TAG = BarcodeScanner.class.getSimpleName();
    private DecoratedBarcodeView barcodeView;
    private BeepManager beepManager;

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private BarcodeAdapter ba;
    private List<BarcodeDetails> barcodeList;

    public BarcodeDetails newBarcode;
    public BarcodeResult barcodeResult;
    public String lastScan;

    public Boolean forceclose;
    static boolean active = false;



    private DialogInterface.OnDismissListener dialogDismiss = new DialogInterface.OnDismissListener() {
        @Override
        public void onDismiss(DialogInterface dialog) {
            barcodeView.resume();
        }
    };

    private BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(final BarcodeResult result) {
            if (!active) return;

            barcodeResult = result;
            if (result.getText() == null || contains(barcodeList, result.getText())) { return; }
            barcodeView.pause();
            lastScan = result.getText();
            barcodeView.setStatusText(result.getText());
            beepManager.playBeepSoundAndVibrate();
            addBarcode(result.toString());

//            // Popup for barcode details
//            final AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(BarcodeScanner.this, R.style.myDialog));
//            final View mView = getLayoutInflater().inflate(R.layout.barcode_details, null);
//            builder.setView(mView);
//            builder.setTitle(result.getText());
//            builder.setNeutralButton("Rescan", (dialog, which) -> {
//                //
//            });
//            builder.setNegativeButton("Cancel", (dialog, which) -> {
//                finish();
//            });
//            builder.setPositiveButton("Confirm", (dialog, which) -> {
//                addBarcode(result.toString());
//            });
//            AlertDialog dialog = builder.create();
//            dialog.setOnDismissListener(dialogDismiss);
//            dialog.show();
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
        }
    };

    public void addBarcode(String barcode) {
        if (!contains(barcodeList, barcode)) {
            newBarcode = new BarcodeDetails(barcode, null, null);
            barcodeList.add(0, newBarcode);
//            ba.notifyDataSetChanged();
//            updateCount();

            if (forceclose) {
                String json = new Gson().toJson(barcodeList);
                SharedPreferences.Editor editor1 = getSharedPreferences("AMS", 0).edit();
                editor1.putString("barcodeData", json);
                editor1.apply();
                finish();
            }
        }
    }

    public void updateCount() {
        TextView tV = findViewById(R.id.txtBarcodeCount);
        Integer count = barcodeList.size();
        if (count == 1) {
            tV.setText(barcodeList.size() + " barcode scanned");
        } else {
            tV.setText(barcodeList.size() + " barcode's scanned");
        }
    }

    public boolean contains(List<BarcodeDetails> list, String name) {
        for (BarcodeDetails item : list) {
            if (item.getBarcode().equals(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode);

        // Get API
        Bundle extras =  getIntent().getExtras();
        forceclose = extras.getBoolean("forceclose", false);

        // CLEAR WHATEVER DATA WAS HERE BEFORE
        SharedPreferences.Editor editor = getSharedPreferences("AMS", 0).edit();
        editor.putString("barcodeData", null);
        editor.apply();

        barcodeView = findViewById(R.id.barcode_scanner);
        barcodeView.decodeContinuous(callback);
        beepManager = new BeepManager(this);

        barcodeList = new ArrayList<>();
        mRecyclerView = findViewById(R.id.lstBarcodes);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mLayoutManager);

        ba = new BarcodeAdapter(barcodeList);
        mRecyclerView.setAdapter(ba);
        mRecyclerView.addOnItemTouchListener(new BarcodeRecyclerItemListener(getApplicationContext(), mRecyclerView, new BarcodeRecyclerItemListener.RecyclerTouchListener(){

            @Override
            public void onClickItem(View v, int position) {

                // EDIT RECORD
            }

            @Override
            public void onLongClickItem(View v, int position) {

                // DELETE RECORD
                barcodeList.remove(position);
                ba.notifyDataSetChanged();
                updateCount();
            }
        }));

        TextView tV = findViewById(R.id.txtBarcodeCount);
        tV.setText("0 barcode's scanned");

        Button btnSend = findViewById(R.id.btnSend);
        if (forceclose) {
            btnSend.setVisibility(View.GONE);
            return;
        }
        else btnSend.setVisibility(View.VISIBLE);
        btnSend.setOnClickListener(v -> {
            String json = new Gson().toJson(barcodeList);

            // STORE BARCODE DATA
            SharedPreferences.Editor editor1 = getSharedPreferences("AMS", 0).edit();
            editor1.putString("barcodeData", json);
            editor1.apply();
            finish();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        active = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        active = false;
    }

    @Override
    protected void onResume() {
        super.onResume();

        barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        barcodeView.pause();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }
}
