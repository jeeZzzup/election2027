export interface PollResult {
  candidate: string;
  percentage: number;
}

export interface Poll {
  id: number;
  institute: string;
  client?: string | null;
  fieldworkStart?: string | null;
  fieldworkEnd?: string | null;
  sampleSize?: number | null;
  scenario?: string | null;
  round: number;
  sourceUrl?: string | null;
  results: PollResult[];
}

export interface Article {
  id: number;
  title: string;
  url: string;
  source?: string | null;
  publishedAt: string;
  summary?: string | null;
}

export interface PageResponse<T> {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

export interface TrendPoint {
  date: string;
  percentage: number;
  polls: number;
}

export interface CandidateTrend {
  candidate: string;
  points: TrendPoint[];
}

export interface CandidateAverage {
  candidate: string;
  percentage: number;
  polls: number;
}

export interface Stats {
  polls: number;
  articles: number;
  institutes: number;
  latestPollDate?: string | null;
  lastCrawl?: string | null;
}
