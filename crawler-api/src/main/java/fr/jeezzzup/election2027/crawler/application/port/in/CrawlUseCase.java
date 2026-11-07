package fr.jeezzzup.election2027.crawler.application.port.in;

import fr.jeezzzup.election2027.crawler.domain.CrawlRun;

import java.util.List;

public interface CrawlUseCase {

    boolean triggerAsync();

    boolean isRunning();

    void runNow();

    List<CrawlRun> latestRuns();
}
