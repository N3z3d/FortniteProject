import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { canDeactivateDraftGuard } from '../../core/guards/draft-active.guard';

// Composants
import { CreateGameComponent } from './create-game/create-game.component';
import { GameDetailComponent } from './game-detail/game-detail.component';

const routes: Routes = [
  { path: '', loadComponent: () => import('./game-home/game-home.component').then(c => c.GameHomeComponent) },
  { path: 'create', component: CreateGameComponent },
  { path: 'join', loadComponent: () => import('./join-game/join-game.component').then(c => c.JoinGameComponent) },
  { path: ':id', component: GameDetailComponent },
  { path: ':id/draft', loadComponent: () => import('../draft/draft.component').then(c => c.DraftComponent) },
  { path: ':id/draft/snake', loadComponent: () => import('../draft/components/snake-draft-page/snake-draft-page.component').then(c => c.SnakeDraftPageComponent), canDeactivate: [canDeactivateDraftGuard] },
  { path: ':id/draft/simultaneous', loadComponent: () => import('../draft/components/simultaneous-draft-page/simultaneous-draft-page.component').then(c => c.SimultaneousDraftPageComponent), canDeactivate: [canDeactivateDraftGuard] },
  { path: ':id/draft/audit', loadComponent: () => import('../draft/components/draft-audit-page/draft-audit-page.component').then(c => c.DraftAuditPageComponent) },
  { path: ':id/teams', loadChildren: () => import('../teams/teams.module').then(m => m.TeamsModule) },
  { path: ':id/leaderboard', loadComponent: () => import('../leaderboard/game-leaderboard-page/game-leaderboard-page.component').then(c => c.GameLeaderboardPageComponent) },
  { path: ':id/dashboard', loadChildren: () => import('../dashboard/dashboard.module').then(m => m.DashboardModule) },
  { path: ':id/trades', loadChildren: () => import('../trades/trades.module').then(m => m.TradesModule) }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class GameRoutingModule { } 