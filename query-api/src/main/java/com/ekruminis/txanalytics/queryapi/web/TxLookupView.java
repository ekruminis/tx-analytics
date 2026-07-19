package com.ekruminis.txanalytics.queryapi.web;

import java.util.List;

public record TxLookupView(
        String txHash,
        List<TxOccurrenceView> occurrences) {
}
