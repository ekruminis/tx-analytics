package ZBRA.tfm;

import java.math.BigDecimal;
import java.util.ArrayList;

import ZBRA.blockchain.Block;
import ZBRA.blockchain.Data;
import ZBRA.blockchain.Miner;
import ZBRA.blockchain.Transaction;

public class SecondPrice extends AbstractTFM {
    private final static String type = "2nd Price Auction";

    public SecondPrice() {
        super(type);
    }

    // used for logging each tx data
    public String[] logStart(int index, String hash, double feePaid, double weight, double size) {
        return new String[] {
                String.valueOf(index),
                hash,
                String.valueOf(feePaid),
                String.valueOf(weight),
                String.valueOf(size),
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
                "Effective Fee",
                "TX Index",
                "TX Hash",
                "TX Offered",
                "TX Weight",
                "TX Size"
        };
    }

    // Main Second-Price Mechanism Implementation
    @Override
    public Data fetchValidTX(ArrayList<Transaction> mempool, double weightLimit, ArrayList<Block> blockchain, Miner miner, double weightTarget) {
        // sort current mempool by highest fee per byte price offered
        mempool.sort((tx1, tx2) -> Double.compare(tx2.getWeightFee(), tx1.getWeightFee()));

        Block block = blockchain.get(blockchain.size() - 1);

        ArrayList<String[]> logs = new ArrayList<String[]>(); // log data for printing later
        ArrayList<Transaction> confirmed = new ArrayList<Transaction>(); // list of *confirmed* transactions
        double weightUsedUp = 0; // total weight used by current block
        double bytesUsedUp = 0; // total bytes used by current block
        BigDecimal rewards = new BigDecimal("0"); // total rewards given to miner

        double effectiveFee = 0;  // fee per byte price to be paid by all included tx

        int index = 1;
        int processed = 0; // count how many TXs were processed

        for (int i = 0; i < mempool.size(); i++) {
            Transaction tx = mempool.get(i);
            double txWeight = tx.getWeight();

            if (txWeight > weightLimit) {
                processed++;
                continue;
            }

            if ((weightUsedUp + txWeight) > weightLimit) {
                break;
            }

            confirmed.add(tx);
            bytesUsedUp += tx.getSize();
            weightUsedUp += txWeight;
            logs.add(logStart(index, tx.getHash(), tx.getTotalFee(), txWeight, tx.getSize()));
            index++;
            processed++;
        }

        if (!confirmed.isEmpty()) {
            effectiveFee = confirmed.get(confirmed.size() - 1).getWeightFee();
            for (Transaction t : confirmed) {
                rewards = rewards.add(BigDecimal.valueOf(t.getWeight() * effectiveFee));
            }
        }

        // remove processed transactions from mempool (optional, depends on your logic)
        if (processed > 0) {
            mempool.subList(0, processed).clear();
        }

        return new Data(mempool, confirmed, rewards, effectiveFee, bytesUsedUp, weightUsedUp, logs);
    }
}
