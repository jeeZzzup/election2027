package fr.jeezzzup.election2027.crawler.domain;

import java.time.Instant;

public record CrawlRun(Long id, String source, Instant startedAt, Instant finishedAt, Status status,
                       int itemsFound, int itemsNew, String message) {

    public enum Status {
        RUNNING,
        SUCCESS,
        FAILED
    }

    public static CrawlRun start(String source, Instant startedAt) {
        return new CrawlRun(null, source, startedAt, null, Status.RUNNING, 0, 0, null);
    }

    public CrawlRun succeed(int found, int added, Instant finished) {
        return new CrawlRun(id, source, startedAt, finished, Status.SUCCESS, found, added, null);
    }

    public CrawlRun fail(String error, Instant finished) {
        return new CrawlRun(id, source, startedAt, finished, Status.FAILED, itemsFound, itemsNew, error);
    }
}
