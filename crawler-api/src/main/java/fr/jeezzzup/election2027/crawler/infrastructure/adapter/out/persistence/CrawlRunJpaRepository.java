package fr.jeezzzup.election2027.crawler.infrastructure.adapter.out.persistence;

import fr.jeezzzup.election2027.crawler.infrastructure.adapter.out.persistence.entity.CrawlRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrawlRunJpaRepository extends JpaRepository<CrawlRunEntity, Long> {

    List<CrawlRunEntity> findTop20ByOrderByStartedAtDesc();
}
