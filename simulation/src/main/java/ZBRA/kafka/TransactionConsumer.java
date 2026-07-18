package ZBRA.kafka;

import java.nio.charset.StandardCharsets;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import ZBRA.blockchain.Transaction;
import ZBRA.engine.CycleAssembler;

@Component
public class TransactionConsumer {

    private final CycleAssembler assembler;

    public TransactionConsumer(CycleAssembler assembler) {
        this.assembler = assembler;
    }

    @KafkaListener(topics = "transactions.raw")
    public void onMessage(
            TransactionMessage message,
            @Header(name = "cycle", required = false) byte[] cycleHeader,
            @Header(name = "dataset", required = false) byte[] datasetHeader,
            @Header(name = "type", required = false) byte[] typeHeader,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {

        if (typeHeader != null && "eof".equals(new String(typeHeader, StandardCharsets.UTF_8))) {
            assembler.markPartitionEof(partition);
            return;
        }

        long cycle = Long.parseLong(new String(cycleHeader, StandardCharsets.UTF_8));
        String datasetHash = new String(datasetHeader, StandardCharsets.UTF_8);
        assembler.accept(partition, cycle, datasetHash, toDomain(message));
    }

    private Transaction toDomain(TransactionMessage m) {
        double size = Double.parseDouble(m.getSize());
        double fee = Double.parseDouble(m.getFee());
        double weight = size;
        return new Transaction(m.getHash(), size, weight, fee);
    }
}
