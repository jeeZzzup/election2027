import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_URL } from './config';
import {
  Article,
  CandidateAverage,
  CandidateTrend,
  PageResponse,
  Poll,
  Stats,
} from './models';

type PollQuery = {
  institute?: string | null;
  from?: string | null;
  to?: string | null;
  round?: number | null;
  page: number;
  size: number;
};

type ArticleQuery = {
  q?: string | null;
  page: number;
  size: number;
};

interface GraphQlResponse<T> {
  data?: Record<string, T>;
  errors?: { message: string }[];
}

/** Appelle l'API de consultation. Les paramètres vides sont omis. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  stats(): Observable<Stats> {
    return this.query<Stats>('stats', '{ stats { polls articles institutes latestPollDate lastCrawl } }');
  }

  polls(variables: PollQuery): Observable<PageResponse<Poll>> {
    return this.query<PageResponse<Poll>>(
      'polls',
      `query Polls($institute: String, $from: Date, $to: Date, $round: Int, $page: Int!, $size: Int!) {
        polls(institute: $institute, from: $from, to: $to, round: $round, page: $page, size: $size) {
          content { id institute client fieldworkStart fieldworkEnd sampleSize scenario round sourceUrl results { candidate percentage } }
          page { size number totalElements totalPages }
        }
      }`,
      variables,
    );
  }

  institutes(): Observable<string[]> {
    return this.query<string[]>('institutes', '{ institutes }');
  }

  averages(round: number, days: number): Observable<CandidateAverage[]> {
    return this.query<CandidateAverage[]>(
      'averages',
      'query Averages($round: Int!, $days: Int!) { averages(round: $round, days: $days) { candidate percentage polls } }',
      { round, days },
    );
  }

  trends(round: number): Observable<CandidateTrend[]> {
    return this.query<CandidateTrend[]>(
      'trends',
      'query Trends($round: Int!) { trends(round: $round) { candidate points { date percentage polls } } }',
      { round },
    );
  }

  articles(variables: ArticleQuery): Observable<PageResponse<Article>> {
    return this.query<PageResponse<Article>>(
      'articles',
      `query Articles($q: String, $page: Int!, $size: Int!) {
        articles(q: $q, page: $page, size: $size) {
          content { id title url source publishedAt summary }
          page { size number totalElements totalPages }
        }
      }`,
      variables,
    );
  }

  private query<T>(field: string, query: string, variables?: object): Observable<T> {
    const normalizedVariables =
      variables &&
      Object.fromEntries(Object.entries(variables).map(([key, value]) => [key, value === '' ? null : value]));
    return this.http.post<GraphQlResponse<T>>(API_URL, { query, variables: normalizedVariables }).pipe(
      map((response) => {
        if (response.errors?.length) {
          throw new Error(response.errors.map((error) => error.message).join('; '));
        }
        const result = response.data?.[field];
        if (result === undefined) {
          throw new Error(`La réponse GraphQL ne contient pas le champ « ${field} ».`);
        }
        return result;
      }),
    );
  }
}
