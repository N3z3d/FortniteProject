import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { signal } from '@angular/core';
import { BehaviorSubject, ReplaySubject, Subject, of, throwError } from 'rxjs';

import { SnakeDraftPageComponent } from './snake-draft-page.component';
import { DraftService } from '../../services/draft.service';
import { WebSocketService, DraftEventMessage } from '../../../../core/services/websocket.service';
import { UserContextService } from '../../../../core/services/user-context.service';
import { ResponsiveService } from '../../../../core/services/responsive.service';
import { AvailablePlayer, DraftBoardState } from '../../models/draft.interface';

// ===== FIXTURES =====

const PLAYERS: AvailablePlayer[] = [
  { id: 'p1', username: 'BUGHA', nickname: 'BUGHA', region: 'NAC', tranche: 'expert', totalPoints: 100, available: true, currentSeason: 1 },
  { id: 'p2', username: 'QUEASY', nickname: 'QUEASY', region: 'EU', tranche: 'expert', totalPoints: 95, available: true, currentSeason: 1 },
  { id: 'p3', username: 'NOOB1', nickname: 'NOOB1', region: 'EU', tranche: 'débutant', totalPoints: 10, available: false, currentSeason: 1 },
];

const DRAFT_STATE: DraftBoardState = {
  draft: { id: 'draft1', gameId: 'game1', status: 'IN_PROGRESS', currentRound: 1 },
  regions: ['EU', 'NAW', 'ASIA'],
  pickExpiresAt: '2026-04-03T10:01:00Z',
  recommendedPlayerId: 'p1',
  participants: [
    { participant: { id: 'part1', username: 'KARIM', gameId: 'game1' }, selections: [], isCurrentTurn: true },
    { participant: { id: 'part2', username: 'THOMAS', gameId: 'game1' }, selections: [], isCurrentTurn: false },
  ],
  availablePlayers: PLAYERS,
  currentParticipant: { id: 'part1', username: 'KARIM', gameId: 'game1' },
};

