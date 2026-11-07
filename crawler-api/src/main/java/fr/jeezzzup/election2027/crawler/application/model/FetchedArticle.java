package fr.jeezzzup.election2027.crawler.application.model;

import java.time.Instant;

public record FetchedArticle(String title, String url, String source, Instant publishedAt, String summary) {
}
