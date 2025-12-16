import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { SimpleLeaderboardComponent } from './simple-leaderboard.component';

const routes: Routes = [
  {
    path: '',
    component: SimpleLeaderboardComponent
  }
];

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    RouterModule.forChild(routes),
    SimpleLeaderboardComponent
  ],
  exports: [
    RouterModule
  ]
})
export class LeaderboardModule { } 