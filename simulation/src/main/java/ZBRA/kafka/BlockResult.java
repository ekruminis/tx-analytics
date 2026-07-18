package ZBRA.kafka;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BlockResult(
        String runId,
        String tfm,
        int height,
        Integer winnerMinerId,
        BigDecimal payout,
        double size,
        long txCount,
        String merkleRoot,
        Double baseFee,
        BigDecimal burned,
        BigDecimal pool,
        Integer unconfirmedCount) {
}
