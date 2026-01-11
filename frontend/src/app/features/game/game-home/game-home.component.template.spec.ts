import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { BehaviorSubject, of } from 'rxjs';

import { GameHomeComponent } from './game-home.component';
import { GameService } from '../services/game.service';
import { UserContextService, UserProfile } from '../../../core/services/user-context.service';
import { GameSelectionService } from '../../../core/services/game-selection.service';
import { UserGamesState, UserGamesStore } from '../../../core/services/user-games.store';
import { Game } from '../models/game.interface';

describe('GameHomeComponent (template)', () => {
  let component: GameHomeComponent;
  let fixture: ComponentFixture<GameHomeComponent>;
  let gameService: jasmine.SpyObj<GameService>;
  let userContextService: jasmine.SpyObj<UserContextService>;
  let gameSelectionService: jasmine.SpyObj<GameSelectionService>;
  let userGamesStore: jasmine.SpyObj<UserGamesStore>;
  let router: jasmine.SpyObj<Router>;
  let stateSubject: BehaviorSubject<UserGamesState>;

  const mockUser: UserProfile = {
    id: '1',
    username: 'Thibaut',
    email: 'thibaut@example.com'
  };

  const mockGames: Game[] = [
    {
      id: '1',
      name: 'Championnat Saison 1',
      creatorName: 'Thibaut',
      maxParticipants: 49,
      status: 'CREATING',
      createdAt: '2024-01-15T10:30:00',
      participantCount: 3,
      canJoin: true
    },
    {
      id: '2',
      name: 'Draft League',
      creatorName: 'Marcel',
      maxParticipants: 20,
      status: 'DRAFTING',
      createdAt: '2024-01-14T15:45:00',
      participantCount: 5,
      canJoin: false
    },
    {
      id: '3',
      name: 'Game Active',
      creatorName: 'Sarah',
      maxParticipants: 20,
      status: 'ACTIVE',
      createdAt: '2024-01-13T10:00:00',
      participantCount: 8,
      canJoin: false
    }
  ];

  const initialState: UserGamesState = {
    games: [],
    loading: false,
    error: null,
    lastLoaded: null
  };

  beforeEach(async () => {
    gameService = jasmine.createSpyObj('GameService', ['getAvailableGames']);
    userContextService = jasmine.createSpyObj('UserContextService', ['getCurrentUser', 'logout']);
    gameSelectionService = jasmine.createSpyObj('GameSelectionService', ['setSelectedGame']);
    router = jasmine.createSpyObj('Router', ['navigate']);

    stateSubject = new BehaviorSubject<UserGamesState>(initialState);
    userGamesStore = jasmine.createSpyObj<UserGamesStore>(
      'UserGamesStore',
      ['loadGames', 'refreshGames'],
      { state$: stateSubject.asObservable() }
    );

    userContextService.getCurrentUser.and.returnValue(mockUser);
    gameService.getAvailableGames.and.returnValue(of([]));
    userGamesStore.loadGames.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [GameHomeComponent, NoopAnimationsModule],
      providers: [
        { provide: GameService, useValue: gameService },
        { provide: UserContextService, useValue: userContextService },
        { provide: GameSelectionService, useValue: gameSelectionService },
        { provide: UserGamesStore, useValue: userGamesStore },
        { provide: Router, useValue: router }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(GameHomeComponent);
    component = fixture.componentInstance;
  });

  it('should render premium empty state when user has no games', () => {
    component.ngOnInit();
    stateSubject.next({ ...initialState, games: [] });
    fixture.detectChanges();

    const emptyState = fixture.nativeElement.querySelector('.premium-empty-state');
    const createButton = fixture.nativeElement.querySelector('.gaming-cta-btn');
    const joinButton = fixture.nativeElement.querySelector('.gaming-secondary-btn');

    expect(emptyState).toBeTruthy();
    expect(createButton).toBeTruthy();
    expect(joinButton).toBeTruthy();
  });

  it('should render one card per game and select the first by default', () => {
    component.ngOnInit();
    stateSubject.next({ ...initialState, games: mockGames });
    fixture.detectChanges();

    const cards: NodeListOf<HTMLElement> = fixture.nativeElement.querySelectorAll('.premium-game-card');
    const selectedCard = fixture.nativeElement.querySelector('.premium-game-card.selected');

    expect(cards.length).toBe(3);
    expect(selectedCard).toBeTruthy();
    expect(component.selectedGame?.id).toBe('1');
  });

  it('should not let the premium card overlay capture pointer events', () => {
    component.ngOnInit();
    stateSubject.next({ ...initialState, games: mockGames });
    fixture.detectChanges();

    const card: HTMLElement | null = fixture.nativeElement.querySelector('.premium-game-card');
    expect(card).toBeTruthy();

    const overlayStyles = window.getComputedStyle(card!, '::after');
    expect(overlayStyles.pointerEvents).toBe('none');
  });

  it('should navigate to game details when clicking "Voir les détails"', () => {
    component.ngOnInit();
    stateSubject.next({ ...initialState, games: mockGames });
    fixture.detectChanges();

    const detailButtons: NodeListOf<HTMLButtonElement> = fixture.nativeElement.querySelectorAll('.detail-btn');
    expect(detailButtons.length).toBe(3);

    detailButtons[0].click();

    expect(router.navigate).toHaveBeenCalledWith(['/games', '1']);
    expect(gameSelectionService.setSelectedGame).toHaveBeenCalledWith(mockGames[0]);
  });

  it('should navigate to game details when clicking the visible details button (hit-testing)', () => {
    component.ngOnInit();
    stateSubject.next({ ...initialState, games: mockGames });
    fixture.detectChanges();

    const detailButton: HTMLButtonElement | null = fixture.nativeElement.querySelector('.detail-btn');
    expect(detailButton).toBeTruthy();
    detailButton?.scrollIntoView();

    const rect = detailButton!.getBoundingClientRect();
    const clientX = rect.left + rect.width / 2;
    const clientY = rect.top + rect.height / 2;
    const target = document.elementFromPoint(clientX, clientY) as HTMLElement | null;

    expect(target).toBeTruthy();
    expect(detailButton!.contains(target!)).toBeTrue();

    target!.click();

    expect(router.navigate).toHaveBeenCalledWith(['/games', '1']);
    expect(gameSelectionService.setSelectedGame).toHaveBeenCalledWith(mockGames[0]);
  });

  it('should navigate to draft when clicking "Entrer dans la draft"', () => {
    component.ngOnInit();
    stateSubject.next({ ...initialState, games: mockGames });
    fixture.detectChanges();

    const draftButton: HTMLButtonElement | null = fixture.nativeElement.querySelector('.draft-btn');
    expect(draftButton).toBeTruthy();

    draftButton?.click();

    expect(router.navigate).toHaveBeenCalledWith(['/games', '2', 'draft']);
  });

  it('should navigate to dashboard when clicking "Tableau de bord"', () => {
    component.ngOnInit();
    stateSubject.next({ ...initialState, games: mockGames });
    fixture.detectChanges();

    const dashboardButton: HTMLButtonElement | null = fixture.nativeElement.querySelector('.dashboard-btn');
    expect(dashboardButton).toBeTruthy();

    dashboardButton?.click();

    expect(router.navigate).toHaveBeenCalledWith(['/games', '3', 'dashboard']);
  });

  it('should navigate to dashboard when clicking the visible dashboard button (hit-testing)', () => {
    component.ngOnInit();
    stateSubject.next({ ...initialState, games: mockGames });
    fixture.detectChanges();

    const dashboardButton: HTMLButtonElement | null = fixture.nativeElement.querySelector('.dashboard-btn');
    expect(dashboardButton).toBeTruthy();
    dashboardButton?.scrollIntoView();

    const rect = dashboardButton!.getBoundingClientRect();
    const clientX = rect.left + rect.width / 2;
    const clientY = rect.top + rect.height / 2;
    const target = document.elementFromPoint(clientX, clientY) as HTMLElement | null;

    expect(target).toBeTruthy();
    expect(dashboardButton!.contains(target!)).toBeTrue();

    target!.click();

    expect(router.navigate).toHaveBeenCalledWith(['/games', '3', 'dashboard']);
  });
});
