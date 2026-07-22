package ZBRA;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.ekruminis.txanalytics.wire.SimCommand;

import ZBRA.kafka.TransactionMessage;
import ZBRA.persistence.BlockRepository;

@SpringBootTest
@Testcontainers
class SimulationPipelineIT {

    private static final String SOURCE_TOPIC = "sim.it.txs";
    private static final String COMMAND_TOPIC = "simulation.commands";
    private static final int PARTITIONS = 1;
    private static final int EXPECTED_BLOCKS = 3;

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    KafkaTemplate<String, Object> kafka;

    @Autowired
    BlockRepository blockRepo;

    @Test
    void minesOneBlockPerCycleAndPersistsThemToPostgres() throws Exception {
        createTopic(SOURCE_TOPIC, PARTITIONS);

        sendTx(1, hash(1), "0.05", "220");
        sendTx(1, hash(2), "0.03", "180");
        sendTx(2, hash(3), "0.04", "210");
        sendTx(2, hash(4), "0.02", "160");
        sendTx(3, hash(5), "0.06", "240");
        sendTx(3, hash(6), "0.01", "150");
        sendEof(0);
        kafka.flush();

        SimCommand command = new SimCommand(
                SOURCE_TOPIC, PARTITIONS, 42L, 4,
                "2024-01-01T00:00:00Z", 600L,
                "it-run", Map.of("first_price", Map.of()));
        kafka.send(COMMAND_TOPIC, command);
        kafka.flush();

        await().atMost(Duration.ofSeconds(60)).pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> assertThat(blockRepo.count()).isEqualTo((long) EXPECTED_BLOCKS));
    }

    private void sendTx(long cycle, String hash, String fee, String size) {
        TransactionMessage msg = new TransactionMessage();
        msg.setHash(hash);
        msg.setFee(fee);
        msg.setSize(size);

        ProducerRecord<String, Object> record = new ProducerRecord<>(SOURCE_TOPIC, 0, hash, msg);
        record.headers().add("cycle", Long.toString(cycle).getBytes(StandardCharsets.UTF_8));
        record.headers().add("dataset", "testdataset".getBytes(StandardCharsets.UTF_8));
        kafka.send(record);
    }

    private void sendEof(int partition) {
        ProducerRecord<String, Object> record =
                new ProducerRecord<>(SOURCE_TOPIC, partition, null, new TransactionMessage());
        record.headers().add("type", "eof".getBytes(StandardCharsets.UTF_8));
        kafka.send(record);
    }

    private static void createTopic(String topic, int partitions) throws Exception {
        Map<String, Object> cfg = Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        try (Admin admin = Admin.create(cfg)) {
            admin.createTopics(List.of(new NewTopic(topic, partitions, (short) 1))).all().get();
        }
    }

    private static String hash(int n) {
        return String.format("%064x", n);
    }
}
