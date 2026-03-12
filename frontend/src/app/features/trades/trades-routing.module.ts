import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { TradingDashboardComponent } from './components/trading-dashboard/trading-dashboard.component';
import { TradeProposalComponent } from './components/trade-proposal/trade-proposal.component';

const routes: Routes = [
  {
    path: '',
    component: TradingDashboardComponent,
    pathMatch: 'full'
  },
  {
    path: 'dashboard',
    redirectTo: '',
    pathMatch: 'full'
  },
  {
    path: 'create',
    component: TradeProposalComponent
  },
  {
    path: 'create/:gameId',
    redirectTo: 'create',
    pathMatch: 'full'
  },
  {
    path: 'list',
    redirectTo: '',
    pathMatch: 'full'
  },
  {
    path: 'new',
    redirectTo: 'create',
    pathMatch: 'full'
  },
  {
    path: 'proposal',
    redirectTo: 'create',
    pathMatch: 'full'
  },
  {
    path: 'history',
    redirectTo: '',
    pathMatch: 'full'
  },
  {
    path: ':id',
    redirectTo: '',
    pathMatch: 'full'
  },
  {
    path: ':id/edit',
    redirectTo: 'create',
    pathMatch: 'full'
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class TradesRoutingModule { }
