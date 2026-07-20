package ZBRA.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinerRosterTopicConfig {

    @Bean
    public NewTopic minersRosterTopic() {
        return new NewTopic("miners.roster", 1, (short) 1);
    }
}
