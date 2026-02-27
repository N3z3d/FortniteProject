import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { signal } from '@angular/core';
import { Subject, of } from 'rxjs';

import { SnakeDraftPageComponent } from './snake-draft-page.component';
import { DraftService } from '../../services/draft.service';
import { WebSocketService, DraftEventMessage } from '../../../../core/services/websocket.service';
import { UserContextService } from '../../../../core/services/user-context.service';
import { ResponsiveService } from '../../../../core/services/responsive.service';
import { AvailablePlayer, DraftBoardState } from '../../models/draft.interface';

// ===== FIXTURES =====

const PLAYERS: AvailablePlayer[] = [
  { id: 'p1', username: 'BUGHA', nickname: 'BUGHA', region: 'NAE', tranche: 'expert', totalPoints: 100, available: true, currentSeason: 1 },
  { id: 'p2', username: 'QUEASY', nickname: 'QUEASY', region: 'EU', tranche: 'expert', totalPoints: 95, available: true, currentSeason: 1 },
  { id: 'p3', username: 'NOOB1', nickname: 'NOOB1', region: 'EU', tranche: 'débutant', totalPoints: 10, available: false, currentSeason: 1 },
];

const DRAFT_STATE: DraftBoardState = {
  draft: { id: 'draft1', gameId: 'game1', status: 'IN_PROGRESS', currentRound: 1 },
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

    draftServiceSpy = jasmine.createSpyObj('DraftService', ['getDraftBoardState']);
    draftServiceSpy.getDraftBoardState.and.returnValue(of(DRAFT_STATE));

    wsServiceSpy = jasmine.createSpyObj('WebSocketService', ['subscribeToDraft', 'publishDraftPick'], {
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
  });

  // ===== INIT =====

  it('should load draft board state on init', () => {
    expect(draftServiceSpy.getDraftBoardState).toHaveBeenCalledWith('game1');
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

  // ===== MY TURN =====

  it('should set isMyTurn when current participant matches logged-in user', () => {
    expect(component.isMyTurn).toBe(true);
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

  it('should publish pick via WebSocket on confirmPick', () => {
    component.onPlayerSelect(PLAYERS[0]);
    component.confirmPick();

    expect(wsServiceSpy.publishDraftPick).toHaveBeenCalledWith('game1', 'part1', 'p1');
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

    expect(wsServiceSpy.publishDraftPick).toHaveBeenCalledWith(
      'game1',
      'part1',
      component.recommendedPlayer!.id
    );
  });

  it('should cancel pick on onPickCancelled', () => {
    component.onPlayerSelect(PLAYERS[0]);
    component.onPickCancelled();

    expect(component.selectedPlayer).toBeNull();
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
    const callsBefore = draftServiceSpy.getDraftBoardState.calls.count();

    draftEvents$.next({ event: 'TURN_CHANGED', draftId: 'draft1' });

    expect(draftServiceSpy.getDraftBoardState.calls.count()).toBeGreaterThan(callsBefore);
  });

  // ===== SKIP LINK =====

  it('should have a skip link pointing to player-list', () => {
    const skip = fixture.nativeElement.querySelector('.skip-link');
    expect(skip).not.toBeNull();
    expect(skip.getAttribute('href')).toBe('#player-list');
  });

  // ===== RECOMMENDATION =====

  it('should recommend player with highest totalPoints', () => {
    expect(component.recommendedPlayer?.username).toBe('BUGHA');
  });
});
