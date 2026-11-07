package fr.jeezzzup.election2027.crawler.application.port.out;

import fr.jeezzzup.election2027.crawler.application.model.FetchedArticle;

import java.util.List;

public interface ArticleSourcePort {

    String name();

    default boolean enabled() {
        return true;
    }

    List<FetchedArticle> fetch() throws Exception;
}
