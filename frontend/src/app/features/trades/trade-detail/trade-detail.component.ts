import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router } from '@angular/router';
import { LoggerService } from '../../../core/services/logger.service';
import { TranslationService } from '../../../core/services/translation.service';
import { TradeDetail, TradeDetailStatus } from '../models/trade-detail.model';
import { TradeDetailService } from '../services/trade-detail.service';

@Component({
  selector: 'app-trade-detail',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './trade-detail.component.html',
  styleUrls: ['./trade-detail.component.scss']
})
export class TradeDetailComponent implements OnInit {
  trade: TradeDetail | null = null;
  isLoading = true;
  tradeId: string = '';
  public readonly t = inject(TranslationService);
  private readonly tradeDetailService = inject(TradeDetailService);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private logger: LoggerService
  ) {}

  ngOnInit(): void {
    this.tradeId = this.route.snapshot.paramMap.get('id') || '';
    this.loadTradeDetail();
  }

  private loadTradeDetail(): void {
    // Simulate API call
    setTimeout(() => {
      this.trade = this.tradeDetailService.getTradeDetail(this.tradeId);
      this.isLoading = false;
    }, 1000);
  }

  completeTrade(): void {
    this.logger.debug('TradeDetail: completing trade', { tradeId: this.tradeId });
    // Implement complete trade logic
  }

  cancelTrade(): void {
    this.logger.debug('TradeDetail: cancelling trade', { tradeId: this.tradeId });
    // Implement cancel trade logic
  }

  goBack(): void {
    this.router.navigate(['/trades']);
  }

  getStatusColor(status: TradeDetailStatus | string): string {
    return this.tradeDetailService.getStatusColor(status);
  }

  getStatusLabel(status: TradeDetailStatus | string): string {
    return this.tradeDetailService.getStatusLabel(status);
  }

  getTimelineLabel(status: TradeDetailStatus | string): string {
    return this.tradeDetailService.getTimelineLabel(status);
  }
}

