import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

// Composants
import { CreateGameComponent } from './create-game/create-game.component';
import { JoinGameComponent } from './join-game/join-game.component';
import { GameDetailComponent } from './game-detail/game-detail.component';

const routes: Routes = [
  { path: '', loadComponent: () => import('./game-home/game-home.component').then(c => c.GameHomeComponent) },
  { path: 'create', component: CreateGameComponent },
  { path: 'join', component: JoinGameComponent },
  { path: ':id', component: GameDetailComponent },
  { path: ':id/draft', loadComponent: () => import('../draft/draft.component').then(c => c.DraftComponent) },
  { path: ':id/teams', loadChildren: () => import('../teams/teams.module').then(m => m.TeamsModule) },
  { path: ':id/leaderboard', loadChildren: () => import('../leaderboard/leaderboard.module').then(m => m.LeaderboardModule) },
  { path: ':id/dashboard', loadChildren: () => import('../dashboard/dashboard.module').then(m => m.DashboardModule) }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class GameRoutingModule { } 