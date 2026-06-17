package ZBRA.blockchain;

import java.math.BigDecimal;
import java.util.ArrayList;

public class Block {
    int index;
    int minerId;
    String parentHash;
    String currentHash;
    double weightLimit; // size limit in weight units;
    double size; // size in bytes
    double weight; // weight in weight units
    double baseFee;
    long txNumber; // number of transactions inside the block
    BigDecimal rewards; // total rewards paid directly to miner
    BigDecimal burned; // total amount burned
    BigDecimal pool; // total of current pool
    Data logs;

    ArrayList<Transaction> confirmedTxs;
    ArrayList<Transaction> unconfirmedTxs;

    // GENESIS block
    public Block() {
        this.index = 0;
        this.minerId = -1;
        this.parentHash = null;
        this.currentHash = "GENESIS";
        this.weightLimit = -1.0;
        this.size = -1.0;
        this.weight = -1.0;
        this.baseFee = -1.0;
        this.rewards = null;
        this.confirmedTxs = null;
        this.unconfirmedTxs = null;
        this.txNumber = -1;
        this.burned = null;
        this.pool = null;
        this.logs = null;
    }

    // general block
    public Block(int index, int minerId, String parentHash, String currentHash, double limit, Data d) {
        this.index = index;
        this.minerId = minerId;
        this.parentHash = parentHash;
        this.currentHash = currentHash;

        this.weightLimit = limit;
        this.size = d.getSize(); // size in bytes
        this.weight = d.getWeight(); // size in weight units
        this.baseFee = d.getBaseFee();

        this.rewards = d.getRewards();
        this.confirmedTxs = d.getConfirmed();
        this.unconfirmedTxs = d.getUnconfirmed();
        this.txNumber = (d.getConfirmed() != null ? d.getConfirmed().size() : 0) + 
                            (d.getUnconfirmed() != null ? d.getUnconfirmed().size() : 0);
        this.burned = d.getBurned();
        this.pool = d.getPool();

        this.logs = d;
    }

    public BigDecimal getPool() { return pool; }

    public int getIndex() {
        return index;
    }

    public int getMinerID() {
        return minerId;
    }

    public String getParentHash() {
        return parentHash;
    }

    public String getCurrentHash() {
        return currentHash;
    }

    public double getWeightLimit() {
        return weightLimit;
    }

    public double getSize() {
        return size;
    }

    public double getBaseFee() {
        return baseFee;
    }

    public BigDecimal getRewards() { return rewards; }

    public ArrayList<Transaction> getConfirmedTXs() {
        return confirmedTxs;
    }

    public ArrayList<Transaction> getUnconfirmedTXs() {
        return unconfirmedTxs;
    }

    public long getTXNumber() {
        return txNumber;
    }

    public BigDecimal getBurned() {
        return burned;
    }

    public void setBaseFee(double bf) {
        this.baseFee = bf;
    }

    public void updatePool(BigDecimal s) {
        this.pool = s;
    }

    public Data getLogs() {
        return logs;
    }

    public int getMinerId() {
        return minerId;
    }

    public double getWeight() {
        return weight;
    }

    public long getTxNumber() {
        return txNumber;
    }

    public ArrayList<Transaction> getConfirmedTxs() {
        return confirmedTxs;
    }

    public ArrayList<Transaction> getUnconfirmedTxs() {
        return unconfirmedTxs;
    }
}

