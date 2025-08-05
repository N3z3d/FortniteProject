import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';

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
    HttpClientModule,
    RouterModule.forChild(routes),
    SimpleLeaderboardComponent
  ],
  exports: [
    RouterModule
  ]
})
export class LeaderboardModule { } 