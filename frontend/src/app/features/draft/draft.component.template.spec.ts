import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { of } from 'rxjs';

import { DraftComponent } from './draft.component';
import { DraftService } from './services/draft.service';
import { DraftBoardState, DraftStatus } from './models/draft.interface';

describe('DraftComponent Progressive Template', () => {
  let component: DraftComponent;
  let fixture: ComponentFixture<DraftComponent>;
  let draftService: jasmine.SpyObj<DraftService>;

  const mockDraftBoardState: DraftBoardState = {
    draft: {
      id: 'draft-123',
      gameId: 'game-123',
      status: 'ACTIVE' as DraftStatus,
      currentRound: 1,
      currentPick: 1,
      totalRounds: 3,
      createdAt: '2025-01-15T10:30:00',
      updatedAt: '2025-01-15T10:35:00',
      startedAt: '2025-01-15T10:30:00',
      finishedAt: null
    },
    participants: [
      {
        participant: {
          id: 'participant-1',
          username: 'user1',
          joinedAt: '2025-01-15T10:30:00',
          isCreator: true,
          draftOrder: 1
        },
        selections: [],
        isCurrentTurn: true,
        timeRemaining: 300,
        hasTimedOut: false
      }
    ],
    availablePlayers: [
      {
        id: 'player-1',
        nickname: 'pixie',
        username: 'pixie',
        region: 'EU',
        tranche: 'T1',
        currentSeason: 2025
      }
    ],
    selectedPlayers: [],
    currentParticipant: {
      id: 'participant-1',
      username: 'user1',
      joinedAt: '2025-01-15T10:30:00',
      isCreator: true,
      draftOrder: 1
    },
    progress: {
      currentRound: 1,
      currentPick: 1,
      totalRounds: 3,
      totalPicks: 3,
      completedPicks: 0,
      progressPercentage: 0,
      estimatedTimeRemaining: null
    },
    rules: {
      maxPlayersPerTeam: 3,
      regionQuotas: {
        EU: 2
      },
      timeLimitPerPick: 300,
      autoPickEnabled: true,
      autoPickDelay: 43200
    }
  };

  beforeEach(async () => {
    const draftServiceSpy = jasmine.createSpyObj<DraftService>('DraftService', [
      'getDraftBoardState',
      'initializeDraft',
      'makePlayerSelection',
      'pauseDraft',
      'resumeDraft',
      'cancelDraft',
      'handleTimeouts'
    ]);

    draftServiceSpy.getDraftBoardState.and.returnValue(of(mockDraftBoardState));

    await TestBed.configureTestingModule({
      imports: [DraftComponent, RouterTestingModule, NoopAnimationsModule, MatSnackBarModule, MatDialogModule],
      providers: [{ provide: DraftService, useValue: draftServiceSpy }]
    }).compileComponents();

    fixture = TestBed.createComponent(DraftComponent);
    component = fixture.componentInstance;
    draftService = TestBed.inject(DraftService) as jasmine.SpyObj<DraftService>;

    spyOn(component as any, 'initializeComponent').and.stub();
    fixture.detectChanges();
  });

  it('renders the main container', () => {
    const container = fixture.debugElement.query(By.css('.progressive-draft'));
    expect(container).toBeTruthy();
  });

  it('renders the status bar with a title', () => {
    component.gameId = 'game-123';
    fixture.detectChanges();

    const statusBar = fixture.debugElement.query(By.css('.draft-status-bar'));
    expect(statusBar).toBeTruthy();

    const title = fixture.debugElement.query(By.css('.draft-title'));
    expect(title).toBeTruthy();
    expect(title.nativeElement.textContent).toContain('Draft');
    expect(title.nativeElement.textContent).toContain('game-123');
  });

  it('renders a progress indicator when draftState is present', () => {
    component.draftState = mockDraftBoardState;
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('.progress-track'))).toBeTruthy();
    expect(fixture.debugElement.query(By.css('.progress-fill'))).toBeTruthy();

    const progressText = fixture.debugElement.query(By.css('.progress-text'));
    expect(progressText).toBeTruthy();
    expect(progressText.nativeElement.textContent).toContain('/');
  });

  it('calls refreshDraftState when the refresh button is clicked', () => {
    spyOn(component, 'refreshDraftState');

    const refreshButton = fixture.debugElement.query(By.css('.draft-actions-minimal button'));
    expect(refreshButton).toBeTruthy();

    refreshButton.nativeElement.click();
    expect(component.refreshDraftState).toHaveBeenCalled();
  });

  it('shows the loading overlay when isLoading is true', () => {
    component.isLoading = true;
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('.loading-overlay'))).toBeTruthy();
  });

  it('shows the selection message when isSelectingPlayer is true', () => {
    component.isLoading = false;
    component.isSelectingPlayer = true;
    fixture.detectChanges();

    const message = fixture.debugElement.query(By.css('.loading-overlay p'));
    expect(message).toBeTruthy();
    expect(message.nativeElement.textContent).toContain('lection');
  });

  it('shows the error state when error is set', () => {
    component.error = 'Erreur de chargement';
    fixture.detectChanges();

    const errorState = fixture.debugElement.query(By.css('.error-state'));
    expect(errorState).toBeTruthy();
    expect(errorState.nativeElement.textContent).toContain('Erreur de chargement');

    const retryButton = fixture.debugElement.query(By.css('.error-state button'));
    expect(retryButton).toBeTruthy();
  });

  it('shows current turn focus when a current turn player is available', () => {
    component.draftState = mockDraftBoardState;
    fixture.detectChanges();

    const focus = fixture.debugElement.query(By.css('.current-turn-focus'));
    expect(focus).toBeTruthy();
    expect(focus.nativeElement.textContent).toContain('user1');
  });

  it('shows smart selection when the current user can select', () => {
    component.gameId = 'game-123';
    component.currentUserId = 'participant-1';
    component.draftState = mockDraftBoardState;
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('.smart-selection'))).toBeTruthy();
    expect(fixture.debugElement.query(By.css('.search-mega input'))).toBeTruthy();
  });

  it('shows waiting state when the current user cannot select', () => {
    component.gameId = 'game-123';
    component.currentUserId = 'participant-2';
    component.draftState = mockDraftBoardState;
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('.waiting-state'))).toBeTruthy();
    expect(fixture.debugElement.query(By.css('.smart-selection'))).toBeFalsy();
  });

  it('shows search results when searchTerm matches players', () => {
    component.gameId = 'game-123';
    component.currentUserId = 'participant-1';
    component.draftState = mockDraftBoardState;
    component.searchTerm = 'pixie';
    fixture.detectChanges();

    const results = fixture.debugElement.query(By.css('.search-results'));
    expect(results).toBeTruthy();
    expect(results.nativeElement.textContent).toContain('pixie');
  });

  it('shows no-results state when searchTerm matches no player', () => {
    component.gameId = 'game-123';
    component.currentUserId = 'participant-1';
    component.draftState = mockDraftBoardState;
    component.searchTerm = 'nope';
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('.no-results'))).toBeTruthy();
  });
});
