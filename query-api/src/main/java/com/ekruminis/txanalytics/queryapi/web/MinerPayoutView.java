package com.ekruminis.txanalytics.queryapi.web;

public record MinerPayoutView(
        int minerId,
        int stake,
        double stakePct,
        long blocksWon,
        double totalPayout) {
}
