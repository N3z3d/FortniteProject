import { Component, Inject, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable, Subject, BehaviorSubject } from 'rxjs';
import { takeUntil, map } from 'rxjs/operators';
import { trigger, transition, style, animate, keyframes, query, stagger } from '@angular/animations';

import { MaterialModule } from '../../../../shared/material/material.module';
import { TradingService, TradeOffer, Player } from '../../services/trading.service';
import { UserContextService } from '../../../../core/services/user-context.service';
import { NotificationService } from '../../../../shared/services/notification.service';
import { LoggerService } from '../../../../core/services/logger.service';
import { TranslationService } from '../../../../core/services/translation.service';

interface TradeDetailsData {
  trade: TradeOffer;
  allowActions?: boolean;
}

interface TradeAction {
  type: 'accept' | 'reject' | 'withdraw' | 'counter';
  label: string;
  icon: string;
  color: 'primary' | 'warn' | 'accent';
  enabled: boolean;
}

interface TradeTimeline {
  date: Date;
  action: string;
  actor: string;
  icon: string;
  description: string;
  status: 'completed' | 'current' | 'pending';
}

@Component({
  selector: 'app-trade-details',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MaterialModule],
  templateUrl: './trade-details.component.html',
  styleUrls: [
    './trade-details.component.scss',
    '../../styles/trading-theme.scss',
    '../../styles/trading-animations.scss'
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  animations: [
    trigger('slideInFromBottom', [
      transition(':enter', [
        style({ transform: 'translateY(100%)', opacity: 0 }),
        animate('0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94)', 
          style({ transform: 'translateY(0)', opacity: 1 })
        )
      ]),
      transition(':leave', [
        animate('0.3s ease-in', 
          style({ transform: 'translateY(100%)', opacity: 0 })
        )
      ])
    ]),
    trigger('playerStagger', [
      transition('* => *', [
        query(':enter', [
          style({ opacity: 0, transform: 'translateX(-30px)' }),
          stagger(100, [
            animate('0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94)', 
              style({ opacity: 1, transform: 'translateX(0)' })
            )
          ])
        ], { optional: true })
      ])
    ]),
    trigger('actionButtonHover', [
      transition('idle => hover', [
        animate('0.3s cubic-bezier(0.68, -0.55, 0.265, 1.55)', 
          style({ transform: 'scale(1.05) translateY(-2px)' })
        )
      ]),
      transition('hover => idle', [
        animate('0.2s ease-out', 
          style({ transform: 'scale(1) translateY(0)' })
        )
      ])
    ]),
    trigger('tradeStatusChange', [
      transition('pending => accepted', [
        animate('1s cubic-bezier(0.68, -0.55, 0.265, 1.55)', keyframes([
          style({ transform: 'scale(1)', background: 'var(--card-pending)', offset: 0 }),
          style({ transform: 'scale(1.02)', background: 'var(--card-receive)', offset: 0.5 }),
          style({ transform: 'scale(1)', background: 'var(--card-receive)', offset: 1 })
        ]))
      ]),
      transition('pending => rejected', [
        animate('0.8s ease-in-out', keyframes([
          style({ transform: 'scale(1) rotateZ(0deg)', offset: 0 }),
          style({ transform: 'scale(0.95) rotateZ(-2deg)', offset: 0.3 }),
          style({ transform: 'scale(1.02) rotateZ(1deg)', offset: 0.7 }),
          style({ transform: 'scale(1) rotateZ(0deg)', offset: 1 })
        ]))
      ])
    ]),
    trigger('timelineProgress', [
      transition(':enter', [
        query('.timeline-connector', [
          style({ height: '0%' }),
          animate('0.8s ease-out', style({ height: '100%' }))
        ], { optional: true }),
        query('.timeline-item', [
          style({ opacity: 0, transform: 'scale(0.8)' }),
          stagger(200, [
            animate('0.5s cubic-bezier(0.68, -0.55, 0.265, 1.55)', 
              style({ opacity: 1, transform: 'scale(1)' })
            )
          ])
        ], { optional: true })
      ])
    ])
  ]
})
export class TradeDetailsComponent implements OnInit, OnDestroy {
  public readonly t = inject(TranslationService);
  private readonly destroy$ = new Subject<void>();

  // Component state
  trade: TradeOffer;
  currentUserId: string;
  isProcessing = new BehaviorSubject<boolean>(false);
  counterOfferForm?: FormGroup;
  showCounterForm = new BehaviorSubject<boolean>(false);
  
  // Observables
  availableActions$!: Observable<TradeAction[]>;
  tradeTimeline$!: Observable<TradeTimeline[]>;
  tradeStats$!: Observable<{
    totalPlayers: number;
    totalValue: number;
    avgPlayerValue: number;
    balancePercentage: number;
    fairnessRating: 'excellent' | 'good' | 'fair' | 'poor';
  }>;

