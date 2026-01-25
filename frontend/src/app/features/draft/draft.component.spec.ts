import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { DraftComponent } from './draft.component';
import { DraftService } from './services/draft.service';
import { UserContextService, UserProfile } from '../../core/services/user-context.service';
import { DraftBoardState, Player, GameParticipant, DraftStatus, PlayerRegion } from './models/draft.interface';
describe('DraftComponent', () => {
  let component: DraftComponent;
  let fixture: ComponentFixture<DraftComponent>;
  let draftService: jasmine.SpyObj<DraftService>;
  let userContextService: jasmine.SpyObj<UserContextService>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;
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
    const snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    const dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);
    draftServiceSpy.getDraftBoardState.and.returnValue(of(mockDraftState));
    userContextSpy.getCurrentUser.and.returnValue(mockUser);
    await TestBed.configureTestingModule({
      imports: [DraftComponent, NoopAnimationsModule],
      providers: [
        { provide: DraftService, useValue: draftServiceSpy },
        { provide: UserContextService, useValue: userContextSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
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
    snackBar = TestBed.inject(MatSnackBar) as jasmine.SpyObj<MatSnackBar>;
    dialog = TestBed.inject(MatDialog) as jasmine.SpyObj<MatDialog>;
    // État de base pour autoriser les actions dans les tests
    component.gameId = '1';
    component.currentUserId = '1';
    component.draftState = mockDraftState;
    spyOn(window, 'confirm').and.returnValue(false);
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
  describe('confirmCancel', () => {
    it('should ask confirmation before draft cancellation', () => {
      component.confirmCancel();
      
      expect(window.confirm).toHaveBeenCalled();
    });
  });
  describe('getRegionLabel', () => {
    it('should return correct region label for EU', () => {
      const result = component.getRegionLabel('EU' as PlayerRegion);
      expect(result).toBe('Europe');
    });
    it('should return correct region label for NAW', () => {
      const result = component.getRegionLabel('NAW' as PlayerRegion);
      expect(result).toBe('Nord-Amérique Ouest');
    });
    it('should return region code for unknown region', () => {
      const result = component.getRegionLabel('UNKNOWN' as PlayerRegion);
      expect(result).toBe('UNKNOWN');
    });
  });
  describe('getTrancheLabel', () => {
    it('should return correct tranche label for T1', () => {
      const result = component.getTrancheLabel('T1');
      expect(result).toBe('Tranche 1');
    });
    it('should return correct tranche label for T2', () => {
      const result = component.getTrancheLabel('T2');
      expect(result).toBe('Tranche 2');
    });
    it('should return tranche code for unknown tranche', () => {
      const result = component.getTrancheLabel('UNKNOWN');
      expect(result).toBe('UNKNOWN');
    });
  });
  describe('getStatusColor', () => {
    it('should return correct color for ACTIVE status', () => {
      const result = component.getStatusColor('ACTIVE' as DraftStatus);
      expect(result).toBe('accent');
    });
    it('should return correct color for PAUSED status', () => {
      const result = component.getStatusColor('PAUSED' as DraftStatus);
      expect(result).toBe('warn');
    });
  });
  describe('getStatusLabel', () => {
    it('should return correct label for ACTIVE status', () => {
      const result = component.getStatusLabel('ACTIVE' as DraftStatus);
      expect(result).toBe('En cours');
    });
    it('should return correct label for PAUSED status', () => {
      const result = component.getStatusLabel('PAUSED' as DraftStatus);
      expect(result).toBe('En pause');
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
  describe('getRegionQuotas', () => {
    it('should return region quotas from draft state', () => {
      component.draftState = mockDraftState;
      
      const result = component.getRegionQuotas();
      
      expect(result).toEqual([
        { region: 'EU', limit: 2 },
        { region: 'NAW', limit: 2 },
        { region: 'ASIA', limit: 1 }
      ]);
    });
  });
});
