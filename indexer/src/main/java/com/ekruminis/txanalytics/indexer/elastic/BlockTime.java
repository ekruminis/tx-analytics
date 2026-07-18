package com.ekruminis.txanalytics.indexer.elastic;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BlockTime {

    private final Instant genesis;
    private final long intervalSeconds;

    public BlockTime(
            @Value("${indexer.block-time.genesis}") String genesis,
            @Value("${indexer.block-time.interval-seconds}") long intervalSeconds) {
        this.genesis = Instant.parse(genesis);
        this.intervalSeconds = intervalSeconds;
    }

    public Instant at(int height) {
        return genesis.plusSeconds((long) height * intervalSeconds);
    }
}
