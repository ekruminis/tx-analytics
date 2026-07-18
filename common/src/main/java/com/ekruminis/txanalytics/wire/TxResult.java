package com.ekruminis.txanalytics.wire;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TxResult(
        String runId,
        String tfm,
        int height,
        String txHash,
        double size,
        double offeredFee,
        double paidFee,
        boolean confirmed,
        Double burned) {
}
