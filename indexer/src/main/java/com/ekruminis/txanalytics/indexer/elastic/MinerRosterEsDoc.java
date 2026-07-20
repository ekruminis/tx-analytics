package com.ekruminis.txanalytics.indexer.elastic;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import com.ekruminis.txanalytics.wire.MinerRoster;

@Document(indexName = "miner_roster")
@Setting(refreshInterval = "5s", replicas = 0)
public class MinerRosterEsDoc {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String experimentId;

    @Field(type = FieldType.Integer)
    private int minerId;

    @Field(type = FieldType.Integer)
    private int stake;

    @Field(type = FieldType.Double)
    private double stakePct;

    @Field(type = FieldType.Integer)
    private int totalStake;

    @Field(type = FieldType.Integer)
    private int numMiners;

    protected MinerRosterEsDoc() {
    }

    public static MinerRosterEsDoc from(MinerRoster roster, MinerRoster.Entry entry) {
        MinerRosterEsDoc d = new MinerRosterEsDoc();
        d.id = roster.experimentId() + ":" + entry.minerId();
        d.experimentId = roster.experimentId();
        d.minerId = entry.minerId();
        d.stake = entry.stake();
        d.stakePct = entry.stakePct();
        d.totalStake = roster.totalStake();
        d.numMiners = roster.numMiners();
        return d;
    }

    public String getId() { return id; }
}
