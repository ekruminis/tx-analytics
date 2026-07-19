package com.ekruminis.txanalytics.queryapi.web;

public record RunSummaryView(
        RunView run,
        BlockStatsView blocks,
        TxFeeStatsView fees) {
}
