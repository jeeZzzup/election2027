package fr.jeezzzup.election2027.crawler.infrastructure.adapter.out.persistence;

import fr.jeezzzup.election2027.crawler.application.port.out.CrawlRunStorePort;
import fr.jeezzzup.election2027.crawler.domain.CrawlRun;
import fr.jeezzzup.election2027.crawler.infrastructure.adapter.out.persistence.entity.CrawlRunEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public class CrawlRunPersistenceAdapter implements CrawlRunStorePort {

    private final CrawlRunJpaRepository runs;

    public CrawlRunPersistenceAdapter(CrawlRunJpaRepository runs) {
        this.runs = runs;
    }

    @Override
    public CrawlRun save(CrawlRun run) {
        CrawlRunEntity entity = run.id() == null
                ? new CrawlRunEntity()
                : runs.findById(run.id()).orElseThrow(() -> new IllegalStateException(
                        "Collecte introuvable : " + run.id()));
        entity.setSource(run.source());
        entity.setStartedAt(run.startedAt());
        entity.setFinishedAt(run.finishedAt());
        entity.setStatus(run.status().name());
        entity.setItemsFound(run.itemsFound());
        entity.setItemsNew(run.itemsNew());
        entity.setMessage(run.message());
        CrawlRunEntity saved = runs.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CrawlRun> findLatest20() {
        return runs.findTop20ByOrderByStartedAtDesc().stream()
                .map(CrawlRunPersistenceAdapter::toDomain)
                .toList();
    }

    private static CrawlRun toDomain(CrawlRunEntity entity) {
        return new CrawlRun(entity.getId(), entity.getSource(), entity.getStartedAt(), entity.getFinishedAt(),
                CrawlRun.Status.valueOf(entity.getStatus()), entity.getItemsFound(), entity.getItemsNew(),
                entity.getMessage());
    }
}
