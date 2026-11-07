package fr.jeezzzup.election2027.crawler.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record Poll(String externalKey, String institute, String client, LocalDate fieldworkStart,
                   LocalDate fieldworkEnd, Integer sampleSize, String scenario, Integer voteRound,
                   String sourceUrl, Instant createdAt, List<PollResult> results) {

    public Poll {
        results = List.copyOf(results);
    }
}
