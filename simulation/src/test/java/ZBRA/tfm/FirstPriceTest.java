package ZBRA.tfm;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import ZBRA.blockchain.Block;
import ZBRA.blockchain.Data;
import ZBRA.blockchain.Transaction;

class FirstPriceTest {

    private static final double LIMIT = 1_000.0;

    @Test
    void includesEveryFittingTxAndPaysSumOfOfferedFees() {
        FirstPrice mechanism = new FirstPrice();
        Transaction a = tx("a", 10, 10, 300);
        Transaction b = tx("b", 10, 10, 200);
        Transaction c = tx("c", 10, 10, 100);

        Data result = mechanism.fetchValidTX(mempool(a, b, c), LIMIT, chain(), null, 0);

        assertThat(result.getConfirmed()).extracting(Transaction::getHash)
                .containsExactlyInAnyOrder("a", "b", "c");
        assertThat(result.getRewards()).isEqualByComparingTo(new BigDecimal("600.0"));
    }

    @Test
    void stopsFillingOnceTheWeightLimitIsReached() {
        FirstPrice mechanism = new FirstPrice();
        Data result = mechanism.fetchValidTX(
                mempool(tx("a", 10, 10, 300), tx("b", 10, 10, 200), tx("c", 10, 10, 100)),
                25.0, chain(), null, 0);

        assertThat(result.getConfirmed()).hasSize(2);
    }

    @Test
    void skipsAnIndividualTxHeavierThanTheLimitButKeepsTheRest() {
        FirstPrice mechanism = new FirstPrice();
        Transaction oversized = tx("big", 10, 2_000, 9_000);
        Transaction ok = tx("ok", 10, 10, 100);

        Data result = mechanism.fetchValidTX(mempool(oversized, ok), LIMIT, chain(), null, 0);

        assertThat(result.getConfirmed()).extracting(Transaction::getHash).containsExactly("ok");
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
