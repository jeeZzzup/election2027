package fr.jeezzzup.election2027.consult.application.port.out;

import fr.jeezzzup.election2027.consult.application.model.Models.PageResult;
import fr.jeezzzup.election2027.consult.domain.Article;

public interface ArticleReadPort {

    PageResult<Article> search(String query, int page, int size);

    long count();
}
