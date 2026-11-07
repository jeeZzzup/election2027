package fr.jeezzzup.election2027.crawler.infrastructure.configuration;

import fr.jeezzzup.election2027.crawler.application.port.out.*;
import fr.jeezzzup.election2027.crawler.application.service.CrawlApplicationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ApplicationConfiguration {

    @Bean
    CrawlApplicationService crawlApplicationService(List<PollSourcePort> pollSources,
                                                   List<ArticleSourcePort> articleSources,
                                                   PollStorePort polls,
                                                   ArticleStorePort articles,
                                                   CrawlRunStorePort runs,
                                                   BackgroundTaskPort backgroundTasks) {
        return new CrawlApplicationService(pollSources, articleSources, polls, articles, runs, backgroundTasks);
    }
}
