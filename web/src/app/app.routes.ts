import { Routes } from '@angular/router';
import { DashboardComponent } from './pages/dashboard.component';
import { PollsComponent } from './pages/polls.component';
import { ArticlesComponent } from './pages/articles.component';

export const routes: Routes = [
  { path: '', component: DashboardComponent, title: 'Tableau de bord – Présidentielle 2027' },
  { path: 'sondages', component: PollsComponent, title: 'Sondages – Présidentielle 2027' },
  { path: 'articles', component: ArticlesComponent, title: 'Actualités – Présidentielle 2027' },
  { path: '**', redirectTo: '' },
];
