package ZBRA.tfm;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ZBRA.blockchain.Block;
import ZBRA.blockchain.Data;
import ZBRA.blockchain.Miner;
import ZBRA.blockchain.Transaction;

class ReservePoolTest {

    private static final double LIMIT = 1_000.0;

    @Test
    void movingAveragePayoutAveragesHistoricalRewards() {
        ReservePool mechanism = new ReservePool();

        ArrayList<Block> chain = new ArrayList<>(List.of(
                blockWithReward(new BigDecimal("10")),
                blockWithReward(new BigDecimal("20")),
                blockWithReward(new BigDecimal("30"))));

        BigDecimal avg = mechanism.movingAveragePayout(chain, 1.0, 100.0);

        assertThat(avg).isEqualByComparingTo(new BigDecimal("20"));
    }

    @Test
    void movingAveragePayoutFallsBackToBaseFeeTimesTargetWhenNoHistory() {
        ReservePool mechanism = new ReservePool();

        ArrayList<Block> chain = new ArrayList<>(List.of(new Block()));

        BigDecimal avg = mechanism.movingAveragePayout(chain, 2.0, 50.0);

        assertThat(avg).isEqualByComparingTo(new BigDecimal("100"));
    }

    @Test
    void excludesTxsWhoseFeePerWeightIsBelowTheBaseFee() {
        ReservePool mechanism = new ReservePool();

        Block parent = new Block();
        parent.setBaseFee(10.0);
        parent.setWeight(100.0);
        parent.updatePool(BigDecimal.ZERO);

        Transaction included = tx("in", 10, 10, 200);
        Transaction marginal = tx("eq", 10, 10, 100);
        Transaction excluded = tx("out", 10, 10, 50);

        Data result = mechanism.fetchValidTX(
                new ArrayList<>(List.of(included, marginal, excluded)),
                LIMIT, new ArrayList<>(List.of(parent)), new Miner(1, 100), 100.0);

        assertThat(result.getConfirmed()).extracting(Transaction::getHash).containsExactly("in", "eq");
        assertThat(result.getBaseFee()).isEqualTo(10.0);
    }

    private static Block blockWithReward(BigDecimal reward) {
        Block b = mock(Block.class);
        when(b.getRewards()).thenReturn(reward);
        return b;
    }

    private static Transaction tx(String hash, double size, double weight, double totalFee) {
        return new Transaction(hash, size, weight, totalFee);
    }
}
