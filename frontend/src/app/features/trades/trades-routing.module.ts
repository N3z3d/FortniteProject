import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { TradeListComponent } from './trade-list/trade-list.component';
import { TradeDetailComponent } from './trade-detail/trade-detail.component';
import { TradeFormComponent } from './trade-form/trade-form.component';
import { TradeHistoryComponent } from './trade-history/trade-history.component';

const routes: Routes = [
  {
    path: '',
    redirectTo: '/trades/list',
    pathMatch: 'full'
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