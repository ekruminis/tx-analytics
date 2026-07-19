package com.ekruminis.txanalytics.queryapi.postgres;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "miners")
public class Miner {

    @Id
    private Long id;
    @Column(name = "experiment_id")
    private UUID experimentId;
    @Column(name = "miner_id")
    private int minerId;
    private int stake;
    @Column(name = "stake_pct")
    private double stakePct;

    protected Miner() {
    }

    public Long getId() { return id; }
    public UUID getExperimentId() { return experimentId; }
    public int getMinerId() { return minerId; }
    public int getStake() { return stake; }
    public double getStakePct() { return stakePct; }
}
