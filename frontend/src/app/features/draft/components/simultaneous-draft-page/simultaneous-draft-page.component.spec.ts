import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { signal } from '@angular/core';
import { Subject, of } from 'rxjs';

import { SimultaneousDraftPageComponent } from './simultaneous-draft-page.component';
import { DraftService } from '../../services/draft.service';
import { SimultaneousDraftService } from '../../services/simultaneous-draft.service';
import {
  WebSocketService,
  SimultaneousEventMessage,
} from '../../../../core/services/websocket.service';
import { UserContextService } from '../../../../core/services/user-context.service';
import { ResponsiveService } from '../../../../core/services/responsive.service';
import { AvailablePlayer, DraftBoardState } from '../../models/draft.interface';
import { CoinFlipAnimationComponent } from '../../../../shared/components/coin-flip-animation/coin-flip-animation.component';

// ===== FIXTURES =====

const PLAYERS: AvailablePlayer[] = [
  { id: 'p1', username: 'BUGHA', nickname: 'BUGHA', region: 'NAE', tranche: 'expert', totalPoints: 100, available: true, currentSeason: 1 },
  { id: 'p2', username: 'QUEASY', nickname: 'QUEASY', region: 'EU', tranche: 'expert', totalPoints: 95, available: true, currentSeason: 1 },
  { id: 'p3', username: 'NOOB', nickname: 'NOOB', region: 'EU', tranche: 'beginner', totalPoints: 5, available: false, currentSeason: 1 },
];

const DRAFT_STATE: DraftBoardState = {
  draft: { id: 'draft1', gameId: 'game1', status: 'IN_PROGRESS', currentRound: 1 },
  participants: [
    { participant: { id: 'part1', username: 'KARIM', gameId: 'game1' }, selections: [], isCurrentTurn: false },
    { participant: { id: 'part2', username: 'THOMAS', gameId: 'game1' }, selections: [], isCurrentTurn: false },
  ],
  availablePlayers: PLAYERS,
};

