package com.ekruminis.txanalytics.queryapi.postgres;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "experiments")
public class Experiment {

    @Id
    private UUID id;
    private long seed;
    private int numMiners;
    @Column(name = "dataset_hash")
    private String datasetHash;
    private Instant startedAt;

    protected Experiment() {
    }

    public UUID getId() { return id; }
    public long getSeed() { return seed; }
    public int getNumMiners() { return numMiners; }
    public String getDatasetHash() { return datasetHash; }
    public Instant getStartedAt() { return startedAt; }
}
