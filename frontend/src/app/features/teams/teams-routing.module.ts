import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { TeamDetailComponent } from './team-detail/team-detail.component';
import { TeamsListComponent } from './teams-list/teams-list.component';

export const TEAMS_ROUTES: Routes = [
  {
    path: '',
    component: TeamsListComponent
  },
  {
    path: 'create',
    loadComponent: () => import('./team-edit/team-edit.component').then(c => c.TeamEditComponent)
  },
  {
    path: 'detail/:id',
    component: TeamDetailComponent
  },
  {
    path: 'edit/:id',
    loadComponent: () => import('./team-edit/team-edit.component').then(c => c.TeamEditComponent)
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./team-edit/team-edit.component').then(c => c.TeamEditComponent)
  },
  {
    path: ':id',
    component: TeamDetailComponent
  }
];

@NgModule({
  imports: [RouterModule.forChild(TEAMS_ROUTES)],
  exports: [RouterModule]
})
export class TeamsRoutingModule { } 
