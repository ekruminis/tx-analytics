package ZBRA.kafka;

import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.ekruminis.txanalytics.wire.TxResult;

@Component
public class TxResultPublisher {

    private final KafkaTemplate<String, TxResult> kafka;

    public TxResultPublisher(KafkaTemplate<String, TxResult> kafka) {
        this.kafka = kafka;
    }

    public void publish(List<TxResult> txs) {
        for (TxResult tx : txs) {
            kafka.send("tx.results", tx.txHash(), tx);
        }
    }
}
