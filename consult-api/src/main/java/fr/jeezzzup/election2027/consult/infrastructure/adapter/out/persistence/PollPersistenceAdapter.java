package fr.jeezzzup.election2027.consult.infrastructure.adapter.out.persistence;

import fr.jeezzzup.election2027.consult.application.model.Models.*;
import fr.jeezzzup.election2027.consult.application.port.out.PollReadPort;
import fr.jeezzzup.election2027.consult.domain.Poll;
import fr.jeezzzup.election2027.consult.domain.PollResult;
import fr.jeezzzup.election2027.consult.infrastructure.adapter.out.persistence.entity.PollEntity;
import fr.jeezzzup.election2027.consult.infrastructure.adapter.out.persistence.entity.PollResultEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class PollPersistenceAdapter implements PollReadPort {

    private final PollJpaRepository polls;

    public PollPersistenceAdapter(PollJpaRepository polls) {
        this.polls = polls;
    }

    @Override
    public PageResult<Poll> search(String institute, LocalDate from, LocalDate to, Integer round, int page, int size) {
        Specification<PollEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (institute != null && !institute.isBlank()) {
                predicates.add(cb.equal(root.get("institute"), institute));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fieldworkEnd"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fieldworkEnd"), to));
            }
            if (round != null) {
                predicates.add(cb.equal(root.get("voteRound"), round));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("fieldworkEnd"), Sort.Order.desc("id")));
        Page<Poll> result = polls.findAll(spec, pageable).map(PollPersistenceAdapter::toDomain);
        return toPageResult(result);
    }

    @Override
    public Optional<Poll> findById(Long id) {
        return polls.findById(id).map(PollPersistenceAdapter::toDomain);
    }

    @Override
    public List<String> findInstitutes() {
        return polls.findInstitutes();
    }

    @Override
    public LocalDate findLatestFieldworkEnd() {
        return polls.findLatestFieldworkEnd();
    }

    @Override
    public List<TrendRow> findTrendRows(int round) {
        return polls.findTrendRows(round);
    }

    @Override
    public long count() {
        return polls.count();
    }

    private static Poll toDomain(PollEntity poll) {
        List<PollResult> results = poll.getResults().stream()
                .map(PollPersistenceAdapter::toDomain)
                .toList();
        return new Poll(poll.getId(), poll.getInstitute(), poll.getClient(), poll.getFieldworkStart(),
                poll.getFieldworkEnd(), poll.getSampleSize(), poll.getScenario(), poll.getVoteRound(),
                poll.getSourceUrl(), results);
    }

    private static PollResult toDomain(PollResultEntity result) {
        return new PollResult(result.getCandidate(), result.getPercentage());
    }

    private static <T> PageResult<T> toPageResult(Page<T> page) {
        return new PageResult<>(page.getContent(), new PageMetadata(page.getSize(), page.getNumber(),
                page.getTotalElements(), page.getTotalPages()));
    }
}
