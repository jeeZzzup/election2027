package fr.jeezzzup.election2027.crawler.application.port.out;

import fr.jeezzzup.election2027.crawler.domain.Poll;

public interface PollStorePort {

    boolean existsByExternalKey(String externalKey);

    Poll save(Poll poll);
}
