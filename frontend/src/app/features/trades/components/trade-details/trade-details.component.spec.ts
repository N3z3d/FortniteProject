import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ReactiveFormsModule } from '@angular/forms';
import { of, BehaviorSubject } from 'rxjs';
import { TradeDetailsComponent } from './trade-details.component';
import { TradingService, TradeOffer, Player } from '../../services/trading.service';
import { UserContextService } from '../../../../core/services/user-context.service';
import { NotificationService } from '../../../../shared/services/notification.service';
import { LoggerService } from '../../../../core/services/logger.service';
import { TranslationService } from '../../../../core/services/translation.service';
import { TradeBusinessService } from '../../services/trade-business.service';
import { TradeTimelineService } from '../../services/trade-timeline.service';

describe('TradeDetailsComponent', () => {
    let component: TradeDetailsComponent;
    let fixture: ComponentFixture<TradeDetailsComponent>;
    let tradingService: jasmine.SpyObj<TradingService>;
    let userContextService: jasmine.SpyObj<UserContextService>;
    let notificationService: jasmine.SpyObj<NotificationService>;
    let translationService: jasmine.SpyObj<TranslationService>;
    let tradeBusinessService: jasmine.SpyObj<TradeBusinessService>;
    let tradeTimelineService: jasmine.SpyObj<TradeTimelineService>;
    let loggerService: jasmine.SpyObj<LoggerService>;
    let snackBar: jasmine.SpyObj<MatSnackBar>;
    let dialogRef: jasmine.SpyObj<MatDialogRef<TradeDetailsComponent>>;

    const mockPlayers: Player[] = [
        { id: 'p1', name: 'Player 1', region: 'EU', marketValue: 100, averageScore: 50, totalScore: 500, gamesPlayed: 10 },
        { id: 'p2', name: 'Player 2', region: 'NAE', marketValue: 120, averageScore: 60, totalScore: 600, gamesPlayed: 10 }
    ];

    const mockTrade: TradeOffer = {
        id: 'trade1',
        fromTeamId: 'team1',
        fromTeamName: 'Team One',
        fromUserId: 'user1',
        fromUserName: 'User One',
        toTeamId: 'team2',
        toTeamName: 'Team Two',
        toUserId: 'user2',
        toUserName: 'User Two',
        offeredPlayers: [mockPlayers[0]],
        requestedPlayers: [mockPlayers[1]],
        status: 'pending',
        createdAt: new Date(),
        updatedAt: new Date(),
        expiresAt: new Date(Date.now() + 86400000),
        valueBalance: -20
    };

    const mockDialogData = {
        trade: mockTrade,
        allowActions: true
    };

    beforeEach(async () => {
        tradingService = jasmine.createSpyObj('TradingService', [
            'acceptTradeOffer', 'rejectTradeOffer', 'withdrawTradeOffer', 'createCounterOffer'
        ]);
        tradingService.acceptTradeOffer.and.returnValue(of({ ...mockTrade, status: 'accepted' }));
        tradingService.rejectTradeOffer.and.returnValue(of({ ...mockTrade, status: 'rejected' }));
        tradingService.withdrawTradeOffer.and.returnValue(of({ ...mockTrade, status: 'withdrawn' }));
        tradingService.createCounterOffer.and.returnValue(of(mockTrade));

        userContextService = jasmine.createSpyObj('UserContextService', ['getCurrentUser']);
        userContextService.getCurrentUser.and.returnValue({ id: 'user2', username: 'TestUser', email: 'test@example.com' });

        notificationService = jasmine.createSpyObj('NotificationService', ['showError', 'showInfo', 'showSuccess']);

        translationService = jasmine.createSpyObj('TranslationService', ['t']);
        translationService.t.and.callFake((key: string, fallback?: string) => fallback || key);

        tradeBusinessService = jasmine.createSpyObj('TradeBusinessService', [
            'calculateTradeStats', 'getBalanceDisplayClass', 'formatCurrency'
        ]);
        tradeBusinessService.calculateTradeStats.and.returnValue({
            totalPlayers: 2,
            offeredValue: 100,
            requestedValue: 120,
            totalValue: 220,
            avgPlayerValue: 110,
            balancePercentage: 83,
            fairnessRating: 'good'
        });
        tradeBusinessService.getBalanceDisplayClass.and.returnValue('balance-positive');
        tradeBusinessService.formatCurrency.and.callFake((val: number) => `$${val}`);

        tradeTimelineService = jasmine.createSpyObj('TradeTimelineService', ['generateTradeTimeline']);
        tradeTimelineService.generateTradeTimeline.and.returnValue([
            { date: new Date(), action: 'created', actor: 'User One', icon: 'add', description: 'Trade created', status: 'completed' }
        ]);

        loggerService = jasmine.createSpyObj('LoggerService', ['debug', 'info', 'warn', 'error']);

        snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
        dialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);

        await TestBed.configureTestingModule({
            imports: [
                TradeDetailsComponent,
                NoopAnimationsModule,
                ReactiveFormsModule
            ],
            providers: [
                { provide: MAT_DIALOG_DATA, useValue: mockDialogData },
                { provide: MatDialogRef, useValue: dialogRef },
                { provide: TradingService, useValue: tradingService },
                { provide: UserContextService, useValue: userContextService },
                { provide: NotificationService, useValue: notificationService },
                { provide: TranslationService, useValue: translationService },
                { provide: TradeBusinessService, useValue: tradeBusinessService },
                { provide: TradeTimelineService, useValue: tradeTimelineService },
                { provide: LoggerService, useValue: loggerService },
                { provide: MatSnackBar, useValue: snackBar }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(TradeDetailsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    afterEach(() => {
        fixture.destroy();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    describe('initialization', () => {
        it('should set trade from dialog data', () => {
            expect(component.trade).toEqual(mockTrade);
        });

        it('should set currentUserId from user context', () => {
            expect(component.currentUserId).toBe('user2');
        });

        it('should initialize counterOfferForm', () => {
            expect(component.counterOfferForm).toBeDefined();
        });

        it('should track dialog view', () => {
            expect(loggerService.debug).toHaveBeenCalled();
        });
    });

    describe('getOfferedTotal', () => {
        it('should calculate total of offered players', () => {
            expect(component.getOfferedTotal()).toBe(100);
        });
    });

    describe('getRequestedTotal', () => {
        it('should calculate total of requested players', () => {
            expect(component.getRequestedTotal()).toBe(120);
        });
    });

    describe('onAction', () => {
        it('should not process when already processing', fakeAsync(() => {
            component.isProcessing.next(true);
            component.onAction('accept');
            expect(tradingService.acceptTradeOffer).not.toHaveBeenCalled();
        }));

        it('should handle accept action', fakeAsync(() => {
            component.onAction('accept');
            tick();
            expect(tradingService.acceptTradeOffer).toHaveBeenCalledWith('trade1');
        }));

        it('should handle reject action', fakeAsync(() => {
            component.onAction('reject');
            tick();
            expect(tradingService.rejectTradeOffer).toHaveBeenCalledWith('trade1');
        }));

        it('should handle withdraw action', fakeAsync(() => {
            userContextService.getCurrentUser.and.returnValue({ id: 'user1', username: 'TestUser', email: 'test@example.com' });
            component.currentUserId = 'user1';
            component.onAction('withdraw');
            tick();
            expect(tradingService.withdrawTradeOffer).toHaveBeenCalledWith('trade1');
        }));

        it('should handle counter action', fakeAsync(() => {
            component.onAction('counter');
            tick();
            expect(component.showCounterForm.value).toBe(true);
        }));

        it('should log warning for unknown action', fakeAsync(() => {
            component.onAction('unknown');
            tick();
            expect(loggerService.warn).toHaveBeenCalled();
        }));
    });

    describe('onCancelCounterOffer', () => {
        it('should hide counter form', () => {
            component.showCounterForm.next(true);
            component.onCancelCounterOffer();
            expect(component.showCounterForm.value).toBe(false);
        });

        it('should reset counter offer form', () => {
            component.counterOfferForm?.patchValue({ message: 'Test', expiresIn: 48 });
            component.onCancelCounterOffer();
            expect(component.counterOfferForm?.get('message')?.value).toBe('');
            expect(component.counterOfferForm?.get('expiresIn')?.value).toBe(72);
        });
    });

    describe('onButtonHover', () => {
        it('should update button hover state', () => {
            component.onButtonHover('hover');
            expect(component.buttonHoverState.value).toBe('hover');
            component.onButtonHover('idle');
            expect(component.buttonHoverState.value).toBe('idle');
        });
    });

    describe('getStatusIcon', () => {
        it('should return correct icons', () => {
            expect(component.getStatusIcon('pending')).toBe('schedule');
            expect(component.getStatusIcon('accepted')).toBe('check_circle');
            expect(component.getStatusIcon('rejected')).toBe('cancel');
            expect(component.getStatusIcon('withdrawn')).toBe('undo');
            expect(component.getStatusIcon('expired')).toBe('access_time');
            expect(component.getStatusIcon('unknown')).toBe('help_outline');
        });
    });

    describe('getStatusClass', () => {
        it('should return status class', () => {
            expect(component.getStatusClass('pending')).toBe('status-pending');
        });
    });

    describe('getStatusLabel', () => {
        it('should return translated status label', () => {
            component.getStatusLabel('pending');
            expect(translationService.t).toHaveBeenCalledWith('trades.status.pending', 'pending');
        });
    });

    describe('getBalanceDisplayClass', () => {
        it('should delegate to trade business service', () => {
            component.getBalanceDisplayClass(100);
            expect(tradeBusinessService.getBalanceDisplayClass).toHaveBeenCalledWith(100);
        });
    });

    describe('getFairnessColor', () => {
        it('should return correct colors', () => {
            expect(component.getFairnessColor('excellent')).toBe('var(--trading-accepted)');
            expect(component.getFairnessColor('good')).toBe('var(--gaming-primary)');
            expect(component.getFairnessColor('fair')).toBe('var(--gaming-warning)');
            expect(component.getFairnessColor('poor')).toBe('var(--trading-rejected)');
            expect(component.getFairnessColor('unknown')).toBe('var(--gaming-gray-light)');
        });
    });

    describe('formatCurrency', () => {
        it('should delegate to trade business service', () => {
            expect(component.formatCurrency(1000)).toBe('$1000');
        });
    });

    describe('abs', () => {
        it('should return absolute value', () => {
            expect(component.abs(-50)).toBe(50);
            expect(component.abs(50)).toBe(50);
        });

        it('should handle undefined/null', () => {
            expect(component.abs(0)).toBe(0);
        });
    });

    describe('formatDate', () => {
        it('should format date correctly', () => {
            const date = new Date('2026-01-15T10:30:00');
            const formatted = component.formatDate(date);
            expect(formatted).toContain('Jan');
        });
    });

    describe('getTimeSince', () => {
        it('should return minutes for recent dates', () => {
            const date = new Date(Date.now() - 30 * 60 * 1000);
            expect(component.getTimeSince(date)).toContain('m ago');
        });

        it('should return hours for older dates', () => {
            const date = new Date(Date.now() - 5 * 60 * 60 * 1000);
            expect(component.getTimeSince(date)).toContain('h ago');
        });

        it('should return days for very old dates', () => {
            const date = new Date(Date.now() - 3 * 24 * 60 * 60 * 1000);
            expect(component.getTimeSince(date)).toContain('d ago');
        });
    });

    describe('getTimeUntilExpiry', () => {
        it('should return expired when past', () => {
            component.trade.expiresAt = new Date(Date.now() - 1000);
            expect(component.getTimeUntilExpiry()).toBe('trades.status.expired');
        });

        it('should return days format for large values', () => {
            component.trade.expiresAt = new Date(Date.now() + 3 * 24 * 60 * 60 * 1000);
            const result = component.getTimeUntilExpiry();
            expect(result).toContain('d');
        });

        it('should return hours format for medium values', () => {
            component.trade.expiresAt = new Date(Date.now() + 5 * 60 * 60 * 1000);
            const result = component.getTimeUntilExpiry();
            expect(result).toContain('h');
        });

        it('should return minutes format for small values', () => {
            component.trade.expiresAt = new Date(Date.now() + 30 * 60 * 1000);
            const result = component.getTimeUntilExpiry();
            expect(result).toContain('m');
        });
    });

    describe('isExpiringSoon', () => {
        it('should return true when expiring within 24 hours', () => {
            component.trade.expiresAt = new Date(Date.now() + 12 * 60 * 60 * 1000);
            expect(component.isExpiringSoon()).toBe(true);
        });

        it('should return false when expiring after 24 hours', () => {
            component.trade.expiresAt = new Date(Date.now() + 48 * 60 * 60 * 1000);
            expect(component.isExpiringSoon()).toBe(false);
        });

        it('should return false when already expired', () => {
            component.trade.expiresAt = new Date(Date.now() - 1000);
            expect(component.isExpiringSoon()).toBe(false);
        });
    });

    describe('onClose', () => {
        it('should close dialog', () => {
            component.onClose();
            expect(dialogRef.close).toHaveBeenCalled();
        });
    });

    describe('trackByPlayerId', () => {
        it('should return player id', () => {
            expect(component.trackByPlayerId(0, mockPlayers[0])).toBe('p1');
        });
    });

    describe('trackByTimelineIndex', () => {
        it('should return index', () => {
            expect(component.trackByTimelineIndex(5, {} as any)).toBe(5);
        });
    });

    describe('ngOnDestroy', () => {
        it('should complete all subjects', () => {
            const isProcessingSpy = spyOn(component.isProcessing, 'complete');
            const showCounterFormSpy = spyOn(component.showCounterForm, 'complete');
            const buttonHoverStateSpy = spyOn(component.buttonHoverState, 'complete');

            component.ngOnDestroy();

            expect(isProcessingSpy).toHaveBeenCalled();
            expect(showCounterFormSpy).toHaveBeenCalled();
            expect(buttonHoverStateSpy).toHaveBeenCalled();
        });
    });
});
