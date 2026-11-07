package fr.jeezzzup.election2027.crawler.infrastructure.adapter.out.source;

import fr.jeezzzup.election2027.crawler.application.model.FetchedArticle;
import fr.jeezzzup.election2027.crawler.application.port.out.ArticleSourcePort;
import fr.jeezzzup.election2027.crawler.infrastructure.configuration.CrawlerProperties;
import fr.jeezzzup.election2027.crawler.infrastructure.adapter.out.http.HttpFetcher;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Recherche d'actualités sur la présidentielle via les flux RSS de Google Actualités. */
@Component
public class GoogleNewsRssSource implements ArticleSourcePort {

    private static final Logger log = LoggerFactory.getLogger(GoogleNewsRssSource.class);

    private final HttpFetcher http;
    private final CrawlerProperties.News config;

    public GoogleNewsRssSource(HttpFetcher http, CrawlerProperties properties) {
        this.http = http;
        this.config = properties.news();
    }

    @Override
    public String name() {
        return "google-news-rss";
    }

    @Override
    public boolean enabled() {
        return config != null && config.enabled() && config.queries() != null && !config.queries().isEmpty();
    }

    @Override
    public List<FetchedArticle> fetch() throws Exception {
        Map<String, FetchedArticle> byUrl = new LinkedHashMap<>();
        for (String query : config.queries()) {
            String url = "https://news.google.com/rss/search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&hl=fr&gl=FR&ceid=FR:fr";
            try {
                Document feed = Jsoup.parse(http.get(url), "", Parser.xmlParser());
                int taken = 0;
                for (Element item : feed.select("item")) {
                    if (taken++ >= config.maxPerQuery()) {
                        break;
                    }
                    String title = text(item, "title");
                    String link = text(item, "link");
                    if (title == null || link == null) {
                        continue;
                    }
                    String description = text(item, "description");
                    String summary = description == null ? null : Jsoup.parse(description).text();
                    byUrl.putIfAbsent(link, new FetchedArticle(title, link, text(item, "source"),
                            parseDate(text(item, "pubDate")), summary));
                }
            } catch (Exception e) {
                log.warn("Requête « {} » en échec : {}", query, e.getMessage());
            }
        }
        return new ArrayList<>(byUrl.values());
    }

    private static String text(Element parent, String tag) {
        Element e = parent.selectFirst(tag);
        if (e == null) {
            return null;
        }
        String t = e.text();
        return t == null || t.isBlank() ? null : t.trim();
    }

    private static Instant parseDate(String s) {
        if (s == null) {
            return null;
        }
        try {
            return ZonedDateTime.parse(s, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        } catch (Exception e) {
            return null;
        }
    }
}
