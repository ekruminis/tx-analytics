package ZBRA.persistence;

import java.math.BigDecimal;

import ZBRA.blockchain.Block;
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
        name = "blocks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"run_id", "height"})
)
public class BlockEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private SimulationRun run;

    @Column(nullable = false)
    private int height;

    @Column(name = "winner_miner_id")
    private Integer winnerMinerId;

    private String parentHash;

    @Column(nullable = false)
    private String currentHash;

    @Column(nullable = false)
    private double size;

    @Column(nullable = false)
    private long txCount;

    @Column(nullable = false, precision = 38, scale = 10)
    private BigDecimal minerPayout;

    @Column(name = "merkle_root", nullable = false, length = 64)
    private String merkleRoot;

    protected BlockEntity() {
    }

    public BlockEntity(SimulationRun run, Block block, String merkleRoot) {
        this.run = run;
        this.height = block.getIndex();
        this.winnerMinerId = block.getIndex() == 0 ? null : block.getMinerID();
        this.parentHash = block.getParentHash();
        this.currentHash = block.getCurrentHash();
        this.size = block.getSize() < 0 ? 0 : block.getSize();
        this.txCount = block.getTXNumber() < 0 ? 0 : block.getTXNumber();
        this.minerPayout = block.getRewards() != null ? block.getRewards() : BigDecimal.ZERO;
        this.merkleRoot = merkleRoot;
    }

    public Long getId() {
        return id;
    }

    public SimulationRun getRun() {
        return run;
    }

    public int getHeight() {
        return height;
    }

    public Integer getWinnerMinerId() {
        return winnerMinerId;
    }

    public String getParentHash() {
        return parentHash;
    }

    public String getCurrentHash() {
        return currentHash;
    }

    public double getSize() {
        return size;
    }

    public long getTxCount() {
        return txCount;
    }

    public BigDecimal getMinerPayout() {
        return minerPayout;
    }

    public String getMerkleRoot() {
        return merkleRoot;
    }

    @Override
    public String toString() {
        return "BlockEntity [id=" + id + ", run=" + run + ", height=" + height + ", winnerMinerId=" + winnerMinerId
                + ", parentHash=" + parentHash + ", currentHash=" + currentHash + ", size=" + size + ", txCount="
                + txCount + ", minerPayout=" + minerPayout + ", merkleRoot=" + merkleRoot + "]";
    }
}
