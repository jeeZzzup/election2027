package fr.jeezzzup.election2027.consult.infrastructure.adapter.out.persistence;

import fr.jeezzzup.election2027.consult.application.port.out.CrawlRunReadPort;
import fr.jeezzzup.election2027.consult.domain.CrawlRun;
import fr.jeezzzup.election2027.consult.infrastructure.adapter.out.persistence.entity.CrawlRunEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class CrawlRunPersistenceAdapter implements CrawlRunReadPort {

    private final CrawlRunJpaRepository runs;

    public CrawlRunPersistenceAdapter(CrawlRunJpaRepository runs) {
        this.runs = runs;
    }

    @Override
    public Optional<CrawlRun> findLatestSuccessfulRun() {
        return runs.findFirstByStatusOrderByFinishedAtDesc(CrawlRun.Status.SUCCESS.name())
                .map(CrawlRunPersistenceAdapter::toDomain);
    }

    private static CrawlRun toDomain(CrawlRunEntity run) {
        return new CrawlRun(run.getId(), run.getSource(), run.getStartedAt(), run.getFinishedAt(),
                CrawlRun.Status.valueOf(run.getStatus()), run.getItemsFound(), run.getItemsNew(), run.getMessage());
    }
}
