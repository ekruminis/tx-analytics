package ZBRA.tfm;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import ZBRA.blockchain.Block;
import ZBRA.blockchain.Data;
import ZBRA.blockchain.Transaction;

class SecondPriceTest {

    private static final double LIMIT = 1_000.0;

    @Test
    void allIncludedTxsPayTheLowestIncludedFeePerWeight() {
        SecondPrice mechanism = new SecondPrice();
        Transaction a = tx("a", 10, 10, 300);
        Transaction b = tx("b", 10, 10, 200);
        Transaction c = tx("c", 10, 10, 100);

        Data result = mechanism.fetchValidTX(mempool(a, b, c), LIMIT, chain(), null, 0);

        assertThat(result.getConfirmed()).hasSize(3);
        assertThat(result.getBaseFee()).isEqualTo(10.0);
        assertThat(result.getRewards()).isEqualByComparingTo(new BigDecimal("300.0"));
    }

    @Test
    void producesNothingWhenNoTxFitsTheLimit() {
        SecondPrice mechanism = new SecondPrice();
        Data result = mechanism.fetchValidTX(
                mempool(tx("big", 10, 2_000, 100)), LIMIT, chain(), null, 0);

        assertThat(result.getConfirmed()).isEmpty();
        assertThat(result.getBaseFee()).isEqualTo(0.0);
        assertThat(result.getRewards()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private static ArrayList<Transaction> mempool(Transaction... txs) {
        return new ArrayList<>(List.of(txs));
    }

    private static ArrayList<Block> chain() {
        return new ArrayList<>(List.of(new Block()));
    }

    private static Transaction tx(String hash, double size, double weight, double totalFee) {
        return new Transaction(hash, size, weight, totalFee);
    }
}
