package com.ekruminis.txanalytics.queryapi.web;

import java.util.List;

public record MetricRanking(
        String metric,
        String description,
        boolean higherIsBetter,
        List<RankedRun> ranked) {
}
