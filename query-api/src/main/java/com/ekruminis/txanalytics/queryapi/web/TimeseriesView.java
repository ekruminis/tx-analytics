package com.ekruminis.txanalytics.queryapi.web;

import java.util.List;
import java.util.UUID;

public record TimeseriesView(
        UUID runId,
        String metric,
        String interval,
        List<Point> points) {

    public record Point(String timestamp, double value) {
    }
}
