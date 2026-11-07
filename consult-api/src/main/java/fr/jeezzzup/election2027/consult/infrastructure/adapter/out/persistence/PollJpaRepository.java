package fr.jeezzzup.election2027.consult.infrastructure.adapter.out.persistence;

import fr.jeezzzup.election2027.consult.application.model.Models.TrendRow;
import fr.jeezzzup.election2027.consult.infrastructure.adapter.out.persistence.entity.PollEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface PollJpaRepository extends JpaRepository<PollEntity, Long>, JpaSpecificationExecutor<PollEntity> {

    @Query("select distinct p.institute from PollEntity p order by p.institute")
    List<String> findInstitutes();

    @Query("select max(p.fieldworkEnd) from PollEntity p")
    LocalDate findLatestFieldworkEnd();

    @Query("""
            select new fr.jeezzzup.election2027.consult.application.model.Models$TrendRow(r.candidate, p.fieldworkEnd, avg(r.percentage), count(r))
            from PollEntity p join p.results r
            where p.voteRound = :round and p.fieldworkEnd is not null
            group by r.candidate, p.fieldworkEnd
            order by p.fieldworkEnd
            """)
    List<TrendRow> findTrendRows(int round);
}
