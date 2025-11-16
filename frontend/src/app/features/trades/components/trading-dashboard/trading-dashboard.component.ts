import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, inject, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable, Subject, BehaviorSubject, combineLatest } from 'rxjs';
import { takeUntil, map, startWith, debounceTime } from 'rxjs/operators';
import { trigger, transition, style, animate, query, stagger } from '@angular/animations';

import { MaterialModule } from '../../../../shared/material/material.module';
import { TradingService, TradeOffer, TradeStats } from '../../services/trading.service';
import { UserContextService } from '../../../../core/services/user-context.service';
import { NotificationService } from '../../../../shared/services/notification.service';

@Component({
  selector: 'app-trading-dashboard',
  standalone: true,
  imports: [CommonModule, MaterialModule],
  templateUrl: './trading-dashboard.component.html',
  styleUrls: [
    './trading-dashboard.component.scss',
    '../../styles/trading-theme.scss',
    '../../styles/trading-animations.scss'
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  animations: [
    trigger('slideInStagger', [
      transition('* => *', [
        query(':enter', [
          style({ opacity: 0, transform: 'translateY(50px)' }),
          stagger(100, [
            animate('0.6s cubic-bezier(0.25, 0.46, 0.45, 0.94)', 
              style({ opacity: 1, transform: 'translateY(0)' })
            )
          ])
        ], { optional: true })
      ])
    ]),
    trigger('cardHover', [
      transition(':enter', [
        style({ transform: 'scale(0.9)', opacity: 0 }),
        animate('0.3s cubic-bezier(0.68, -0.55, 0.265, 1.55)',
          style({ transform: 'scale(1)', opacity: 1 })
        )
      ])
    ]),
    trigger('statusChange', [
      transition('pending => accepted', [
        animate('0.8s cubic-bezier(0.68, -0.55, 0.265, 1.55)', 
          style({ transform: 'scale(1.05)' })
        ),
        animate('0.3s ease-out', 
          style({ transform: 'scale(1)' })
        )
      ]),
      transition('pending => rejected', [
        animate('0.4s ease-in-out', 
          style({ transform: 'rotateX(180deg)' })
        ),
        animate('0.3s ease-out', 
          style({ transform: 'rotateX(0deg)' })
        )
      ])
    ])
  ]
})
export class TradingDashboardComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();
  private readonly searchSubject = new BehaviorSubject<string>('');
  
  // Injected services
  private readonly tradingService = inject(TradingService);
  public readonly userContextService = inject(UserContextService);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly notificationService = inject(NotificationService);

  @ViewChild('searchInput', { static: false }) searchInput?: ElementRef<HTMLInputElement>;

  // Observable streams
  trades$!: Observable<TradeOffer[]>;
  tradingStats$!: Observable<TradeStats | null>;
  loading$!: Observable<boolean>;
  error$!: Observable<string | null>;

  // Filtered and categorized trades
  pendingTrades$!: Observable<TradeOffer[]>;
  sentTrades$!: Observable<TradeOffer[]>;
  receivedTrades$!: Observable<TradeOffer[]>;
  completedTrades$!: Observable<TradeOffer[]>;

  // UI State
  selectedTab = new BehaviorSubject<number>(0);
  searchQuery$ = this.searchSubject.asObservable();
  isRefreshing = new BehaviorSubject<boolean>(false);
  
  // Stats for dashboard cards
  dashboardStats$ = combineLatest([
    this.tradingService.tradingStats$,
    this.tradingService.trades$
  ]).pipe(
    map(([stats, trades]) => ({
      totalTrades: stats?.totalTrades || 0,
      pendingOffers: trades.filter(t => t.status === 'pending').length,
      successfulTrades: stats?.successfulTrades || 0,
      averageTradeValue: stats?.averageTradeValue || 0,
      activeTrades: trades.filter(t => t.status === 'pending').length,
      recentActivity: trades.filter(t => 
        new Date().getTime() - new Date(t.updatedAt).getTime() < 24 * 60 * 60 * 1000
      ).length
    }))
  );

  // Filter options
  filterOptions = [
    { value: 'all', label: 'All Trades', icon: 'swap_horiz' },
    { value: 'pending', label: 'Pending', icon: 'schedule' },
    { value: 'sent', label: 'Sent', icon: 'call_made' },
    { value: 'received', label: 'call_received', icon: 'call_received' },
    { value: 'completed', label: 'Completed', icon: 'check_circle' }
  ];

  selectedFilter = new BehaviorSubject<string>('all');

  constructor() {
    // Initialize observable streams
    this.trades$ = this.tradingService.trades$;
    this.tradingStats$ = this.tradingService.tradingStats$;
    this.loading$ = this.tradingService.loading$;
    this.error$ = this.tradingService.error$;

    // Setup filtered trades
    this.setupFilteredTrades();
  }

  ngOnInit(): void {
    this.loadInitialData();
    this.setupAutoRefresh();
    this.handleNotifications();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.searchSubject.complete();
    this.selectedTab.complete();
    this.selectedFilter.complete();
    this.isRefreshing.complete();
  }

  private setupFilteredTrades(): void {
    const userId = this.userContextService.getCurrentUser()?.id;

    const filteredTrades$ = combineLatest([
      this.trades$,
      this.searchQuery$.pipe(
        debounceTime(300),
        startWith('')
      ),
      this.selectedFilter.asObservable()
    ]).pipe(
      map(([trades, searchQuery, filter]) => {
        let filtered = trades;

        // Apply search filter
        if (searchQuery.trim()) {
          const query = searchQuery.toLowerCase();
          filtered = filtered.filter(trade => 
            trade.fromUserName.toLowerCase().includes(query) ||
            trade.toUserName.toLowerCase().includes(query) ||
            trade.offeredPlayers.some(p => p.name.toLowerCase().includes(query)) ||
            trade.requestedPlayers.some(p => p.name.toLowerCase().includes(query))
          );
        }

        // Apply status filter
        switch (filter) {
          case 'pending':
            return filtered.filter(t => t.status === 'pending');
          case 'sent':
            return filtered.filter(t => t.fromUserId === userId);
          case 'received':
            return filtered.filter(t => t.toUserId === userId);
          case 'completed':
            return filtered.filter(t => ['accepted', 'rejected'].includes(t.status));
          default:
            return filtered;
        }
      })
    );

    // Setup categorized trades
    this.pendingTrades$ = filteredTrades$.pipe(
      map(trades => trades.filter(t => t.status === 'pending'))
    );

    this.sentTrades$ = filteredTrades$.pipe(
      map(trades => trades.filter(t => t.fromUserId === userId))
    );

    this.receivedTrades$ = filteredTrades$.pipe(
      map(trades => trades.filter(t => t.toUserId === userId))
    );

    this.completedTrades$ = filteredTrades$.pipe(
      map(trades => trades.filter(t => ['accepted', 'rejected'].includes(t.status)))
    );
  }

  private loadInitialData(): void {
    // Load trades and stats
    this.tradingService.getTrades()
      .pipe(takeUntil(this.destroy$))
      .subscribe();

    this.tradingService.getTradingStats()
      .pipe(takeUntil(this.destroy$))
      .subscribe();
  }

  private setupAutoRefresh(): void {
    // Refresh data every 30 seconds
    setInterval(() => {
      this.refreshData(false);
    }, 30000);
  }

  private handleNotifications(): void {
    // Listen for new trades and show notifications
    this.trades$.pipe(
      takeUntil(this.destroy$)
    ).subscribe(trades => {
      const pendingForUser = trades.filter(trade => 
        trade.status === 'pending' && 
        trade.toUserId === this.userContextService.getCurrentUser()?.id
      );

      if (pendingForUser.length > 0) {
        this.showTradeNotification(pendingForUser.length);
      }
    });

    // Listen for errors
    this.error$.pipe(
      takeUntil(this.destroy$)
    ).subscribe(error => {
      if (error) {
        this.notificationService.showError(error);
      }
    });
  }

  // UI Event Handlers
  onSearchChange(query: string): void {
    this.searchSubject.next(query);
  }

  onFilterChange(filter: string): void {
    this.selectedFilter.next(filter);
  }

  onTabChange(index: number): void {
    this.selectedTab.next(index);
  }

  onCreateTrade(): void {
    this.router.navigate(['/trades/create']);
  }

  onViewTradeDetails(trade: TradeOffer): void {
    this.router.navigate(['/trades', trade.id]);
  }

  onAcceptTrade(trade: TradeOffer): void {
    this.tradingService.acceptTradeOffer(trade.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updatedTrade) => {
          this.showSuccessMessage(`Trade accepted successfully!`);
          this.triggerCelebration();
        },
        error: (error) => {
          this.notificationService.showError('Failed to accept trade');
        }
      });
  }

  onRejectTrade(trade: TradeOffer): void {
    this.tradingService.rejectTradeOffer(trade.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updatedTrade) => {
          this.showSuccessMessage(`Trade rejected`);
        },
        error: (error) => {
          this.notificationService.showError('Failed to reject trade');
        }
      });
  }

  onWithdrawTrade(trade: TradeOffer): void {
    this.tradingService.withdrawTradeOffer(trade.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updatedTrade) => {
          this.showSuccessMessage(`Trade withdrawn`);
        },
        error: (error) => {
          this.notificationService.showError('Failed to withdraw trade');
        }
      });
  }

  refreshData(showLoader: boolean = true): void {
    if (showLoader) {
      this.isRefreshing.next(true);
    }

    this.tradingService.refreshData()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.isRefreshing.next(false);
          if (showLoader) {
            this.showSuccessMessage('Data refreshed successfully');
          }
        },
        error: (error) => {
          this.isRefreshing.next(false);
          this.notificationService.showError('Failed to refresh data');
        }
      });
  }

  // Utility Methods
  getTradeStatusIcon(status: string): string {
    switch (status) {
      case 'pending': return 'schedule';
      case 'accepted': return 'check_circle';
      case 'rejected': return 'cancel';
      case 'withdrawn': return 'undo';
      case 'expired': return 'access_time';
      default: return 'help_outline';
    }
  }

  getTradeStatusClass(status: string): string {
    return `status-${status}`;
  }

  getValueDisplayClass(value: number): string {
    if (value > 0) return 'value-positive';
    if (value < 0) return 'value-negative';
    return 'value-neutral';
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(value);
  }

  abs(value: number): number {
    return Math.abs(value ?? 0);
  }

  formatDate(date: Date): string {
    return new Intl.DateTimeFormat('en-US', {
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit'
    }).format(date);
  }

  getTimeSince(date: Date): string {
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / (1000 * 60));
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    return `${diffDays}d ago`;
  }

  isTradeExpiringSoon(trade: TradeOffer): boolean {
    const hoursUntilExpiry = (trade.expiresAt.getTime() - Date.now()) / (1000 * 60 * 60);
    return hoursUntilExpiry < 24 && hoursUntilExpiry > 0;
  }

  canAcceptTrade(trade: TradeOffer): boolean {
    const userId = this.userContextService.getCurrentUser()?.id;
    return trade.status === 'pending' && trade.toUserId === userId;
  }

  canRejectTrade(trade: TradeOffer): boolean {
    const userId = this.userContextService.getCurrentUser()?.id;
    return trade.status === 'pending' && trade.toUserId === userId;
  }

  canWithdrawTrade(trade: TradeOffer): boolean {
    const userId = this.userContextService.getCurrentUser()?.id;
    return trade.status === 'pending' && trade.fromUserId === userId;
  }

  private showTradeNotification(count: number): void {
    this.notificationService.showInfo(
      `You have ${count} new trade offer${count > 1 ? 's' : ''}`,
      'View Trades'
    );
  }

  private showSuccessMessage(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 3000,
      panelClass: ['success-snackbar'],
      horizontalPosition: 'end',
      verticalPosition: 'top'
    });
  }

  private triggerCelebration(): void {
    // Trigger confetti animation for successful trades
    this.createConfetti();
  }

  private createConfetti(): void {
    const colors = ['#ff6b6b', '#4ecdc4', '#45b7d1', '#96ceb4', '#feca57'];
    const confettiContainer = document.createElement('div');
    confettiContainer.className = 'confetti-container';
    document.body.appendChild(confettiContainer);

    for (let i = 0; i < 50; i++) {
      const confetti = document.createElement('div');
      confetti.className = 'confetti';
      confetti.style.backgroundColor = colors[Math.floor(Math.random() * colors.length)];
      confetti.style.left = Math.random() * 100 + 'vw';
      confetti.style.animationDelay = Math.random() * 3 + 's';
      confetti.style.animationDuration = Math.random() * 3 + 2 + 's';
      confettiContainer.appendChild(confetti);
    }

    setTimeout(() => {
      document.body.removeChild(confettiContainer);
    }, 6000);
  }

  // TrackBy function for performance optimization
  trackByTradeId(index: number, trade: TradeOffer): string {
    return trade.id;
  }
}