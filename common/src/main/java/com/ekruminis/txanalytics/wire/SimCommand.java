package com.ekruminis.txanalytics.wire;

import java.util.Map;

public record SimCommand(
        String sourceTopic,
        int partitions,
        long seed,
        int numMiners,
        String blockTimeGenesis,
        long blockTimeIntervalSeconds,
        String label,
        Map<String, Map<String, String>> tfms) {
}
