import { Routes } from '@angular/router';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./admin-dashboard/admin-dashboard.component').then(c => c.AdminDashboardComponent)
  },
  {
    path: 'errors',
    loadComponent: () =>
      import('./error-journal/error-journal.component').then(c => c.ErrorJournalComponent)
  },
  {
    path: 'pipeline',
    loadComponent: () =>
      import('./pipeline/admin-pipeline-page/admin-pipeline-page.component').then(
        c => c.AdminPipelinePageComponent
      )
  },
  {
    path: 'incidents',
    loadComponent: () =>
      import('./incidents/admin-incident-list.component').then(
        c => c.AdminIncidentListComponent
      )
  }
];
