package fr.jeezzzup.election2027.crawler.domain;

import java.math.BigDecimal;

public record PollResult(String candidate, BigDecimal percentage) {
}
