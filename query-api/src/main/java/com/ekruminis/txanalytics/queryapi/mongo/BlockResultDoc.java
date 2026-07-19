package com.ekruminis.txanalytics.queryapi.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "block_results")
public class BlockResultDoc {

    @Id
    private String id;
    private String runId;
    private String tfm;
    private int height;
    private long mempoolSize;
    private double totalOfferedFee;
    private Double baseFee;
    private Double burned;
    private Double pool;
    private Integer unconfirmedCount;
    private Double poolContribution;
    private Boolean minerTookFromPool;
    private Double totalUserPay;

    public String getId() { return id; }
    public String getRunId() { return runId; }
    public String getTfm() { return tfm; }
    public int getHeight() { return height; }
    public long getMempoolSize() { return mempoolSize; }
    public double getTotalOfferedFee() { return totalOfferedFee; }
    public Double getBaseFee() { return baseFee; }
    public Double getBurned() { return burned; }
    public Double getPool() { return pool; }
    public Integer getUnconfirmedCount() { return unconfirmedCount; }
    public Double getPoolContribution() { return poolContribution; }
    public Boolean getMinerTookFromPool() { return minerTookFromPool; }
    public Double getTotalUserPay() { return totalUserPay; }
}
