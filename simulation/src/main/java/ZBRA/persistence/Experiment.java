package ZBRA.persistence;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "experiments")
public class Experiment {

    @Id
    private UUID id;

    @Column(nullable = false)
    private long seed;

    @Column(nullable = false)
    private int numMiners;

    @Column(name = "dataset_hash", nullable = false)
    private String datasetHash;

    @Column(nullable = false)
    private Instant startedAt;

    protected Experiment() {
    }

    public Experiment(UUID id, long seed, int numMiners, String datasetHash) {
        this.id = id;
        this.seed = seed;
        this.numMiners = numMiners;
        this.datasetHash = datasetHash;
        this.startedAt = Instant.now();
    }

    public static UUID deterministicId(long seed, int numMiners, String datasetHash) {
        String canonical = seed + "|" + numMiners + "|" + datasetHash;
        return UUID.nameUUIDFromBytes(canonical.getBytes(StandardCharsets.UTF_8));
    }

    public static Experiment fromProperties(SimulationRun.RunProperties p, String datasetHash) {
        UUID id = deterministicId(p.seed(), p.numMiners(), datasetHash);
        return new Experiment(id, p.seed(), p.numMiners(), datasetHash);
    }

    public UUID getId() {
        return id;
    }

    public long getSeed() {
        return seed;
    }

    public int getNumMiners() {
        return numMiners;
    }

    public String getDatasetHash() {
        return datasetHash;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    @Override
    public String toString() {
        return "Experiment [id=" + id + ", seed=" + seed + ", numMiners=" + numMiners + ", datasetHash=" + datasetHash
                + ", startedAt=" + startedAt + "]";
    }
}
