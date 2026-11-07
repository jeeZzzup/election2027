package fr.jeezzzup.election2027.crawler.application.service;

import fr.jeezzzup.election2027.crawler.application.model.FetchedArticle;
import fr.jeezzzup.election2027.crawler.application.model.FetchedPoll;
import fr.jeezzzup.election2027.crawler.application.port.in.CrawlUseCase;
import fr.jeezzzup.election2027.crawler.application.port.out.*;
import fr.jeezzzup.election2027.crawler.domain.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class CrawlApplicationService implements CrawlUseCase {

    private static final System.Logger log = System.getLogger(CrawlApplicationService.class.getName());
    private final List<PollSourcePort> pollSources;
    private final List<ArticleSourcePort> articleSources;
    private final PollStorePort polls;
    private final ArticleStorePort articles;
    private final CrawlRunStorePort runs;
    private final BackgroundTaskPort backgroundTasks;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public CrawlApplicationService(List<PollSourcePort> pollSources, List<ArticleSourcePort> articleSources,
                                   PollStorePort polls, ArticleStorePort articles, CrawlRunStorePort runs,
                                   BackgroundTaskPort backgroundTasks) {
        this.pollSources = List.copyOf(pollSources);
        this.articleSources = List.copyOf(articleSources);
        this.polls = polls;
        this.articles = articles;
        this.runs = runs;
        this.backgroundTasks = backgroundTasks;
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public boolean triggerAsync() {
        if (!running.compareAndSet(false, true)) {
            return false;
        }
        try {
            backgroundTasks.submit(this::runReserved);
            return true;
        } catch (RuntimeException exception) {
            running.set(false);
            throw exception;
        }
    }

    @Override
    public void runNow() {
        if (running.compareAndSet(false, true)) {
            runReserved();
        }
    }

    @Override
    public List<CrawlRun> latestRuns() {
        return runs.findLatest20();
    }

    private void runReserved() {
        try {
            for (PollSourcePort source : pollSources) {
                if (source.enabled()) {
                    runSource(source.name(), () -> importPolls(source.fetch()));
                }
            }
            for (ArticleSourcePort source : articleSources) {
                if (source.enabled()) {
                    runSource(source.name(), () -> importArticles(source.fetch()));
                }
            }
        } finally {
            running.set(false);
        }
    }

    private void runSource(String source, CrawlJob job) {
        CrawlRun run = runs.save(CrawlRun.start(source, Instant.now()));
        try {
            int[] counts = job.execute();
            runs.save(run.succeed(counts[0], counts[1], Instant.now()));
            log.log(System.Logger.Level.INFO, "[{0}] {1} élément(s) trouvé(s), {2} nouveau(x)",
                    source, counts[0], counts[1]);
        } catch (Exception exception) {
            String message = cut(exception.getClass().getSimpleName() + " : " + exception.getMessage(), 1900);
            runs.save(run.fail(message, Instant.now()));
            log.log(System.Logger.Level.ERROR, "[" + source + "] échec de la collecte", exception);
        }
    }

    private int[] importPolls(List<FetchedPoll> fetched) {
        int fresh = 0;
        for (FetchedPoll item : fetched) {
            if (item.results().isEmpty()) {
                continue;
            }
            String key = sha1(item.institute() + "|" + item.fieldworkStart() + "|" + item.fieldworkEnd()
                    + "|" + item.scenario() + "|" + item.round());
            if (polls.existsByExternalKey(key)) {
                continue;
            }
            List<PollResult> results = item.results().entrySet().stream()
                    .map(result -> new PollResult(cut(result.getKey(), 255), result.getValue()))
                    .toList();
            polls.save(new Poll(key, cut(item.institute(), 255), cut(item.client(), 255), item.fieldworkStart(),
                    item.fieldworkEnd(), item.sampleSize(), cut(item.scenario(), 255), item.round(),
                    cut(item.sourceUrl(), 1000), Instant.now(), results));
            fresh++;
        }
        return new int[]{fetched.size(), fresh};
    }

    private int[] importArticles(List<FetchedArticle> fetched) {
        int fresh = 0;
        for (FetchedArticle item : fetched) {
            String key = sha1(item.url());
            if (articles.existsByExternalKey(key)) {
                continue;
            }
            articles.save(new Article(key, cut(item.title(), 500), cut(item.url(), 1500),
                    cut(item.source(), 255), item.publishedAt() == null ? Instant.now() : item.publishedAt(),
                    cut(item.summary(), 1900), Instant.now()));
            fresh++;
        }
        return new int[]{fetched.size(), fresh};
    }

    private static String cut(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static String sha1(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-1 is not available", exception);
        }
    }

    @FunctionalInterface
    private interface CrawlJob {
        int[] execute() throws Exception;
    }
}
