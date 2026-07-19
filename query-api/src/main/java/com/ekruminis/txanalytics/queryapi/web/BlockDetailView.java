package com.ekruminis.txanalytics.queryapi.web;

import java.math.BigDecimal;
import java.util.UUID;

import com.ekruminis.txanalytics.queryapi.mongo.BlockResultDoc;
import com.ekruminis.txanalytics.queryapi.postgres.Block;
import com.ekruminis.txanalytics.queryapi.postgres.SimulationRun;

public record BlockDetailView(
        UUID runId,
        String tfm,
        String mechanismParams,
        int height,
        String parentHash,
        String currentHash,
        String merkleRoot,
        Integer winnerMinerId,
        double size,
        long txCount,
        BigDecimal minerPayout,
        Long mempoolSize,
        Double totalOfferedFee,
        Double baseFee,
        Double burned,
        Double pool,
        Integer unconfirmedCount,
        Double poolContribution,
        Boolean minerTookFromPool,
        Double totalUserPay) {

    public static BlockDetailView of(Block b, SimulationRun run, BlockResultDoc outcomes) {
        return new BlockDetailView(
                b.getRunId(), run.getTfmType(), run.getMechanismParams(),
                b.getHeight(), b.getParentHash(), b.getCurrentHash(), b.getMerkleRoot(),
                b.getWinnerMinerId(), b.getSize(), b.getTxCount(), b.getMinerPayout(),
                outcomes == null ? null : outcomes.getMempoolSize(),
                outcomes == null ? null : outcomes.getTotalOfferedFee(),
                outcomes == null ? null : outcomes.getBaseFee(),
                outcomes == null ? null : outcomes.getBurned(),
                outcomes == null ? null : outcomes.getPool(),
                outcomes == null ? null : outcomes.getUnconfirmedCount(),
                outcomes == null ? null : outcomes.getPoolContribution(),
                outcomes == null ? null : outcomes.getMinerTookFromPool(),
                outcomes == null ? null : outcomes.getTotalUserPay());
    }
}
