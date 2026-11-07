package fr.jeezzzup.election2027.consult.application.service;

import fr.jeezzzup.election2027.consult.application.model.Models.PageMetadata;
import fr.jeezzzup.election2027.consult.application.model.Models.PageResult;
import fr.jeezzzup.election2027.consult.domain.CrawlRun;
import fr.jeezzzup.election2027.consult.application.port.out.ArticleReadPort;
import fr.jeezzzup.election2027.consult.application.port.out.CrawlRunReadPort;
import fr.jeezzzup.election2027.consult.application.port.out.PollReadPort;
import fr.jeezzzup.election2027.consult.domain.Poll;
import fr.jeezzzup.election2027.consult.domain.PollResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultationServiceTest {

    @Mock
    private PollReadPort polls;

    @Mock
    private ArticleReadPort articles;

    @Mock
    private CrawlRunReadPort runs;

    @InjectMocks
    private ConsultationService service;

    @Test
    void searchPollsMapsDomainModelsAndKeepsPageMetadata() {
        Poll poll = new Poll(7L, "Institut", null, null, LocalDate.parse("2026-10-01"), 1000,
                "Scénario", 1, null, List.of(
                new PollResult("Candidat B", new BigDecimal("12.50")),
                new PollResult("Candidat A", new BigDecimal("25.00"))));
        PageMetadata metadata = new PageMetadata(20, 2, 45, 3);
        when(polls.search(null, null, null, null, 0, 1))
                .thenReturn(new PageResult<>(List.of(poll), metadata));

        var result = service.searchPolls(null, null, null, null, -1, 0);

        assertEquals(metadata, result.page());
        assertEquals(7L, result.content().getFirst().id());
        assertEquals("Candidat A", result.content().getFirst().results().getFirst().candidate());
        assertEquals(new BigDecimal("25.00"), result.content().getFirst().results().getFirst().percentage());
        verify(polls).search(null, null, null, null, 0, 1);
    }

    @Test
    void statsMapsLastCrawlInstantToUtcOffsetDateTime() {
        Instant finishedAt = Instant.parse("2026-11-07T18:00:00Z");
        when(polls.findInstitutes()).thenReturn(List.of("Institut"));
        when(runs.findLatestSuccessfulRun()).thenReturn(Optional.of(new CrawlRun(3L, "source",
                finishedAt.minusSeconds(60), finishedAt, CrawlRun.Status.SUCCESS, 10, 2, null)));

        var stats = service.stats();

        assertEquals(OffsetDateTime.ofInstant(finishedAt, ZoneOffset.UTC), stats.lastCrawl());
    }

    @Test
    void searchArticlesMapsPublishedAtInstantToUtcOffsetDateTime() {
        Instant publishedAt = Instant.parse("2026-11-07T18:00:00Z");
        when(articles.search(null, 0, 20)).thenReturn(new PageResult<>(
                List.of(new fr.jeezzzup.election2027.consult.domain.Article(1L, "title", "url", "source",
                        publishedAt, "summary")),
                new PageMetadata(20, 0, 1, 1)));

        var result = service.searchArticles(null, 0, 20);

        assertEquals(OffsetDateTime.ofInstant(publishedAt, ZoneOffset.UTC),
                result.content().getFirst().publishedAt());
    }
}
