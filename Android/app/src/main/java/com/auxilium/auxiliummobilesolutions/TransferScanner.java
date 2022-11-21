package com.auxilium.auxiliummobilesolutions;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.auxilium.auxiliummobilesolutions.barcode.BarcodeAdapter;
import com.auxilium.auxiliummobilesolutions.barcode.BarcodeDetails;
import com.auxilium.auxiliummobilesolutions.barcode.BarcodeRecyclerItemListener;
import com.google.gson.Gson;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.BeepManager;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Andre on 5/12/2017.
 */

public class TransferScanner extends Activity {

    private DecoratedBarcodeView barcodeView;
    private BeepManager beepManager;
    private String lastScan;
    private TextView txtTransfercode;
    private Button btnConfirm, btnFinish, btnRescan;
    private Boolean confirm = false;
    private static Api api;

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private BarcodeAdapter ba;
    private List<BarcodeDetails> barcodeList;

    public BarcodeDetails newBarcode;
    public BarcodeResult barcodeResult;

    private BarcodeCallback callbackxx = new BarcodeCallback() {
        @Override
        public void barcodeResult(final BarcodeResult result) {
            if (!confirm) {
                // FIRST STEP
                // SCAN BIG BAG AND CONFIRM SCAN
                if (result.getText() == null || result.getText().equals(lastScan)) {
                    // Prevent duplicate scans
                    return;
                }
                lastScan = result.getText();
                txtTransfercode.setText(lastScan);
                btnFinish.setVisibility(View.VISIBLE);
                confirm = true;
                api.request("{'$/slice/xinsert':{" +
                        "slice: 51376," +
                        "rows:[" +
                            "{scan:\"" + txtTransfercode.getText() + "\"}" +
                        "]" +
                        "}}");
            } else {

                barcodeResult = result;

                if (result.getText() == null || result.getText().equals(txtTransfercode.getText()) || contains(barcodeList, result.getText())) {
                    return;
                }

                barcodeView.pause();
                lastScan = result.getText();
                barcodeView.setStatusText(result.getText());
                beepManager.playBeepSoundAndVibrate();

                addBarcode(result.toString());

            }

        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);

        // Get API
        Bundle extras =  getIntent().getExtras();
        api = (Api)extras.getSerializable("api");

        barcodeView = findViewById(R.id.transfer_scanner);
        barcodeView.decodeContinuous(callbackxx);
        beepManager = new BeepManager(this);

        txtTransfercode = findViewById(R.id.txtTransferScancode);
        btnConfirm = findViewById(R.id.btnConfirm);
        btnFinish = findViewById(R.id.btnFinish);
        btnRescan = findViewById(R.id.btnRescan);

        updateCount();

        barcodeList = new ArrayList<>();
        mRecyclerView = findViewById(R.id.lstTransfercodes);
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

        btnFinish.setOnClickListener(v -> {
            String json = new Gson().toJson(barcodeList);

            // STORE BARCODE DATA
            SharedPreferences.Editor editor = getSharedPreferences("AMS", 0).edit();
            editor.putString("transferCode", txtTransfercode.getText().toString());
            editor.putString("barcodeData", json.toString());
            editor.apply();
            finish();
        });
    }

    public boolean contains(List<BarcodeDetails> list, String name) {
        for (BarcodeDetails item : list) {
            if (item.getBarcode().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public void addBarcode(String barcode) {
        if (!contains(barcodeList, barcode)) {
            newBarcode = new BarcodeDetails(barcode, null, null);
            barcodeList.add(0, newBarcode);
            ba.notifyDataSetChanged();
            updateCount();
        }
        barcodeView.resume();
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

    public void updateCount() {
        TextView tV = (TextView) findViewById(R.id.txtTransferBarcodeCount);
        Integer count = 0;
        if (barcodeList != null) count = barcodeList.size();
        if (count == 1) {
            tV.setText("1 barcode scanned");
        } else {
            tV.setText(count + " barcode's scanned");
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }
}
