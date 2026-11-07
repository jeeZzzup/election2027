package fr.jeezzzup.election2027.crawler.infrastructure.adapter.in.web;

import fr.jeezzzup.election2027.crawler.application.port.in.CrawlUseCase;
import fr.jeezzzup.election2027.crawler.domain.CrawlRun;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/crawl")
public class CrawlController {

    private final CrawlUseCase crawl;

    public CrawlController(CrawlUseCase crawl) {
        this.crawl = crawl;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> trigger() {
        if (crawl.triggerAsync()) {
            return ResponseEntity.accepted().body(Map.of("status", "started"));
        }
        return ResponseEntity.status(409).body(Map.of("status", "already_running"));
    }

    @GetMapping("/status")
    public Map<String, Boolean> status() {
        return Map.of("running", crawl.isRunning());
    }

    @GetMapping("/runs")
    public List<CrawlRunResponse> lastRuns() {
        return crawl.latestRuns().stream().map(CrawlRunResponse::from).toList();
    }

    public record CrawlRunResponse(Long id, String source, Instant startedAt, Instant finishedAt, String status,
                                   int itemsFound, int itemsNew, String message) {

        static CrawlRunResponse from(CrawlRun run) {
            return new CrawlRunResponse(run.id(), run.source(), run.startedAt(), run.finishedAt(),
                    run.status().name(), run.itemsFound(), run.itemsNew(), run.message());
        }
    }
}
