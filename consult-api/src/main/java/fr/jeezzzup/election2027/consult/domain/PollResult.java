package fr.jeezzzup.election2027.consult.domain;

import java.math.BigDecimal;

public record PollResult(String candidate, BigDecimal percentage) {
}
