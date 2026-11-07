package fr.jeezzzup.election2027.consult.domain;

import java.time.LocalDate;
import java.util.List;

public record Poll(Long id, String institute, String client, LocalDate fieldworkStart, LocalDate fieldworkEnd,
                   Integer sampleSize, String scenario, Integer voteRound, String sourceUrl, List<PollResult> results) {

    public Poll {
        results = List.copyOf(results);
    }
}
