package fr.jeezzzup.election2027.consult.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "article")
@Getter
@Setter
@NoArgsConstructor
public class ArticleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_key", nullable = false, unique = true)
    private String externalKey;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 1500)
    private String url;

    private String source;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    @Column(length = 2000)
    private String summary;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
