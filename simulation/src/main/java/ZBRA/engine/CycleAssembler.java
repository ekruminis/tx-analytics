package ZBRA.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ZBRA.blockchain.Transaction;

public class CycleAssembler {

    private final List<SimulationEngine> engines;
    private final int expectedPartitions;

    private final Map<Long, List<Transaction>> reorderBuffer = new HashMap<>();
    private final Map<Integer, Long> partitionHeads = new HashMap<>();
    private long nextCycle = -1;
    private String datasetHash;

    public CycleAssembler(List<SimulationEngine> engines, int expectedPartitions) {
        this.engines = engines;
        this.expectedPartitions = expectedPartitions;
    }

    public void accept(int partition, long cycle, String datasetHash, Transaction tx) {
        if (this.datasetHash == null) {
            this.datasetHash = datasetHash;
        }
        reorderBuffer.computeIfAbsent(cycle, k -> new ArrayList<>()).add(tx);
        partitionHeads.merge(partition, cycle, Math::max);
        drainComplete();
    }

    public void markPartitionEof(int partition) {
        partitionHeads.put(partition, Long.MAX_VALUE);
        drainComplete();
    }

    private void drainComplete() {
        if (partitionHeads.size() < expectedPartitions) {
            return;
        }
        long watermark = Collections.min(partitionHeads.values());

        long limit;
        if (watermark == Long.MAX_VALUE) {
            if (reorderBuffer.isEmpty()) {
                return;
            }
            limit = Collections.max(reorderBuffer.keySet()) + 1;
        } else {
            limit = watermark;
        }

        if (nextCycle < 0) {
            nextCycle = reorderBuffer.isEmpty() ? limit : Collections.min(reorderBuffer.keySet());
        }

        while (nextCycle < limit) {
            List<Transaction> txs = reorderBuffer.remove(nextCycle);
            List<Transaction> cycleTxs = txs == null ? List.of() : txs;
            for (SimulationEngine engine : engines) {
                engine.mineCycle(datasetHash, nextCycle, cycleTxs);
            }
            nextCycle++;
        }
    }
}
