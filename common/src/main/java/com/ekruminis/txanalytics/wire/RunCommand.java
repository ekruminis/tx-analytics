package com.ekruminis.txanalytics.wire;

import java.util.Map;

public record RunCommand(
        String dataset,
        Integer partitions,
        Long seed,
        Integer numMiners,
        Pacing pacing,
        BlockTimeConfig blockTime,
        String label,
        Map<String, Map<String, String>> tfms) {

    public record Pacing(Double meanTxPerCycle, Double alpha, Long seed) {
    }

    public record BlockTimeConfig(String genesis, Long intervalSeconds) {
    }
}
