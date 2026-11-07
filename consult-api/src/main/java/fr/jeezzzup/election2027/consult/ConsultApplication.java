package fr.jeezzzup.election2027.consult;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan("fr.jeezzzup.election2027.consult.infrastructure.adapter.out.persistence.entity")
public class ConsultApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsultApplication.class, args);
    }
}
