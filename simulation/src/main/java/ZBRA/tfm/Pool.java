package ZBRA.tfm;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

import ZBRA.blockchain.Block;
import ZBRA.blockchain.Data;
import ZBRA.blockchain.Miner;
import ZBRA.blockchain.Transaction;

public class Pool extends AbstractTFM {
    private final static String type = "Reserve Pool";
    private double maxSharedTake = 0.5; // percentage of how much of the total payout can be taken away from shared pool
    private double blockRewardPercentage = 1.0; // percentage of the current block payout they can take extra for a block reward
    private Integer WINDOW_SIZE = 144; // number of blocks to consider for moving average miner payout

    public Pool() {
        super(type);
    }

    public Pool(double maxSharedTake) {
        super(type);
        this.maxSharedTake = maxSharedTake;
    }


    // used for logging each tx data
    public String[] logStart(int index, String hash, double feeTotal, double feeBase, double feeTip, double weight, double size) {
        return new String[] {
                String.valueOf(index),
                hash,
                String.valueOf(feeTotal),
                String.valueOf(feeBase),
                String.valueOf(feeTip),
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
                "Miner Rewards",
                "Block Reward (total)",
                "Block Reward in Base Fees",
                "Block Reward in Tips",
                "Block Size",
                "Block Weight",
                "Number of TX",
                "Base Fee",
                "Reserve Pool Balance",
                "Pool Contribution",
                "TX Index",
                "TX Hash",
                "TX Paid",
                "TX Base Fee",
                "TX Tip",
                "TX Weight",
                "TX Size"
        };
    }

