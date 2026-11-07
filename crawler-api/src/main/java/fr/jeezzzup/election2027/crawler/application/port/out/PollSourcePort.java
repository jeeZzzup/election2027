package fr.jeezzzup.election2027.crawler.application.port.out;

import fr.jeezzzup.election2027.crawler.application.model.FetchedPoll;

import java.util.List;

public interface PollSourcePort {

    String name();

    default boolean enabled() {
        return true;
    }

    List<FetchedPoll> fetch() throws Exception;
}
