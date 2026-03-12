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
  },
  {
    path: 'games',
    loadComponent: () =>
      import(
        './games-supervision/admin-games-supervision/admin-games-supervision.component'
      ).then(c => c.AdminGamesSupervisionComponent)
  },
  {
    path: 'database',
    loadComponent: () =>
      import('./db-explorer/admin-db-explorer.component').then(c => c.AdminDbExplorerComponent)
  },
  {
    path: 'users',
    loadComponent: () =>
      import('./user-list/admin-user-list.component').then(c => c.AdminUserListComponent)
  },
  {
    path: 'logs',
    loadComponent: () =>
      import('./logs/admin-logs.component').then(c => c.AdminLogsComponent)
  }
];
