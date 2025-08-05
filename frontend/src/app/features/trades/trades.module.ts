import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { MaterialModule } from '../../shared/material/material.module';
import { TradesRoutingModule } from './trades-routing.module';

// Import standalone components
import { TradeListComponent } from './trade-list/trade-list.component';
import { TradeDetailComponent } from './trade-detail/trade-detail.component';
import { TradeFormComponent } from './trade-form/trade-form.component';
import { TradeHistoryComponent } from './trade-history/trade-history.component';

@NgModule({
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MaterialModule,
    TradesRoutingModule,
    // Import standalone components
    TradeListComponent,
    TradeDetailComponent,
    TradeFormComponent,
    TradeHistoryComponent
  ]
})
export class TradesModule { }