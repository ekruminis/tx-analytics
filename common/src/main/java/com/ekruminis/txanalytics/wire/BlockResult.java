package com.ekruminis.txanalytics.wire;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BlockResult(
        String runId,
        String experimentId,
        String experimentLabel,
        String tfm,
        int height,
        long timestamp,
        int winnerMinerId,
        double payout,
        double size,
        long txCount,
        long mempoolSize,
        double totalOfferedFee,
        String merkleRoot,
        Double baseFee,
        Double burned,
        Double pool,
        Integer unconfirmedCount,
        Double poolContribution,
        Boolean minerTookFromPool,
        Double totalUserPay) {
}
