package fr.jeezzzup.election2027.crawler.application.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record FetchedPoll(String institute, String client, LocalDate fieldworkStart, LocalDate fieldworkEnd,
                          Integer sampleSize, String scenario, int round, String sourceUrl,
                          Map<String, BigDecimal> results) {

    public FetchedPoll {
        results = results == null ? Map.of() : Map.copyOf(results);
    }
}
