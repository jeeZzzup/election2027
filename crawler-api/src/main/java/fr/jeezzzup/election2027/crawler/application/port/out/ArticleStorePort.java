package fr.jeezzzup.election2027.crawler.application.port.out;

import fr.jeezzzup.election2027.crawler.domain.Article;

public interface ArticleStorePort {

    boolean existsByExternalKey(String externalKey);

    Article save(Article article);
}
