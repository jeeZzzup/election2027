package fr.jeezzzup.election2027.consult.application.port.out;

import fr.jeezzzup.election2027.consult.application.model.Models.PageResult;
import fr.jeezzzup.election2027.consult.application.model.Models.TrendRow;
import fr.jeezzzup.election2027.consult.domain.Poll;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PollReadPort {

    PageResult<Poll> search(String institute, LocalDate from, LocalDate to, Integer round, int page, int size);

    Optional<Poll> findById(Long id);

    List<String> findInstitutes();

    LocalDate findLatestFieldworkEnd();

    List<TrendRow> findTrendRows(int round);

    long count();
}
