package fr.jeezzzup.election2027.crawler.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "poll_result")
@Getter
@Setter
@NoArgsConstructor
public class PollResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poll_id", nullable = false)
    private PollEntity poll;

    @Column(nullable = false)
    private String candidate;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;
}
