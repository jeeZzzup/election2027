package fr.jeezzzup.election2027.consult.application.port.in;

import fr.jeezzzup.election2027.consult.application.model.Models.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ConsultationUseCase {

    PageResult<PollDto> searchPolls(String institute, LocalDate from, LocalDate to, Integer round, int page, int size);

    Optional<PollDto> getPoll(Long id);

    List<String> institutes();

    List<CandidateTrend> trends(int round, LocalDate since);

    List<CandidateAverage> latestAverages(int round, int days);

    PageResult<ArticleDto> searchArticles(String query, int page, int size);

    StatsDto stats();
}
