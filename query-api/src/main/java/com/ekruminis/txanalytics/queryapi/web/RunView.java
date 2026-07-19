package com.ekruminis.txanalytics.queryapi.web;

import java.time.Instant;
import java.util.UUID;

import com.ekruminis.txanalytics.queryapi.postgres.Experiment;
import com.ekruminis.txanalytics.queryapi.postgres.SimulationRun;

public record RunView(
        UUID id,
        String tfm,
        String mechanismParams,
        Instant startedAt,
        UUID experimentId,
        long seed,
        int numMiners,
        String datasetHash) {

    public static RunView of(SimulationRun r, Experiment e) {
        return new RunView(r.getId(), r.getTfmType(), r.getMechanismParams(), r.getStartedAt(),
                r.getExperimentId(), e.getSeed(), e.getNumMiners(), e.getDatasetHash());
    }
}
