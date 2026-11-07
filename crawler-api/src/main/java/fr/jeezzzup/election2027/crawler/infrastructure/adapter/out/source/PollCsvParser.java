package fr.jeezzzup.election2027.crawler.infrastructure.adapter.out.source;

import fr.jeezzzup.election2027.crawler.application.model.FetchedPoll;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Lecteur CSV tolérant : le format exact du fichier source n'étant pas figé, les colonnes sont
 * reconnues par alias (sans accents ni casse) et deux dispositions sont gérées :
 * <ul>
 *   <li><b>format « long »</b> : une ligne par candidat (colonnes candidat + pourcentage) ;</li>
 *   <li><b>format « large »</b> : une ligne par sondage, une colonne numérique par candidat.</li>
 * </ul>
 * Si le fichier a une autre forme, aucune ligne n'est retenue et la source le signale dans le journal.
 */
final class PollCsvParser {

    private static final List<String> INSTITUTE = List.of("institut", "institut_de_sondage", "sondeur", "organisme");
    private static final List<String> CLIENT = List.of("commanditaire", "client", "media", "commanditaires");
    private static final List<String> START = List.of("date_debut", "debut_terrain", "terrain_debut", "date_debut_terrain", "debut", "date_terrain_debut");
    private static final List<String> END = List.of("date_fin", "fin_terrain", "terrain_fin", "date_fin_terrain",
            "fin", "date", "date_terrain_fin");
    private static final List<String> PUBLICATION_DATE = List.of("date_publication", "date_publication_iso");
    private static final List<String> FIELDWORK_PERIOD = List.of("terrain", "periode_terrain", "periode_de_terrain");
    private static final List<String> SAMPLE = List.of("echantillon", "taille_echantillon", "effectif", "taille");
    private static final List<String> SCENARIO = List.of("hypothese", "scenario", "hypothese_testee");
    private static final List<String> ROUND = List.of("tour", "tour_scrutin", "round");
    private static final List<String> URL = List.of("notice_url", "url_notice", "lien_notice", "lien", "url",
            "source_url", "url_source", "notice", "source");
    private static final List<String> CANDIDATE = List.of("candidat", "nom_candidat", "candidat_nom", "candidate");
    private static final List<String> VALUE = List.of("pourcentage", "score", "intentions", "intentions_de_vote",
            "intentions_vote_pct", "valeur", "pct", "resultat", "percentage");
    private static final Set<String> IGNORED = Set.of("id", "total", "nsp", "marge", "marge_erreur", "annee", "mois", "ordre", "rang", "vague");
    private static final Set<String> NUMERIC_TYPES = Set.of("int", "float", "decimal", "number");

