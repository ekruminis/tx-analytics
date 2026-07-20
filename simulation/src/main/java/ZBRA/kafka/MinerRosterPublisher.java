package ZBRA.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.ekruminis.txanalytics.wire.MinerRoster;

@Component
public class MinerRosterPublisher {

    private final KafkaTemplate<String, MinerRoster> kafka;

    public MinerRosterPublisher(KafkaTemplate<String, MinerRoster> kafka) {
        this.kafka = kafka;
    }

    public void publish(MinerRoster roster) {
        kafka.send("miners.roster", roster.experimentId(), roster);
    }
}
