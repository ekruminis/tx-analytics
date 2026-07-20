package com.ekruminis.txanalytics.indexer.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.ekruminis.txanalytics.wire.BlockResult;
import com.ekruminis.txanalytics.wire.MinerRoster;
import com.ekruminis.txanalytics.wire.TxResult;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, BlockResult> blockResultListenerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrap,
            @Value("${spring.kafka.consumer.group-id}") String groupId,
            @Value("${spring.kafka.consumer.auto-offset-reset}") String autoOffsetReset) {
        return typedFactory(bootstrap, groupId, autoOffsetReset, BlockResult.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TxResult> txResultListenerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrap,
            @Value("${spring.kafka.consumer.group-id}") String groupId,
            @Value("${spring.kafka.consumer.auto-offset-reset}") String autoOffsetReset) {
        ConcurrentKafkaListenerContainerFactory<String, TxResult> factory =
                typedFactory(bootstrap, groupId, autoOffsetReset, TxResult.class);
        factory.setBatchListener(true);
        factory.setConcurrency(12);
        factory.getContainerProperties().getKafkaConsumerProperties()
                .put("max.poll.records", "5000");
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, BlockResult> esBlockResultListenerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrap,
            @Value("${indexer.elastic.group-id}") String groupId,
            @Value("${spring.kafka.consumer.auto-offset-reset}") String autoOffsetReset) {
        return typedFactory(bootstrap, groupId, autoOffsetReset, BlockResult.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TxResult> esTxResultListenerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrap,
            @Value("${indexer.elastic.group-id}") String groupId,
            @Value("${spring.kafka.consumer.auto-offset-reset}") String autoOffsetReset) {
        ConcurrentKafkaListenerContainerFactory<String, TxResult> factory =
                typedFactory(bootstrap, groupId, autoOffsetReset, TxResult.class);
        factory.setBatchListener(true);
        factory.setConcurrency(12);
        factory.getContainerProperties().getKafkaConsumerProperties()
                .put("max.poll.records", "5000");
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MinerRoster> esMinerRosterListenerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrap,
            @Value("${indexer.elastic.group-id}") String groupId,
            @Value("${spring.kafka.consumer.auto-offset-reset}") String autoOffsetReset) {
        return typedFactory(bootstrap, groupId, autoOffsetReset, MinerRoster.class);
    }

    private static <T> ConcurrentKafkaListenerContainerFactory<String, T> typedFactory(
            String bootstrap, String groupId, String autoOffsetReset, Class<T> valueType) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        JsonDeserializer<T> valueDeserializer = new JsonDeserializer<>(valueType);
        valueDeserializer.addTrustedPackages("*");
        valueDeserializer.setUseTypeHeaders(false);

        ConsumerFactory<String, T> consumerFactory =
                new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer);
        ConcurrentKafkaListenerContainerFactory<String, T> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}
