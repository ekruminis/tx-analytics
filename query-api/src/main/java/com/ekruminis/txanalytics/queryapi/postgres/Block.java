package com.ekruminis.txanalytics.queryapi.postgres;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "blocks")
public class Block {

    @Id
    private Long id;
    @Column(name = "run_id")
    private UUID runId;
    private int height;
    @Column(name = "winner_miner_id")
    private Integer winnerMinerId;
    private String parentHash;
    private String currentHash;
    @Column(name = "merkle_root")
    private String merkleRoot;
    private double size;
    private long txCount;
    private BigDecimal minerPayout;

    protected Block() {
    }

    public Long getId() { return id; }
    public UUID getRunId() { return runId; }
    public int getHeight() { return height; }
    public Integer getWinnerMinerId() { return winnerMinerId; }
    public String getParentHash() { return parentHash; }
    public String getCurrentHash() { return currentHash; }
    public String getMerkleRoot() { return merkleRoot; }
    public double getSize() { return size; }
    public long getTxCount() { return txCount; }
    public BigDecimal getMinerPayout() { return minerPayout; }
}
