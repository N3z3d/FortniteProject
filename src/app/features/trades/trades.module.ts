import { NgModule } from '@angular/core';
import { TradesRoutingModule } from './trades-routing.module';
import { TradeListComponent } from './trade-list/trade-list.component';
import { TradeDetailComponent } from './trade-detail/trade-detail.component';
import { TradeFormComponent } from './trade-form/trade-form.component';
import { TradeHistoryComponent } from './trade-history/trade-history.component';

@NgModule({
  imports: [
    TradesRoutingModule,
    TradeListComponent,
    TradeDetailComponent,
    TradeFormComponent,
    TradeHistoryComponent
  ],
})
export class TradesModule { } 