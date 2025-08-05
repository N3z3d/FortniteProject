import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TeamsRoutingModule } from './teams-routing.module';
import { TeamDetailComponent } from './team-detail/team-detail.component';
import { TeamsListComponent } from './teams-list/teams-list.component';

@NgModule({
  imports: [
    CommonModule,
    RouterModule,
    TeamsRoutingModule,
    TeamDetailComponent,
    TeamsListComponent
  ],
  exports: [TeamDetailComponent, TeamsListComponent]
})
export class TeamsModule { } 