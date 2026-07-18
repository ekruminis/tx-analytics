package ZBRA.persistence;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "simulation_runs")
public class SimulationRun {

    @ConfigurationProperties(prefix = "simulation")
    public record RunProperties(
            String tfm,
            long seed,
            int numMiners,
            Map<String, Map<String, String>> tfms) {

        public Map<String, String> mechanismParams() {
            if (tfms == null) {
                return Map.of();
            }
            return tfms.getOrDefault(tfm, Map.of());
        }
    }

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    @Column(nullable = false)
    private String tfmType;

    @Column(nullable = false)
    private String mechanismParams;

    @Column(nullable = false)
    private Instant startedAt;

    protected SimulationRun() {
    }

    public SimulationRun(UUID id, Experiment experiment, String tfmType, String mechanismParams) {
        this.id = id;
        this.experiment = experiment;
        this.tfmType = tfmType;
        this.mechanismParams = mechanismParams;
        this.startedAt = Instant.now();
    }

    public static String canonicalise(Map<String, String> mechanismParams) {
        return new TreeMap<>(mechanismParams).entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(","));
    }

    public static UUID deterministicId(String tfm, long seed, int numMiners, String canonicalParams) {
        String canonical = String.join("|",
                tfm,
                Long.toString(seed),
                Integer.toString(numMiners),
                canonicalParams);
        return UUID.nameUUIDFromBytes(canonical.getBytes(StandardCharsets.UTF_8));
    }

    public static SimulationRun fromProperties(RunProperties p, Experiment experiment) {
        String params = canonicalise(p.mechanismParams());
        UUID id = deterministicId(p.tfm(), p.seed(), p.numMiners(), params);
        return new SimulationRun(id, experiment, p.tfm(), params);
    }

    public UUID getId() {
        return id;
    }

    public Experiment getExperiment() {
        return experiment;
    }

    public String getTfmType() {
        return tfmType;
    }

    public String getMechanismParams() {
        return mechanismParams;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SimulationRun{");
        sb.append("id=").append(id);
        sb.append(", experiment=").append(experiment);
        sb.append(", tfmType=").append(tfmType);
        sb.append(", mechanismParams=").append(mechanismParams);
        sb.append(", startedAt=").append(startedAt);
        sb.append('}');
        return sb.toString();
    }
}
