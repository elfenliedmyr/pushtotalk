package com.auxilium.auxiliummobilesolutions.barcode;

/**
 * Created by Andre on 5/5/2017.
 */

public class BarcodeDetails {

    String scancode, comodity;
    Double total;

    public BarcodeDetails(String barcode, String commodity, Double value) {
        this.scancode = barcode;
        this.comodity = commodity;
        this.total = value;
    }

    public String getBarcode() {
        return scancode;
    }

    public void setBarcode(String barcode) {
        this.scancode = barcode;
    }

    public String getCommodity() {
        return comodity;
    }

    public void setCommodity(String commodity) {
        this.comodity = commodity;
    }

    public Double getValue() {
        return total;
    }

    public void setValue(Double value) {
        this.total = value;
    }

    @Override
    public boolean equals(Object obj) {

        if(obj instanceof BarcodeDetails) {
            BarcodeDetails temp = (BarcodeDetails) obj;
            return this.scancode == temp.scancode;
        }
        return false;

    }
    @Override
    public int hashCode() {
        // TODO Auto-generated method stub

        return (this.scancode.hashCode());
    }
}
