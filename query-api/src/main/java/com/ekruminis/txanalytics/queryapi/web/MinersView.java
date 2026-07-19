package com.ekruminis.txanalytics.queryapi.web;

import java.util.List;
import java.util.UUID;

public record MinersView(
        UUID runId,
        List<MinerPayoutView> miners) {
}
