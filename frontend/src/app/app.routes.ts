import { Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';
import { GameSelectionGuard } from './core/guards/game-selection.guard';
import { gamesResolver } from './features/game/resolvers/games.resolver';
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
        path: 'games',
        loadChildren: () => import('./features/game/game.module').then(m => m.GameModule),
        resolve: { userGames: gamesResolver }
      },
      {
        path: 'auth',
        loadChildren: () => import('./features/auth/auth.module').then(m => m.AuthModule)
      },
      {
        // BE-P0-02: Require game selection before accessing leaderboard
        path: 'leaderboard',
        loadChildren: () => import('./features/leaderboard/leaderboard.module').then(m => m.LeaderboardModule),
        canActivate: [GameSelectionGuard]
      },
      {
        // BE-P0-02: Require game selection before accessing teams
        path: 'teams',
        loadChildren: () => import('./features/teams/teams.module').then(m => m.TeamsModule),
        canActivate: [GameSelectionGuard]
      },
      {
        // BE-P0-02: Require game selection before accessing trades
        path: 'trades',
        loadChildren: () => import('./features/trades/trades.module').then(m => m.TradesModule),
        canActivate: [GameSelectionGuard]
      },
      {
        // BE-P0-02: Require game selection before accessing dashboard
        path: 'dashboard',
        loadChildren: () => import('./features/dashboard/dashboard.module').then(m => m.DashboardModule),
        canActivate: [GameSelectionGuard]
      },
      {
        // BE-P0-02: Require game selection before accessing draft
        path: 'draft',
        loadComponent: () => import('./features/draft/draft.component').then(c => c.DraftComponent),
        canActivate: [GameSelectionGuard]
      },
      {
        path: 'profile',
        loadComponent: () => import('./features/profile/profile.component').then(c => c.ProfileComponent)
      },
      {
        path: 'settings',
        loadComponent: () => import('./features/settings/settings.component').then(c => c.SettingsComponent)
      },
      {
        path: 'contact',
        loadComponent: () => import('./features/legal/legal.component').then(c => c.LegalComponent),
        data: { pageType: 'contact' }
      },
      {
        path: 'legal-notice',
        loadComponent: () => import('./features/legal/legal.component').then(c => c.LegalComponent),
        data: { pageType: 'legal-notice' }
      },
      {
        path: 'privacy',
        loadComponent: () => import('./features/legal/legal.component').then(c => c.LegalComponent),
        data: { pageType: 'privacy' }
      }
    ]
  },
  {
    path: '**',
    loadComponent: () => import('./features/not-found/not-found.component').then(c => c.NotFoundComponent)
  }
]; 
