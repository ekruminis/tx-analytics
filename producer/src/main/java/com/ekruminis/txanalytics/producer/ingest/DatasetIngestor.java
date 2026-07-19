package com.ekruminis.txanalytics.producer.ingest;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.ekruminis.txanalytics.producer.model.Transaction;
import com.ekruminis.txanalytics.producer.pacing.PacingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class DatasetIngestor {

    private static final Logger log = LoggerFactory.getLogger(DatasetIngestor.class);

    private final KafkaTemplate<String, Object> kafka;
    private final ObjectMapper objectMapper;

    public DatasetIngestor(KafkaTemplate<String, Object> kafka, ObjectMapper objectMapper) {
        this.kafka = kafka;
        this.objectMapper = objectMapper;
    }

    public void ingest(Path file, String topic, int partitions, String datasetHash,
                       double meanTxPerCycle, double alpha, long seed) {
        try {
            List<Transaction> txs = objectMapper.readValue(
                    file.toFile(), new TypeReference<List<Transaction>>() {});
            log.info("ingesting {} txs from {} -> {} (seed {})", txs.size(), file, topic, seed);
            Collections.shuffle(txs, new Random(seed));

            PacingService pacing = new PacingService(meanTxPerCycle, alpha, seed);
            byte[] datasetBytes = datasetHash.getBytes(StandardCharsets.UTF_8);

            AtomicLong failures = new AtomicLong();
            long count = 0;
            long cycles = 0;
            Iterator<Transaction> iter = txs.iterator();
            while (iter.hasNext()) {
                int batchSize = pacing.sampleBatchSize();
                cycles++;
                for (int i = 0; i < batchSize && iter.hasNext(); i++) {
                    Transaction tx = iter.next();
                    ProducerRecord<String, Object> record = new ProducerRecord<>(topic, tx.getHash(), tx);
                    record.headers().add("cycle", Long.toString(cycles).getBytes(StandardCharsets.UTF_8));
                    record.headers().add("dataset", datasetBytes);
                    kafka.send(record).whenComplete((res, ex) -> {
                        if (ex != null) {
                            failures.incrementAndGet();
                        }
                    });
                    count++;
                }
            }

            kafka.flush();
            if (failures.get() > 0) {
                throw new IllegalStateException(failures.get() + " of " + count
                        + " records failed to send — aborting before EOF; " + topic + " left incomplete");
            }

            Transaction eofPayload = new Transaction();
            for (int p = 0; p < partitions; p++) {
                ProducerRecord<String, Object> record = new ProducerRecord<>(topic, p, null, eofPayload);
                record.headers().add("type", "eof".getBytes(StandardCharsets.UTF_8));
                record.headers().add("dataset", datasetBytes);
                kafka.send(record);
            }
            kafka.flush();
            log.info("ingestion done: {} txs, {} cycles, {} EOF sentinels", count, cycles, partitions);
        } catch (Exception e) {
            throw new IllegalStateException("ingestion failed for " + file, e);
        }
    }

    public static String hashFile(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[1 << 20];
            int read;
            while ((read = in.read(buf)) != -1) {
                md.update(buf, 0, read);
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("could not hash dataset file " + path, e);
        }
    }
}
