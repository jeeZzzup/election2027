package fr.jeezzzup.election2027.crawler.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "poll")
@Getter
@Setter
@NoArgsConstructor
public class PollEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_key", nullable = false, unique = true)
    private String externalKey;

    @Column(nullable = false)
    private String institute;

    private String client;

    @Column(name = "fieldwork_start")
    private LocalDate fieldworkStart;

    @Column(name = "fieldwork_end")
    private LocalDate fieldworkEnd;

    @Column(name = "sample_size")
    private Integer sampleSize;

    private String scenario;

    @Column(name = "vote_round", nullable = false)
    private Integer voteRound;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<PollResultEntity> results = new ArrayList<>();
}
