package ZBRA.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TxResultsTopicConfig {

    @Bean
    public NewTopic txResultsTopic() {
        return new NewTopic("tx.results", 12, (short) 1);
    }
}
