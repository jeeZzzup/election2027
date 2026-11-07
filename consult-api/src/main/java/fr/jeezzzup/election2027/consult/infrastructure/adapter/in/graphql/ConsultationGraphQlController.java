package fr.jeezzzup.election2027.consult.infrastructure.adapter.in.graphql;

import fr.jeezzzup.election2027.consult.application.model.Models.*;
import fr.jeezzzup.election2027.consult.application.port.in.ConsultationUseCase;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.util.List;

@Controller
public class ConsultationGraphQlController {

    private final ConsultationUseCase consultation;

    public ConsultationGraphQlController(ConsultationUseCase consultation) {
        this.consultation = consultation;
    }

    @QueryMapping
    public PageResult<PollDto> polls(@Argument String institute, @Argument LocalDate from, @Argument LocalDate to,
                                     @Argument Integer round, @Argument Integer page, @Argument Integer size) {
        return consultation.searchPolls(institute, from, to, round, defaultValue(page, 0), defaultValue(size, 20));
    }

    @QueryMapping
    public PollDto poll(@Argument Long id) {
        return consultation.getPoll(id).orElse(null);
    }

    @QueryMapping
    public List<String> institutes() {
        return consultation.institutes();
    }

    @QueryMapping
    public List<CandidateTrend> trends(@Argument Integer round, @Argument LocalDate since) {
        return consultation.trends(defaultValue(round, 1), since);
    }

    @QueryMapping
    public List<CandidateAverage> averages(@Argument Integer round, @Argument Integer days) {
        return consultation.latestAverages(defaultValue(round, 1), defaultValue(days, 30));
    }

    @QueryMapping
    public PageResult<ArticleDto> articles(@Argument String q, @Argument Integer page, @Argument Integer size) {
        return consultation.searchArticles(q, defaultValue(page, 0), defaultValue(size, 20));
    }

    @QueryMapping
    public StatsDto stats() {
        return consultation.stats();
    }

    private static int defaultValue(Integer value, int fallback) {
        return value == null ? fallback : value;
    }
}