describe('SimultaneousDraftPageComponent', () => {
  let component: SimultaneousDraftPageComponent;
  let fixture: ComponentFixture<SimultaneousDraftPageComponent>;

  let draftServiceSpy: jasmine.SpyObj<DraftService>;
  let simultaneousServiceSpy: jasmine.SpyObj<SimultaneousDraftService>;
  let wsServiceSpy: jasmine.SpyObj<WebSocketService>;
  let dialogSpy: jasmine.SpyObj<MatDialog>;
  let dialogRefSpy: jasmine.SpyObj<MatDialogRef<CoinFlipAnimationComponent>>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;
  let wsConnected$: Subject<boolean>;
  let simultaneousEvents$: Subject<SimultaneousEventMessage>;

  beforeEach(async () => {
    wsConnected$ = new Subject<boolean>();
    simultaneousEvents$ = new Subject<SimultaneousEventMessage>();

    draftServiceSpy = jasmine.createSpyObj('DraftService', ['getDraftBoardState']);
    draftServiceSpy.getDraftBoardState.and.returnValue(of(DRAFT_STATE));

    simultaneousServiceSpy = jasmine.createSpyObj('SimultaneousDraftService', ['getStatus', 'submitSelection']);
    simultaneousServiceSpy.getStatus.and.returnValue(
      of({ windowId: 'win1', submitted: 1, total: 5 })
    );
    simultaneousServiceSpy.submitSelection.and.returnValue(of(undefined));

    wsServiceSpy = jasmine.createSpyObj('WebSocketService', ['subscribeToSimultaneous'], {
      isConnected$: wsConnected$.asObservable(),
      simultaneousEvents: simultaneousEvents$.asObservable(),
    });

    dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['afterClosed']);
    dialogRefSpy.afterClosed.and.returnValue(of(null));

    dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);
    dialogSpy.open.and.returnValue(dialogRefSpy);

    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      imports: [SimultaneousDraftPageComponent, NoopAnimationsModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'game1' } } } },
        { provide: DraftService, useValue: draftServiceSpy },
        { provide: SimultaneousDraftService, useValue: simultaneousServiceSpy },
        { provide: WebSocketService, useValue: wsServiceSpy },
        { provide: MatDialog, useValue: dialogSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        { provide: UserContextService, useValue: { getCurrentUser: () => ({ username: 'KARIM' }) } },
        { provide: ResponsiveService, useValue: { isMobile: signal(false) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SimultaneousDraftPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ===== INIT =====

  it('should load draft board state on init', () => {
    expect(draftServiceSpy.getDraftBoardState).toHaveBeenCalledWith('game1');
  });

  it('should subscribe to simultaneous WS events on init', () => {
    expect(wsServiceSpy.subscribeToSimultaneous).toHaveBeenCalledWith('game1');
  });

  it('should start in submitting phase', () => {
    expect(component.phase).toBe('submitting');
  });

  it('should load submission count from initial status', () => {
    expect(component.submittedCount).toBe(1);
    expect(component.totalCount).toBe(5);
    expect(component.windowId).toBe('win1');
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

  // ===== SUBMIT =====

  it('should call submitSelection and transition to waiting phase', () => {
    component.onPlayerSelect(PLAYERS[0]);
    component.submitSelection();

    expect(simultaneousServiceSpy.submitSelection).toHaveBeenCalledWith(
      'game1', 'win1', 'part1', 'p1'
    );
    expect(component.phase).toBe('waiting');
    expect(component.selectedPlayer).toBeNull();
  });

  it('should not submit when no player is selected', () => {
    component.submitSelection();
    expect(simultaneousServiceSpy.submitSelection).not.toHaveBeenCalled();
  });

  // ===== WEBSOCKET EVENTS =====

  it('should update submission count on SUBMISSION_COUNT event', () => {
    simultaneousEvents$.next({ type: 'SUBMISSION_COUNT', draftId: 'draft1', submitted: 3, total: 5 });
    expect(component.submittedCount).toBe(3);
    expect(component.totalCount).toBe(5);
  });

  it('should transition to done on ALL_RESOLVED event', () => {
    simultaneousEvents$.next({ type: 'ALL_RESOLVED', draftId: 'draft1' });
    expect(component.phase).toBe('done');
  });

  it('should open coin flip dialog on CONFLICT_RESOLVED event', () => {
    simultaneousEvents$.next({
      type: 'CONFLICT_RESOLVED',
      draftId: 'draft1',
      winnerParticipantId: 'part2',
      loserParticipantId: 'part1',
      contestedPlayerId: 'p1',
    });

    expect(dialogSpy.open).toHaveBeenCalledWith(
      CoinFlipAnimationComponent,
      jasmine.objectContaining({ data: jasmine.any(Object) })
    );
  });

  it('should enter reselecting phase when loser closes dialog with reselect', fakeAsync(() => {
    dialogRefSpy.afterClosed.and.returnValue(of({ action: 'reselect' }));

    simultaneousEvents$.next({
      type: 'CONFLICT_RESOLVED',
      draftId: 'draft1',
      winnerParticipantId: 'part2',
      loserParticipantId: 'part1', // KARIM = part1 = loser
      contestedPlayerId: 'p1',
    });
    tick();

    expect(component.phase).toBe('reselecting');
    expect(component.lostSlotLabel).toBe('BUGHA');
  }));

  it('should show toast and stay in waiting when winner closes dialog', fakeAsync(() => {
    component.phase = 'waiting';
    dialogRefSpy.afterClosed.and.returnValue(of({ action: 'reselect' }));

    simultaneousEvents$.next({
      type: 'CONFLICT_RESOLVED',
      draftId: 'draft1',
      winnerParticipantId: 'part1', // KARIM = part1 = winner
      loserParticipantId: 'part2',
      contestedPlayerId: 'p1',
    });
    tick();

    expect(snackBarSpy.open).toHaveBeenCalledWith(
      jasmine.stringContaining('BUGHA'),
      undefined,
      jasmine.any(Object)
    );
    expect(component.phase).toBe('waiting');
  }));

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

  // ===== FILTER =====

  it('should filter players on onFilterChange', () => {
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

  // ===== TEMPLATE PHASES =====

  it('should show player list in submitting phase', () => {
    expect(component.phase).toBe('submitting');
    const playerList = fixture.nativeElement.querySelector('#player-list');
    expect(playerList).not.toBeNull();
  });

  it('should show waiting zone in waiting phase', () => {
    component.phase = 'waiting';
    fixture.detectChanges();

    const waitingZone = fixture.nativeElement.querySelector('.waiting-zone');
    expect(waitingZone).not.toBeNull();
  });

  it('should show done zone in done phase', () => {
    component.phase = 'done';
    fixture.detectChanges();

    const doneZone = fixture.nativeElement.querySelector('.done-zone');
    expect(doneZone).not.toBeNull();
  });

  // ===== ACCESSIBILITY =====

  it('should have a skip link pointing to player-list', () => {
    const skip = fixture.nativeElement.querySelector('.skip-link');
    expect(skip).not.toBeNull();
    expect(skip.getAttribute('href')).toBe('#player-list');
  });
});
