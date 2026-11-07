package fr.jeezzzup.election2027.consult.domain;

import java.time.Instant;

public record CrawlRun(Long id, String source, Instant startedAt, Instant finishedAt, Status status,
                       int itemsFound, int itemsNew, String message) {

    public enum Status {
        RUNNING,
        SUCCESS,
        FAILED
    }
}
