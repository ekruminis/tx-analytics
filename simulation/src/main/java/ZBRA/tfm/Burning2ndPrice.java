package ZBRA.tfm;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.PriorityQueue;

import ZBRA.blockchain.Block;
import ZBRA.blockchain.Data;
import ZBRA.blockchain.Miner;
import ZBRA.blockchain.Transaction;

public class Burning2ndPrice extends AbstractTFM {
    private final static String type = "Burning 2nd Price Auction";

    public Burning2ndPrice() {
        super(type);
    }

    // used for logging each tx data
    public String[] logStart(int index, String confirmed, String hash, double feeOffered, double feePaid, double weight, double size) {
        return new String[] {
                String.valueOf(index),
                confirmed,
                hash,
                String.valueOf(feeOffered),
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
                "Number of Confirmed TX",
                "Number of Unconfirmed TX",
                "Effective Fee",
                "Fees Burned",
                "TX Index",
                "TX Confirmed",
                "TX Hash",
                "TX Offered",
                "TX Paid",
                "TX Weight",
                "TX Size"
        };
    }

    // Main Burning Second-Price Mechanism Implementation
    @Override
    public Data fetchValidTX(ArrayList<Transaction> mempool, double weightLimit, ArrayList<Block> blockchain, Miner miner, double weightTarget) {
        Block block = blockchain.get(blockchain.size() - 1);
        PriorityQueue<Transaction> mempoolQueue = new PriorityQueue<>((tx1, tx2) -> Double.compare(tx2.getWeightFee(), tx1.getWeightFee()));
        mempoolQueue.addAll(mempool);

        double sizeUsedUp = 0;
        double weightUsedUp = 0;

        ArrayList<String[]> logs = new ArrayList<>();
        BigDecimal totalUserPay = new BigDecimal("0");
        BigDecimal minerRewards = new BigDecimal("0");
        BigDecimal burned = new BigDecimal("0");

        double effectiveFee = 0;
        int index = 1;

        ArrayList<Transaction> txList = new ArrayList<>();
        ArrayList<Transaction> confirmedTxList = new ArrayList<>();
        ArrayList<Transaction> unconfirmedTxList = new ArrayList<>();

        while (!mempoolQueue.isEmpty()) {
            Transaction tx = mempoolQueue.poll();
            double txWeight = tx.getWeight();
            double txSize = tx.getSize();

            if (txWeight > weightLimit) {
                continue;
            }

            if ((weightUsedUp + txWeight) > weightLimit) {
                break;
            }

            txList.add(tx);
            sizeUsedUp += txSize;
            weightUsedUp += txWeight;
        }

        int split = txList.size() / 2;
        confirmedTxList = new ArrayList<>(txList.subList(0, split));
        unconfirmedTxList = new ArrayList<>(txList.subList(split, txList.size()));

        if (!unconfirmedTxList.isEmpty()) {
            effectiveFee = unconfirmedTxList.get(0).getWeightFee();
        }

        for (Transaction t : confirmedTxList) {
            double feePaid = t.getWeight() * effectiveFee;
            totalUserPay = totalUserPay.add(BigDecimal.valueOf(feePaid));
            logs.add(logStart(index, "YES", t.getHash(), t.getTotalFee(), feePaid, t.getWeight(), t.getSize()));
            index++;
        }

        for (Transaction t : unconfirmedTxList) {
            minerRewards = minerRewards.add(new BigDecimal(t.getTotalFee()));
            logs.add(logStart(index, "no", t.getHash(), t.getTotalFee(), 0, t.getWeight(), t.getSize()));
            index++;
        }

        burned = totalUserPay.subtract(minerRewards);

        // refill mempool with unconfirmed tx
        mempool.clear();
        mempool.addAll(mempoolQueue);
        mempool.addAll(unconfirmedTxList);

        return new Data(mempool, confirmedTxList, unconfirmedTxList, minerRewards, effectiveFee, burned, sizeUsedUp, weightUsedUp, logs);
    }
}