    // Main EIP-1559 Mechanism Implementation
    @Override
    public Data fetchValidTX(ArrayList<Transaction> mempool, double weightLimit, ArrayList<Block> blockchain, Miner miner, double weightTarget) {
        // Sort mempool by highest fee per byte
        mempool.sort((tx1, tx2) -> Double.compare(tx2.getWeightFee(), tx1.getWeightFee()));

        Block block = blockchain.get(blockchain.size() - 1);

        double sizeUsedUp = 0;
        double weightUsedUp = 0;

        ArrayList<String[]> logs = new ArrayList<>();
        ArrayList<Transaction> txList = new ArrayList<>();

        BigDecimal currentPoolBalance = block.getPool();
        BigDecimal minerRewards = BigDecimal.ZERO;
        BigDecimal blockFeeTotal = BigDecimal.ZERO;
        BigDecimal blockTipTotal = BigDecimal.ZERO;
        BigDecimal blockBaseFeeTotal = BigDecimal.ZERO;
        BigDecimal blockPoolContribution = BigDecimal.ZERO;

        boolean minerTakenPublic = false;
        boolean minerTakenPrivate = false;

        double baseFee = block.getBaseFee() * (1.0 + 0.0625 * ((block.getWeight() - weightTarget) / weightTarget));

        int index = 1;
        int i = 0;
        int mempoolSize = mempool.size();

        // Iterate by index instead of removing elements
        while (i < mempoolSize) {
            Transaction tx = mempool.get(i);
            double txSize = tx.getSize();
            double txWeight = tx.getWeight();

            if (txWeight > weightLimit) {
                i++;
                continue;
            }
            if ((weightUsedUp + txWeight) > weightLimit) {
                break;
            }
            if (tx.getWeightFee() < baseFee) {
                break;
            }

            txList.add(tx);
            BigDecimal txTotalFee = BigDecimal.valueOf(tx.getTotalFee());
            BigDecimal txBaseFee = BigDecimal.valueOf(baseFee * txWeight);
            BigDecimal txTip = txTotalFee.subtract(txBaseFee);

            blockFeeTotal = blockFeeTotal.add(txTotalFee);
            blockBaseFeeTotal = blockBaseFeeTotal.add(txBaseFee);
            blockTipTotal = blockTipTotal.add(txTip);

            sizeUsedUp += txSize;
            weightUsedUp += txWeight;

            logs.add(logStart(index, tx.getHash(), tx.getTotalFee(), txBaseFee.doubleValue(), txTip.doubleValue(), txWeight, txSize));
            index++;
            i++;
        }

        // Remove confirmed TXs from mempool in one batch operation
        mempool.subList(0, txList.size()).clear();

        BigDecimal optimalPayout = movingAveragePayout(blockchain, baseFee, weightTarget);
        BigDecimal difference = blockFeeTotal.subtract(optimalPayout);
        BigDecimal maxPublicTakeout = optimalPayout.multiply(BigDecimal.valueOf(maxSharedTake));

        if (currentPoolBalance.signum() == 1 && difference.signum() == -1) {
            difference = difference.negate();

            if (miner.getPublicPoolEffect().signum() == 1) {
                if (miner.getPublicPoolEffect().compareTo(difference) >= 0) {
                    minerRewards = optimalPayout;
                    minerTakenPublic = true;
                    currentPoolBalance = currentPoolBalance.subtract(difference);
                    miner.updatePublicPoolEffect(miner.getPublicPoolEffect().subtract(difference).max(BigDecimal.ZERO));
                    blockPoolContribution = blockPoolContribution.subtract(difference);
                } else {
                    if (difference.compareTo(maxPublicTakeout) >= 0) {
                        miner.updatePublicPoolEffect(miner.getPublicPoolEffect().subtract(maxPublicTakeout).max(BigDecimal.ZERO));
                        currentPoolBalance = currentPoolBalance.subtract(maxPublicTakeout);
                        blockPoolContribution = blockPoolContribution.subtract(maxPublicTakeout);
                        minerRewards = minerRewards.add(maxPublicTakeout);
                        minerTakenPublic = true;
                        difference = difference.subtract(maxPublicTakeout);

                        if (miner.getPublicPoolEffect().signum() == 1 && difference.signum() == 1) {
                            if (miner.getPublicPoolEffect().compareTo(difference) >= 0) {
                                miner.updatePublicPoolEffect(miner.getPublicPoolEffect().subtract(difference).max(BigDecimal.ZERO));
                                currentPoolBalance = currentPoolBalance.subtract(difference);
                                blockPoolContribution = blockPoolContribution.subtract(difference);
                                minerRewards = minerRewards.add(difference);
                            } else {
                                currentPoolBalance = currentPoolBalance.subtract(miner.getPublicPoolEffect());
                                minerRewards = minerRewards.add(miner.getPublicPoolEffect());
                                blockPoolContribution = blockPoolContribution.subtract(miner.getPublicPoolEffect());
                                miner.updatePublicPoolEffect(BigDecimal.ZERO);
                            }
                        }
                    } else {
                        currentPoolBalance = currentPoolBalance.subtract(difference);
                        minerRewards = minerRewards.add(difference);
                        miner.updatePublicPoolEffect(miner.getPublicPoolEffect().subtract(difference).max(BigDecimal.ZERO));
                        blockPoolContribution = blockPoolContribution.subtract(difference);
                        minerTakenPublic = true;
                    }
                }
            } else {
                if (currentPoolBalance.compareTo(maxPublicTakeout) >= 0) {
                    minerRewards = minerRewards.add(maxPublicTakeout);
                    currentPoolBalance = currentPoolBalance.subtract(maxPublicTakeout);
                    miner.updatePublicPoolEffect(miner.getPublicPoolEffect().subtract(maxPublicTakeout).max(BigDecimal.ZERO));
                    blockPoolContribution = blockPoolContribution.subtract(maxPublicTakeout);
                    minerTakenPublic = true;
                } else {
                    minerRewards = minerRewards.add(currentPoolBalance);
                    miner.updatePublicPoolEffect(miner.getPublicPoolEffect().subtract(currentPoolBalance).max(BigDecimal.ZERO));
                    blockPoolContribution = blockPoolContribution.subtract(currentPoolBalance);
                    minerTakenPublic = true;
                    currentPoolBalance = BigDecimal.ZERO;
                }
            }
        } else if (difference.signum() == 1) {
            minerRewards = optimalPayout;
            BigDecimal poolContribution = blockFeeTotal.subtract(minerRewards);
            currentPoolBalance = currentPoolBalance.add(poolContribution);
            miner.updatePublicPoolEffect(miner.getPublicPoolEffect().add(poolContribution));
            blockPoolContribution = blockPoolContribution.add(poolContribution);
        }

        return new Data(mempool, txList, minerRewards, baseFee, currentPoolBalance, sizeUsedUp, weightUsedUp,
                blockPoolContribution, minerTakenPublic, minerTakenPrivate, blockFeeTotal, blockTipTotal, logs);
    }

    public BigDecimal movingAveragePayout(ArrayList<Block> blockchain, double baseFee, double weightTarget) {
        // Find the first block with a non-null reward (skip genesis if needed)
        int firstValidIndex = 0;
        while (firstValidIndex < blockchain.size() && blockchain.get(firstValidIndex).getRewards() == null) {
            firstValidIndex++;
        }

        int startIndex = blockchain.size() >= this.WINDOW_SIZE + firstValidIndex
            ? blockchain.size() - this.WINDOW_SIZE
            : firstValidIndex;

        BigDecimal sumPayouts = BigDecimal.ZERO;
        int count = 0;
        for (int i = startIndex; i < blockchain.size(); i++) {
            BigDecimal reward = blockchain.get(i).getRewards();
            if (reward != null) {
                sumPayouts = sumPayouts.add(reward);
                count++;
            }
        }

        if (count == 0) {
            // Fallback: use current block's optimal payout if no valid history
            return BigDecimal.valueOf(baseFee * weightTarget).setScale(10, RoundingMode.HALF_UP);
        }
        return sumPayouts.divide(BigDecimal.valueOf(count), 10, RoundingMode.HALF_UP);
    }

}
