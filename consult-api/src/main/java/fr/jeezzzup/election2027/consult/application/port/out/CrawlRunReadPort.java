package fr.jeezzzup.election2027.consult.application.port.out;

import java.util.Optional;
import fr.jeezzzup.election2027.consult.domain.CrawlRun;

public interface CrawlRunReadPort {

    Optional<CrawlRun> findLatestSuccessfulRun();
}
