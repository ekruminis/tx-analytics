package com.ekruminis.txanalytics.wire;

import java.util.List;

public record MinerRoster(
        String experimentId,
        int numMiners,
        int totalStake,
        List<Entry> miners) {

    public record Entry(
            int minerId,
            int stake,
            double stakePct) {
    }
}
