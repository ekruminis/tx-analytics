package com.ekruminis.txanalytics.queryapi.postgres;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "simulation_runs")
public class SimulationRun {

    @Id
    private UUID id;
    @Column(name = "experiment_id")
    private UUID experimentId;
    private String tfmType;
    private String mechanismParams;
    private Instant startedAt;

    protected SimulationRun() {
    }

    public UUID getId() { return id; }
    public UUID getExperimentId() { return experimentId; }
    public String getTfmType() { return tfmType; }
    public String getMechanismParams() { return mechanismParams; }
    public Instant getStartedAt() { return startedAt; }
}
