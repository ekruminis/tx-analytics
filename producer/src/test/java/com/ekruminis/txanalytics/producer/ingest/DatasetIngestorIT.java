package com.ekruminis.txanalytics.producer.ingest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.ekruminis.txanalytics.producer.model.Transaction;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@Testcontainers
class DatasetIngestorIT {

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @DynamicPropertySource
    static void kafkaProps(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Autowired
    DatasetIngestor ingestor;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void ingestsEveryTransactionThenEmitsEofSentinel(@org.junit.jupiter.api.io.TempDir Path tmp) throws Exception {
        int txCount = 100;
        String topic = "ingest.test";
        List<Transaction> dataset = sampleDataset(txCount);

        Path datasetFile = tmp.resolve("transactions.json");
        objectMapper.writeValue(datasetFile.toFile(), dataset);

        createTopic(topic, 1);

        ingestor.ingest(datasetFile, topic, 1,
                "testhash", 10, 0.5, 15L);

        List<ConsumerRecord<String, Transaction>> data = new ArrayList<>();
        int eofSentinels = 0;

        try (KafkaConsumer<String, Transaction> consumer = new KafkaConsumer<>(consumerProps())) {
            consumer.subscribe(List.of(topic));
            long deadline = System.currentTimeMillis() + Duration.ofSeconds(30).toMillis();
            while (data.size() < txCount && System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, Transaction> polled = consumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<String, Transaction> record : polled) {
                    if (isEof(record)) {
                        eofSentinels++;
                    } else {
                        data.add(record);
                    }
                }
            }
        }

        assertThat(data).hasSize(txCount);
        assertThat(eofSentinels).isEqualTo(1);
        assertThat(data).allSatisfy(r -> assertThat(r.key()).isEqualTo(r.value().getHash()));
        assertThat(data)
                .extracting(r -> r.value().getHash())
                .containsExactlyInAnyOrderElementsOf(dataset.stream().map(Transaction::getHash).toList());
    }

    private static List<Transaction> sampleDataset(int n) {
        List<Transaction> txs = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Transaction t = new Transaction();
            t.setHash(String.format("%064x", i));
            t.setFee(String.format("0.0000%04d", i));
            t.setSize(String.valueOf(200 + i));
            txs.add(t);
        }
        return txs;
    }

    private static boolean isEof(ConsumerRecord<String, Transaction> record) {
        Header type = record.headers().lastHeader("type");
        return type != null && "eof".equals(new String(type.value(), StandardCharsets.UTF_8));
    }

    private static void createTopic(String topic, int partitions) throws Exception {
        Map<String, Object> cfg = Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        try (Admin admin = Admin.create(cfg)) {
            admin.createTopics(List.of(new NewTopic(topic, partitions, (short) 1))).all().get();
        }
    }

    private static Map<String, Object> consumerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "it-verifier");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Transaction.class.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return props;
    }
}
