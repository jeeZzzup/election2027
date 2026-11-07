package fr.jeezzzup.election2027.crawler.infrastructure.adapter.out.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PollCsvParserTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void recognizesTabularIntentionsResourceAndParsesItsRows() throws Exception {
        JsonNode profile = mapper.readTree("""
                {"profile":{"header":["candidat","intentions_vote_pct","institut","date_publication","terrain","url_source"]}}
                """);
        JsonNode rows = mapper.readTree("""
                [
                  {"__id":1,"candidat":"Candidate A","intentions_vote_pct":32.5,"institut":"Elabe","date_publication":"2026-10-17","terrain":"terrain 14-16 octobre 2026","url_source":"https://example.test/notice"},
                  {"__id":2,"candidat":"Candidate B","intentions_vote_pct":16.5,"institut":"Elabe","date_publication":"2026-10-17","terrain":"terrain 14-16 octobre 2026","url_source":"https://example.test/notice"}
                ]
                """);

        assertTrue(PollCsvParser.supportsTabularProfile(profile));

        var polls = PollCsvParser.parseTabularData(rows, "https://example.test/resource.csv");

        assertEquals(1, polls.size());
        assertEquals("Elabe", polls.getFirst().institute());
        assertEquals(LocalDate.parse("2026-10-14"), polls.getFirst().fieldworkStart());
        assertEquals(LocalDate.parse("2026-10-16"), polls.getFirst().fieldworkEnd());
        assertEquals("https://example.test/notice", polls.getFirst().sourceUrl());
        assertEquals(new BigDecimal("32.5"), polls.getFirst().results().get("Candidate A"));
        assertEquals(new BigDecimal("16.5"), polls.getFirst().results().get("Candidate B"));
    }

    @Test
    void ignoresTabularResourcesWithoutCandidateResults() throws Exception {
        JsonNode profile = mapper.readTree("""
                {"profile":{
                  "header":["institut","date_publication","echantillon","principaux_enseignements"],
                  "columns":{
                    "institut":{"python_type":"string"},
                    "date_publication":{"python_type":"date"},
                    "echantillon":{"python_type":"int"},
                    "principaux_enseignements":{"python_type":"string"}
                  }
                }}
                """);

        assertFalse(PollCsvParser.supportsTabularProfile(profile));
    }

    @Test
    void recognizesWideTabularResourcesWithNumericCandidateColumns() throws Exception {
        JsonNode profile = mapper.readTree("""
                {"profile":{
                  "header":["institut","date_publication","echantillon","Candidate A"],
                  "columns":{
                    "institut":{"python_type":"string"},
                    "date_publication":{"python_type":"date"},
                    "echantillon":{"python_type":"int"},
                    "Candidate A":{"python_type":"float"}
                  }
                }}
                """);

        assertTrue(PollCsvParser.supportsTabularProfile(profile));
    }
}
