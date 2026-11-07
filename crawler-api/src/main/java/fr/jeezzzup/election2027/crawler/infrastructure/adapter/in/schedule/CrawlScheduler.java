package fr.jeezzzup.election2027.crawler.infrastructure.adapter.in.schedule;

import fr.jeezzzup.election2027.crawler.application.port.in.CrawlUseCase;
import fr.jeezzzup.election2027.crawler.infrastructure.configuration.CrawlerProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CrawlScheduler {

    private final CrawlUseCase crawl;
    private final CrawlerProperties properties;

    public CrawlScheduler(CrawlUseCase crawl, CrawlerProperties properties) {
        this.crawl = crawl;
        this.properties = properties;
    }

    @Scheduled(cron = "${crawler.cron}")
    public void scheduledRun() {
        crawl.runNow();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (properties.runOnStartup()) {
            crawl.triggerAsync();
        }
    }
}
