package com.ekruminis.txanalytics.queryapi.web;

import java.util.List;

public record ComparisonView(
        List<RunSummaryView> runs,
        List<MetricRanking> rankings) {
}
