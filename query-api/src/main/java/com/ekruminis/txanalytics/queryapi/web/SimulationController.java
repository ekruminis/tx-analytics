package com.ekruminis.txanalytics.queryapi.web;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ekruminis.txanalytics.wire.RunCommand;

@RestController
public class SimulationController {

    private static final String INGEST_TOPIC = "ingest.commands";

    private final KafkaTemplate<String, RunCommand> kafka;

    public SimulationController(KafkaTemplate<String, RunCommand> kafka) {
        this.kafka = kafka;
    }

    @PostMapping("/simulations")
    public ResponseEntity<Map<String, Object>> launch(@RequestBody RunCommand command) {
        if (command.tfms() == null || command.tfms().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "at least one tfm is required");
        }
        kafka.send(INGEST_TOPIC, command);

        Map<String, Object> body = new HashMap<>();
        body.put("status", "accepted");
        body.put("message", "simulation launched; poll /experiments and /runs for results");
        body.put("dataset", command.dataset() != null ? command.dataset() : "txs-week.json (default)");
        body.put("tfms", command.tfms().keySet());
        return ResponseEntity.accepted().body(body);
    }
}
