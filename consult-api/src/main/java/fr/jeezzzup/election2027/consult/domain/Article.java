package fr.jeezzzup.election2027.consult.domain;

import java.time.Instant;

public record Article(Long id, String title, String url, String source, Instant publishedAt, String summary) {
}
