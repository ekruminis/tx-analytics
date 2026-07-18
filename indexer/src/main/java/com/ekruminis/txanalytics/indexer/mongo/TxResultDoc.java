package com.ekruminis.txanalytics.indexer.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.ekruminis.txanalytics.wire.TxResult;

@Document(collection = "tx_results")
@CompoundIndexes({
        @CompoundIndex(name = "run_txhash", def = "{'runId': 1, 'txHash': 1}")
})
public class TxResultDoc {

    @Id
    private String id;

    private String runId;

    @Indexed
    private String tfm;

    private int height;

    @Indexed
    private String txHash;

    private double size;
    private double offeredFee;
    private double paidFee;
    private boolean confirmed;
    private Double burned;

    protected TxResultDoc() {
    }

    public static TxResultDoc from(TxResult r) {
        TxResultDoc d = new TxResultDoc();
        d.id = r.runId() + ":" + r.txHash() + ":" + r.height();
        d.runId = r.runId();
        d.tfm = r.tfm();
        d.height = r.height();
        d.txHash = r.txHash();
        d.size = r.size();
        d.offeredFee = r.offeredFee();
        d.paidFee = r.paidFee();
        d.confirmed = r.confirmed();
        d.burned = r.burned();
        return d;
    }

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

    @Override
    public String toString() {
        return "TxResultDoc [id=" + id + ", runId=" + runId + ", tfm=" + tfm + ", height=" + height + ", txHash="
                + txHash + ", size=" + size + ", offeredFee=" + offeredFee + ", paidFee=" + paidFee + ", confirmed="
                + confirmed + ", burned=" + burned + "]";
    }
}
