package fr.jeezzzup.election2027.crawler.infrastructure.adapter.out.persistence;

import fr.jeezzzup.election2027.crawler.infrastructure.adapter.out.persistence.entity.PollEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PollJpaRepository extends JpaRepository<PollEntity, Long> {

    boolean existsByExternalKey(String externalKey);
}
