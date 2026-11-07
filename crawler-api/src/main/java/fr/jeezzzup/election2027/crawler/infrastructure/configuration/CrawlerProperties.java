package fr.jeezzzup.election2027.crawler.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "crawler")
public record CrawlerProperties(String cron, boolean runOnStartup, DataGouv datagouv, News news) {

    public record DataGouv(boolean enabled, String datasetSlug, String tabularApiBaseUrl) {
    }

    public record News(boolean enabled, int maxPerQuery, List<String> queries) {
    }
}
