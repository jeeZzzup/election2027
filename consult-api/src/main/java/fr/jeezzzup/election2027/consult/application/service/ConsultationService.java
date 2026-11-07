package fr.jeezzzup.election2027.consult.application.service;

import fr.jeezzzup.election2027.consult.application.model.Models.*;
import fr.jeezzzup.election2027.consult.application.port.in.ConsultationUseCase;
import fr.jeezzzup.election2027.consult.application.port.out.ArticleReadPort;
import fr.jeezzzup.election2027.consult.application.port.out.CrawlRunReadPort;
import fr.jeezzzup.election2027.consult.application.port.out.PollReadPort;
import fr.jeezzzup.election2027.consult.domain.Article;
import fr.jeezzzup.election2027.consult.domain.Poll;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneOffset;
import java.time.LocalDate;
import java.util.*;

public class ConsultationService implements ConsultationUseCase {

    private final PollReadPort polls;
    private final ArticleReadPort articles;
    private final CrawlRunReadPort runs;

    public ConsultationService(PollReadPort polls, ArticleReadPort articles, CrawlRunReadPort runs) {
        this.polls = polls;
        this.articles = articles;
        this.runs = runs;
    }

    @Override
    public PageResult<PollDto> searchPolls(String institute, LocalDate from, LocalDate to, Integer round,
                                           int page, int size) {
        PageResult<Poll> result = polls.search(institute, from, to, round, Math.max(page, 0), clamp(size));
        return mapPage(result, this::toDto);
    }

    @Override
    public Optional<PollDto> getPoll(Long id) {
        return polls.findById(id).map(this::toDto);
    }

    @Override
    public List<String> institutes() {
        return polls.findInstitutes();
    }

    @Override
    public List<CandidateTrend> trends(int round, LocalDate since) {
        Map<String, List<TrendPoint>> byCandidate = new TreeMap<>();
        for (TrendRow row : polls.findTrendRows(round)) {
            if (since != null && row.date().isBefore(since)) {
                continue;
            }
            byCandidate.computeIfAbsent(row.candidate(), key -> new ArrayList<>())
                    .add(new TrendPoint(row.date(), round2(row.average()), row.polls()));
        }
        List<CandidateTrend> result = new ArrayList<>();
        byCandidate.forEach((candidate, points) -> result.add(new CandidateTrend(candidate, points)));
        return result;
    }

    @Override
    public List<CandidateAverage> latestAverages(int round, int days) {
        List<TrendRow> rows = polls.findTrendRows(round);
        if (rows.isEmpty()) {
            return List.of();
        }
        LocalDate latest = rows.stream().map(TrendRow::date).max(Comparator.naturalOrder()).orElseThrow();
        LocalDate cutoff = latest.minusDays(Math.max(days, 0));
        Map<String, double[]> acc = new HashMap<>();
        for (TrendRow row : rows) {
            if (row.date().isBefore(cutoff)) {
                continue;
            }
            double[] values = acc.computeIfAbsent(row.candidate(), key -> new double[2]);
            values[0] += row.average();
            values[1] += 1;
        }
        List<CandidateAverage> result = new ArrayList<>();
        acc.forEach((candidate, values) -> result.add(
                new CandidateAverage(candidate, round2(values[0] / values[1]), (int) values[1])));
        result.sort(Comparator.comparingDouble(CandidateAverage::percentage).reversed());
        return result;
    }

    @Override
    public PageResult<ArticleDto> searchArticles(String query, int page, int size) {
        PageResult<Article> result = articles.search(query, Math.max(page, 0), clamp(size));
        return mapPage(result, article -> new ArticleDto(article.id(), article.title(), article.url(),
                article.source(), article.publishedAt() == null ? null : article.publishedAt().atOffset(ZoneOffset.UTC),
                article.summary()));
    }

    @Override
    public StatsDto stats() {
        List<String> instituteNames = polls.findInstitutes();
        return new StatsDto(
                polls.count(),
                articles.count(),
                instituteNames.size(),
                polls.findLatestFieldworkEnd(),
                runs.findLatestSuccessfulRun()
                        .map(run -> run.finishedAt() == null ? null : run.finishedAt().atOffset(ZoneOffset.UTC))
                        .orElse(null));
    }

    private PollDto toDto(Poll poll) {
        List<ResultDto> results = poll.results().stream()
                .map(result -> new ResultDto(result.candidate(), result.percentage()))
                .sorted(Comparator.comparing(ResultDto::percentage).reversed())
                .toList();
        return new PollDto(poll.id(), poll.institute(), poll.client(), poll.fieldworkStart(), poll.fieldworkEnd(),
                poll.sampleSize(), poll.scenario(), poll.voteRound(), poll.sourceUrl(), results);
    }

    private static <S, T> PageResult<T> mapPage(PageResult<S> page, java.util.function.Function<S, T> mapper) {
        return new PageResult<>(page.content().stream().map(mapper).toList(), page.page());
    }

    private static int clamp(int size) {
        return Math.min(Math.max(size, 1), 100);
    }

    private static double round2(Double value) {
        return value == null ? 0 : BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
