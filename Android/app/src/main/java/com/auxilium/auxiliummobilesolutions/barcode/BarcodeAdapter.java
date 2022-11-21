package com.auxilium.auxiliummobilesolutions.barcode;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.auxilium.auxiliummobilesolutions.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Andre on 5/5/2017.
 */

public class BarcodeAdapter extends RecyclerView.Adapter<BarcodeAdapter.ViewHolderBarcodes> {

    private List<BarcodeDetails> barcodeDetailsList = new ArrayList<>();

    public class ViewHolderBarcodes extends RecyclerView.ViewHolder {

        private TextView txtBarcode, txtType, txtAmt;

        public ViewHolderBarcodes(View itemView) {
            super(itemView);

            txtBarcode = (TextView) itemView.findViewById(R.id.txtBarcode);
            txtType = (TextView) itemView.findViewById(R.id.txtType);
            txtAmt = (TextView) itemView.findViewById(R.id.txtAmount);
        }
    }

    public BarcodeAdapter (List<BarcodeDetails> barcodeDetailsList) {
        this.barcodeDetailsList = barcodeDetailsList;
    }

    @Override
    public void onBindViewHolder(ViewHolderBarcodes holder, int position) {
        BarcodeDetails curBarcode = barcodeDetailsList.get(position);
        holder.txtBarcode.setText(curBarcode.getBarcode());
        if (curBarcode.getCommodity() != null) {
            holder.txtType.setText(curBarcode.getCommodity());
        }
        if (curBarcode.getValue() != null) {
            holder.txtAmt.setText(curBarcode.getValue().toString());
        }
    }

    @Override
    public int getItemCount() {
        return barcodeDetailsList.size();
    }

    @Override
    public ViewHolderBarcodes onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.barcode_row, parent, false);
        return new ViewHolderBarcodes(view);
    }

}
