package com.ekruminis.txanalytics.producer.pacing;

import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// stateful Gamma-Poisson (Negative Binomial) batch-size sampler
@Component
public class PacingService {

    private final RandomGenerator rng;
    private final GammaDistribution gamma;

    public PacingService(
            @Value("${producer.pacing.mean-tx-per-cycle}") double mean,
            @Value("${producer.pacing.alpha}") double alpha,
            @Value("${producer.pacing.seed}") long seed) {
        this.rng = new Well19937c(seed);

        double shape = 1.0 / alpha;
        double scale = mean * alpha;
        this.gamma = new GammaDistribution(rng, shape, scale);
    }

    public int sampleBatchSize() {
        double lambda = gamma.sample();
        PoissonDistribution poisson = new PoissonDistribution(
                rng, lambda,
                PoissonDistribution.DEFAULT_EPSILON,
                PoissonDistribution.DEFAULT_MAX_ITERATIONS);
        return poisson.sample();
    }
}

