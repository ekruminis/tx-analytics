package ZBRA.engine;

import java.time.Instant;
import java.util.Map;

import org.springframework.stereotype.Component;

import ZBRA.kafka.BlockResultPublisher;
import ZBRA.kafka.TxResultPublisher;
import ZBRA.persistence.BlockRepository;
import ZBRA.persistence.ExperimentRepository;
import ZBRA.persistence.MinerRepository;
import ZBRA.persistence.SimulationRun;
import ZBRA.persistence.SimulationRunRepository;

@Component
public class SimulationEngineFactory {

    private final ExperimentRepository experimentRepo;
    private final SimulationRunRepository runRepo;
    private final BlockRepository blockRepo;
    private final MinerRepository minerRepo;
    private final BlockResultPublisher blockResultPublisher;
    private final TxResultPublisher txResultPublisher;

    public SimulationEngineFactory(ExperimentRepository experimentRepo,
                                   SimulationRunRepository runRepo,
                                   BlockRepository blockRepo,
                                   MinerRepository minerRepo,
                                   BlockResultPublisher blockResultPublisher,
                                   TxResultPublisher txResultPublisher) {
        this.experimentRepo = experimentRepo;
        this.runRepo = runRepo;
        this.blockRepo = blockRepo;
        this.minerRepo = minerRepo;
        this.blockResultPublisher = blockResultPublisher;
        this.txResultPublisher = txResultPublisher;
    }

    public SimulationEngine create(String tfm, long seed, int numMiners, Map<String, String> params,
                                   Instant blockTimeGenesis, long blockTimeIntervalSeconds) {
        Map<String, String> effectiveParams = applyDefaults(tfm, params);
        SimulationRun.RunProperties props =
                new SimulationRun.RunProperties(tfm, seed, numMiners, Map.of(tfm, effectiveParams));
        return new SimulationEngine(props, experimentRepo, runRepo, blockRepo, minerRepo,
                blockResultPublisher, txResultPublisher, blockTimeGenesis, blockTimeIntervalSeconds);
    }

    private static Map<String, String> applyDefaults(String tfm, Map<String, String> params) {
        Map<String, String> defaults = defaultsFor(tfm);
        if (params == null || params.isEmpty()) {
            return defaults;
        }
        java.util.Map<String, String> merged = new java.util.HashMap<>(defaults);
        merged.putAll(params);
        return merged;
    }

    private static Map<String, String> defaultsFor(String tfm) {
        return switch (tfm) {
            case "first_price", "second_price", "burning_second_price" ->
                    Map.of("size_limit", "2000000");
            case "eip1559" ->
                    Map.of("size_limit", "4000000", "target", "2000000",
                            "base_fee", "0.0000002333");
            case "reserve_pool" ->
                    Map.of("size_limit", "4000000", "target", "2000000",
                            "base_fee", "0.0000002333", "reserve_base", "134.38", "window", "144");
            default -> throw new IllegalArgumentException("unknown tfm: " + tfm);
        };
    }
}
