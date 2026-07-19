package ZBRA.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommandTopicConfig {

    @Bean
    public NewTopic simulationCommandsTopic() {
        return new NewTopic("simulation.commands", 1, (short) 1);
    }
}
