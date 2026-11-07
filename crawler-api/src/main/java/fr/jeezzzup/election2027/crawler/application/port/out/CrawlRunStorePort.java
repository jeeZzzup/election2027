package fr.jeezzzup.election2027.crawler.application.port.out;

import fr.jeezzzup.election2027.crawler.domain.CrawlRun;

import java.util.List;

public interface CrawlRunStorePort {

    CrawlRun save(CrawlRun run);

    List<CrawlRun> findLatest20();
}
