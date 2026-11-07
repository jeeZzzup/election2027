package fr.jeezzzup.election2027.crawler.application.service;

import fr.jeezzzup.election2027.crawler.application.model.FetchedPoll;
import fr.jeezzzup.election2027.crawler.application.port.out.*;
import fr.jeezzzup.election2027.crawler.domain.CrawlRun;
import fr.jeezzzup.election2027.crawler.domain.Poll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CrawlApplicationServiceTest {

    @Test
    void asyncTriggerReservesTheRunUntilTheSubmittedTaskFinishes() {
        PollSourcePort source = pollSource(List.of());
        BackgroundTaskPort background = mock(BackgroundTaskPort.class);
        CrawlApplicationService service = service(List.of(source), List.of(), background);
        AtomicReference<Runnable> submitted = new AtomicReference<>();
        doAnswer(invocation -> {
            submitted.set(invocation.getArgument(0));
            return null;
        }).when(background).submit(any(Runnable.class));

        assertTrue(service.triggerAsync());
        assertTrue(service.isRunning());
        assertFalse(service.triggerAsync());

        submitted.get().run();

        assertFalse(service.isRunning());
        verify(background, times(1)).submit(any(Runnable.class));
    }

    @Test
    void runNowPersistsPollsAndRecordsSuccessfulSourceRun() {
        FetchedPoll fetched = new FetchedPoll("Institut", "Client", LocalDate.parse("2026-10-01"),
                LocalDate.parse("2026-10-02"), 1000, "Scénario", 1, "https://example.test/poll",
                Map.of("Candidate", new BigDecimal("12.50")));
        PollStorePort pollStore = mock(PollStorePort.class);
        CrawlRunStorePort runStore = mock(CrawlRunStorePort.class);
        when(pollStore.existsByExternalKey(any())).thenReturn(false);
        when(pollStore.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(runStore.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        CrawlApplicationService service = new CrawlApplicationService(
                List.of(pollSource(List.of(fetched))), List.of(), pollStore, mock(ArticleStorePort.class), runStore,
                mock(BackgroundTaskPort.class));

        service.runNow();

        var savedPoll = org.mockito.ArgumentCaptor.forClass(Poll.class);
        verify(pollStore).save(savedPoll.capture());
        assertEquals("Institut", savedPoll.getValue().institute());
        assertEquals("Candidate", savedPoll.getValue().results().getFirst().candidate());
        var savedRuns = org.mockito.ArgumentCaptor.forClass(CrawlRun.class);
        verify(runStore, times(2)).save(savedRuns.capture());
        assertEquals(CrawlRun.Status.RUNNING, savedRuns.getAllValues().getFirst().status());
        CrawlRun completed = savedRuns.getAllValues().getLast();
        assertEquals(CrawlRun.Status.SUCCESS, completed.status());
        assertEquals(1, completed.itemsFound());
        assertEquals(1, completed.itemsNew());
        assertFalse(service.isRunning());
    }

    private static PollSourcePort pollSource(List<FetchedPoll> polls) {
        return new PollSourcePort() {
            @Override
            public String name() {
                return "test";
            }

            @Override
            public List<FetchedPoll> fetch() {
                return polls;
            }
        };
    }

    private static CrawlApplicationService service(List<PollSourcePort> sources, List<ArticleSourcePort> articleSources,
                                                   BackgroundTaskPort background) {
        CrawlRunStorePort runStore = mock(CrawlRunStorePort.class);
        when(runStore.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        return new CrawlApplicationService(sources, articleSources, mock(PollStorePort.class),
                mock(ArticleStorePort.class), runStore, background);
    }
}
