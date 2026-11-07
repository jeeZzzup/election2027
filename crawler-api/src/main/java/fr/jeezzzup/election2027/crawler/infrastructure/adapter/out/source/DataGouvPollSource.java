package fr.jeezzzup.election2027.crawler.infrastructure.adapter.out.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.jeezzzup.election2027.crawler.application.model.FetchedPoll;
import fr.jeezzzup.election2027.crawler.application.port.out.PollSourcePort;
import fr.jeezzzup.election2027.crawler.infrastructure.adapter.out.http.HttpFetcher;
import fr.jeezzzup.election2027.crawler.infrastructure.configuration.CrawlerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lit les sondages du jeu de données ouvert « Présidentielle 2027 : sondages, candidatures et
 * projection départementale » (Repère 2027, Licence Ouverte 2.0) via l'API tabulaire de data.gouv.fr.
 * Les ressources tabulaires compatibles sont découvertes à partir de leur profil de colonnes.
 */
@Component
public class DataGouvPollSource implements PollSourcePort {

    private static final Logger log = LoggerFactory.getLogger(DataGouvPollSource.class);
    private static final int PAGE_SIZE = 100;

    private final HttpFetcher http;
    private final CrawlerProperties.DataGouv config;
    private final ObjectMapper mapper = new ObjectMapper();

    public DataGouvPollSource(HttpFetcher http, CrawlerProperties properties) {
        this.http = http;
        this.config = properties.datagouv();
    }

    @Override
    public String name() {
        return "data.gouv.fr";
    }

    @Override
    public boolean enabled() {
        return config != null && config.enabled();
    }

    @Override
    public List<FetchedPoll> fetch() throws Exception {
        String datasetUrl = "https://www.data.gouv.fr/api/1/datasets/" + config.datasetSlug() + "/";
        JsonNode dataset = mapper.readTree(http.get(datasetUrl));
        String apiBaseUrl = config.tabularApiBaseUrl().replaceAll("/+$", "");

        List<FetchedPoll> polls = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (JsonNode resource : dataset.path("resources")) {
            String title = resource.path("title").asText("");
            String format = resource.path("format").asText("").toLowerCase();
            if (!format.equals("csv")) {
                skipped.add(title + " [" + format + "]");
                continue;
            }
            String resourceId = resource.path("id").asText();
            if (resourceId.isBlank()) {
                log.warn("Ressource CSV « {} » ignorée : identifiant manquant", title);
                continue;
            }

            String resourceApiUrl = apiBaseUrl + "/resources/" + resourceId;
            JsonNode profile = mapper.readTree(http.get(resourceApiUrl + "/profile/"));
            if (!PollCsvParser.supportsTabularProfile(profile)) {
                skipped.add(title + " [colonnes non compatibles]");
                continue;
            }

            String fallbackUrl = resource.path("url").asText(
                    "https://www.data.gouv.fr/datasets/" + config.datasetSlug());
            List<FetchedPoll> parsed = fetchResourceData(resourceApiUrl, fallbackUrl);
            log.info("Ressource « {} » : {} sondage(s) lu(s)", title, parsed.size());
            if (parsed.isEmpty()) {
                log.warn("Ressource « {} » ignorée : aucune ligne exploitable via l'API tabulaire", title);
            }
            polls.addAll(parsed);
        }
        if (polls.isEmpty()) {
            log.warn("Aucun sondage extrait. Ressources écartées : {}", skipped);
        }
        return polls;
    }

    private List<FetchedPoll> fetchResourceData(String resourceApiUrl, String fallbackUrl) throws Exception {
        Map<PollKey, FetchedPoll> polls = new LinkedHashMap<>();
        Set<String> visitedPages = new HashSet<>();
        String nextUrl = resourceApiUrl + "/data/?page_size=" + PAGE_SIZE;
        while (nextUrl != null && !nextUrl.isBlank()) {
            if (!visitedPages.add(nextUrl)) {
                throw new IllegalStateException("Boucle de pagination détectée pour " + resourceApiUrl);
            }
            JsonNode page = mapper.readTree(http.get(nextUrl));
            for (FetchedPoll poll : PollCsvParser.parseTabularData(page.path("data"), fallbackUrl)) {
                PollKey key = new PollKey(poll.institute(), poll.client(), poll.fieldworkStart(), poll.fieldworkEnd(),
                        poll.sampleSize(), poll.scenario(), poll.round(), poll.sourceUrl());
                FetchedPoll previous = polls.get(key);
                if (previous == null) {
                    polls.put(key, poll);
                } else {
                    Map<String, BigDecimal> mergedResults = new LinkedHashMap<>(previous.results());
                    mergedResults.putAll(poll.results());
                    polls.put(key, new FetchedPoll(previous.institute(), previous.client(), previous.fieldworkStart(),
                            previous.fieldworkEnd(), previous.sampleSize(), previous.scenario(), previous.round(),
                            previous.sourceUrl(), mergedResults));
                }
            }
            JsonNode next = page.path("links").path("next");
            nextUrl = next.isTextual() && !next.asText().isBlank() ? next.asText() : null;
        }
        return new ArrayList<>(polls.values());
    }

    private record PollKey(String institute, String client, LocalDate fieldworkStart,
                           LocalDate fieldworkEnd, Integer sampleSize, String scenario, int round,
                           String sourceUrl) {
    }
}
