import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialogModule } from '@angular/material/dialog';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { FormsModule } from '@angular/forms';
import { of } from 'rxjs';

import { DraftComponent } from './draft.component';
import { DraftService } from './services/draft.service';
import { DraftBoardState, DraftStatus, Player, GameParticipant, PlayerRegion } from './models/draft.interface';

describe('DraftComponent Template', () => {
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
      },
      {
        participant: {
          id: 'participant-2',
          username: 'user2',
          joinedAt: '2025-01-15T10:31:00',
          isCreator: false,
          draftOrder: 2
        },
        selections: [],
        isCurrentTurn: false,
        timeRemaining: null,
        hasTimedOut: false
      }
    ],
    availablePlayers: [
      {
        id: 'player-1',
        nickname: 'pixie',
        username: 'pixie',
        region: 'EU',
        tranche: '1',
        currentSeason: 2025
      },
      {
        id: 'player-2',
        nickname: 'Muz',
        username: 'Muz',
        region: 'NAC',
        tranche: '1',
        currentSeason: 2025
      },
      {
        id: 'player-3',
        nickname: 'White',
        username: 'White',
        region: 'BR',
        tranche: '2',
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
      totalPicks: 6,
      completedPicks: 0,
      progressPercentage: 0,
      estimatedTimeRemaining: null
    },
    rules: {
      maxPlayersPerTeam: 3,
      regionQuotas: {
        EU: 2,
        NAC: 2,
        BR: 1,
        ASIA: 1,
        OCE: 1,
        ME: 1
      },
      timeLimitPerPick: 300,
      autoPickEnabled: true,
      autoPickDelay: 43200
    }
  };

  beforeEach(async () => {
    const draftServiceSpy = jasmine.createSpyObj('DraftService', [
      'getDraftBoardState',
      'initializeDraft',
      'makePlayerSelection',
      'pauseDraft',
      'resumeDraft',
      'cancelDraft',
      'handleTimeouts'
    ]);

    await TestBed.configureTestingModule({
      imports: [
        DraftComponent,
        RouterTestingModule,
        NoopAnimationsModule,
        MatSnackBarModule,
        MatDialogModule,
        MatCardModule,
        MatButtonModule,
        MatChipsModule,
        MatProgressBarModule,
        MatFormFieldModule,
        MatInputModule,
        MatSelectModule,
        MatIconModule,
        MatToolbarModule,
        FormsModule
      ],
      providers: [
        { provide: DraftService, useValue: draftServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DraftComponent);
    component = fixture.componentInstance;
    draftService = TestBed.inject(DraftService) as jasmine.SpyObj<DraftService>;
  });

  describe('Structure de base', () => {
    it('should have a main container', () => {
      const container = fixture.debugElement.query(By.css('.draft-container'));
      expect(container).toBeTruthy();
    });

    it('should have a header section', () => {
      const header = fixture.debugElement.query(By.css('.draft-header'));
      expect(header).toBeTruthy();
    });

    it('should have a back button in header', () => {
      const backButton = fixture.debugElement.query(By.css('.back-button'));
      expect(backButton).toBeTruthy();
      expect(backButton.nativeElement.textContent).toContain('Retour');
    });

    it('should have a title in header', () => {
      const title = fixture.debugElement.query(By.css('.draft-title'));
      expect(title).toBeTruthy();
      expect(title.nativeElement.textContent).toContain('Draft');
    });
  });

  describe('État de chargement', () => {
    it('should show loading spinner when isLoading is true', () => {
      component.isLoading = true;
      fixture.detectChanges();

      const loadingSpinner = fixture.debugElement.query(By.css('.loading-spinner'));
      expect(loadingSpinner).toBeTruthy();
    });

    it('should hide loading spinner when isLoading is false', () => {
      component.isLoading = false;
      fixture.detectChanges();

      const loadingSpinner = fixture.debugElement.query(By.css('.loading-spinner'));
      expect(loadingSpinner).toBeFalsy();
    });
  });

  describe('Gestion des erreurs', () => {
    it('should show error message when error is present', () => {
      component.error = 'Erreur de chargement';
      fixture.detectChanges();

      const errorMessage = fixture.debugElement.query(By.css('.error-message'));
      expect(errorMessage).toBeTruthy();
      expect(errorMessage.nativeElement.textContent).toContain('Erreur de chargement');
    });

    it('should hide error message when error is null', () => {
      component.error = null;
      fixture.detectChanges();

      const errorMessage = fixture.debugElement.query(By.css('.error-message'));
      expect(errorMessage).toBeFalsy();
    });
  });

  describe('Statut du draft', () => {
    beforeEach(() => {
      component.draftState = mockDraftBoardState;
      fixture.detectChanges();
    });

    it('should display draft status', () => {
      const statusChip = fixture.debugElement.query(By.css('.draft-status'));
      expect(statusChip).toBeTruthy();
      expect(statusChip.nativeElement.textContent).toContain('En cours');
    });

    it('should display current round and pick', () => {
      const roundInfo = fixture.debugElement.query(By.css('.round-info'));
      expect(roundInfo).toBeTruthy();
      expect(roundInfo.nativeElement.textContent).toContain('Round 1');
      expect(roundInfo.nativeElement.textContent).toContain('Pick 1');
    });

    it('should display progress bar', () => {
      const progressBar = fixture.debugElement.query(By.css('.progress-bar'));
      expect(progressBar).toBeTruthy();
    });

    it('should display progress percentage', () => {
      const progressText = fixture.debugElement.query(By.css('.progress-text'));
      expect(progressText).toBeTruthy();
      expect(progressText.nativeElement.textContent).toContain('0%');
    });
  });

  describe('Actions de draft', () => {
    beforeEach(() => {
      component.draftState = mockDraftBoardState;
      fixture.detectChanges();
    });

    it('should have pause button when draft is active', () => {
      const pauseButton = fixture.debugElement.query(By.css('.pause-button'));
      expect(pauseButton).toBeTruthy();
      expect(pauseButton.nativeElement.textContent).toContain('Pause');
    });

    it('should have resume button when draft is paused', () => {
      component.draftState!.draft.status = 'PAUSED';
      fixture.detectChanges();

      const resumeButton = fixture.debugElement.query(By.css('.resume-button'));
      expect(resumeButton).toBeTruthy();
      expect(resumeButton.nativeElement.textContent).toContain('Reprendre');
    });

    it('should have cancel button', () => {
      const cancelButton = fixture.debugElement.query(By.css('.cancel-button'));
      expect(cancelButton).toBeTruthy();
      expect(cancelButton.nativeElement.textContent).toContain('Annuler');
    });

    it('should have refresh button', () => {
      const refreshButton = fixture.debugElement.query(By.css('.refresh-button'));
      expect(refreshButton).toBeTruthy();
    });

    it('should have handle timeouts button', () => {
      const timeoutsButton = fixture.debugElement.query(By.css('.timeouts-button'));
      expect(timeoutsButton).toBeTruthy();
      expect(timeoutsButton.nativeElement.textContent).toContain('Gérer Timeouts');
    });
  });

  describe('Filtres et recherche', () => {
    beforeEach(() => {
      component.draftState = mockDraftBoardState;
      fixture.detectChanges();
    });

    it('should have search input', () => {
      const searchInput = fixture.debugElement.query(By.css('.search-input'));
      expect(searchInput).toBeTruthy();
      expect(searchInput.nativeElement.placeholder).toContain('Rechercher un joueur');
    });

    it('should have region filter select', () => {
      const regionSelect = fixture.debugElement.query(By.css('.region-select'));
      expect(regionSelect).toBeTruthy();
    });

    it('should have tranche filter select', () => {
      const trancheSelect = fixture.debugElement.query(By.css('.tranche-select'));
      expect(trancheSelect).toBeTruthy();
    });

    it('should have clear filters button', () => {
      const clearButton = fixture.debugElement.query(By.css('.clear-filters-button'));
      expect(clearButton).toBeTruthy();
      expect(clearButton.nativeElement.textContent).toContain('Effacer');
    });
  });

  describe('Liste des joueurs', () => {
    beforeEach(() => {
      component.draftState = mockDraftBoardState;
      fixture.detectChanges();
    });

    it('should have players container', () => {
      const playersContainer = fixture.debugElement.query(By.css('.players-container'));
      expect(playersContainer).toBeTruthy();
    });

    it('should display available players', () => {
      const playerCards = fixture.debugElement.queryAll(By.css('.player-card'));
      expect(playerCards.length).toBe(3); // 3 joueurs dans le mock
    });

    it('should display player nickname', () => {
      const playerCards = fixture.debugElement.queryAll(By.css('.player-card'));
      expect(playerCards[0].query(By.css('.player-nickname')).nativeElement.textContent).toContain('pixie');
    });

    it('should display player region', () => {
      const playerCards = fixture.debugElement.queryAll(By.css('.player-card'));
      const regionChip = playerCards[0].query(By.css('.region-chip'));
      expect(regionChip).toBeTruthy();
      expect(regionChip.nativeElement.textContent).toContain('Europe');
    });

    it('should display player tranche', () => {
      const playerCards = fixture.debugElement.queryAll(By.css('.player-card'));
      const trancheChip = playerCards[0].query(By.css('.tranche-chip'));
      expect(trancheChip).toBeTruthy();
      expect(trancheChip.nativeElement.textContent).toContain('Tranche 1');
    });

    it('should have select button for each player', () => {
      const playerCards = fixture.debugElement.queryAll(By.css('.player-card'));
      const selectButtons = playerCards[0].queryAll(By.css('.select-button'));
      expect(selectButtons.length).toBeGreaterThan(0);
    });

    it('should disable select button when not user turn', () => {
      component.currentUserId = 'participant-2'; // Pas le tour de cet utilisateur
      fixture.detectChanges();

      const playerCards = fixture.debugElement.queryAll(By.css('.player-card'));
      const selectButton = playerCards[0].query(By.css('.select-button'));
      expect(selectButton.nativeElement.disabled).toBeTrue();
    });

    it('should enable select button when user turn', () => {
      component.currentUserId = 'participant-1'; // C'est le tour de cet utilisateur
      fixture.detectChanges();

      const playerCards = fixture.debugElement.queryAll(By.css('.player-card'));
      const selectButton = playerCards[0].query(By.css('.select-button'));
      expect(selectButton.nativeElement.disabled).toBeFalse();
    });
  });

  describe('Participants', () => {
    beforeEach(() => {
      component.draftState = mockDraftBoardState;
      fixture.detectChanges();
    });

    it('should have participants container', () => {
      const participantsContainer = fixture.debugElement.query(By.css('.participants-container'));
      expect(participantsContainer).toBeTruthy();
    });

    it('should display all participants', () => {
      const participantCards = fixture.debugElement.queryAll(By.css('.participant-card'));
      expect(participantCards.length).toBe(2); // 2 participants dans le mock
    });

    it('should display participant username', () => {
      const participantCards = fixture.debugElement.queryAll(By.css('.participant-card'));
      expect(participantCards[0].query(By.css('.participant-username')).nativeElement.textContent).toContain('user1');
    });

    it('should highlight current participant', () => {
      const participantCards = fixture.debugElement.queryAll(By.css('.participant-card'));
      const currentParticipant = participantCards[0];
      expect(currentParticipant.nativeElement.classList).toContain('current-turn');
    });

    it('should display participant draft order', () => {
      const participantCards = fixture.debugElement.queryAll(By.css('.participant-card'));
      const orderBadge = participantCards[0].query(By.css('.draft-order'));
      expect(orderBadge).toBeTruthy();
      expect(orderBadge.nativeElement.textContent).toContain('1');
    });

    it('should display creator badge for creator', () => {
      const participantCards = fixture.debugElement.queryAll(By.css('.participant-card'));
      const creatorBadge = participantCards[0].query(By.css('.creator-badge'));
      expect(creatorBadge).toBeTruthy();
      expect(creatorBadge.nativeElement.textContent).toContain('Créateur');
    });
  });

  describe('Sélections des participants', () => {
    beforeEach(() => {
      component.draftState = mockDraftBoardState;
      // Ajouter des sélections
      component.draftState!.participants[0].selections = [
        {
          id: 'pick-1',
          draftId: 'draft-123',
          participantId: 'participant-1',
          playerId: 'player-1',
          round: 1,
          pickNumber: 1,
          selectionTime: '2025-01-15T10:35:00',
          timeTakenSeconds: 30,
          autoPick: false
        }
      ];
      fixture.detectChanges();
    });

    it('should display participant selections', () => {
      const participantCards = fixture.debugElement.queryAll(By.css('.participant-card'));
      const selections = participantCards[0].queryAll(By.css('.selection-item'));
      expect(selections.length).toBe(1);
    });

    it('should display selection details', () => {
      const participantCards = fixture.debugElement.queryAll(By.css('.participant-card'));
      const selection = participantCards[0].query(By.css('.selection-item'));
      expect(selection.nativeElement.textContent).toContain('pixie');
    });
  });

  describe('Responsive design', () => {
    beforeEach(() => {
      component.draftState = mockDraftBoardState;
      fixture.detectChanges();
    });

    it('should have responsive grid layout', () => {
      const gridContainer = fixture.debugElement.query(By.css('.draft-grid'));
      expect(gridContainer).toBeTruthy();
    });

    it('should have mobile-friendly layout', () => {
      const mobileContainer = fixture.debugElement.query(By.css('.mobile-layout'));
      expect(mobileContainer).toBeTruthy();
    });

    it('should have desktop-friendly layout', () => {
      const desktopContainer = fixture.debugElement.query(By.css('.desktop-layout'));
      expect(desktopContainer).toBeTruthy();
    });
  });

  describe('Interactions utilisateur', () => {
    beforeEach(() => {
      component.draftState = mockDraftBoardState;
      component.currentUserId = 'participant-1';
      fixture.detectChanges();
    });

    it('should call selectPlayer when select button is clicked', () => {
      spyOn(component, 'selectPlayer');
      const playerCards = fixture.debugElement.queryAll(By.css('.player-card'));
      const selectButton = playerCards[0].query(By.css('.select-button'));
      
      selectButton.nativeElement.click();
      
      expect(component.selectPlayer).toHaveBeenCalledWith(mockDraftBoardState.availablePlayers[0]);
    });

    it('should call onBack when back button is clicked', () => {
      spyOn(component, 'onBack');
      const backButton = fixture.debugElement.query(By.css('.back-button'));
      
      backButton.nativeElement.click();
      
      expect(component.onBack).toHaveBeenCalled();
    });

    it('should call pauseDraft when pause button is clicked', () => {
      spyOn(component, 'pauseDraft');
      const pauseButton = fixture.debugElement.query(By.css('.pause-button'));
      
      pauseButton.nativeElement.click();
      
      expect(component.pauseDraft).toHaveBeenCalled();
    });

    it('should call clearFilters when clear button is clicked', () => {
      spyOn(component, 'clearFilters');
      const clearButton = fixture.debugElement.query(By.css('.clear-filters-button'));
      
      clearButton.nativeElement.click();
      
      expect(component.clearFilters).toHaveBeenCalled();
    });

    it('should update search term when search input changes', () => {
      const searchInput = fixture.debugElement.query(By.css('.search-input'));
      searchInput.nativeElement.value = 'pixie';
      searchInput.nativeElement.dispatchEvent(new Event('input'));
      
      expect(component.searchTerm).toBe('pixie');
    });

    it('should update selected region when region select changes', () => {
      const regionSelect = fixture.debugElement.query(By.css('.region-select'));
      regionSelect.nativeElement.value = 'EU';
      regionSelect.nativeElement.dispatchEvent(new Event('change'));
      
      expect(component.selectedRegion).toBe('EU');
    });
  });

  describe('États vides', () => {
    it('should show empty state when no players available', () => {
      component.draftState = { ...mockDraftBoardState, availablePlayers: [] };
      fixture.detectChanges();

      const emptyState = fixture.debugElement.query(By.css('.empty-state'));
      expect(emptyState).toBeTruthy();
      expect(emptyState.nativeElement.textContent).toContain('Aucun joueur disponible');
    });

    it('should show empty state when no participants', () => {
      component.draftState = { ...mockDraftBoardState, participants: [] };
      fixture.detectChanges();

      const emptyState = fixture.debugElement.query(By.css('.empty-state'));
      expect(emptyState).toBeTruthy();
      expect(emptyState.nativeElement.textContent).toContain('Aucun participant');
    });
  });

  describe('Accessibilité', () => {
    beforeEach(() => {
      component.draftState = mockDraftBoardState;
      fixture.detectChanges();
    });

    it('should have proper ARIA labels', () => {
      const searchInput = fixture.debugElement.query(By.css('.search-input'));
      expect(searchInput.nativeElement.getAttribute('aria-label')).toBeTruthy();
    });

    it('should have proper button roles', () => {
      const buttons = fixture.debugElement.queryAll(By.css('button'));
      buttons.forEach(button => {
        expect(button.nativeElement.getAttribute('role')).toBeTruthy();
      });
    });

    it('should have proper heading structure', () => {
      const headings = fixture.debugElement.queryAll(By.css('h1, h2, h3, h4, h5, h6'));
      expect(headings.length).toBeGreaterThan(0);
    });
  });
}); 