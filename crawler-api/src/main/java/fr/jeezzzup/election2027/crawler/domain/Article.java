package fr.jeezzzup.election2027.crawler.domain;

import java.time.Instant;

public record Article(String externalKey, String title, String url, String source, Instant publishedAt,
                     String summary, Instant createdAt) {
}
