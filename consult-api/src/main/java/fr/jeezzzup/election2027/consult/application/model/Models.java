package fr.jeezzzup.election2027.consult.application.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class Models {

    private Models() {
    }

    public record ResultDto(String candidate, BigDecimal percentage) {
    }

    public record PollDto(Long id, String institute, String client, LocalDate fieldworkStart, LocalDate fieldworkEnd,
                          Integer sampleSize, String scenario, Integer round, String sourceUrl,
                          List<ResultDto> results) {
    }

    public record ArticleDto(Long id, String title, String url, String source, OffsetDateTime publishedAt,
                             String summary) {
    }

    public record TrendPoint(LocalDate date, double percentage, long polls) {
    }

    public record CandidateTrend(String candidate, List<TrendPoint> points) {
    }

    public record CandidateAverage(String candidate, double percentage, int polls) {
    }

    public record StatsDto(long polls, long articles, long institutes, LocalDate latestPollDate,
                           OffsetDateTime lastCrawl) {
    }

    public record TrendRow(String candidate, LocalDate date, Double average, Long polls) {
    }

    public record PageMetadata(int size, long number, long totalElements, long totalPages) {
    }

    public record PageResult<T>(List<T> content, PageMetadata page) {
    }
}
