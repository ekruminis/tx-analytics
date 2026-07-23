package com.ekruminis.txanalytics.producer.ingest;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.ekruminis.txanalytics.wire.RunCommand;
import com.ekruminis.txanalytics.wire.SimCommand;

@Component
public class IngestCommandConsumer {

    private static final Logger log = LoggerFactory.getLogger(IngestCommandConsumer.class);

    private static final String DEFAULT_DATASET = "txs-test.json";
    private static final int DEFAULT_PARTITIONS = 3;
    private static final long DEFAULT_SIM_SEED = 7653L;
    private static final int DEFAULT_NUM_MINERS = 10;
    private static final double DEFAULT_MEAN_TX = 2471.0;
    private static final double DEFAULT_ALPHA = 0.02;
    private static final long DEFAULT_PACING_SEED = 12345L;
    private static final String DEFAULT_GENESIS = "2023-06-01T00:00:00Z";
    private static final long DEFAULT_INTERVAL_SECONDS = 600L;

    private final DatasetIngestor ingestor;
    private final TopicOps topicOps;
    private final KafkaTemplate<String, Object> kafka;
    private final DatasetResolver datasetResolver;

    public IngestCommandConsumer(DatasetIngestor ingestor,
                                 TopicOps topicOps,
                                 KafkaTemplate<String, Object> kafka,
                                 DatasetResolver datasetResolver) {
        this.ingestor = ingestor;
        this.topicOps = topicOps;
        this.kafka = kafka;
        this.datasetResolver = datasetResolver;
    }

    @KafkaListener(topics = "ingest.commands")
    public void onCommand(RunCommand cmd) {
        if (cmd.tfms() == null || cmd.tfms().isEmpty()) {
            log.warn("ignoring command with no tfms");
            return;
        }
        String dataset = orElse(cmd.dataset(), DEFAULT_DATASET);
        int partitions = orElse(cmd.partitions(), DEFAULT_PARTITIONS);
        long simSeed = orElse(cmd.seed(), DEFAULT_SIM_SEED);
        int numMiners = orElse(cmd.numMiners(), DEFAULT_NUM_MINERS);
        double meanTx = pacing(cmd, RunCommand.Pacing::meanTxPerCycle, DEFAULT_MEAN_TX);
        double alpha = pacing(cmd, RunCommand.Pacing::alpha, DEFAULT_ALPHA);
        long pacingSeed = pacingSeed(cmd);
        String genesis = blockTime(cmd, RunCommand.BlockTimeConfig::genesis, DEFAULT_GENESIS);
        long interval = blockTimeInterval(cmd);

        Path file = datasetResolver.resolve(dataset);
        String hash = DatasetIngestor.hashFile(file);
        String topic = "transactions." + hash;

        if (topicOps.isComplete(topic, partitions)) {
            log.info("dataset {} (hash {}) already ingested on {} partitions — reusing", dataset, hash, partitions);
        } else {
            topicOps.recreate(topic, partitions);
            ingestor.ingest(file, topic, partitions, hash, meanTx, alpha, pacingSeed);
        }

        SimCommand sim = new SimCommand(topic, partitions, simSeed, numMiners,
                genesis, interval, cmd.label(), cmd.tfms());
        kafka.send("simulation.commands", String.valueOf(simSeed), sim);
        log.info("handed off to simulation: topic={} seed={} numMiners={} tfms={}",
                topic, simSeed, numMiners, cmd.tfms().keySet());
    }

    private static <T> T orElse(T value, T fallback) {
        return value != null ? value : fallback;
    }

    private static double pacing(RunCommand cmd,
                                 java.util.function.Function<RunCommand.Pacing, Double> field,
                                 double fallback) {
        if (cmd.pacing() == null) {
            return fallback;
        }
        Double v = field.apply(cmd.pacing());
        return v != null ? v : fallback;
    }

    private static long pacingSeed(RunCommand cmd) {
        if (cmd.pacing() == null || cmd.pacing().seed() == null) {
            return DEFAULT_PACING_SEED;
        }
        return cmd.pacing().seed();
    }

    private static String blockTime(RunCommand cmd,
                                    java.util.function.Function<RunCommand.BlockTimeConfig, String> field,
                                    String fallback) {
        if (cmd.blockTime() == null) {
            return fallback;
        }
        String v = field.apply(cmd.blockTime());
        return v != null ? v : fallback;
    }

    private static long blockTimeInterval(RunCommand cmd) {
        if (cmd.blockTime() == null || cmd.blockTime().intervalSeconds() == null) {
            return DEFAULT_INTERVAL_SECONDS;
        }
        return cmd.blockTime().intervalSeconds();
    }
}
