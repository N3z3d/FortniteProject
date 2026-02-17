import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { BehaviorSubject, of, throwError } from 'rxjs';

import { TradeProposalComponent } from './trade-proposal.component';
import { TradingService, Player, Team } from '../../services/trading.service';
import { TradeBusinessService } from '../../services/trade-business.service';
import { UserContextService } from '../../../../core/services/user-context.service';
import { TranslationService } from '../../../../core/services/translation.service';
import { UiErrorFeedbackService } from '../../../../core/services/ui-error-feedback.service';
import { LoggerService } from '../../../../core/services/logger.service';

describe('TradeProposalComponent', () => {
  let fixture: ComponentFixture<TradeProposalComponent>;
  let component: TradeProposalComponent;
  let tradingServiceSpy: jasmine.SpyObj<TradingService>;
  let tradeBusinessSpy: jasmine.SpyObj<TradeBusinessService>;
  let uiFeedbackSpy: jasmine.SpyObj<UiErrorFeedbackService>;
  let loggerSpy: jasmine.SpyObj<LoggerService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let translationService: TranslationService;
  let loadingSubject: BehaviorSubject<boolean>;
  let errorSubject: BehaviorSubject<string | null>;

  const mockPlayer = (id: string, name: string, value = 1000): Player => ({
    id, name, region: 'EU', averageScore: 50, totalScore: 500,
    gamesPlayed: 10, marketValue: value
  });

  const mockTeam = (id: string, ownerId: string, players: Player[] = []): Team => ({
    id, name: `Team ${id}`, ownerId, ownerName: `Owner ${ownerId}`,
    players, totalValue: 5000, currentScore: 100, gameId: 'game-1'
  });

  beforeEach(async () => {
    localStorage.removeItem('app_language');

    loadingSubject = new BehaviorSubject<boolean>(false);
    errorSubject = new BehaviorSubject<string | null>(null);

    tradingServiceSpy = jasmine.createSpyObj<TradingService>(
      'TradingService',
      ['getTeams', 'calculateTradeBalance', 'isTradeBalanced', 'createTradeOffer'],
      {
        teams$: of([]),
        loading$: loadingSubject.asObservable(),
        error$: errorSubject.asObservable()
      }
    );

    tradingServiceSpy.getTeams.and.returnValue(of([]));
    tradingServiceSpy.calculateTradeBalance.and.returnValue(0);
    tradingServiceSpy.isTradeBalanced.and.returnValue(true);
    tradingServiceSpy.createTradeOffer.and.returnValue(of({} as any));

    tradeBusinessSpy = jasmine.createSpyObj<TradeBusinessService>(
      'TradeBusinessService',
      [
        'validateTradeProposal', 'calculateBalancePercentage', 'calculateTradeBalance',
        'isTradeBalanced', 'canMovePlayer', 'formatCurrency', 'getBalanceDisplayClass',
        'getBalanceIcon'
      ]
    );
    tradeBusinessSpy.validateTradeProposal.and.returnValue(false);
    tradeBusinessSpy.calculateBalancePercentage.and.returnValue(50);
    tradeBusinessSpy.calculateTradeBalance.and.returnValue(0);
    tradeBusinessSpy.isTradeBalanced.and.returnValue(true);
    tradeBusinessSpy.canMovePlayer.and.returnValue(true);
    tradeBusinessSpy.formatCurrency.and.callFake((v: number) => `$${v}`);
    tradeBusinessSpy.getBalanceDisplayClass.and.returnValue('balanced');
    tradeBusinessSpy.getBalanceIcon.and.returnValue('balance');

    const userContextSpy = jasmine.createSpyObj<UserContextService>('UserContextService', ['getCurrentUser']);
    userContextSpy.getCurrentUser.and.returnValue({ id: 'user-1', username: 'testuser', email: 'test@test.com' });

    uiFeedbackSpy = jasmine.createSpyObj<UiErrorFeedbackService>('UiErrorFeedbackService', ['showSuccessMessage', 'showError']);
    loggerSpy = jasmine.createSpyObj<LoggerService>('LoggerService', ['debug', 'info', 'warn', 'error']);
    const dialogSpy = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [TradeProposalComponent, NoopAnimationsModule],
      providers: [
        { provide: TradingService, useValue: tradingServiceSpy },
        { provide: TradeBusinessService, useValue: tradeBusinessSpy },
        { provide: UserContextService, useValue: userContextSpy },
        { provide: UiErrorFeedbackService, useValue: uiFeedbackSpy },
        { provide: LoggerService, useValue: loggerSpy },
        { provide: MatDialog, useValue: dialogSpy },
        { provide: Router, useValue: routerSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            parent: { params: of({ id: 'game-1' }) },
            snapshot: { queryParamMap: { get: () => null } }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TradeProposalComponent);
    component = fixture.componentInstance;
    translationService = TestBed.inject(TranslationService);
  });

  describe('i18n', () => {
    it('affiche le titre en francais par defaut', () => {
      fixture.detectChanges();

      const title = fixture.nativeElement.querySelector('.proposal-title') as HTMLElement;
      expect(title?.textContent).toContain(translationService.t('trades.proposal.title'));
    });

    it('affiche le titre en anglais quand la langue est en', () => {
      translationService.setLanguage('en');
      fixture.detectChanges();

      const title = fixture.nativeElement.querySelector('.proposal-title') as HTMLElement;
      expect(title?.textContent).toContain(translationService.t('trades.proposal.title'));
    });

    it('affiche le titre d\'erreur quand une erreur est presente', () => {
      errorSubject.next('Erreur');
      fixture.detectChanges();

      const errorTitle = fixture.nativeElement.querySelector('.error-container h3') as HTMLElement;
      expect(errorTitle?.textContent).toContain(translationService.t('trades.proposal.errorTitle'));
    });

    it('affiche le message de chargement quand loading$ est true', () => {
      loadingSubject.next(true);
      fixture.detectChanges();

      const loadingText = fixture.nativeElement.querySelector('.loading-container p') as HTMLElement;
      expect(loadingText?.textContent).toContain(translationService.t('trades.proposal.loading.teamsAndPlayers'));
    });
  });

  describe('initialization', () => {
    it('creates the component', () => {
      fixture.detectChanges();
      expect(component).toBeTruthy();
    });

    it('initializes the trade form with targetTeam control', () => {
      expect(component.tradeForm).toBeTruthy();
      expect(component.tradeForm.get('targetTeam')).toBeTruthy();
    });

    it('loads teams on init when gameId is present', () => {
      fixture.detectChanges();
      expect(tradingServiceSpy.getTeams).toHaveBeenCalledWith('game-1');
    });

    it('logs warning and skips team loading when gameId is missing', () => {
      fixture.detectChanges();
      tradingServiceSpy.getTeams.calls.reset();

      (component as any).gameId = null;
      (component as any).loadInitialData();

      expect(loggerSpy.warn).toHaveBeenCalledWith('TradeProposalComponent: missing gameId, cannot load teams');
      expect(tradingServiceSpy.getTeams).not.toHaveBeenCalled();
    });

    it('emits initial trade state', (done) => {
      component.tradeState$.subscribe(state => {
        expect(state.offeredPlayers).toEqual([]);
        expect(state.requestedPlayers).toEqual([]);
        expect(state.selectedTeam).toBeNull();
        done();
      });
    });
  });

  describe('player actions', () => {
    const playerA = mockPlayer('p1', 'Player A', 500);
    const playerB = mockPlayer('p2', 'Player B', 800);
    const targetPlayerC = mockPlayer('p3', 'Target C', 600);

    it('addToOffered moves player from available to offered', (done) => {
      fixture.detectChanges();

      // Set initial state with available players
      (component as any).updateTradeState({
        availablePlayers: [playerA, playerB],
        offeredPlayers: []
      });

      component.addToOffered(playerA);

      component.tradeState$.subscribe(state => {
        expect(state.offeredPlayers).toContain(playerA);
        expect(state.availablePlayers).not.toContain(playerA);
        done();
      });
    });

    it('removeFromOffered moves player back to available', (done) => {
      fixture.detectChanges();

      (component as any).updateTradeState({
        availablePlayers: [],
        offeredPlayers: [playerA]
      });

      component.removeFromOffered(playerA);

      component.tradeState$.subscribe(state => {
        expect(state.offeredPlayers).not.toContain(playerA);
        expect(state.availablePlayers).toContain(playerA);
        done();
      });
    });

    it('addToRequested moves player from target to requested', (done) => {
      fixture.detectChanges();

      (component as any).updateTradeState({
        targetTeamPlayers: [targetPlayerC],
        requestedPlayers: []
      });

      component.addToRequested(targetPlayerC);

      component.tradeState$.subscribe(state => {
        expect(state.requestedPlayers).toContain(targetPlayerC);
        expect(state.targetTeamPlayers).not.toContain(targetPlayerC);
        done();
      });
    });

    it('removeFromRequested moves player back to target team', (done) => {
      fixture.detectChanges();

      (component as any).updateTradeState({
        targetTeamPlayers: [],
        requestedPlayers: [targetPlayerC]
      });

      component.removeFromRequested(targetPlayerC);

      component.tradeState$.subscribe(state => {
        expect(state.requestedPlayers).not.toContain(targetPlayerC);
        expect(state.targetTeamPlayers).toContain(targetPlayerC);
        done();
      });
    });

    it('does nothing when player not found in available', () => {
      fixture.detectChanges();
      const unknownPlayer = mockPlayer('unknown', 'Unknown');

      (component as any).updateTradeState({
        availablePlayers: [playerA],
        offeredPlayers: []
      });

      component.addToOffered(unknownPlayer);

      expect(component['tradeStateSubject'].value.offeredPlayers.length).toBe(0);
    });
  });

  describe('drag and drop', () => {
    it('onDragStarted sets drag state to dragging', (done) => {
      component.onDragStarted();

      component.dragState.subscribe(state => {
        expect(state).toBe('dragging');
        done();
      });
    });

    it('onDragEnded sets drag state to idle', (done) => {
      component.onDragStarted();
      component.onDragEnded();

      component.dragState.subscribe(state => {
        expect(state).toBe('idle');
        done();
      });
    });
  });

  describe('search', () => {
    it('onSearchChange updates search subject', (done) => {
      component.searchQuery$.subscribe(query => {
        if (query === 'test') {
          expect(query).toBe('test');
          done();
        }
      });

      component.onSearchChange('test');
    });
  });

  describe('template helpers', () => {
    it('formatCurrency delegates to business service', () => {
      expect(component.formatCurrency(1500)).toBe('$1500');
      expect(tradeBusinessSpy.formatCurrency).toHaveBeenCalledWith(1500);
    });

    it('getBalanceDisplayClass delegates to business service', () => {
      expect(component.getBalanceDisplayClass(100)).toBe('balanced');
      expect(tradeBusinessSpy.getBalanceDisplayClass).toHaveBeenCalledWith(100);
    });

    it('getBalanceIcon delegates to business service', () => {
      expect(component.getBalanceIcon(-50)).toBe('balance');
      expect(tradeBusinessSpy.getBalanceIcon).toHaveBeenCalledWith(-50);
    });

    it('trackByPlayerId returns player id', () => {
      const player = mockPlayer('p1', 'Player');
      expect(component.trackByPlayerId(0, player)).toBe('p1');
    });
  });

  describe('navigation', () => {
    it('onCancel navigates to trades page', () => {
      component.onCancel();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/trades']);
    });
  });

  describe('onClearTrade', () => {
    it('moves all offered and requested players back', (done) => {
      fixture.detectChanges();

      const offered = mockPlayer('p1', 'Offered');
      const requested = mockPlayer('p2', 'Requested');

      (component as any).updateTradeState({
        offeredPlayers: [offered],
        requestedPlayers: [requested],
        availablePlayers: [],
        targetTeamPlayers: []
      });

      component.onClearTrade();

      component.tradeState$.subscribe(state => {
        expect(state.offeredPlayers).toEqual([]);
        expect(state.requestedPlayers).toEqual([]);
        expect(state.availablePlayers).toContain(offered);
        expect(state.targetTeamPlayers).toContain(requested);
        done();
      });
    });
  });

  describe('onSubmitTrade', () => {
    it('shows error when form is invalid', async () => {
      fixture.detectChanges();
      component.tradeForm.get('targetTeam')?.setValue('');

      await component.onSubmitTrade();

      expect(tradingServiceSpy.createTradeOffer).not.toHaveBeenCalled();
    });

    it('shows error when trade state is invalid', async () => {
      fixture.detectChanges();
      component.tradeForm.get('targetTeam')?.setValue('team-1');
      tradeBusinessSpy.validateTradeProposal.and.returnValue(false);

      await component.onSubmitTrade();

      expect(uiFeedbackSpy.showError).toHaveBeenCalled();
    });

    it('submits trade offer when valid', async () => {
      fixture.detectChanges();

      const team = mockTeam('team-2', 'user-2');
      const offered = mockPlayer('p1', 'Offered');
      const requested = mockPlayer('p2', 'Requested');

      component.tradeForm.get('targetTeam')?.setValue('team-2');
      tradeBusinessSpy.validateTradeProposal.and.returnValue(true);

      (component as any).updateTradeState({
        selectedTeam: team,
        offeredPlayers: [offered],
        requestedPlayers: [requested],
        isValid: true
      });

      await component.onSubmitTrade();

      expect(tradingServiceSpy.createTradeOffer).toHaveBeenCalled();
      expect(uiFeedbackSpy.showSuccessMessage).toHaveBeenCalled();
    });

    it('shows error on submission failure', async () => {
      fixture.detectChanges();

      const team = mockTeam('team-2', 'user-2');
      component.tradeForm.get('targetTeam')?.setValue('team-2');
      tradeBusinessSpy.validateTradeProposal.and.returnValue(true);
      tradingServiceSpy.createTradeOffer.and.returnValue(throwError(() => new Error('fail')));

      (component as any).updateTradeState({
        selectedTeam: team,
        offeredPlayers: [mockPlayer('p1', 'P')],
        requestedPlayers: [mockPlayer('p2', 'P')],
        isValid: true
      });

      await component.onSubmitTrade();

      expect(uiFeedbackSpy.showError).toHaveBeenCalled();
    });

    it('resets isSubmitting after completion', async () => {
      fixture.detectChanges();

      const team = mockTeam('team-2', 'user-2');
      component.tradeForm.get('targetTeam')?.setValue('team-2');
      tradeBusinessSpy.validateTradeProposal.and.returnValue(true);

      (component as any).updateTradeState({
        selectedTeam: team,
        offeredPlayers: [mockPlayer('p1', 'P')],
        requestedPlayers: [mockPlayer('p2', 'P')],
        isValid: true
      });

      await component.onSubmitTrade();

      expect(component.isSubmitting.value).toBeFalse();
    });
  });

  describe('cleanup', () => {
    it('completes subjects on destroy', () => {
      fixture.detectChanges();
      component.ngOnDestroy();

      expect(component.isSubmitting.isStopped).toBeTrue();
      expect(component.dragState.isStopped).toBeTrue();
    });
  });
});
