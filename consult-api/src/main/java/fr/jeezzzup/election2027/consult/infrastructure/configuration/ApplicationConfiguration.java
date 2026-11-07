package fr.jeezzzup.election2027.consult.infrastructure.configuration;

import fr.jeezzzup.election2027.consult.application.port.out.ArticleReadPort;
import fr.jeezzzup.election2027.consult.application.port.out.CrawlRunReadPort;
import fr.jeezzzup.election2027.consult.application.port.out.PollReadPort;
import fr.jeezzzup.election2027.consult.application.service.ConsultationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {

    @Bean
    ConsultationService consultationService(PollReadPort polls, ArticleReadPort articles, CrawlRunReadPort runs) {
        return new ConsultationService(polls, articles, runs);
    }
}
