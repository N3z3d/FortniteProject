import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { MaterialModule } from '../../shared/material/material.module';
import { TradesRoutingModule } from './trades-routing.module';

// Import new premium components
import { TradingDashboardComponent } from './components/trading-dashboard/trading-dashboard.component';
import { TradeProposalComponent } from './components/trade-proposal/trade-proposal.component';
import { TradeDetailsComponent } from './components/trade-details/trade-details.component';

// Import existing standalone components
import { TradeListComponent } from './trade-list/trade-list.component';
import { TradeDetailComponent } from './trade-detail/trade-detail.component';
import { TradeFormComponent } from './trade-form/trade-form.component';
import { TradeHistoryComponent } from './trade-history/trade-history.component';

// Import services
import { TradingService } from './services/trading.service';

@NgModule({
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MaterialModule,
    TradesRoutingModule,
    // Import new premium components
    TradingDashboardComponent,
    TradeProposalComponent,
    TradeDetailsComponent,
    // Import existing standalone components
    TradeListComponent,
    TradeDetailComponent,
    TradeFormComponent,
    TradeHistoryComponent
  ],
  providers: [
    TradingService
  ],
  exports: [
    TradingDashboardComponent,
    TradeProposalComponent,
    TradeDetailsComponent
  ]
})
export class TradesModule { }