package fr.jeezzzup.election2027.consult.infrastructure.adapter.out.persistence;

import fr.jeezzzup.election2027.consult.application.model.Models.PageMetadata;
import fr.jeezzzup.election2027.consult.application.model.Models.PageResult;
import fr.jeezzzup.election2027.consult.application.port.out.ArticleReadPort;
import fr.jeezzzup.election2027.consult.domain.Article;
import fr.jeezzzup.election2027.consult.infrastructure.adapter.out.persistence.entity.ArticleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Repository
@Transactional(readOnly = true)
public class ArticlePersistenceAdapter implements ArticleReadPort {

    private final ArticleJpaRepository articles;

    public ArticlePersistenceAdapter(ArticleJpaRepository articles) {
        this.articles = articles;
    }

    @Override
    public PageResult<Article> search(String query, int page, int size) {
        Specification<ArticleEntity> spec = (root, criteriaQuery, cb) -> {
            if (query == null || query.isBlank()) {
                return cb.conjunction();
            }
            String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("summary")), like),
                    cb.like(cb.lower(root.get("source")), like));
        };
        Page<Article> result = articles.findAll(spec, PageRequest.of(page, size,
                        Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"))))
                .map(article -> new Article(article.getId(), article.getTitle(), article.getUrl(),
                        article.getSource(), article.getPublishedAt(), article.getSummary()));
        return new PageResult<>(result.getContent(), new PageMetadata(result.getSize(), result.getNumber(),
                result.getTotalElements(), result.getTotalPages()));
    }

    @Override
    public long count() {
        return articles.count();
    }
}
