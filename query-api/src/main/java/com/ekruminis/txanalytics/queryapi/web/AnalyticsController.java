package com.ekruminis.txanalytics.queryapi.web;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ekruminis.txanalytics.queryapi.service.AnalyticsService;

@RestController
public class AnalyticsController {

    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) {
        this.service = service;
    }

    @GetMapping("/runs/{id}/summary")
    public RunSummaryView summary(@PathVariable UUID id) {
        return service.summary(id);
    }

    @GetMapping("/runs/{id}/fee-distribution")
    public TxFeeStatsView feeDistribution(@PathVariable UUID id) {
        return service.feeDistribution(id);
    }

    @GetMapping("/runs/{id}/miners")
    public MinersView miners(@PathVariable UUID id) {
        return service.miners(id);
    }

    @GetMapping("/runs/{id}/timeseries")
    public TimeseriesView timeseries(
            @PathVariable UUID id,
            @RequestParam String metric,
            @RequestParam(defaultValue = "day") String interval) {
        return service.timeseries(id, metric, interval);
    }
}
