package com.ekruminis.txanalytics.producer;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

import com.ekruminis.txanalytics.producer.model.Transaction;
import com.ekruminis.txanalytics.producer.pacing.PacingService;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
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
            @Value("${producer.input-file}") String inputFile) {
        return args -> {
            Path path = Paths.get(inputFile);
            System.out.println("Reading transactions from " + path.toAbsolutePath());

            long count = 0;
            long skipped = 0;
            long cycles = 0;
            long start = System.currentTimeMillis();

            try (JsonParser parser = objectMapper.getFactory().createParser(path.toFile())) {
                if (parser.nextToken() != JsonToken.START_ARRAY) {
                    throw new IllegalStateException("Expected JSON array at root of " + path);
                }

                boolean sourceExhausted = false;
                while (!sourceExhausted) {
                    int batchSize = pacingService.sampleBatchSize();
                    cycles++;

                    int sentInBatch = 0;
                    while (sentInBatch < batchSize) {
                        // Parser wedge — unrecoverable, stop entire run
                        JsonToken token;
                        try {
                            token = parser.nextToken();
                        } catch (Exception e) {
                            System.err.println("Parser wedged at count=" + count
                                    + " (skipped=" + skipped + "): " + e.getMessage() + " — stopping");
                            sourceExhausted = true;
                            break;
                        }
                        if (token == null || token == JsonToken.END_ARRAY) {
                            sourceExhausted = true;
                            break;
                        }
                        if (token != JsonToken.START_OBJECT) continue;

                        try {
                            Transaction tx = objectMapper.readValue(parser, Transaction.class);
                            kafkaTemplate.send("transactions.raw", tx.getHash(), tx);
                            count++;
                            sentInBatch++;
                            if (count % 10_000 == 0) {
                                System.out.println("Sent " + count + " (skipped=" + skipped + ", cycles=" + cycles + ")");
                            }
                        } catch (Exception e) {
                            skipped++;
                            try {
                                parser.skipChildren();
                            } catch (Exception ignore) {
                                // parser can't recover; next nextToken() should bail out of the loop
                            }
                        }
                    }
                }
            } finally {
                kafkaTemplate.flush();
                long elapsedMs = System.currentTimeMillis() - start;
                double tps = elapsedMs == 0 ? 0 : (count * 1000.0) / elapsedMs;
                System.out.printf("Done. sent=%d skipped=%d cycles=%d elapsed=%dms throughput=%.0f tx/sec%n",
                        count, skipped, cycles, elapsedMs, tps);
            }
        };
    }
}
