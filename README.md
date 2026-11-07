# Election présidentielle 2027 – sondages et actualités

Application en trois parties autour de l'élection présidentielle française de 2027 :

```
 data.gouv.fr (sondages)  ─┐
 Google Actualités (RSS)  ─┴─▶  crawler-api  ──écrit──▶  PostgreSQL  ◀──lit──  consult-api  ◀──GraphQL──  web (Angular)
                               Spring Boot :8081                                Spring Boot :8082            :4200
```

| Dossier | Rôle |
|---|---|
| `crawler-api/` | Collecte les sondages et articles sur le web, dédoublonne et stocke en base (Java 21 / Spring Boot 3.3 / Rest / Clean Architecture)  |
| `consult-api/` | Permet de consulter les sondages, tendances, articles, statistiques.. (Java 21 / Spring Boot 3.3 / GraphQL / Hexagonale Architecture) |
| `web/` | **Interface Angular 18** de consultation des sondages, tendances, articles, statistiques.. |
| `db/init.sql` | Schéma PostgreSQL (créé au premier démarrage du conteneur). |

Chaque API conserve son propre modèle métier et ses entités de persistance : `crawler-api` et `consult-api` partagent le schéma PostgreSQL, mais n'ont pas de dépendance de domaine entre elles.

## Prérequis

Java 21, Maven 3.9+, Node 20+ (ou 22), Docker (pour PostgreSQL).

## Démarrage

```bash
# 1. Base de données
docker compose up -d

# 2. Compiler les deux API
mvn -DskipTests package

# 3. API de collecte (port 8081) : lance une première collecte au démarrage
java -jar crawler-api/target/crawler-api-0.1.0.jar

# 4. API de consultation (port 8082), dans un autre terminal
java -jar consult-api/target/consult-api-0.1.0.jar

# 5. Interface web (port 4200), dans un autre terminal
cd web && npm install && npm start
```

Ouvrir <http://localhost:4200>. Explorateur GraphiQL : <http://localhost:8082/graphiql>.

## API de collecte (8081)

| Méthode | URL | Description |
|---|---|---|
| `POST` | `/api/crawl` | Lance une collecte immédiate (202, ou 409 si déjà en cours) |
| `GET` | `/api/crawl/status` | Collecte en cours ou non |
| `GET` | `/api/crawl/runs` | 20 dernières exécutions : source, statut, éléments trouvés / nouveaux, erreur |

La collecte tourne aussi toutes les 6 heures (`crawler.cron` dans `crawler-api/src/main/resources/application.yml`). Les doublons sont écartés par empreinte SHA-1 (institut + dates de terrain + hypothèse + tour pour les sondages, URL pour les articles) : relancer une collecte est sans danger.

### Sources

- **Sondages – `DataGouvPollSource`** : jeu de données ouvert « Présidentielle 2027 : sondages, candidatures et projection départementale » (association Repère 2027, Licence Ouverte 2.0). La source découvre les ressources du jeu via l'API de data.gouv.fr, puis utilise leur profil dans l'API tabulaire pour écarter celles qui ne contiennent pas les colonnes utiles. Elle lit ensuite les données tabulaires par pages de 100 lignes (et suit le lien de page suivante) : aucun téléchargement direct des CSV n'est nécessaire. `PollCsvParser` reconnaît les colonnes de métadonnées par alias et prend en charge les deux dispositions de résultats : format « long » (une ligne par candidat) et format « large » (une colonne numérique par candidat). Les lignes du format long correspondant au même sondage sont regroupées et leurs résultats fusionnés.
- **Actualités – `GoogleNewsRssSource`** : flux RSS de recherche Google Actualités, pour les requêtes listées dans `crawler.news.queries`.

