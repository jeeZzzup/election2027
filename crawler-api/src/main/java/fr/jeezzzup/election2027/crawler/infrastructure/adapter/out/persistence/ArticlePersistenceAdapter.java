package fr.jeezzzup.election2027.crawler.infrastructure.adapter.out.persistence;

import fr.jeezzzup.election2027.crawler.application.port.out.ArticleStorePort;
import fr.jeezzzup.election2027.crawler.domain.Article;
import fr.jeezzzup.election2027.crawler.infrastructure.adapter.out.persistence.entity.ArticleEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class ArticlePersistenceAdapter implements ArticleStorePort {

    private final ArticleJpaRepository articles;

    public ArticlePersistenceAdapter(ArticleJpaRepository articles) {
        this.articles = articles;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByExternalKey(String externalKey) {
        return articles.existsByExternalKey(externalKey);
    }

    @Override
    public Article save(Article article) {
        ArticleEntity entity = new ArticleEntity();
        entity.setExternalKey(article.externalKey());
        entity.setTitle(article.title());
        entity.setUrl(article.url());
        entity.setSource(article.source());
        entity.setPublishedAt(article.publishedAt());
        entity.setSummary(article.summary());
        entity.setCreatedAt(article.createdAt());
        articles.save(entity);
        return article;
    }
}
