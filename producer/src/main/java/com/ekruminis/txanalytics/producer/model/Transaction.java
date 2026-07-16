package com.ekruminis.txanalytics.producer.model;

// e.g. {"hash":"5d49d423003e79659b760304927c6f60ba15072a072e44cae5ab5a5a36348272","fee":"0.00006662","size":"195"}
public class Transaction {
    private String hash;
    private String fee;
    private String size;

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getFee() {
        return fee;
    }

    public void setFee(String fee) {
        this.fee = fee;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Transaction{");
        sb.append("hash=").append(hash);
        sb.append(", fee=").append(fee);
        sb.append(", size=").append(size);
        sb.append('}');
        return sb.toString();
    }
}
