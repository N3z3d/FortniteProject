import { Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';
import { MainLayoutComponent } from './shared/components/main-layout/main-layout.component';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/login',
    pathMatch: 'full'
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(c => c.LoginComponent)
  },
  {
    path: 'diagnostic',
    loadComponent: () => import('./features/diagnostic/diagnostic.component').then(c => c.DiagnosticComponent)
  },
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [AuthGuard],
    children: [
      {
        path: '',
        redirectTo: '/games',
        pathMatch: 'full'
      },
      {
        path: 'games',
        loadComponent: () => import('./features/game/game-home/game-home.component').then(c => c.GameHomeComponent)
      },
      {
        path: 'games/create',
        loadComponent: () => import('./features/game/create-game/create-game.component').then(c => c.CreateGameComponent)
      },
      {
        path: 'games/join',
        loadComponent: () => import('./features/game/join-game/join-game.component').then(c => c.JoinGameComponent)
      },
      {
        path: 'games/:id',
        loadComponent: () => import('./features/game/game-detail/game-detail.component').then(c => c.GameDetailComponent)
      },
      {
        path: 'games/:id/draft',
        loadComponent: () => import('./features/draft/draft.component').then(c => c.DraftComponent)
      },
      {
        path: 'auth',
        loadChildren: () => import('./features/auth/auth.module').then(m => m.AuthModule)
      },
      {
        path: 'leaderboard',
        loadChildren: () => import('./features/leaderboard/leaderboard.module').then(m => m.LeaderboardModule)
      },
      {
        path: 'teams',
        loadChildren: () => import('./features/teams/teams.module').then(m => m.TeamsModule)
      },
      {
        path: 'trades',
        loadChildren: () => import('./features/trades/trades.module').then(m => m.TradesModule)
      },
      {
        path: 'dashboard',
        loadChildren: () => import('./features/dashboard/dashboard.module').then(m => m.DashboardModule)
      },
      {
        path: 'draft',
        loadComponent: () => import('./features/draft/draft.component').then(c => c.DraftComponent)
      }
    ]
  },
  {
    path: '**',
    loadComponent: () => import('./features/not-found/not-found.component').then(c => c.NotFoundComponent)
  }
]; 