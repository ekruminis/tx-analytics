package com.ekruminis.txanalytics.queryapi.web;

import java.math.BigDecimal;

import com.ekruminis.txanalytics.queryapi.postgres.Block;

public record BlockView(
        int height,
        String parentHash,
        String currentHash,
        String merkleRoot,
        Integer winnerMinerId,
        double size,
        long txCount,
        BigDecimal minerPayout) {

    public static BlockView of(Block b) {
        return new BlockView(b.getHeight(), b.getParentHash(), b.getCurrentHash(), b.getMerkleRoot(),
                b.getWinnerMinerId(), b.getSize(), b.getTxCount(), b.getMinerPayout());
    }
}