    private static final Pattern DASH_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}.*");
    private static final Pattern FRENCH_DATE_RANGE = Pattern.compile(
            "(?i).*?(\\d{1,2})\\s*(?:-|–|au|à)\\s*(\\d{1,2})\\s+([\\p{L}.]+)\\s+(\\d{4}).*");
    private static final Map<String, Integer> FRENCH_MONTHS = Map.ofEntries(
            Map.entry("janvier", 1), Map.entry("janv", 1),
            Map.entry("fevrier", 2), Map.entry("fevr", 2),
            Map.entry("mars", 3), Map.entry("avril", 4), Map.entry("avr", 4),
            Map.entry("mai", 5), Map.entry("juin", 6), Map.entry("juillet", 7), Map.entry("juil", 7),
            Map.entry("aout", 8), Map.entry("septembre", 9), Map.entry("sept", 9),
            Map.entry("octobre", 10), Map.entry("oct", 10), Map.entry("novembre", 11), Map.entry("nov", 11),
            Map.entry("decembre", 12), Map.entry("dec", 12));
    private static final DateTimeFormatter FR_DATE = DateTimeFormatter.ofPattern("d/M/yyyy");

    private PollCsvParser() {
    }

    static boolean supportsTabularProfile(JsonNode profile) {
        JsonNode headerNode = profile.path("profile").path("header");
        if (!headerNode.isArray()) {
            return false;
        }

        Map<String, String> headers = new LinkedHashMap<>();
        headerNode.forEach(header -> headers.putIfAbsent(normalize(header.asText()), header.asText()));
        if (find(headers, INSTITUTE) == null
                || (find(headers, START) == null && find(headers, END) == null
                && find(headers, PUBLICATION_DATE) == null)) {
            return false;
        }
        if (find(headers, CANDIDATE) != null && find(headers, VALUE) != null) {
            return true;
        }

        JsonNode columns = profile.path("profile").path("columns");
        Set<String> metadata = new HashSet<>(IGNORED);
        for (List<String> aliases : List.of(INSTITUTE, CLIENT, START, END, PUBLICATION_DATE, FIELDWORK_PERIOD,
                SAMPLE, SCENARIO, ROUND, URL, CANDIDATE, VALUE)) {
            metadata.addAll(aliases);
        }
        var columnNames = columns.fieldNames();
        while (columnNames.hasNext()) {
            String column = columnNames.next();
            if (!metadata.contains(normalize(column))
                    && NUMERIC_TYPES.contains(columns.path(column).path("python_type").asText())) {
                return true;
            }
        }
        return false;
    }

    static List<FetchedPoll> parseTabularData(JsonNode rows, String defaultUrl) throws IOException {
        if (!rows.isArray() || rows.isEmpty()) {
            return List.of();
        }

        Set<String> headerSet = new LinkedHashSet<>();
        rows.forEach(row -> row.fieldNames().forEachRemaining(headerSet::add));
        String[] headers = headerSet.toArray(String[]::new);
        StringBuilder csv = new StringBuilder();
        try (CSVPrinter printer = new CSVPrinter(csv,
                CSVFormat.DEFAULT.builder().setHeader(headers).build())) {
            for (JsonNode row : rows) {
                List<String> values = new ArrayList<>(headers.length);
                for (String header : headers) {
                    JsonNode value = row.get(header);
                    values.add(value == null || value.isNull() ? "" : value.isTextual() ? value.asText() : value.toString());
                }
                printer.printRecord(values);
            }
        }
        return parse(csv.toString(), defaultUrl);
    }

    static List<FetchedPoll> parse(String csv, String defaultUrl) throws IOException {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        if (csv.startsWith("﻿")) {
            csv = csv.substring(1);
        }
        String firstLine = csv.lines().findFirst().orElse("");
        char delimiter = count(firstLine, ';') > count(firstLine, ',') ? ';' : ',';
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();

        try (CSVParser parser = format.parse(new StringReader(csv))) {
            Map<String, String> byNorm = new LinkedHashMap<>();
            for (String header : parser.getHeaderNames()) {
                byNorm.putIfAbsent(normalize(header), header);
            }
            String cInstitute = find(byNorm, INSTITUTE);
            String cClient = find(byNorm, CLIENT);
            String cStart = find(byNorm, START);
            String cEnd = find(byNorm, END);
            String cPublicationDate = find(byNorm, PUBLICATION_DATE);
            String cFieldworkPeriod = find(byNorm, FIELDWORK_PERIOD);
            String cSample = find(byNorm, SAMPLE);
            String cScenario = find(byNorm, SCENARIO);
            String cRound = find(byNorm, ROUND);
            String cUrl = find(byNorm, URL);
            String cCandidate = find(byNorm, CANDIDATE);
            String cValue = find(byNorm, VALUE);
            boolean longFormat = cCandidate != null && cValue != null;

            List<CSVRecord> records = parser.getRecords();

            List<String> candidateColumns = new ArrayList<>();
            if (!longFormat) {
                Set<String> meta = new HashSet<>();
                for (String c : new String[]{cInstitute, cClient, cStart, cEnd, cSample, cScenario, cRound, cUrl}) {
                    if (c != null) {
                        meta.add(c);
                    }
                }
                for (String header : parser.getHeaderNames()) {
                    if (meta.contains(header) || IGNORED.contains(normalize(header))) {
                        continue;
                    }
                    if (records.stream().anyMatch(r -> number(get(r, header)) != null)) {
                        candidateColumns.add(header);
                    }
                }
            }

            Map<String, Acc> groups = new LinkedHashMap<>();
            for (CSVRecord record : records) {
                String institute = get(record, cInstitute);
                LocalDate start = date(get(record, cStart));
                LocalDate end = date(get(record, cEnd));
                DateRange fieldworkPeriod = fieldworkRange(get(record, cFieldworkPeriod));
                if (fieldworkPeriod != null) {
                    if (start == null) {
                        start = fieldworkPeriod.start();
                    }
                    if (end == null) {
                        end = fieldworkPeriod.end();
                    }
                }
                if (end == null) {
                    end = date(get(record, cPublicationDate));
                }
                if (end == null) {
                    end = start;
                }
                if (institute == null || end == null) {
                    continue;
                }
                String scenario = get(record, cScenario);
                int round = round(get(record, cRound));
                String key = institute + "|" + start + "|" + end + "|" + scenario + "|" + round
                        + (longFormat ? "" : "|" + record.getRecordNumber());
                Acc acc = groups.get(key);
                if (acc == null) {
                    acc = new Acc(institute, get(record, cClient), start, end, sample(get(record, cSample)),
                            scenario, round, Optional.ofNullable(get(record, cUrl)).orElse(defaultUrl));
                    groups.put(key, acc);
                }
                if (longFormat) {
                    String candidate = get(record, cCandidate);
                    BigDecimal value = number(get(record, cValue));
                    if (candidate != null && value != null) {
                        acc.results.put(candidate, value);
                    }
                } else {
                    for (String column : candidateColumns) {
                        BigDecimal value = number(get(record, column));
                        if (value != null) {
                            acc.results.put(column, value);
                        }
                    }
                }
            }

            List<FetchedPoll> polls = new ArrayList<>();
            for (Acc a : groups.values()) {
                if (!a.results.isEmpty()) {
                    polls.add(new FetchedPoll(a.institute, a.client, a.start, a.end, a.sample, a.scenario, a.round, a.url, a.results));
                }
            }
            return polls;
        }
    }

    /** Colonnes détectées, pour le journal en cas de fichier inexploitable. */
    static String describeHeader(String csv) {
        return csv == null ? "" : csv.lines().findFirst().orElse("");
    }

    private static final class Acc {
        final String institute;
        final String client;
        final LocalDate start;
        final LocalDate end;
        final Integer sample;
        final String scenario;
        final int round;
        final String url;
        final Map<String, BigDecimal> results = new LinkedHashMap<>();

        Acc(String institute, String client, LocalDate start, LocalDate end, Integer sample, String scenario, int round, String url) {
            this.institute = institute;
            this.client = client;
            this.start = start;
            this.end = end;
            this.sample = sample;
            this.scenario = scenario;
            this.round = round;
            this.url = url;
        }
    }

    private static String find(Map<String, String> byNorm, List<String> aliases) {
        for (String alias : aliases) {
            if (byNorm.containsKey(alias)) {
                return byNorm.get(alias);
            }
        }
        return null;
    }

    private static String get(CSVRecord record, String header) {
        if (header == null || !record.isMapped(header) || !record.isSet(header)) {
            return null;
        }
        String value = record.get(header);
        return value == null || value.isBlank() ? null : value.trim();
    }

    static String normalize(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        return n.replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    private static BigDecimal number(String s) {
        if (s == null) {
            return null;
        }
        String cleaned = s.replace("%", "").replace(",", ".").replaceAll("[\\s ]", "");
        try {
            BigDecimal value = new BigDecimal(cleaned);
            return value.signum() < 0 || value.compareTo(BigDecimal.valueOf(100)) > 0 ? null : value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDate date(String s) {
        if (s == null) {
            return null;
        }
        try {
            if (DASH_DATE.matcher(s).matches()) {
                return LocalDate.parse(s.substring(0, 10));
            }
            return LocalDate.parse(s, FR_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static DateRange fieldworkRange(String value) {
        if (value == null) {
            return null;
        }
        var matcher = FRENCH_DATE_RANGE.matcher(value);
        if (!matcher.matches()) {
            return null;
        }
        Integer month = FRENCH_MONTHS.get(normalize(matcher.group(3)));
        if (month == null) {
            return null;
        }
        try {
            int year = Integer.parseInt(matcher.group(4));
            return new DateRange(
                    LocalDate.of(year, month, Integer.parseInt(matcher.group(1))),
                    LocalDate.of(year, month, Integer.parseInt(matcher.group(2))));
        } catch (DateTimeException | NumberFormatException exception) {
            return null;
        }
    }

    private record DateRange(LocalDate start, LocalDate end) {
    }

    private static Integer sample(String s) {
        if (s == null) {
            return null;
        }
        String digits = s.replaceAll("[^0-9]", "");
        if (digits.isEmpty() || digits.length() > 7) {
            return null;
        }
        return Integer.parseInt(digits);
    }

    private static int round(String s) {
        if (s == null) {
            return 1;
        }
        String lower = s.toLowerCase(Locale.ROOT);
        return lower.contains("2") || lower.contains("second") || lower.contains("deux") ? 2 : 1;
    }

    private static long count(String s, char c) {
        return s.chars().filter(ch -> ch == c).count();
    }
}
