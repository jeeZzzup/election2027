package fr.jeezzzup.election2027.crawler.infrastructure.adapter.out.source;

import fr.jeezzzup.election2027.crawler.infrastructure.adapter.out.http.HttpFetcher;
import fr.jeezzzup.election2027.crawler.infrastructure.configuration.CrawlerProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataGouvPollSourceTest {

    @Test
    void readsAndCombinesAllPagesFromTheTabularApi() throws Exception {
        String baseUrl = "https://tabular-api.data.gouv.fr/api";
        String datasetSlug = "presidentielle-2027";
        String resourceUrl = baseUrl + "/resources/resource-1";
        String firstPageUrl = resourceUrl + "/data/?page_size=100";

        HttpFetcher http = mock(HttpFetcher.class);
        when(http.get("https://www.data.gouv.fr/api/1/datasets/" + datasetSlug + "/"))
                .thenReturn("""
                        {"resources":[{"id":"resource-1","title":"Any CSV title","format":"csv","url":"https://example.test/polls.csv"}]}
                        """);
        when(http.get(resourceUrl + "/profile/")).thenReturn("""
                {"profile":{"header":["candidat","intentions_vote_pct","institut","date_publication"]}}
                """);
        when(http.get(firstPageUrl)).thenReturn("""
                {"data":[{"candidat":"Candidate A","intentions_vote_pct":32.5,"institut":"Elabe","date_publication":"2026-10-17"}],
                 "links":{"next":"https://tabular-api.data.gouv.fr/api/resources/resource-1/data/?page=2&page_size=100"}}
                """);
        when(http.get("https://tabular-api.data.gouv.fr/api/resources/resource-1/data/?page=2&page_size=100"))
                .thenReturn("""
                        {"data":[{"candidat":"Candidate B","intentions_vote_pct":16.5,"institut":"Elabe","date_publication":"2026-10-17"}],
                         "links":{"next":null}}
                        """);
        CrawlerProperties properties = new CrawlerProperties("0 0 */6 * * *", false,
                new CrawlerProperties.DataGouv(true, datasetSlug, baseUrl),
                new CrawlerProperties.News(false, 0, List.of()));

        var polls = new DataGouvPollSource(http, properties).fetch();

        assertEquals(1, polls.size());
        assertEquals(2, polls.getFirst().results().size());
        verify(http).get("https://tabular-api.data.gouv.fr/api/resources/resource-1/data/?page=2&page_size=100");
    }
}
