package com.ekruminis.txanalytics.queryapi.web;

import java.util.UUID;

public record TxOccurrenceView(
        UUID runId,
        String tfm,
        UUID experimentId,
        long seed,
        int numMiners,
        String datasetHash,
        String mechanismParams,
        int height,
        String blockHash,
        double offeredFee,
        double paidFee,
        boolean confirmed,
        Double burned) {
}
