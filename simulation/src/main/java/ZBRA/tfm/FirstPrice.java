package ZBRA.tfm;

import java.math.BigDecimal;
import java.util.ArrayList;

import ZBRA.blockchain.Block;
import ZBRA.blockchain.Data;
import ZBRA.blockchain.Miner;
import ZBRA.blockchain.Transaction;

public class FirstPrice extends AbstractTFM {
    private final static String type = "1st Price Auction";

    public FirstPrice() {
        super(type);
    }

    // used for logging each tx data
    public String[] logStart(int index, String hash, double feePaid, double weight, double size) {  
        return new String[] {
                String.valueOf(index),
                hash,
                String.valueOf(feePaid),
                String.valueOf(weight),
                String.valueOf(size)
        };
    }

    @Override
    public String[] logHeaders() {
       return new String[] {
                "Time",
                "Block Height",
                "Parent Hash",
                "Current Hash",
                "Miner ID",
                "Block Reward",
                "Block Size",
                "Block Weight",
                "Number of TX",
                "TX Index",
                "TX Hash",
                "TX Fee",
                "TX Weight",
                "TX Size"
        };
    }

    // Main First-Price Mechanism Implementation
    public Data fetchValidTX(ArrayList<Transaction> mempool, double weightLimit, ArrayList<Block> blockchain, Miner miner, double weightTarget) {
        // sort current mempool by highest fee per weight offered
        mempool.sort((tx1, tx2) -> Double.compare(tx2.getWeightFee(), tx1.getWeightFee()));

        Block block = blockchain.get(blockchain.size() - 1);

        ArrayList<String[]> logs = new ArrayList<>();
        ArrayList<Transaction> confirmed = new ArrayList<>();
        double weightUsedUp = 0;
        double bytesUsedUp = 0;
        BigDecimal rewards = BigDecimal.ZERO;
        int index = 1;

        int i = 0;
        int mempoolSize = mempool.size();

        while (i < mempoolSize) {
            Transaction tx = mempool.get(i);
            double txWeight = tx.getWeight();

            if (txWeight > weightLimit) {
                i++;
                continue;
            }
            if ((weightUsedUp + txWeight) > weightLimit) {
                break;
            }

            confirmed.add(tx);
            bytesUsedUp += tx.getSize();
            weightUsedUp += txWeight;
            rewards = rewards.add(BigDecimal.valueOf(tx.getTotalFee()));
            logs.add(logStart(index, tx.getHash(), tx.getTotalFee(), tx.getWeight(), tx.getSize()));

            i++;
            index++;
        }

        // Bulk remove confirmed transactions after processing
        mempool.subList(0, confirmed.size()).clear();

        return new Data(mempool, confirmed, rewards, bytesUsedUp, weightUsedUp, logs);
    }
}
