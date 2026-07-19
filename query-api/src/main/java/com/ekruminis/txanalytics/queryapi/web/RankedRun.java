package com.ekruminis.txanalytics.queryapi.web;

import java.util.UUID;

public record RankedRun(
        UUID runId,
        String tfm,
        double value,
        int rank) {
}
