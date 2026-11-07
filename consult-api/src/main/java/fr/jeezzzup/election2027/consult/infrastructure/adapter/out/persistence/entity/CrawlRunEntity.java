package fr.jeezzzup.election2027.consult.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "crawl_run")
@Getter
@Setter
@NoArgsConstructor
public class CrawlRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String source;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "items_found", nullable = false)
    private int itemsFound;

    @Column(name = "items_new", nullable = false)
    private int itemsNew;

    @Column(length = 2000)
    private String message;
}
