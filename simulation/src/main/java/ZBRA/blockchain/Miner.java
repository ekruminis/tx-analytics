package ZBRA.blockchain;

import java.math.BigDecimal;

public class Miner {
    int stake;
    int nodeID;
    BigDecimal winnings; // total rewards earned by miner
    BigDecimal privatePool; // miners individual total in private pool (for 'pool' TFM)
    BigDecimal publicPoolEffect; // positive/negative change on shared pool
    // double malice;

    public Miner(int id, int s) {
        this.nodeID = id;
        this.stake = s;
        this.winnings = new BigDecimal("0");
        this.privatePool = new BigDecimal("0");
        this.publicPoolEffect = new BigDecimal("0");
    }

    public Miner(int id, int s, BigDecimal pp, BigDecimal ppe) {
        this.nodeID = id;
        this.stake = s;
        this.winnings = new BigDecimal("0");
        this.privatePool = pp;
        this.publicPoolEffect = ppe;
    }

    public int getStake() {
        return stake;
    }

    public int getID() {
        return nodeID;
    }

    public void updateWinnings(BigDecimal w) {
        this.winnings = winnings.add(w);
    }

    public BigDecimal getRewards() {
        return winnings;
    }

    public BigDecimal getPrivatePool() {
        return privatePool;
    }

    public BigDecimal getPublicPoolEffect() {
        return publicPoolEffect;
    }

    public void updatePublicPoolEffect(BigDecimal ppe) { this.publicPoolEffect = publicPoolEffect.add(ppe); }

    public void updatePrivatePool(BigDecimal pp) { this.privatePool = privatePool.add(pp); }

}
