package com.ekruminis.txanalytics.producer.ingest;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TopicOps {

    private static final Logger log = LoggerFactory.getLogger(TopicOps.class);

    private final String bootstrapServers;

    public TopicOps(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public boolean isComplete(String topic, int expectedPartitions) {
        try (KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(consumerProps())) {
            List<PartitionInfo> infos = consumer.partitionsFor(topic, Duration.ofSeconds(5));
            if (infos == null || infos.size() != expectedPartitions) {
                return false;
            }
            List<TopicPartition> tps = infos.stream()
                    .map(pi -> new TopicPartition(topic, pi.partition())).toList();
            consumer.assign(tps);
            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(tps);
            for (TopicPartition tp : tps) {
                long end = endOffsets.get(tp);
                if (end == 0) {
                    return false;
                }
                consumer.seek(tp, end - 1);
            }
            Set<Integer> eofSeen = new HashSet<>();
            long deadline = System.currentTimeMillis() + 10_000;
            while (eofSeen.size() < tps.size() && System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, byte[]> r : consumer.poll(Duration.ofMillis(500))) {
                    Header type = r.headers().lastHeader("type");
                    if (type != null && "eof".equals(new String(type.value(), StandardCharsets.UTF_8))) {
                        eofSeen.add(r.partition());
                    }
                }
            }
            return eofSeen.size() == tps.size();
        } catch (Exception e) {
            log.warn("completeness check failed for {} — treating as incomplete", topic, e);
            return false;
        }
    }

    public void recreate(String topic, int partitions) {
        try (AdminClient admin = AdminClient.create(adminProps())) {
            if (admin.listTopics().names().get().contains(topic)) {
                admin.deleteTopics(List.of(topic)).all().get();
                long deadline = System.currentTimeMillis() + 15_000;
                while (admin.listTopics().names().get().contains(topic)
                        && System.currentTimeMillis() < deadline) {
                    Thread.sleep(300);
                }
            }
            admin.createTopics(List.of(new NewTopic(topic, partitions, (short) 1))).all().get();
            log.info("created topic {} with {} partitions", topic, partitions);
        } catch (Exception e) {
            throw new IllegalStateException("failed to recreate topic " + topic, e);
        }
    }

    private Properties adminProps() {
        Properties p = new Properties();
        p.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return p;
    }

    private Properties consumerProps() {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        p.put(ConsumerConfig.GROUP_ID_CONFIG, "producer-completeness-" + UUID.randomUUID());
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        return p;
    }
}
