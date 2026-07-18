package ZBRA.persistence;

import ZBRA.blockchain.Miner;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "miners",
        uniqueConstraints = @UniqueConstraint(columnNames = {"experiment_id", "miner_id"})
)
public class MinerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    @Column(name = "miner_id", nullable = false)
    private int minerId;

    @Column(nullable = false)
    private int stake;

    @Column(name = "stake_pct", nullable = false)
    private double stakePct;

    protected MinerEntity() {
    }

    public MinerEntity(Experiment experiment, Miner miner, int totalStake) {
        this.experiment = experiment;
        this.minerId = miner.getID();
        this.stake = miner.getStake();
        this.stakePct = Math.round(miner.getStake() * 10000.0 / totalStake) / 100.0;
    }

    public Long getId() {
        return id;
    }

    public Experiment getExperiment() {
        return experiment;
    }

    public int getMinerId() {
        return minerId;
    }

    public int getStake() {
        return stake;
    }

    public double getStakePct() {
        return stakePct;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MinerEntity{");
        sb.append("id=").append(id);
        sb.append(", experiment=").append(experiment);
        sb.append(", minerId=").append(minerId);
        sb.append(", stake=").append(stake);
        sb.append(", stakePct=").append(stakePct);
        sb.append('}');
        return sb.toString();
    }
}