describe('SnakeDraftPageComponent', () => {
  let component: SnakeDraftPageComponent;
  let fixture: ComponentFixture<SnakeDraftPageComponent>;

  let draftServiceSpy: jasmine.SpyObj<DraftService>;
  let wsServiceSpy: jasmine.SpyObj<WebSocketService>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;
  let wsConnected$: Subject<boolean>;
  let draftEvents$: Subject<DraftEventMessage>;

  beforeEach(async () => {
    wsConnected$ = new Subject<boolean>();
    draftEvents$ = new Subject<DraftEventMessage>();

    draftServiceSpy = jasmine.createSpyObj('DraftService', ['getSnakeBoardState', 'submitSnakePick']);
    draftServiceSpy.getSnakeBoardState.and.returnValue(of(DRAFT_STATE));
    draftServiceSpy.submitSnakePick.and.returnValue(of(DRAFT_STATE));

    wsServiceSpy = jasmine.createSpyObj('WebSocketService', ['connect', 'subscribeToDraft', 'disconnect'], {
      isConnected$: wsConnected$.asObservable(),
      draftEvents: draftEvents$.asObservable(),
    });

    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      imports: [SnakeDraftPageComponent, NoopAnimationsModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'game1' } } } },
        { provide: DraftService, useValue: draftServiceSpy },
        { provide: WebSocketService, useValue: wsServiceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        { provide: UserContextService, useValue: { getCurrentUser: () => ({ username: 'KARIM' }) } },
        { provide: ResponsiveService, useValue: { isMobile: signal(false) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SnakeDraftPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    // Simulate WebSocket connection so subscribeToDraft() is triggered
    wsConnected$.next(true);
  });

  // ===== INIT =====

  it('should load draft board state on init', () => {
    expect(draftServiceSpy.getSnakeBoardState).toHaveBeenCalledWith('game1', undefined);
  });

  it('should subscribe to WebSocket draft events on init', () => {
    expect(wsServiceSpy.subscribeToDraft).toHaveBeenCalledWith('game1');
  });

  it('should populate participants from draft state', () => {
    expect(component.participants.length).toBe(2);
    expect(component.participants[0].username).toBe('KARIM');
  });

  it('should set currentIndex to current participant position', () => {
    expect(component.currentIndex).toBe(0);
  });

  it('should hydrate pickExpiresAt from board state', () => {
    expect(component.pickExpiresAt).toBe('2026-04-03T10:01:00Z');
  });

  // ===== MY TURN =====

  it('should set isMyTurn when current participant matches logged-in user', () => {
    expect(component.isMyTurn).toBe(true);
  });

  it('should enter my-turn immediately on first load when the logged-in user is the current picker', () => {
    expect(component.phase).toBe('my-turn');
  });

  it('should show TON TOUR badge when isMyTurn', () => {
    const badge = fixture.nativeElement.querySelector('.my-turn-badge');
    expect(badge).not.toBeNull();
  });

  it('should hide TON TOUR badge when not my turn', () => {
    component.isMyTurn = false;
    fixture.detectChanges();

    const badge = fixture.nativeElement.querySelector('.my-turn-badge');
    expect(badge).toBeNull();
  });

  // ===== FILTER =====

  it('should update filteredPlayers on filter change', () => {
    component.onFilterChange({
      searchTerm: 'bugha',
      region: null,
      tranche: null,
      hideUnavailable: false,
      hideTaken: false,
    });

    expect(component.filteredPlayers.length).toBe(1);
    expect(component.filteredPlayers[0].username).toBe('BUGHA');
  });

  it('should hide unavailable players when hideUnavailable is true', () => {
    component.onFilterChange({
      searchTerm: '',
      region: null,
      tranche: null,
      hideUnavailable: true,
      hideTaken: false,
    });

    expect(component.filteredPlayers.every(p => p.available !== false)).toBe(true);
  });

  it('should filter by region', () => {
    component.onFilterChange({
      searchTerm: '',
      region: 'EU',
      tranche: null,
      hideUnavailable: false,
      hideTaken: false,
    });

    expect(component.filteredPlayers.every(p => p.region === 'EU')).toBe(true);
  });

  // ===== AUTO REGION FILTER (Task 4) =====

  it('should auto-filter players to cursor region when currentRegion is not GLOBAL', async () => {
    const euState: DraftBoardState = {
      ...DRAFT_STATE,
      currentRegion: 'EU',
      availablePlayers: PLAYERS, // BUGHA=NAC, QUEASY=EU, NOOB1=EU
    };
    draftServiceSpy.getSnakeBoardState.and.returnValue(of(euState));

    // Re-init component to apply EU state
    fixture = TestBed.createComponent(SnakeDraftPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.currentRegion).toBe('EU');
    expect(component.filteredPlayers.every(p => p.region === 'EU')).toBe(true);
    expect(component.filteredPlayers.some(p => p.region === 'NAC')).toBe(false);
  });

  it('should show all players when currentRegion is undefined', async () => {
    const noRegionState: DraftBoardState = {
      ...DRAFT_STATE,
      currentRegion: undefined,
      availablePlayers: PLAYERS,
    };
    draftServiceSpy.getSnakeBoardState.and.returnValue(of(noRegionState));

    fixture = TestBed.createComponent(SnakeDraftPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    // When currentRegion is undefined, all players should be shown (no filtering)
    expect(component.filteredPlayers.length).toBe(PLAYERS.length);
  });

  it('should update currentRegion from WS PICK_PROMPT event', () => {
    // Board state returned after WS event now carries the correct region
    const asiaState: DraftBoardState = { ...DRAFT_STATE, currentRegion: 'ASIA', availablePlayers: PLAYERS };
    draftServiceSpy.getSnakeBoardState.and.returnValue(of(asiaState));

    draftEvents$.next({ event: 'PICK_PROMPT', draftId: 'draft1', region: 'ASIA' });

    expect(component.currentRegion).toBe('ASIA');
  });

  it('should use configured draft regions instead of deriving them from catalogue players', () => {
    expect(component.regions).toEqual(['EU', 'NAW', 'ASIA']);
  });

  it('should switch region and reload board state when selecting another configured region', () => {
    draftServiceSpy.getSnakeBoardState.and.returnValue(
      of({ ...DRAFT_STATE, currentRegion: 'NAW' })
    );
    component.currentRegion = 'EU';
    component.selectedPlayer = PLAYERS[1];

    component.onRegionSelect('NAW');

    expect(component.currentRegion).toBe('NAW');
    expect(component.selectedPlayer).toBeNull();
    expect(draftServiceSpy.getSnakeBoardState).toHaveBeenCalledWith('game1', 'NAW');
  });

  it('should ignore region selection when the requested region is already active', () => {
    draftServiceSpy.getSnakeBoardState.calls.reset();
    component.currentRegion = 'EU';

    component.onRegionSelect('EU');

    expect(draftServiceSpy.getSnakeBoardState).not.toHaveBeenCalled();
  });

  it('should submit pick with currentRegion when region is not GLOBAL', () => {
    component.currentRegion = 'EU';
    component.isMyTurn = true;
    component.onPlayerSelect(PLAYERS[1]); // QUEASY is EU
    component.confirmPick();

    expect(draftServiceSpy.submitSnakePick).toHaveBeenCalledWith('game1', 'p2', 'EU');
  });

  // ===== PLAYER SELECTION =====

  it('should select a player on onPlayerSelect', () => {
    component.onPlayerSelect(PLAYERS[0]);
    expect(component.selectedPlayer?.id).toBe('p1');
  });

  it('should deselect player on second onPlayerSelect call', () => {
    component.onPlayerSelect(PLAYERS[0]);
    component.onPlayerSelect(PLAYERS[0]);
    expect(component.selectedPlayer).toBeNull();
  });

  // ===== CONFIRM PICK =====

  it('should submit pick via snake runtime service on confirmPick', () => {
    component.onPlayerSelect(PLAYERS[0]);
    component.confirmPick();

    expect(draftServiceSpy.submitSnakePick).toHaveBeenCalledWith('game1', 'p1', 'GLOBAL');
  });

  it('should show narrative toast after pick confirmation', () => {
    component.onPlayerSelect(PLAYERS[0]);
    component.confirmPick();

    expect(snackBarSpy.open).toHaveBeenCalledWith(
      jasmine.stringContaining('BUGHA'),
      undefined,
      jasmine.any(Object)
    );
  });

  it('should clear selectedPlayer after confirmPick', () => {
    component.onPlayerSelect(PLAYERS[0]);
    component.confirmPick();

    expect(component.selectedPlayer).toBeNull();
  });

  // ===== TIMER EXPIRY / AUTO-PICK =====

  it('should auto-pick recommended player on timer expired', () => {
    component.onTimerExpired();

    expect(draftServiceSpy.submitSnakePick).toHaveBeenCalledWith(
      'game1',
      component.recommendedPlayer!.id,
      'GLOBAL'
    );
  });

  it('should ignore timer expiry when it is not my turn', () => {
    draftServiceSpy.submitSnakePick.calls.reset();
    component.isMyTurn = false;

    component.onTimerExpired();

    expect(draftServiceSpy.submitSnakePick).not.toHaveBeenCalled();
  });

  it('should ignore confirmPick when a pick is already pending', () => {
    draftServiceSpy.submitSnakePick.calls.reset();
    component.onPlayerSelect(PLAYERS[0]);
    component.isPickPending = true;

    component.confirmPick();

    expect(draftServiceSpy.submitSnakePick).not.toHaveBeenCalled();
  });

  it('should cancel pick on onPickCancelled', () => {
    component.onPlayerSelect(PLAYERS[0]);
    component.onPickCancelled();

    expect(component.selectedPlayer).toBeNull();
    expect(component.isPickPending).toBe(false);
  });

  // ===== PICK ERROR HANDLING =====

  it('should show snackbar on 409 conflict (player already picked)', () => {
    draftServiceSpy.submitSnakePick.and.returnValue(throwError(() => ({ status: 409 })));
    component.onPlayerSelect(PLAYERS[0]);

    component.confirmPick();

    expect(snackBarSpy.open).toHaveBeenCalledWith(
      jasmine.stringContaining('déjà été sélectionné'),
      undefined,
      jasmine.objectContaining({ panelClass: 'snack-pick-error' })
    );
    expect(component.isPickPending).toBe(false);
  });

  it('should show backend message on 400 error', () => {
    draftServiceSpy.submitSnakePick.and.returnValue(
      throwError(() => ({ status: 400, error: { message: 'Tranche violation: rank too high' } }))
    );
    component.onPlayerSelect(PLAYERS[0]);

    component.confirmPick();

    expect(snackBarSpy.open).toHaveBeenCalledWith(
      jasmine.stringContaining('trop bien class'),
      undefined,
      jasmine.objectContaining({ panelClass: 'snack-pick-error' })
    );
    expect(component.isPickPending).toBe(false);
  });

  it('should show the expected region when backend reports a region mismatch', () => {
    draftServiceSpy.submitSnakePick.and.returnValue(
      throwError(() => ({
        status: 400,
        error: { message: 'Joueur hors region - region attendue : EU (player region: OCE)' },
      }))
    );
    component.onPlayerSelect(PLAYERS[0]);

    component.confirmPick();

    expect(snackBarSpy.open).toHaveBeenCalledWith(
      jasmine.stringContaining('region attendue : EU'),
      undefined,
      jasmine.objectContaining({ panelClass: 'snack-pick-error' })
    );
    expect(component.isPickPending).toBe(false);
  });

  it('should show fallback message on 400 error without backend message', () => {
    draftServiceSpy.submitSnakePick.and.returnValue(throwError(() => ({ status: 400 })));
    component.onPlayerSelect(PLAYERS[0]);

    component.confirmPick();

    expect(snackBarSpy.open).toHaveBeenCalledWith(
      jasmine.stringContaining('région'),
      undefined,
      jasmine.objectContaining({ panelClass: 'snack-pick-error' })
    );
    expect(component.isPickPending).toBe(false);
  });

  it('should show generic snackbar on 500 server error', () => {
    draftServiceSpy.submitSnakePick.and.returnValue(throwError(() => ({ status: 500 })));
    component.onPlayerSelect(PLAYERS[0]);

    component.confirmPick();

    expect(snackBarSpy.open).toHaveBeenCalledWith(
      jasmine.stringContaining('Erreur inattendue'),
      undefined,
      jasmine.objectContaining({ panelClass: 'snack-pick-error' })
    );
    expect(component.isPickPending).toBe(false);
  });

  // ===== WEBSOCKET STATUS =====

  it('should show WS banner when disconnected', () => {
    wsConnected$.next(false);
    fixture.detectChanges();

    const banner = fixture.nativeElement.querySelector('.ws-banner');
    expect(banner).not.toBeNull();
  });

  it('should hide WS banner when connected', () => {
    wsConnected$.next(true);
    fixture.detectChanges();

    const banner = fixture.nativeElement.querySelector('.ws-banner');
    expect(banner).toBeNull();
  });

  // ===== WEBSOCKET EVENTS =====

  it('should refresh draft state on TURN_CHANGED event', () => {
    const callsBefore = draftServiceSpy.getSnakeBoardState.calls.count();

    draftEvents$.next({ event: 'TURN_CHANGED', draftId: 'draft1' });

    expect(draftServiceSpy.getSnakeBoardState.calls.count()).toBeGreaterThan(callsBefore);
  });

  it('should ignore replayed draft events from another draft', () => {
    const callsBefore = draftServiceSpy.getSnakeBoardState.calls.count();
    component.currentRegion = 'EU';

    draftEvents$.next({ event: 'PICK_PROMPT', draftId: 'draft-foreign', region: 'NAW' });

    expect(component.currentRegion).toBe('EU');
    expect(draftServiceSpy.getSnakeBoardState.calls.count()).toBe(callsBefore);
  });

  it('should set pickExpiresAt from PICK_PROMPT event', () => {
    const expires = new Date(Date.now() + 30_000).toISOString();
    draftServiceSpy.getSnakeBoardState.and.returnValue(of({ ...DRAFT_STATE, pickExpiresAt: expires }));

    draftEvents$.next({ event: 'PICK_PROMPT', draftId: 'draft1', expiresAt: expires } as any);

    expect(component.pickExpiresAt).toBe(expires);
  });

  // ===== RECONNECTION BANNER =====

  it('should set isReconnecting=true when WS disconnects', () => {
    wsConnected$.next(true);  // first emission after skip(1)
    wsConnected$.next(false); // genuine disconnect

    expect(component.isReconnecting).toBe(true);
  });

  it('should set isReconnecting=false when WS reconnects', () => {
    wsConnected$.next(true);  // connect
    wsConnected$.next(false); // disconnect → isReconnecting=true
    wsConnected$.next(true);  // reconnect → isReconnecting=false

    expect(component.isReconnecting).toBe(false);
  });

  it('should show reconnection text in ws-banner when disconnected', () => {
    component.wsConnected = false;
    fixture.detectChanges();

    const banner = fixture.nativeElement.querySelector('.ws-banner');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('Reconnexion en cours');
  });

  // ===== SKIP LINK =====

  it('should have a skip link pointing to player-list', () => {
    const skip = fixture.nativeElement.querySelector('.skip-link');
    expect(skip).not.toBeNull();
    expect(skip.getAttribute('href')).toBe('#player-list');
  });

  // ===== RECOMMENDATION =====

  it('should use backend-recommended player from board state', () => {
    expect(component.recommendedPlayer?.username).toBe('BUGHA');
  });

  // ===== TRANCHE ELIGIBILITY =====

  it('should mark player ineligible when tranche floor below trancheFloor', () => {
    component.tranchesEnabled = true;
    component.trancheFloor = 10;
    const lowPlayer: AvailablePlayer = { id: 'px', username: 'LOW', nickname: 'LOW', region: 'EU', tranche: '1-5', totalPoints: 5, currentSeason: 1 };
    // Dynamic rank: rank 3 < floor 10 → ineligible
    (component as any).regionPlayerRanks = new Map([['px', 3]]);

    expect(component.isPlayerEligible(lowPlayer)).toBe(false);
  });

  it('should mark player eligible when tranche floor >= trancheFloor', () => {
    component.tranchesEnabled = true;
    component.trancheFloor = 10;
    const highPlayer: AvailablePlayer = { id: 'py', username: 'HIGH', nickname: 'HIGH', region: 'EU', tranche: '15-20', totalPoints: 50, currentSeason: 1 };
    // Dynamic rank: rank 15 >= floor 10 → eligible
    (component as any).regionPlayerRanks = new Map([['py', 15]]);

    expect(component.isPlayerEligible(highPlayer)).toBe(true);
  });

  it('should always return eligible when tranches are disabled', () => {
    component.tranchesEnabled = false;
    component.trancheFloor = 100;
    const player: AvailablePlayer = { id: 'pz', username: 'ANY', nickname: 'ANY', region: 'EU', tranche: '1-5', totalPoints: 5, currentSeason: 1 };

    expect(component.isPlayerEligible(player)).toBe(true);
  });

  it('should filter ineligible players when hideIneligible is toggled', () => {
    component.tranchesEnabled = true;
    component.trancheFloor = 50;
    component.draft = {
      ...DRAFT_STATE,
      availablePlayers: [
        { id: 'a', username: 'A', nickname: 'A', region: 'EU', tranche: '1-5', totalPoints: 5, currentSeason: 1 },
        { id: 'b', username: 'B', nickname: 'B', region: 'EU', tranche: '50-60', totalPoints: 55, currentSeason: 1 },
      ],
    };
    // Dynamic ranks: 'a' rank 1 < floor 50 → ineligible, 'b' rank 50 >= floor 50 → eligible
    (component as any).regionPlayerRanks = new Map([['a', 1], ['b', 50]]);

    component.toggleHideIneligible();

    expect(component.hideIneligible).toBe(true);
    expect(component.filteredPlayers.length).toBe(1);
    expect(component.filteredPlayers[0].username).toBe('B');
  });

  it('should show tranche info bar when tranchesEnabled and trancheFloor > 1', () => {
    component.tranchesEnabled = true;
    component.trancheFloor = 10;
    fixture.detectChanges();

    const bar = fixture.nativeElement.querySelector('.tranche-info-bar');
    expect(bar).not.toBeNull();
    expect(bar.textContent).toContain('10');
  });

  it('should hide tranche info bar when trancheFloor is 1', () => {
    component.tranchesEnabled = true;
    component.trancheFloor = 1;
    fixture.detectChanges();

    const bar = fixture.nativeElement.querySelector('.tranche-info-bar');
    expect(bar).toBeNull();
  });

  it('should capture trancheFloor and tranchesEnabled from board state', () => {
    const stateWithTranches: DraftBoardState = {
      ...DRAFT_STATE,
      trancheFloor: 25,
      tranchesEnabled: false,
    };
    draftServiceSpy.getSnakeBoardState.and.returnValue(of(stateWithTranches));

    fixture = TestBed.createComponent(SnakeDraftPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.trancheFloor).toBe(25);
    expect(component.tranchesEnabled).toBe(false);
  });

  // ===== PHASE SYSTEM — BUG-01 mount during turn transition =====

  it('should show draft-content after first state load', () => {
    const content = fixture.nativeElement.querySelector('.draft-content');
    expect(content).not.toBeNull();
  });

  it('should keep draft-content mounted when phase is waiting', () => {
    component.phase = 'waiting';
    fixture.detectChanges();

    const content = fixture.nativeElement.querySelector('.draft-content');
    expect(content).not.toBeNull();
  });

  it('should hide draft-content when phase is idle', () => {
    component.phase = 'idle';
    fixture.detectChanges();

    const content = fixture.nativeElement.querySelector('.draft-content');
    expect(content).toBeNull();
  });

  it('should set phase to waiting when PICK_PROMPT event names another participant as next', () => {
    // Arrange: next board state has THOMAS as current participant
    const thomasState: DraftBoardState = {
      ...DRAFT_STATE,
      currentParticipant: { id: 'part2', username: 'THOMAS', gameId: 'game1' },
    };
    draftServiceSpy.getSnakeBoardState.and.returnValue(of(thomasState));
    component.phase = 'warmup';

    draftEvents$.next({ event: 'PICK_PROMPT', draftId: 'draft1', participantId: 'part2', participantUsername: 'THOMAS' });

    // KARIM is logged in; THOMAS is next → not my turn
    expect(component.phase).toBe('waiting');
    expect(component.isMyTurn).toBe(false);
  });

  it('should transition phase optimistically to my-turn on PICK_PROMPT with own username', () => {
    component.phase = 'waiting';
    draftEvents$.next({ event: 'PICK_PROMPT', draftId: 'draft1', participantId: 'part1', participantUsername: 'KARIM' });

    expect(component.phase).toBe('my-turn');
    expect(component.isMyTurn).toBe(true);
  });

  it('should resolve the next player from participantId when participantUsername is missing', () => {
    component.phase = 'waiting';

    draftEvents$.next({ event: 'PICK_PROMPT', draftId: 'draft1', participantId: 'part1' });

    expect(component.phase).toBe('my-turn');
    expect(component.isMyTurn).toBe(true);
  });

  // ===== BUG-09: WS banner should not show on initial mount =====

  it('should have wsConnected=true initially (BUG-09: no false flash)', () => {
    // wsConnected starts true; trackConnectionStatus uses skip(1) so the
    // initial BehaviorSubject(false) emission from production WS service is ignored
    expect(component.wsConnected).toBeTrue();
    const banner = fixture.nativeElement.querySelector('.ws-banner');
    expect(banner).toBeNull();
  });
});

// BUG-09 — Separate suite using BehaviorSubject to simulate real WS service
describe('SnakeDraftPageComponent — BUG-09 WS banner regression', () => {
  let fixture: ComponentFixture<SnakeDraftPageComponent>;
  let wsStartedFalse$: BehaviorSubject<boolean>;

  beforeEach(async () => {
    wsStartedFalse$ = new BehaviorSubject<boolean>(false);
    const wsSpy = jasmine.createSpyObj('WebSocketService', ['connect', 'subscribeToDraft', 'disconnect'], {
      isConnected$: wsStartedFalse$.asObservable(),
      draftEvents: new Subject<DraftEventMessage>().asObservable(),
    });
    const draftSpy = jasmine.createSpyObj('DraftService', ['getSnakeBoardState', 'submitSnakePick']);
    draftSpy.getSnakeBoardState.and.returnValue(of(DRAFT_STATE));

    await TestBed.configureTestingModule({
      imports: [SnakeDraftPageComponent, NoopAnimationsModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'game1' } } } },
        { provide: DraftService, useValue: draftSpy },
        { provide: WebSocketService, useValue: wsSpy },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj('MatSnackBar', ['open']) },
        { provide: UserContextService, useValue: { getCurrentUser: () => ({ username: 'KARIM' }) } },
        { provide: ResponsiveService, useValue: { isMobile: signal(false) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SnakeDraftPageComponent);
    fixture.detectChanges();
  });

  it('should NOT show WS banner when isConnected$ starts false (BUG-09)', () => {
    // Real WS service starts as BehaviorSubject(false), emits false immediately
    // skip(1) ignores that initial emission → banner not shown
    const banner = fixture.nativeElement.querySelector('.ws-banner');
    expect(banner).toBeNull();
  });

  it('should show WS banner after genuine disconnect (false after true)', () => {
    wsStartedFalse$.next(true);  // 1st emission — skipped by skip(1)
    wsStartedFalse$.next(false); // 2nd emission — not skipped
    fixture.detectChanges();

    const banner = fixture.nativeElement.querySelector('.ws-banner');
    expect(banner).not.toBeNull();
  });
});

// Task 6.2 — ReplaySubject(1) reconnection fix
describe('ReplaySubject(1) — reconnection event replay', () => {
  it('new subscriber receives last emitted event without waiting for next emission', () => {
    const replay = new ReplaySubject<number>(1);
    replay.next(42);

    let received: number | undefined;
    replay.subscribe(v => (received = v));

    expect(received).toBe(42);
  });

  it('plain Subject does NOT replay last event to late subscribers', () => {
    const subject = new Subject<number>();
    subject.next(42);

    let received: number | undefined;
    subject.subscribe(v => (received = v));

    expect(received).toBeUndefined();
  });
});
