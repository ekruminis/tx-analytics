package com.ekruminis.txanalytics.queryapi.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "tx_results")
public class TxResultDoc {

    @Id
    private String id;
    private String runId;
    private String tfm;
    private int height;
    private String txHash;
    private double size;
    private double offeredFee;
    private double paidFee;
    private boolean confirmed;
    private Double burned;

    public String getId() { return id; }
    public String getRunId() { return runId; }
    public String getTfm() { return tfm; }
    public int getHeight() { return height; }
    public String getTxHash() { return txHash; }
    public double getSize() { return size; }
    public double getOfferedFee() { return offeredFee; }
    public double getPaidFee() { return paidFee; }
    public boolean isConfirmed() { return confirmed; }
    public Double getBurned() { return burned; }
}
