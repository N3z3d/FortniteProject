import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { TradingDashboardComponent } from './components/trading-dashboard/trading-dashboard.component';
import { TradeProposalComponent } from './components/trade-proposal/trade-proposal.component';
import { TradeListComponent } from './trade-list/trade-list.component';
import { TradeDetailComponent } from './trade-detail/trade-detail.component';
import { TradeFormComponent } from './trade-form/trade-form.component';
import { TradeHistoryComponent } from './trade-history/trade-history.component';

const routes: Routes = [
  {
    path: '',
    component: TradingDashboardComponent,
    pathMatch: 'full'
  },
  {
    path: 'dashboard',
    component: TradingDashboardComponent
  },
  {
    path: 'create',
    component: TradeProposalComponent
  },
  {
    path: 'create/:gameId',
    component: TradeProposalComponent
  },
  {
    path: 'list',
    component: TradeListComponent
  },
  {
    path: 'new',
    component: TradeFormComponent
  },
  {
    path: 'proposal',
    component: TradeProposalComponent
  },
  {
    path: 'history',
    component: TradeHistoryComponent
  },
  {
    path: ':id',
    component: TradeDetailComponent
  },
  {
    path: ':id/edit',
    component: TradeFormComponent
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class TradesRoutingModule { }