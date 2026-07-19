package com.ekruminis.txanalytics.queryapi.web;

import java.time.Instant;
import java.util.UUID;

import com.ekruminis.txanalytics.queryapi.postgres.Experiment;

public record ExperimentView(
        UUID id,
        long seed,
        int numMiners,
        String datasetHash,
        Instant startedAt,
        long runCount) {

    public static ExperimentView of(Experiment e, long runCount) {
        return new ExperimentView(e.getId(), e.getSeed(), e.getNumMiners(),
                e.getDatasetHash(), e.getStartedAt(), runCount);
    }
}