Pour ajouter une source (site d'un institut, Wikipédia, autre flux…), implémenter le port applicatif `PollSourcePort` ou `ArticleSourcePort` dans un adaptateur sortant de l'infrastructure ; l'application la découvre via l'injection Spring.

Le collecteur suit une Clean Architecture : `domain/` contient des modèles sans annotations JPA ; `application/port/in` définit le cas d'usage et `application/port/out` les contrats vers les sources, la persistance et l'exécution asynchrone ; `application/service` orchestre la collecte. Le contrôleur HTTP et le planificateur sont des adaptateurs entrants sous `infrastructure/adapter/in`, tandis que les sources web, l'exécuteur asynchrone et les adaptateurs JPA sont sous `infrastructure/adapter/out`. Les entités ORM sont isolées dans `infrastructure/adapter/out/persistence/entity`.

## API GraphQL de consultation (8082)

L'endpoint unique est `POST /graphql`. Les champs de la requête racine sont :

| Champ | Arguments | Description |
|---|---|---|
| `polls` | `institute`, `from`, `to`, `round`, `page`, `size` | Sondages paginés, du plus récent au plus ancien |
| `poll` | `id` | Détail d'un sondage |
| `institutes` | — | Instituts présents en base |
| `trends` | `round`, `since` | Évolution par candidat (moyenne par date de fin de terrain) |
| `averages` | `round`, `days` | Score moyen par candidat sur la période récente |
| `articles` | `q`, `page`, `size` | Articles, recherche simple dans titre / résumé / source |
| `stats` | — | Compteurs et date de dernière collecte réussie |

Exemple :

```graphql
query {
  polls(round: 1, page: 0, size: 20) {
    content { id institute fieldworkEnd results { candidate percentage } }
    page { size number totalElements totalPages }
  }
  stats { polls articles institutes latestPollDate lastCrawl }
}
```

Le résultat paginé conserve la forme `{ content, page: { size, number, totalElements, totalPages } }`. Les types `Date`, `DateTime` et `BigDecimal` sont des scalaires GraphQL dédiés.

Une collection Postman importable, avec des requêtes de test pour chaque champ GraphQL et le contrôle de santé, est disponible dans `postman/consult-api.postman_collection.json`. Après import, définissez `baseUrl` si l'API n'écoute pas sur `http://localhost:8082` ; `pollId` peut être modifié pour tester le détail d'un autre sondage.

Le code suit une architecture hexagonale : les résolveurs GraphQL de `infrastructure.adapter.in.graphql` sont les adaptateurs entrants ; les interfaces `application.port.in` définissent les cas d'usage et `application.port.out` les ports de lecture. Le domaine contient des modèles Java indépendants de JPA ; `infrastructure.adapter.out.persistence.entity` contient les entités ORM, et `infrastructure.adapter.out.persistence` les repositories, mappers et adaptateurs qui implémentent les ports. Les modèles de réponse et de pagination sont regroupés dans `application.model.Models`.

## Configuration

Variables d'environnement reconnues par les deux API : `DB_URL`, `DB_USER`, `DB_PASSWORD`. L'API de consultation accepte aussi `CORS_ALLOWED_ORIGINS` (par défaut `http://localhost:4200`). L'URL de l'API de consultation utilisée par Angular est dans `web/src/app/config.ts`.

## Limites à connaître

- **Moyennes et courbes** : elles mélangent toutes les hypothèses testées par un même institut à une même date. C'est une vue d'ensemble, pas une moyenne pondérée de type agrégateur de sondages.
- **Ressources data.gouv.fr** : seules les ressources CSV dont le profil tabulaire contient les métadonnées de sondage et les résultats attendus sont traitées. Les autres sont ignorées ; si aucune donnée exploitable n'est trouvée, consulter les avertissements du journal de `crawler-api` (ressources écartées ou sans ligne exploitable). Pour prendre en charge de nouvelles colonnes, compléter les alias et règles de détection dans `PollCsvParser`.
- **Sécurité** : `POST /api/crawl` n'est pas authentifié. À protéger (ou ne pas exposer) avant toute mise en ligne.
- **Marge d'erreur** : un sondage n'est pas une prévision ; les écarts de quelques points sont dans la marge d'erreur.
