package fr.jeezzzup.election2027.crawler.infrastructure.adapter.out.persistence;

import fr.jeezzzup.election2027.crawler.application.port.out.PollStorePort;
import fr.jeezzzup.election2027.crawler.domain.Poll;
import fr.jeezzzup.election2027.crawler.infrastructure.adapter.out.persistence.entity.PollEntity;
import fr.jeezzzup.election2027.crawler.infrastructure.adapter.out.persistence.entity.PollResultEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class PollPersistenceAdapter implements PollStorePort {

    private final PollJpaRepository polls;

    public PollPersistenceAdapter(PollJpaRepository polls) {
        this.polls = polls;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByExternalKey(String externalKey) {
        return polls.existsByExternalKey(externalKey);
    }

    @Override
    public Poll save(Poll poll) {
        PollEntity entity = new PollEntity();
        entity.setExternalKey(poll.externalKey());
        entity.setInstitute(poll.institute());
        entity.setClient(poll.client());
        entity.setFieldworkStart(poll.fieldworkStart());
        entity.setFieldworkEnd(poll.fieldworkEnd());
        entity.setSampleSize(poll.sampleSize());
        entity.setScenario(poll.scenario());
        entity.setVoteRound(poll.voteRound());
        entity.setSourceUrl(poll.sourceUrl());
        entity.setCreatedAt(poll.createdAt());
        poll.results().forEach(result -> {
            PollResultEntity resultEntity = new PollResultEntity();
            resultEntity.setPoll(entity);
            resultEntity.setCandidate(result.candidate());
            resultEntity.setPercentage(result.percentage());
            entity.getResults().add(resultEntity);
        });
        polls.save(entity);
        return poll;
    }
}
