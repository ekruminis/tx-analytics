package com.ekruminis.txanalytics.indexer.elastic;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import com.ekruminis.txanalytics.wire.BlockResult;

@Document(indexName = "block_results")
@Setting(refreshInterval = "30s", replicas = 0)
public class BlockResultEsDoc {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String runId;

    @Field(type = FieldType.Keyword)
    private String experimentId;

    @Field(type = FieldType.Keyword)
    private String experimentLabel;

    @Field(type = FieldType.Keyword)
    private String tfm;

    @Field(type = FieldType.Integer)
    private int height;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant timestamp;

    @Field(type = FieldType.Integer)
    private int winnerMinerId;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 100_000_000)
    private double payout;

    @Field(type = FieldType.Double)
    private double size;

    @Field(type = FieldType.Long)
    private long txCount;

    @Field(type = FieldType.Long)
    private long mempoolSize;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 100_000_000)
    private double totalOfferedFee;

    @Field(type = FieldType.Double)
    private Double baseFee;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 100_000_000)
    private Double burned;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 100_000_000)
    private Double pool;

    @Field(type = FieldType.Integer)
    private Integer unconfirmedCount;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 100_000_000)
    private Double poolContribution;

    @Field(type = FieldType.Boolean)
    private Boolean minerTookFromPool;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 100_000_000)
    private Double totalUserPay;

    protected BlockResultEsDoc() {
    }

    public static BlockResultEsDoc from(BlockResult r) {
        BlockResultEsDoc d = new BlockResultEsDoc();
        d.id = r.runId() + ":" + r.height();
        d.runId = r.runId();
        d.experimentId = r.experimentId();
        d.experimentLabel = r.experimentLabel();
        d.tfm = r.tfm();
        d.height = r.height();
        d.timestamp = Instant.ofEpochMilli(r.timestamp());
        d.winnerMinerId = r.winnerMinerId();
        d.payout = r.payout();
        d.size = r.size();
        d.txCount = r.txCount();
        d.mempoolSize = r.mempoolSize();
        d.totalOfferedFee = r.totalOfferedFee();
        d.baseFee = r.baseFee();
        d.burned = r.burned();
        d.pool = r.pool();
        d.unconfirmedCount = r.unconfirmedCount();
        d.poolContribution = r.poolContribution();
        d.minerTookFromPool = r.minerTookFromPool();
        d.totalUserPay = r.totalUserPay();
        return d;
    }

    public String getId() { return id; }
}
