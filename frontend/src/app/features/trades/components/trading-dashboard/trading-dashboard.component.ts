import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, inject, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { Observable, Subject, BehaviorSubject, combineLatest, interval, of } from 'rxjs';
import { takeUntil, map, startWith, debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { trigger, transition, style, animate, query, stagger } from '@angular/animations';

import { MaterialModule } from '../../../../shared/material/material.module';
import { UiErrorFeedbackService } from '../../../../core/services/ui-error-feedback.service';
import { TradingService, TradeOffer, TradeStats } from '../../services/trading.service';
import { UserContextService } from '../../../../core/services/user-context.service';
import { TranslationService } from '../../../../core/services/translation.service';
import { LoggerService } from '../../../../core/services/logger.service';
import { GameSelectionService } from '../../../../core/services/game-selection.service';
import { secureRandomFloat, secureRandomPick } from '../../../../shared/utils/secure-random.util';
import { TradeDetailsComponent } from '../trade-details/trade-details.component';

const SEARCH_DEBOUNCE_MS = 300;

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
  private readonly route = inject(ActivatedRoute);
  private readonly dialog = inject(MatDialog);
  private readonly uiFeedback = inject(UiErrorFeedbackService);
  private readonly logger = inject(LoggerService);
  private readonly gameSelectionService = inject(GameSelectionService);
  public readonly t = inject(TranslationService);

  // Game context
  private gameId: string | null = null;

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
      activeTrades: trades.filter(t => t.status === 'pending').length,
      recentActivity: trades.filter(t =>
        new Date().getTime() - new Date(t.updatedAt).getTime() < 24 * 60 * 60 * 1000
      ).length
    }))
  );

  // Filter options - dynamically translated
  get filterOptions() {
    return [
      { value: 'all', label: this.t.t('trades.filters.all'), icon: 'swap_horiz' },
      { value: 'pending', label: this.t.t('trades.tabs.pending'), icon: 'schedule' },
      { value: 'sent', label: this.t.t('trades.tabs.sent'), icon: 'call_made' },
      { value: 'received', label: this.t.t('trades.tabs.received'), icon: 'call_received' },
      { value: 'completed', label: this.t.t('trades.filters.completed'), icon: 'check_circle' }
    ];
  }

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
    this.subscribeToGameContext();
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
    const filteredTrades$ = combineLatest([
      this.trades$,
      this.searchQuery$.pipe(
        debounceTime(SEARCH_DEBOUNCE_MS),
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
            return filtered.filter(trade => this.isCurrentUserTradeSender(trade));
          case 'received':
            return filtered.filter(trade => this.isCurrentUserTradeReceiver(trade));
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
      map(trades => trades.filter(trade => this.isCurrentUserTradeSender(trade)))
    );

    this.receivedTrades$ = filteredTrades$.pipe(
      map(trades => trades.filter(trade => this.isCurrentUserTradeReceiver(trade)))
    );

    this.completedTrades$ = filteredTrades$.pipe(
      map(trades => trades.filter(t => ['accepted', 'rejected'].includes(t.status)))
    );
  }

  private loadInitialData(): void {
    if (!this.gameId) {
      this.logger.warn('TradingDashboardComponent: missing gameId, skip initial load');
      return;
    }

    // Load trades and stats with gameId
    this.tradingService.getTrades(this.gameId)
      .pipe(takeUntil(this.destroy$))
      .subscribe();

    this.tradingService.getTradingStats(this.gameId)
      .pipe(takeUntil(this.destroy$))
      .subscribe();
  }

  private setupAutoRefresh(): void {
    interval(30000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.refreshData(false);
      });
  }

  private handleNotifications(): void {
    // Listen for new trades and show notifications
    this.trades$.pipe(
      takeUntil(this.destroy$)
    ).subscribe(trades => {
      const pendingForUser = trades.filter(trade => 
        trade.status === 'pending' && 
        this.isCurrentUserTradeReceiver(trade)
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
        this.uiFeedback.showError({ message: error }, 'trades.errors.loadTrades');
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
    if (this.gameId) {
      this.router.navigate(['/games', this.gameId, 'trades', 'create']);
    }
  }

  onViewTradeDetails(trade: TradeOffer): void {
    this.dialog.open(TradeDetailsComponent, {
      width: '960px',
      maxWidth: '95vw',
      data: {
        trade,
        allowActions: true
      }
    });
  }

  onTradeCardKeydown(event: KeyboardEvent, trade: TradeOffer): void {
    if (event.key !== 'Enter' && event.key !== ' ') {
      return;
    }

    event.preventDefault();
    this.onViewTradeDetails(trade);
  }

  onAcceptTrade(trade: TradeOffer): void {
    this.tradingService.acceptTradeOffer(trade.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.showSuccessMessage(this.t.t('trades.messages.tradeAccepted'));
          this.triggerCelebration();
        },
        error: () => {
          this.uiFeedback.showErrorFromKey('trades.errors.acceptOffer');
        }
      });
  }

  onRejectTrade(trade: TradeOffer): void {
    this.tradingService.rejectTradeOffer(trade.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.showSuccessMessage(this.t.t('trades.messages.tradeRejected'));
        },
        error: () => {
          this.uiFeedback.showErrorFromKey('trades.errors.rejectOffer');
        }
      });
  }

  onWithdrawTrade(trade: TradeOffer): void {
    this.tradingService.withdrawTradeOffer(trade.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.showSuccessMessage(this.t.t('trades.messages.tradeWithdrawn'));
        },
        error: () => {
          this.uiFeedback.showErrorFromKey('trades.errors.withdrawOffer');
        }
      });
  }

  refreshData(showLoader: boolean = true): void {
    if (!this.gameId) return;

    if (showLoader) {
      this.isRefreshing.next(true);
    }

    // Clear cache and reload with gameId
    this.tradingService.clearAllCaches();

    this.tradingService.getTrades(this.gameId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.isRefreshing.next(false);
          if (showLoader) {
            this.showSuccessMessage(this.t.t('trades.messages.refreshed', 'Données actualisées'));
          }
        },
        error: () => {
          this.isRefreshing.next(false);
          this.uiFeedback.showErrorFromKey('trades.errors.refreshFailed');
        }
      });

    this.tradingService.getTradingStats(this.gameId)
      .pipe(takeUntil(this.destroy$))
      .subscribe();
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
    return trade.status === 'pending' && this.isCurrentUserTradeReceiver(trade);
  }

  canRejectTrade(trade: TradeOffer): boolean {
    return trade.status === 'pending' && this.isCurrentUserTradeReceiver(trade);
  }

  canWithdrawTrade(trade: TradeOffer): boolean {
    return trade.status === 'pending' && this.isCurrentUserTradeSender(trade);
  }

  isCurrentUserTradeSender(trade: TradeOffer): boolean {
    return this.isCurrentUserIdentity(trade.fromUserId, trade.fromUserName);
  }

  isCurrentUserTradeReceiver(trade: TradeOffer): boolean {
    return this.isCurrentUserIdentity(trade.toUserId, trade.toUserName);
  }

  getCompletedTradeCounterpartName(trade: TradeOffer): string {
    return this.isCurrentUserTradeSender(trade) ? trade.toUserName : trade.fromUserName;
  }

  private showTradeNotification(count: number): void {
    this.uiFeedback.showInfoMessage(this.buildNewTradeOffersMessage(count), 5000);
  }

  private buildNewTradeOffersMessage(count: number): string {
    const fallbackMessage = `You have ${count} new trade offer${count > 1 ? 's' : ''}`;
    const translatedTemplate = this.t.t('trades.messages.newOffers');
    const template =
      translatedTemplate && translatedTemplate !== 'trades.messages.newOffers'
        ? translatedTemplate
        : fallbackMessage;

    return template.replace('{count}', String(count));
  }

  private showSuccessMessage(message: string): void {
    this.uiFeedback.showSuccessMessage(message, 3000);
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
      confetti.style.backgroundColor = secureRandomPick(colors);
      confetti.style.left = `${secureRandomFloat() * 100}vw`;
      confetti.style.animationDelay = `${secureRandomFloat() * 3}s`;
      confetti.style.animationDuration = `${secureRandomFloat() * 3 + 2}s`;
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

  private subscribeToGameContext(): void {
    const parentParams$ = this.route.parent?.params ?? of({});

    combineLatest([
      parentParams$.pipe(startWith(this.route.parent?.snapshot.params ?? {})),
      this.route.params.pipe(startWith(this.route.snapshot.params ?? {}))
    ])
      .pipe(
        map(([parentParams, routeParams]) => {
          const parentGameId = (parentParams as Record<string, string | undefined>)['id'];
          if (parentGameId) {
            return parentGameId;
          }

          const routeGameId = (routeParams as Record<string, string | undefined>)['gameId'];
          if (routeGameId) {
            return routeGameId;
          }

          return this.gameSelectionService.getSelectedGame()?.id ?? null;
        }),
        distinctUntilChanged(),
        takeUntil(this.destroy$)
      )
      .subscribe(gameId => {
        this.gameId = gameId;
        this.loadInitialData();
      });
  }

  private isCurrentUserIdentity(userId: string, username: string): boolean {
    const currentUser = this.userContextService.getCurrentUser();
    if (!currentUser) {
      return false;
    }

    if (currentUser.id === userId) {
      return true;
    }

    return currentUser.username.trim().toLowerCase() === username.trim().toLowerCase();
  }
}
