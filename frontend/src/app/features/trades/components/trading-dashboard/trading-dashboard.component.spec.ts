import { ComponentFixture, TestBed, fakeAsync, tick, flush } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, BehaviorSubject, Subject } from 'rxjs';
import { TradingDashboardComponent } from './trading-dashboard.component';
import { TradingService, TradeOffer, TradeStats } from '../../services/trading.service';
import { UserContextService } from '../../../../core/services/user-context.service';
import { NotificationService } from '../../../../shared/services/notification.service';
import { TranslationService } from '../../../../core/services/translation.service';

describe('TradingDashboardComponent', () => {
    let component: TradingDashboardComponent;
    let fixture: ComponentFixture<TradingDashboardComponent>;
    let tradingService: jasmine.SpyObj<TradingService>;
    let userContextService: jasmine.SpyObj<UserContextService>;
    let notificationService: jasmine.SpyObj<NotificationService>;
    let translationService: jasmine.SpyObj<TranslationService>;
    let router: Router;
    let snackBar: jasmine.SpyObj<MatSnackBar>;
    let dialog: jasmine.SpyObj<MatDialog>;

    const mockTrades: TradeOffer[] = [
        {
            id: '1',
            fromTeamId: 'team1',
            fromTeamName: 'Team One',
            fromUserId: 'user1',
            fromUserName: 'User One',
            toTeamId: 'team2',
            toTeamName: 'Team Two',
            toUserId: 'user2',
            toUserName: 'User Two',
            offeredPlayers: [{ id: 'p1', name: 'Player 1', region: 'EU', marketValue: 100, averageScore: 50, totalScore: 500, gamesPlayed: 10 }],
            requestedPlayers: [{ id: 'p2', name: 'Player 2', region: 'NAE', marketValue: 120, averageScore: 60, totalScore: 600, gamesPlayed: 10 }],
            status: 'pending',
            createdAt: new Date(),
            updatedAt: new Date(),
            expiresAt: new Date(Date.now() + 86400000),
            valueBalance: -20
        },
        {
            id: '2',
            fromTeamId: 'team2',
            fromTeamName: 'Team Two',
            fromUserId: 'user2',
            fromUserName: 'User Two',
            toTeamId: 'team1',
            toTeamName: 'Team One',
            toUserId: 'user1',
            toUserName: 'User One',
            offeredPlayers: [{ id: 'p3', name: 'Player 3', region: 'NAW', marketValue: 80, averageScore: 40, totalScore: 400, gamesPlayed: 10 }],
            requestedPlayers: [{ id: 'p4', name: 'Player 4', region: 'EU', marketValue: 90, averageScore: 45, totalScore: 450, gamesPlayed: 10 }],
            status: 'accepted',
            createdAt: new Date(),
            updatedAt: new Date(),
            expiresAt: new Date(Date.now() + 86400000),
            valueBalance: -10
        }
    ];

    const mockStats: TradeStats = {
        totalTrades: 10,
        successfulTrades: 5,
        pendingOffers: 3,
        receivedOffers: 2
    };

    const tradesSubject = new BehaviorSubject<TradeOffer[]>(mockTrades);
    const statsSubject = new BehaviorSubject<TradeStats | null>(mockStats);
    const loadingSubject = new BehaviorSubject<boolean>(false);
    const errorSubject = new BehaviorSubject<string | null>(null);
    const paramsSubject = new BehaviorSubject({ id: 'game123' });

    beforeEach(async () => {
        tradingService = jasmine.createSpyObj('TradingService', [
            'getTrades', 'getTradingStats', 'acceptTradeOffer', 'rejectTradeOffer',
            'withdrawTradeOffer', 'clearAllCaches'
        ], {
            trades$: tradesSubject.asObservable(),
            tradingStats$: statsSubject.asObservable(),
            loading$: loadingSubject.asObservable(),
            error$: errorSubject.asObservable()
        });
        tradingService.getTrades.and.returnValue(of(mockTrades));
        tradingService.getTradingStats.and.returnValue(of(mockStats));
        tradingService.acceptTradeOffer.and.returnValue(of(mockTrades[0]));
        tradingService.rejectTradeOffer.and.returnValue(of(mockTrades[0]));
        tradingService.withdrawTradeOffer.and.returnValue(of(mockTrades[0]));

        userContextService = jasmine.createSpyObj('UserContextService', ['getCurrentUser']);
        userContextService.getCurrentUser.and.returnValue({ id: 'user1', username: 'TestUser', email: 'test@example.com' });

        notificationService = jasmine.createSpyObj('NotificationService', ['showError', 'showInfo', 'showSuccess']);

        translationService = jasmine.createSpyObj('TranslationService', ['t']);
        translationService.t.and.callFake((key: string, fallback?: string) => fallback || key);

        snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
        dialog = jasmine.createSpyObj('MatDialog', ['open']);

        await TestBed.configureTestingModule({
            imports: [
                TradingDashboardComponent,
                NoopAnimationsModule,
                RouterTestingModule.withRoutes([])
            ],
            providers: [
                { provide: TradingService, useValue: tradingService },
                { provide: UserContextService, useValue: userContextService },
                { provide: NotificationService, useValue: notificationService },
                { provide: TranslationService, useValue: translationService },
                { provide: MatSnackBar, useValue: snackBar },
                { provide: MatDialog, useValue: dialog },
                {
                    provide: ActivatedRoute,
                    useValue: {
                        parent: { params: paramsSubject.asObservable() },
                        params: of({})
                    }
                }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(TradingDashboardComponent);
        component = fixture.componentInstance;
        router = TestBed.inject(Router);
        fixture.detectChanges();
    });

    afterEach(() => {
        fixture.destroy();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    describe('initialization', () => {
        it('should initialize observable streams', () => {
            expect(component.trades$).toBeDefined();
            expect(component.tradingStats$).toBeDefined();
            expect(component.loading$).toBeDefined();
            expect(component.error$).toBeDefined();
        });

        it('should have filter options', () => {
            expect(component.filterOptions.length).toBe(5);
        });

        it('should load initial data on init', () => {
            expect(tradingService.getTrades).toHaveBeenCalledWith('game123');
            expect(tradingService.getTradingStats).toHaveBeenCalledWith('game123');
        });
    });

    describe('filterOptions', () => {
        it('should contain all filter options', () => {
            const values = component.filterOptions.map(f => f.value);
            expect(values).toContain('all');
            expect(values).toContain('pending');
            expect(values).toContain('sent');
            expect(values).toContain('received');
            expect(values).toContain('completed');
        });

        it('should have icons for each filter', () => {
            component.filterOptions.forEach(filter => {
                expect(filter.icon).toBeDefined();
            });
        });
    });

    describe('onSearchChange', () => {
        it('should update search subject', fakeAsync(() => {
            component.onSearchChange('test query');
            tick(300);
            component.searchQuery$.subscribe(query => {
                expect(query).toBe('test query');
            });
        }));
    });

    describe('onFilterChange', () => {
        it('should update selected filter', () => {
            component.onFilterChange('pending');
            component.selectedFilter.subscribe(filter => {
                expect(filter).toBe('pending');
            });
        });
    });

    describe('onTabChange', () => {
        it('should update selected tab', () => {
            component.onTabChange(2);
            component.selectedTab.subscribe(tab => {
                expect(tab).toBe(2);
            });
        });
    });

    describe('onCreateTrade', () => {
        it('should navigate to create trade page', () => {
            const navigateSpy = spyOn(router, 'navigate');
            (component as any).gameId = 'game123';
            component.onCreateTrade();
            expect(navigateSpy).toHaveBeenCalledWith(['/games', 'game123', 'trades', 'create']);
        });
    });

    describe('onViewTradeDetails', () => {
        it('should navigate to trade details page', () => {
            const navigateSpy = spyOn(router, 'navigate');
            (component as any).gameId = 'game123';
            component.onViewTradeDetails(mockTrades[0]);
            expect(navigateSpy).toHaveBeenCalledWith(['/games', 'game123', 'trades', '1']);
        });
    });

    describe('onAcceptTrade', () => {
        it('should call trading service to accept trade', () => {
            component.onAcceptTrade(mockTrades[0]);
            expect(tradingService.acceptTradeOffer).toHaveBeenCalledWith('1');
        });
    });

    describe('onRejectTrade', () => {
        it('should call trading service to reject trade', () => {
            component.onRejectTrade(mockTrades[0]);
            expect(tradingService.rejectTradeOffer).toHaveBeenCalledWith('1');
        });
    });

    describe('onWithdrawTrade', () => {
        it('should call trading service to withdraw trade', () => {
            component.onWithdrawTrade(mockTrades[0]);
            expect(tradingService.withdrawTradeOffer).toHaveBeenCalledWith('1');
        });
    });

    describe('refreshData', () => {
        it('should clear caches and reload data', () => {
            (component as any).gameId = 'game123';
            component.refreshData(true);
            expect(tradingService.clearAllCaches).toHaveBeenCalled();
            expect(tradingService.getTrades).toHaveBeenCalledWith('game123');
        });
    });

    describe('utility methods', () => {
        describe('getTradeStatusIcon', () => {
            it('should return correct icons for each status', () => {
                expect(component.getTradeStatusIcon('pending')).toBe('schedule');
                expect(component.getTradeStatusIcon('accepted')).toBe('check_circle');
                expect(component.getTradeStatusIcon('rejected')).toBe('cancel');
                expect(component.getTradeStatusIcon('withdrawn')).toBe('undo');
                expect(component.getTradeStatusIcon('expired')).toBe('access_time');
                expect(component.getTradeStatusIcon('unknown')).toBe('help_outline');
            });
        });

        describe('getTradeStatusClass', () => {
            it('should return correct class format', () => {
                expect(component.getTradeStatusClass('pending')).toBe('status-pending');
                expect(component.getTradeStatusClass('accepted')).toBe('status-accepted');
            });
        });

        describe('formatDate', () => {
            it('should format date correctly', () => {
                const date = new Date('2026-01-15T10:30:00');
                const formatted = component.formatDate(date);
                expect(formatted).toContain('Jan');
                expect(formatted).toContain('15');
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

        describe('isTradeExpiringSoon', () => {
            it('should return true for trades expiring within 24 hours', () => {
                const trade = {
                    ...mockTrades[0],
                    expiresAt: new Date(Date.now() + 12 * 60 * 60 * 1000)
                };
                expect(component.isTradeExpiringSoon(trade)).toBe(true);
            });

            it('should return false for trades expiring after 24 hours', () => {
                const trade = {
                    ...mockTrades[0],
                    expiresAt: new Date(Date.now() + 48 * 60 * 60 * 1000)
                };
                expect(component.isTradeExpiringSoon(trade)).toBe(false);
            });
        });

        describe('canAcceptTrade', () => {
            it('should return true if user is receiver and trade is pending', () => {
                const trade = { ...mockTrades[0], toUserId: 'user1', status: 'pending' };
                expect(component.canAcceptTrade(trade as TradeOffer)).toBe(true);
            });

            it('should return false if user is not receiver', () => {
                const trade = { ...mockTrades[0], toUserId: 'user3', status: 'pending' };
                expect(component.canAcceptTrade(trade as TradeOffer)).toBe(false);
            });
        });

        describe('canWithdrawTrade', () => {
            it('should return true if user is sender and trade is pending', () => {
                const trade = { ...mockTrades[0], fromUserId: 'user1', status: 'pending' };
                expect(component.canWithdrawTrade(trade as TradeOffer)).toBe(true);
            });
        });

        describe('trackByTradeId', () => {
            it('should return trade id', () => {
                expect(component.trackByTradeId(0, mockTrades[0])).toBe('1');
            });
        });
    });

    describe('ngOnDestroy', () => {
        it('should complete subjects on destroy', () => {
            const selectedTabSpy = spyOn(component.selectedTab, 'complete');
            const selectedFilterSpy = spyOn(component.selectedFilter, 'complete');
            const isRefreshingSpy = spyOn(component.isRefreshing, 'complete');

            component.ngOnDestroy();

            expect(selectedTabSpy).toHaveBeenCalled();
            expect(selectedFilterSpy).toHaveBeenCalled();
            expect(isRefreshingSpy).toHaveBeenCalled();
        });
    });
});
