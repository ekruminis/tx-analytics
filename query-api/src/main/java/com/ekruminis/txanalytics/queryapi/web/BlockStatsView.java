package com.ekruminis.txanalytics.queryapi.web;

public record BlockStatsView(
        long blockCount,
        DistributionView payout,
        DistributionView blockSize,
        double avgFillRatio,
        DistributionView mempoolSize,
        DistributionView totalOfferedFee,
        DistributionView burned,
        DistributionView pool) {
}
