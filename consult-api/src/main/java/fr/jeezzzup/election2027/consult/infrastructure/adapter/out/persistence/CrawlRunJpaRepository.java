package fr.jeezzzup.election2027.consult.infrastructure.adapter.out.persistence;

import fr.jeezzzup.election2027.consult.infrastructure.adapter.out.persistence.entity.CrawlRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CrawlRunJpaRepository extends JpaRepository<CrawlRunEntity, Long> {

    Optional<CrawlRunEntity> findFirstByStatusOrderByFinishedAtDesc(String status);
}
