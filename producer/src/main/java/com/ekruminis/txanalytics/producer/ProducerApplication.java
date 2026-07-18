package com.ekruminis.txanalytics.producer;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

import com.ekruminis.txanalytics.producer.model.Transaction;
import com.ekruminis.txanalytics.producer.pacing.PacingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication
public class ProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProducerApplication.class, args);
    }

    @Bean
    public CommandLineRunner sendTransactions(
            KafkaTemplate<String, Transaction> kafkaTemplate,
            ObjectMapper objectMapper,
            PacingService pacingService,
            @Value("${producer.input-file}") String inputFile,
            @Value("${producer.seed}") long seed,
            @Value("${producer.partitions}") int partitions) {
        return args -> {
            Path path = Paths.get(inputFile);
            System.out.println("Reading transactions from " + path.toAbsolutePath());

            String datasetHash = hashFile(path);
            System.out.println("Input dataset hash: " + datasetHash);

            List<Transaction> txs = objectMapper.readValue(
                    path.toFile(),
                    new TypeReference<List<Transaction>>() {});
            System.out.println("Loaded " + txs.size() + " transactions; shuffling with seed " + seed);
            Collections.shuffle(txs, new Random(seed));

            long count = 0;
            long cycles = 0;
            long start = System.currentTimeMillis();
            Iterator<Transaction> iter = txs.iterator();

            while (iter.hasNext()) {
                int batchSize = pacingService.sampleBatchSize();
                cycles++;
                for (int i = 0; i < batchSize && iter.hasNext(); i++) {
                    Transaction tx = iter.next();
                    ProducerRecord<String, Transaction> record =
                            new ProducerRecord<>("transactions.raw", tx.getHash(), tx);
                    record.headers().add("cycle",
                            Long.toString(cycles).getBytes(StandardCharsets.UTF_8));
                    record.headers().add("dataset",
                            datasetHash.getBytes(StandardCharsets.UTF_8));
                    kafkaTemplate.send(record);
                    count++;
                    if (count % 10_000 == 0) {
                        System.out.println("Sent " + count + " (cycles=" + cycles + ")");
                    }
                }
            }

            Transaction eofPayload = new Transaction();
            for (int p = 0; p < partitions; p++) {
                ProducerRecord<String, Transaction> record =
                        new ProducerRecord<>("transactions.raw", p, null, eofPayload);
                record.headers().add("type", "eof".getBytes(StandardCharsets.UTF_8));
                record.headers().add("dataset",
                        datasetHash.getBytes(StandardCharsets.UTF_8));
                kafkaTemplate.send(record);
            }
            System.out.println("Sent EOF sentinels to " + partitions + " partitions");

            kafkaTemplate.flush();
            long elapsedMs = System.currentTimeMillis() - start;
            double tps = elapsedMs == 0 ? 0 : (count * 1000.0) / elapsedMs;
            System.out.printf("Done. sent=%d cycles=%d elapsed=%dms throughput=%.0f tx/sec%n",
                    count, cycles, elapsedMs, tps);
        };
    }

    private static String hashFile(Path path) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(path)) {
            byte[] buf = new byte[1 << 20]; // 1 MB
            int read;
            while ((read = in.read(buf)) != -1) {
                md.update(buf, 0, read);
            }
        }
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(String.format("%02x", digest[i]));
        }
        return sb.toString();
    }
}
