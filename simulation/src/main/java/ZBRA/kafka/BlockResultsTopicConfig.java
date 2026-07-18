package ZBRA.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BlockResultsTopicConfig {

    @Bean
    public NewTopic blocksResultsTopic() {
        return new NewTopic("blocks.results", 3, (short) 1);
    }
}
