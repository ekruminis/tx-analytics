package ZBRA.kafka;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Component;

import com.ekruminis.txanalytics.wire.SimCommand;

import ZBRA.blockchain.Transaction;
import ZBRA.engine.CycleAssembler;
import ZBRA.engine.SimulationEngine;
import ZBRA.engine.SimulationEngineFactory;

@Component
public class SimulationCommandConsumer {

    private static final Logger log = LoggerFactory.getLogger(SimulationCommandConsumer.class);

    private final SimulationEngineFactory engineFactory;
    private final String bootstrapServers;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public SimulationCommandConsumer(SimulationEngineFactory engineFactory,
                                     @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        this.engineFactory = engineFactory;
        this.bootstrapServers = bootstrapServers;
    }

    @KafkaListener(topics = "simulation.commands")
    public void onCommand(SimCommand command) {
        log.info("received sim command: topic={} seed={} numMiners={} tfms={} label={}",
                command.sourceTopic(), command.seed(), command.numMiners(),
                command.tfms().keySet(), command.label());
        executor.submit(() -> execute(command));
    }

    private void execute(SimCommand command) {
        try {
            Instant genesis = Instant.parse(command.blockTimeGenesis());
            List<SimulationEngine> engines = command.tfms().entrySet().stream()
                    .map(e -> engineFactory.create(e.getKey(), command.seed(), command.numMiners(),
                            e.getValue(), genesis, command.blockTimeIntervalSeconds()))
                    .toList();
            CycleAssembler assembler = new CycleAssembler(engines, command.partitions());
            replayStream(command.sourceTopic(), command.partitions(), assembler);
            log.info("sim command complete: {} TFM(s) mined from {}", engines.size(), command.sourceTopic());
        } catch (Exception e) {
            log.error("sim command failed", e);
        }
    }

    private void replayStream(String topic, int partitions, CycleAssembler assembler) {
        try (KafkaConsumer<String, TransactionMessage> consumer = new KafkaConsumer<>(consumerProps())) {
            consumer.subscribe(List.of(topic));
            Set<Integer> eofPartitions = new HashSet<>();
            while (eofPartitions.size() < partitions) {
                ConsumerRecords<String, TransactionMessage> records = consumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<String, TransactionMessage> record : records) {
                    Header type = record.headers().lastHeader("type");
                    if (type != null && "eof".equals(new String(type.value(), StandardCharsets.UTF_8))) {
                        eofPartitions.add(record.partition());
                        assembler.markPartitionEof(record.partition());
                        continue;
                    }
                    long cycle = Long.parseLong(header(record, "cycle"));
                    String datasetHash = header(record, "dataset");
                    assembler.accept(record.partition(), cycle, datasetHash, toDomain(record.value()));
                }
            }
        }
    }

    private Properties consumerProps() {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        p.put(ConsumerConfig.GROUP_ID_CONFIG, "sim-cmd-" + UUID.randomUUID());
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        p.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TransactionMessage.class.getName());
        p.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        p.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return p;
    }

    private static String header(ConsumerRecord<String, TransactionMessage> record, String key) {
        return new String(record.headers().lastHeader(key).value(), StandardCharsets.UTF_8);
    }

    private static Transaction toDomain(TransactionMessage m) {
        double size = Double.parseDouble(m.getSize());
        double fee = Double.parseDouble(m.getFee());
        return new Transaction(m.getHash(), size, size, fee);
    }
}
