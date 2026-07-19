package com.ekruminis.txanalytics.queryapi.web;

import java.util.Map;

public record DistributionView(
        long count,
        Double min,
        Double max,
        Double avg,
        Double sum,
        Map<String, Double> percentiles) {

    public static DistributionView empty() {
        return new DistributionView(0, null, null, null, null, Map.of());
    }
}
