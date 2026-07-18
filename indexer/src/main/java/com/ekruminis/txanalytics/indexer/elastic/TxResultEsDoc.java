package com.ekruminis.txanalytics.indexer.elastic;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import com.ekruminis.txanalytics.wire.TxResult;

@Document(indexName = "tx_results")
@Setting(refreshInterval = "30s", replicas = 0)
public class TxResultEsDoc {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String runId;

    @Field(type = FieldType.Keyword)
    private String tfm;

    @Field(type = FieldType.Integer)
    private int height;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant timestamp;

    @Field(type = FieldType.Double)
    private double size;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 100_000_000)
    private double offeredFee;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 100_000_000)
    private double paidFee;

    @Field(type = FieldType.Boolean)
    private boolean confirmed;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 100_000_000)
    private Double burned;

    protected TxResultEsDoc() {
    }

    public static TxResultEsDoc from(TxResult r, Instant timestamp) {
        TxResultEsDoc d = new TxResultEsDoc();
        d.id = r.runId() + ":" + r.txHash() + ":" + r.height();
        d.runId = r.runId();
        d.tfm = r.tfm();
        d.height = r.height();
        d.timestamp = timestamp;
        d.size = r.size();
        d.offeredFee = r.offeredFee();
        d.paidFee = r.paidFee();
        d.confirmed = r.confirmed();
        d.burned = r.burned();
        return d;
    }

    public String getId() { return id; }
}
