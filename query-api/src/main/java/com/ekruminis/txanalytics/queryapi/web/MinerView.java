package com.ekruminis.txanalytics.queryapi.web;

import com.ekruminis.txanalytics.queryapi.postgres.Miner;

public record MinerView(
        int minerId,
        int stake,
        double stakePct) {

    public static MinerView of(Miner m) {
        return new MinerView(m.getMinerId(), m.getStake(), m.getStakePct());
    }
}
