package fr.jeezzzup.election2027.consult.infrastructure.adapter.out.persistence;

import fr.jeezzzup.election2027.consult.infrastructure.adapter.out.persistence.entity.ArticleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ArticleJpaRepository extends JpaRepository<ArticleEntity, Long>, JpaSpecificationExecutor<ArticleEntity> {
}
