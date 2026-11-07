package fr.jeezzzup.election2027.crawler.infrastructure.adapter.out.persistence;

import fr.jeezzzup.election2027.crawler.infrastructure.adapter.out.persistence.entity.ArticleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleJpaRepository extends JpaRepository<ArticleEntity, Long> {

    boolean existsByExternalKey(String externalKey);
}
