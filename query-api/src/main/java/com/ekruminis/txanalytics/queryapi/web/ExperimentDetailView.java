package com.ekruminis.txanalytics.queryapi.web;

import java.util.List;

public record ExperimentDetailView(
        ExperimentView experiment,
        List<RunView> runs,
        List<MinerView> miners) {
}