  // Animation states
  buttonHoverState = new BehaviorSubject<'idle' | 'hover'>('idle');

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: TradeDetailsData,
    private dialogRef: MatDialogRef<TradeDetailsComponent>,
    private tradingService: TradingService,
    public userContextService: UserContextService,
    private snackBar: MatSnackBar,
    private notificationService: NotificationService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef,
    private logger: LoggerService
  ) {
    this.trade = data.trade;
    this.currentUserId = this.userContextService.getCurrentUser()?.id || '';
    
    this.setupObservables();
    this.setupCounterOfferForm();
  }

  ngOnInit(): void {
    // Mark dialog as opened for analytics
    this.trackDialogView();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.isProcessing.complete();
    this.showCounterForm.complete();
    this.buttonHoverState.complete();
  }

  private setupObservables(): void {
    this.availableActions$ = this.isProcessing.asObservable().pipe(
      map(processing => this.getAvailableActions(processing))
    );

    this.tradeTimeline$ = new BehaviorSubject(this.generateTradeTimeline()).asObservable();
    
    this.tradeStats$ = new BehaviorSubject(this.calculateTradeStats()).asObservable();
  }

  private setupCounterOfferForm(): void {
    this.counterOfferForm = this.fb.group({
      message: ['', [Validators.maxLength(500)]],
      expiresIn: [72, [Validators.required, Validators.min(1), Validators.max(168)]]
    });
  }

  private getAvailableActions(processing: boolean): TradeAction[] {
    const actions: TradeAction[] = [];
    
    if (this.trade.status !== 'pending' || processing) {
      return actions;
    }

    const isReceiver = this.trade.toUserId === this.currentUserId;
    const isSender = this.trade.fromUserId === this.currentUserId;

    if (isReceiver) {
      actions.push({
        type: 'accept',
        label: this.t.t('trades.details.acceptTrade'),
        icon: 'check_circle',
        color: 'primary',
        enabled: !processing
      });

      actions.push({
        type: 'reject',
        label: this.t.t('trades.details.rejectTrade'),
        icon: 'cancel',
        color: 'warn',
        enabled: !processing
      });

      actions.push({
        type: 'counter',
        label: this.t.t('trades.details.counterOffer'),
        icon: 'reply',
        color: 'accent',
        enabled: !processing
      });
    }

    if (isSender) {
      actions.push({
        type: 'withdraw',
        label: this.t.t('trades.details.withdrawOffer'),
        icon: 'undo',
        color: 'warn',
        enabled: !processing
      });
    }

    return actions;
  }

  private generateTradeTimeline(): TradeTimeline[] {
    const timeline: TradeTimeline[] = [this.buildProposedTimelineEntry()];

    if (this.trade.status === 'pending') {
      timeline.push(...this.buildPendingTimelineEntries());
    } else {
      timeline.push(this.buildStatusTimelineEntry());
    }

    return timeline.sort((a, b) => a.date.getTime() - b.date.getTime());
  }

  private buildProposedTimelineEntry(): TradeTimeline {
    return {
      date: this.trade.createdAt,
      action: this.t.t('trades.details.timelineProposedAction'),
      actor: this.trade.fromUserName,
      icon: 'send',
      description: this.t.t('trades.details.timelineProposedDesc'),
      status: 'completed'
    };
  }

  private buildPendingTimelineEntries(): TradeTimeline[] {
    return [
      {
        date: new Date(),
        action: this.t.t('trades.details.timelineAwaitingAction'),
        actor: this.trade.toUserName,
        icon: 'schedule',
        description: this.t.t('trades.details.timelineAwaitingDesc'),
        status: 'current'
      },
      {
        date: this.trade.expiresAt,
        action: this.t.t('trades.details.timelineExpiresAction'),
        actor: this.t.t('trades.details.system'),
        icon: 'access_time',
        description: this.t.t('trades.details.timelineExpiresDesc'),
        status: 'pending'
      }
    ];
  }

  private buildStatusTimelineEntry(): TradeTimeline {
    const status = this.trade.status;

    return {
      date: this.trade.updatedAt,
      action: this.getStatusLabel(status),
      actor: this.getStatusTimelineActor(status),
      icon: this.getStatusTimelineIcon(status),
      description: this.getStatusTimelineDescription(status),
      status: 'completed'
    };
  }

  private getStatusTimelineActor(status: string): string {
    if (status === 'withdrawn') return this.trade.fromUserName;
    if (status === 'expired') return this.t.t('trades.details.system');
    return this.trade.toUserName;
  }

  private getStatusTimelineIcon(status: string): string {
    switch (status) {
      case 'accepted': return 'check_circle';
      case 'rejected': return 'cancel';
      case 'withdrawn': return 'undo';
      case 'expired': return 'access_time';
      default: return 'help_outline';
    }
  }

  private getStatusTimelineDescription(status: string): string {
    switch (status) {
      case 'accepted': return this.t.t('trades.details.timelineAcceptedDesc');
      case 'rejected': return this.t.t('trades.details.timelineRejectedDesc');
      case 'withdrawn': return this.t.t('trades.details.timelineWithdrawnDesc');
      case 'expired': return this.t.t('trades.details.timelineExpiredDesc');
      default: return this.t.t('trades.details.timelineStatusUpdatedDesc');
    }
  }

  private calculateTradeStats() {
    const offeredPlayers = this.trade.offeredPlayers;
    const requestedPlayers = this.trade.requestedPlayers;
    const totalPlayers = offeredPlayers.length + requestedPlayers.length;
    
    const offeredValue = offeredPlayers.reduce((sum, p) => sum + p.marketValue, 0);
    const requestedValue = requestedPlayers.reduce((sum, p) => sum + p.marketValue, 0);
    const totalValue = offeredValue + requestedValue;
    const avgPlayerValue = totalPlayers > 0 ? totalValue / totalPlayers : 0;
    
    const balancePercentage = totalValue > 0 ? Math.abs(offeredValue - requestedValue) / totalValue * 100 : 0;
    
    let fairnessRating: 'excellent' | 'good' | 'fair' | 'poor';
    if (balancePercentage <= 5) fairnessRating = 'excellent';
    else if (balancePercentage <= 15) fairnessRating = 'good';
    else if (balancePercentage <= 25) fairnessRating = 'fair';
    else fairnessRating = 'poor';

    return {
      totalPlayers,
      totalValue,
      avgPlayerValue,
      balancePercentage,
      fairnessRating
    };
  }

  // Template helper totals (avoid complex reducers inline in template)
  getOfferedTotal(): number {
    return this.trade.offeredPlayers.reduce((sum, p) => sum + (p.marketValue || 0), 0);
  }

  getRequestedTotal(): number {
    return this.trade.requestedPlayers.reduce((sum, p) => sum + (p.marketValue || 0), 0);
  }

  // Action handlers
  async onAction(actionType: string): Promise<void> {
    if (this.isProcessing.value) return;

    this.isProcessing.next(true);
    
    try {
      switch (actionType) {
        case 'accept':
          await this.acceptTrade();
          break;
        case 'reject':
          await this.rejectTrade();
          break;
        case 'withdraw':
          await this.withdrawTrade();
          break;
        case 'counter':
          this.showCounterOfferForm();
          break;
      default:
        this.logger.warn('TradeDetails: unknown action type', { actionType });
      }
    } catch (error) {
      this.logger.error('TradeDetails: action failed', error);
      this.showErrorMessage(this.t.t('trades.errors.actionFailed'));
    } finally {
      this.isProcessing.next(false);
    }
  }

  private async acceptTrade(): Promise<void> {
    const updatedTrade = await this.tradingService.acceptTradeOffer(this.trade.id).toPromise();
    this.trade = updatedTrade!;
    this.showSuccessMessage(this.t.t('trades.messages.tradeAccepted'));
    this.triggerConfetti();
    this.closeDialogWithResult('accepted');
  }

  private async rejectTrade(): Promise<void> {
    const updatedTrade = await this.tradingService.rejectTradeOffer(this.trade.id).toPromise();
    this.trade = updatedTrade!;
    this.showSuccessMessage(this.t.t('trades.messages.tradeRejected'));
    this.closeDialogWithResult('rejected');
  }

  private async withdrawTrade(): Promise<void> {
    const updatedTrade = await this.tradingService.withdrawTradeOffer(this.trade.id).toPromise();
    this.trade = updatedTrade!;
    this.showSuccessMessage(this.t.t('trades.messages.tradeWithdrawn'));
    this.closeDialogWithResult('withdrawn');
  }

  private showCounterOfferForm(): void {
    this.showCounterForm.next(true);
    this.isProcessing.next(false);
  }

  async onSubmitCounterOffer(): Promise<void> {
    if (!this.counterOfferForm?.valid) return;

    this.isProcessing.next(true);
    
    try {
      const formValue = this.counterOfferForm.value;
      const expiresAt = new Date();
      expiresAt.setHours(expiresAt.getHours() + formValue.expiresIn);

      const counterOffer = {
        // Reverse the offer - what they offered becomes what we request
        offeredPlayers: this.trade.requestedPlayers,
        requestedPlayers: this.trade.offeredPlayers,
        message: formValue.message || undefined,
        expiresAt
      };

      await this.tradingService.createCounterOffer(this.trade.id, counterOffer).toPromise();
      this.showSuccessMessage(this.t.t('trades.messages.counterOfferSent'));
      this.closeDialogWithResult('counter');
    } catch (error) {
      this.showErrorMessage(this.t.t('trades.errors.counterOffer'));
    } finally {
      this.isProcessing.next(false);
    }
  }

  onCancelCounterOffer(): void {
    this.showCounterForm.next(false);
    this.counterOfferForm?.reset({
      message: '',
      expiresIn: 72
    });
  }

  // UI helpers
  onButtonHover(state: 'idle' | 'hover'): void {
    this.buttonHoverState.next(state);
  }

  getStatusIcon(status: string): string {
    switch (status) {
      case 'pending': return 'schedule';
      case 'accepted': return 'check_circle';
      case 'rejected': return 'cancel';
      case 'withdrawn': return 'undo';
      case 'expired': return 'access_time';
      default: return 'help_outline';
    }
  }

  getStatusClass(status: string): string {
    return `status-${status}`;
  }

  getStatusLabel(status: string): string {
    const key = status?.toLowerCase() || '';
    return this.t.t(`trades.status.${key}`, status);
  }

  getBalanceDisplayClass(balance: number): string {
    if (balance > 0) return 'positive';
    if (balance < 0) return 'negative';
    return 'neutral';
  }

  getFairnessColor(rating: string): string {
    switch (rating) {
      case 'excellent': return 'var(--trading-accepted)';
      case 'good': return 'var(--gaming-primary)';
      case 'fair': return 'var(--gaming-warning)';
      case 'poor': return 'var(--trading-rejected)';
      default: return 'var(--gaming-gray-light)';
    }
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
    return Math.abs(value ? 0);
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

  getTimeUntilExpiry(): string {
    const now = new Date();
    const diffMs = this.trade.expiresAt.getTime() - now.getTime();
    
    if (diffMs <= 0) return this.t.t('trades.status.expired');
    
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffMins = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));
    
    if (diffHours >= 24) {
      const days = Math.floor(diffHours / 24);
      return `${days}d ${diffHours % 24}h`;
    }
    
    if (diffHours > 0) {
      return `${diffHours}h ${diffMins}m`;
    }
    
    return `${diffMins}m`;
  }

  isExpiringSoon(): boolean {
    const hoursUntilExpiry = (this.trade.expiresAt.getTime() - Date.now()) / (1000 * 60 * 60);
    return hoursUntilExpiry < 24 && hoursUntilExpiry > 0;
  }

  // Dialog management
  onClose(): void {
    this.dialogRef.close();
  }

  private closeDialogWithResult(result: string): void {
    setTimeout(() => {
      this.dialogRef.close({ result, trade: this.trade });
    }, 2000);
  }

  // Utility methods
  private showSuccessMessage(message: string): void {
    this.snackBar.open(message, this.t.t('common.close'), {
      duration: 4000,
      panelClass: ['success-snackbar'],
      horizontalPosition: 'end',
      verticalPosition: 'top'
    });
  }

  private showErrorMessage(message: string): void {
    this.snackBar.open(message, this.t.t('common.close'), {
      duration: 4000,
      panelClass: ['error-snackbar'],
      horizontalPosition: 'end',
      verticalPosition: 'top'
    });
  }

  private triggerConfetti(): void {
    // Create confetti effect for successful trade acceptance
    const colors = ['#00ff7f', '#4ecdc4', '#45b7d1', '#96ceb4', '#feca57'];
    const confettiContainer = document.createElement('div');
    confettiContainer.className = 'confetti-container';
    document.body.appendChild(confettiContainer);

    for (let i = 0; i < 30; i++) {
      const confetti = document.createElement('div');
      confetti.className = 'confetti celebration-burst';
      confetti.style.backgroundColor = colors[Math.floor(Math.random() * colors.length)];
      confetti.style.left = Math.random() * 100 + 'vw';
      confetti.style.top = '50vh';
      confetti.style.animationDelay = Math.random() * 0.5 + 's';
      confettiContainer.appendChild(confetti);
    }

    setTimeout(() => {
      document.body.removeChild(confettiContainer);
    }, 3000);
  }

  private trackDialogView(): void {
    // Analytics tracking would go here
    this.logger.debug('TradeDetails: viewed', { tradeId: this.trade.id });
  }

  trackByPlayerId(index: number, player: Player): string {
    return player.id;
  }

  trackByTimelineIndex(index: number, item: TradeTimeline): number {
    return index;
  }
}
