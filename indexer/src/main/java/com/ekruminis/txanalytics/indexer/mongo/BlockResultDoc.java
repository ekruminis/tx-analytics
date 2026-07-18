package com.ekruminis.txanalytics.indexer.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.ekruminis.txanalytics.wire.BlockResult;

@Document(collection = "block_results")
@CompoundIndexes({
        @CompoundIndex(name = "run_height_uq", def = "{'runId': 1, 'height': 1}", unique = true)
})
public class BlockResultDoc {

    @Id
    private String id;

    private String runId;

    @Indexed
    private String tfm;

    private int height;
    private int winnerMinerId;
    private double payout;
    private double size;
    private long txCount;
    private long mempoolSize;
    private double totalOfferedFee;
    private String merkleRoot;

    private Double baseFee;
    private Double burned;
    private Double pool;
    private Integer unconfirmedCount;
    private Double poolContribution;
    private Boolean minerTookFromPool;
    private Double totalUserPay;

    protected BlockResultDoc() {
    }

    public static BlockResultDoc from(BlockResult r) {
        BlockResultDoc doc = new BlockResultDoc();
        doc.id = r.runId() + ":" + r.height();
        doc.runId = r.runId();
        doc.tfm = r.tfm();
        doc.height = r.height();
        doc.winnerMinerId = r.winnerMinerId();
        doc.payout = r.payout();
        doc.size = r.size();
        doc.txCount = r.txCount();
        doc.mempoolSize = r.mempoolSize();
        doc.totalOfferedFee = r.totalOfferedFee();
        doc.merkleRoot = r.merkleRoot();
        doc.baseFee = r.baseFee();
        doc.burned = r.burned();
        doc.pool = r.pool();
        doc.unconfirmedCount = r.unconfirmedCount();
        doc.poolContribution = r.poolContribution();
        doc.minerTookFromPool = r.minerTookFromPool();
        doc.totalUserPay = r.totalUserPay();
        return doc;
    }

    public String getId() { return id; }
    public String getRunId() { return runId; }
    public String getTfm() { return tfm; }
    public int getHeight() { return height; }
    public int getWinnerMinerId() { return winnerMinerId; }
    public double getPayout() { return payout; }
    public double getSize() { return size; }
    public long getTxCount() { return txCount; }
    public long getMempoolSize() { return mempoolSize; }
    public double getTotalOfferedFee() { return totalOfferedFee; }
    public String getMerkleRoot() { return merkleRoot; }
    public Double getBaseFee() { return baseFee; }
    public Double getBurned() { return burned; }
    public Double getPool() { return pool; }
    public Integer getUnconfirmedCount() { return unconfirmedCount; }
    public Double getPoolContribution() { return poolContribution; }
    public Boolean getMinerTookFromPool() { return minerTookFromPool; }
    public Double getTotalUserPay() { return totalUserPay; }

    @Override
    public String toString() {
        return "BlockResultDoc [id=" + id + ", runId=" + runId + ", tfm=" + tfm + ", height=" + height
                + ", winnerMinerId=" + winnerMinerId + ", payout=" + payout + ", size=" + size + ", txCount=" + txCount
                + ", mempoolSize=" + mempoolSize + ", totalOfferedFee=" + totalOfferedFee + ", merkleRoot=" + merkleRoot
                + ", baseFee=" + baseFee + ", burned=" + burned + ", pool=" + pool + ", unconfirmedCount="
                + unconfirmedCount + ", poolContribution=" + poolContribution + ", minerTookFromPool="
                + minerTookFromPool + ", totalUserPay=" + totalUserPay + "]";
    }
}
