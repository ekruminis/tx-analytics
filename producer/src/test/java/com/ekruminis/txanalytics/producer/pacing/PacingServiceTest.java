package com.ekruminis.txanalytics.producer.pacing;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class PacingServiceTest {

    @Test
    void sameSeedProducesIdenticalSequences() {
        PacingService a = new PacingService(100, 0.3, 145L);
        PacingService b = new PacingService(100, 0.3, 145L);

        for (int i = 0; i < 100; i++) {
            assertThat(a.sampleBatchSize()).isEqualTo(b.sampleBatchSize());
        }
    }

    @Test
    void batchSizesAreNeverNegative() {
        PacingService pacing = new PacingService(50, 0.5, 79L);

        for (int i = 0; i < 1_000; i++) {
            assertThat(pacing.sampleBatchSize()).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    void longRunAverageConvergesOnConfiguredMean() {
        double mean = 50;
        PacingService pacing = new PacingService(mean, 0.5, 42L);

        int samples = 20_000;
        long total = 0;
        for (int i = 0; i < samples; i++) {
            total += pacing.sampleBatchSize();
        }
        double average = (double) total / samples;

        assertThat(average).isBetween(mean * 0.85, mean * 1.15);
    }

    @Test
    void differentSeedsDiverge() {
        PacingService a = new PacingService(50, 1.23, 1L);
        PacingService b = new PacingService(50, 1.23, 2L);

        List<Integer> seqA = new ArrayList<>();
        List<Integer> seqB = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            seqA.add(a.sampleBatchSize());
            seqB.add(b.sampleBatchSize());
        }

        assertThat(seqA).isNotEqualTo(seqB);
    }
}
