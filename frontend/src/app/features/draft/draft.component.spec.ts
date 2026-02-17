import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { DraftComponent } from './draft.component';
import { DraftService } from './services/draft.service';
import { UserContextService, UserProfile } from '../../core/services/user-context.service';
import { DraftBoardState, Player, DraftStatus, PlayerRegion } from './models/draft.interface';
import { UiErrorFeedbackService } from '../../core/services/ui-error-feedback.service';
describe('DraftComponent', () => {
  let component: DraftComponent;
  let fixture: ComponentFixture<DraftComponent>;
  let draftService: jasmine.SpyObj<DraftService>;
  let userContextService: jasmine.SpyObj<UserContextService>;
  let uiFeedback: jasmine.SpyObj<UiErrorFeedbackService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  const mockUser: UserProfile = { id: '1', username: 'Thibaut', email: 'thibaut@test.com' };
  const mockDraftState: DraftBoardState = {
    draft: {
      id: 'draft-1',
      gameId: '1',
      status: 'ACTIVE' as DraftStatus,
      currentRound: 1,
      totalRounds: 5,
      currentPick: 1
    },
    participants: [
      {
        id: '1',
        username: 'Thibaut',
        draftOrder: 1,
        isCurrentTurn: true,
        selections: [],
        isCreator: true
      }
    ],
    availablePlayers: [
      {
        id: '1',
        username: 'player1',
        nickname: 'Player1',
        region: 'EU' as PlayerRegion,
        tranche: 'T1',
        currentSeason: 2025,
        selected: false,
        available: true
      }
    ],
    rules: {
      maxPlayersPerTeam: 5,
      timeLimitPerPick: 60,
      autoPickEnabled: true,
      regionQuotas: { 'EU': 2, 'NAW': 2, 'ASIA': 1 }
    },
    lastUpdated: new Date().toISOString()
  };
  beforeEach(async () => {
    const draftServiceSpy = jasmine.createSpyObj('DraftService', [
      'getDraftBoardState', 'makePlayerSelection', 'pauseDraft', 'resumeDraft', 'cancelDraft'
    ]);
    const userContextSpy = jasmine.createSpyObj('UserContextService', ['getCurrentUser']);
    const uiFeedbackSpy = jasmine.createSpyObj('UiErrorFeedbackService', [
      'showSuccessMessage',
      'showErrorMessage'
    ]);
    const dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);
    dialogSpy.open.and.returnValue({ afterClosed: () => of(false) } as any);
    draftServiceSpy.getDraftBoardState.and.returnValue(of(mockDraftState));
    userContextSpy.getCurrentUser.and.returnValue(mockUser);
    await TestBed.configureTestingModule({
      imports: [DraftComponent, NoopAnimationsModule],
      providers: [
        { provide: DraftService, useValue: draftServiceSpy },
        { provide: UserContextService, useValue: userContextSpy },
        { provide: UiErrorFeedbackService, useValue: uiFeedbackSpy },
        { provide: MatDialog, useValue: dialogSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            params: of({ id: '1' }),
            snapshot: { paramMap: convertToParamMap({ id: '1' }) }
          }
        }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(DraftComponent);
    component = fixture.componentInstance;
    draftService = TestBed.inject(DraftService) as jasmine.SpyObj<DraftService>;
    userContextService = TestBed.inject(UserContextService) as jasmine.SpyObj<UserContextService>;
    uiFeedback = TestBed.inject(UiErrorFeedbackService) as jasmine.SpyObj<UiErrorFeedbackService>;
    dialog = TestBed.inject(MatDialog) as jasmine.SpyObj<MatDialog>;
    // État de base pour autoriser les actions dans les tests
    component.gameId = '1';
    component.currentUserId = '1';
    component.draftState = mockDraftState;

  });
  it('should create', () => {
    expect(component).toBeTruthy();
  });
  describe('ngOnInit', () => {
    it('should initialize component and load draft state', () => {
      component.ngOnInit();
      
      expect(draftService.getDraftBoardState).toHaveBeenCalledWith('1');
      expect(component.draftState).toEqual(mockDraftState);
    });
  });
  describe('cancelDraft', () => {
    it('should open confirmation dialog before draft cancellation', () => {
      component.cancelDraft();

      expect(dialog.open).toHaveBeenCalled();
    });
  });
  describe('selectPlayer', () => {
    it('should select player successfully', () => {
      const player: Player = {
        id: '1',
        username: 'player1',
        nickname: 'Player1',
        region: 'EU' as PlayerRegion,
        tranche: 'T1',
        currentSeason: 2025,
        selected: false,
        available: true
      };
      draftService.makePlayerSelection.and.returnValue(of({} as any));
      component.selectPlayer(player);
      
      expect(draftService.makePlayerSelection).toHaveBeenCalledWith('1', '1');
      expect(uiFeedback.showSuccessMessage).toHaveBeenCalled();
    });

    it('should show error message when selection fails', () => {
      const player: Player = {
        id: '1',
        username: 'player1',
        nickname: 'Player1',
        region: 'EU' as PlayerRegion,
        tranche: 'T1',
        currentSeason: 2025,
        selected: false,
        available: true
      };
      draftService.makePlayerSelection.and.returnValue(throwError(() => new Error('selection failed')));

      component.selectPlayer(player);

      expect(uiFeedback.showErrorMessage).toHaveBeenCalled();
    });
  });
  describe('pauseDraft', () => {
    it('should pause draft successfully', () => {
      draftService.pauseDraft.and.returnValue(of(true));
      component.pauseDraft();
      
      expect(draftService.pauseDraft).toHaveBeenCalledWith('1');
    });
  });
  describe('resumeDraft', () => {
    it('should resume draft successfully', () => {
      draftService.resumeDraft.and.returnValue(of(true));
      component.resumeDraft();
      
      expect(draftService.resumeDraft).toHaveBeenCalledWith('1');
    });
  });
  describe('isCurrentUserTurn', () => {
    it('should return true when current user is active', () => {
      component.currentUserId = '1';
      component.draftState = mockDraftState;
      
      const result = component.isCurrentUserTurn();
      
      expect(result).toBe(true);
    });
    it('should return false when current user is not active', () => {
      component.currentUserId = '2';
      component.draftState = mockDraftState;
      
      const result = component.isCurrentUserTurn();
      
      expect(result).toBe(false);
    });
  });
  describe('canSelectPlayer', () => {
    it('should return true when user can select player', () => {
      component.currentUserId = '1';
      component.draftState = mockDraftState;
      component.isSelectingPlayer = false;
      
      const result = component.canSelectPlayer();
      
      expect(result).toBe(true);
    });
    it('should return false when user is selecting player', () => {
      component.currentUserId = '1';
      component.draftState = mockDraftState;
      component.isSelectingPlayer = true;
      
      const result = component.canSelectPlayer();
      
      expect(result).toBe(false);
    });
  });
  describe('getFilteredPlayers', () => {
    it('should return all players when no filters applied', () => {
      component.draftState = mockDraftState;
      component.selectedRegion = 'ALL';
      component.searchTerm = '';
      component.selectedTranche = 'ALL';
      
      const result = component.getFilteredPlayers();
      
      expect(result).toEqual(mockDraftState.availablePlayers);
    });
    it('should filter players by region', () => {
      component.draftState = mockDraftState;
      component.selectedRegion = 'EU';
      component.searchTerm = '';
      component.selectedTranche = 'ALL';
      
      const result = component.getFilteredPlayers();
      
      expect(result.length).toBe(1);
      expect(result[0].region).toBe('EU');
    });
  });
});



