package ZBRA;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import ZBRA.persistence.SimulationRun;

@SpringBootApplication
@EnableConfigurationProperties(SimulationRun.RunProperties.class)
public class SimulationApplication {
    public static void main(String[] args) {
        SpringApplication.run(SimulationApplication.class, args);
    }
}