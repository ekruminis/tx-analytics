package com.ekruminis.txanalytics.queryapi.web;

public record TxFeeStatsView(
        long txCount,
        long confirmedCount,
        long unconfirmedCount,
        DistributionView offeredFee,
        DistributionView paidFee,
        double totalPaid,
        double totalBurned) {
}
