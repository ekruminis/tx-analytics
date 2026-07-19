package com.ekruminis.txanalytics.queryapi.web;

import com.ekruminis.txanalytics.queryapi.mongo.TxResultDoc;

public record BlockTxView(
        String txHash,
        double size,
        double offeredFee,
        double paidFee,
        boolean confirmed,
        Double burned) {

    public static BlockTxView of(TxResultDoc d) {
        return new BlockTxView(d.getTxHash(), d.getSize(), d.getOfferedFee(),
                d.getPaidFee(), d.isConfirmed(), d.getBurned());
    }
}
