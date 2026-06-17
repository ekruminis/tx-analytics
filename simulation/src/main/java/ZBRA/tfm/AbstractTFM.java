package ZBRA.tfm;

import java.util.ArrayList;

import ZBRA.blockchain.Block;
import ZBRA.blockchain.Data;
import ZBRA.blockchain.Miner;
import ZBRA.blockchain.Transaction;

// Abstract class for different TFM styles
public abstract class AbstractTFM {
    protected String type;

    public AbstractTFM(String t) {
        this.type = t;
    }

    public String getType() {
        return type;
    }

    abstract public String[] logHeaders();

    abstract public Data fetchValidTX(ArrayList<Transaction> mempool, double weightLimit, ArrayList<Block> block, Miner miner, double target);
}
