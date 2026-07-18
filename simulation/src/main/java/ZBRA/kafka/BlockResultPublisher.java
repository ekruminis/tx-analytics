package ZBRA.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class BlockResultPublisher {

    private final KafkaTemplate<String, BlockResult> kafka;

    public BlockResultPublisher(KafkaTemplate<String, BlockResult> kafka) {
        this.kafka = kafka;
    }

    public void publish(BlockResult result) {
        kafka.send("blocks.results", result.runId(), result);
    }
}
