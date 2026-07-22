package ZBRA.tfm;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import ZBRA.blockchain.Block;
import ZBRA.blockchain.Data;
import ZBRA.blockchain.Transaction;

class EIP1559Test {

    private static final double BASE_FEE = 10.0;
    private static final double TARGET = 100.0;
    private static final double WEIGHT_LIMIT = 1_000.0;

    @Test
    void includesTxsAtOrAboveBaseFeeAndSplitsBurnFromTip() {
        EIP1559 mechanism = new EIP1559();

        Block parent = parentBlock();

        Transaction premium = tx("a1", 10, 10, 200);
        Transaction atBase  = tx("b2", 10, 10, 100);
        Transaction tooLow  = tx("c3", 10, 10, 50);

        ArrayList<Transaction> mempool = new ArrayList<>(List.of(atBase, tooLow, premium));

        Data result = mechanism.fetchValidTX(mempool, WEIGHT_LIMIT,
                new ArrayList<>(List.of(parent)), null, TARGET);

        assertThat(result.getConfirmed()).extracting(Transaction::getHash)
                .containsExactly("a1", "b2");

        assertThat(result.getBaseFee()).isEqualTo(BASE_FEE);

        assertThat(result.getBurned()).isEqualByComparingTo(new BigDecimal("200.0"));

        assertThat(result.getRewards()).isEqualByComparingTo(new BigDecimal("100.0"));

        assertThat(result.getMempool()).extracting(Transaction::getHash).containsExactly("c3");
    }

    @Test
    void skipsTxsLargerThanTheWeightLimitButKeepsScanning() {
        EIP1559 mechanism = new EIP1559();
        Block parent = parentBlock();

        Transaction oversized = tx("big", 5_000, 20, 10_000);
        Transaction normal    = tx("ok",  10, 10, 200);

        ArrayList<Transaction> mempool = new ArrayList<>(List.of(oversized, normal));

        Data result = mechanism.fetchValidTX(mempool, WEIGHT_LIMIT,
                new ArrayList<>(List.of(parent)), null, TARGET);

        assertThat(result.getConfirmed()).extracting(Transaction::getHash).containsExactly("ok");
    }

    private static Block parentBlock() {
        Block parent = new Block();
        parent.setBaseFee(BASE_FEE);
        parent.setWeight(TARGET);
        return parent;
    }

    private static Transaction tx(String hash, double size, double weight, double totalFee) {
        return new Transaction(hash, size, weight, totalFee);
    }
}
