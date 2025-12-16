import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { CreateGameComponent } from './create-game.component';
import { GameService } from '../services/game.service';
import { CreateGameRequest } from '../models/game.interface';

describe('CreateGameComponent', () => {
  let component: CreateGameComponent;
  let fixture: ComponentFixture<CreateGameComponent>;
  let gameService: jasmine.SpyObj<GameService>;
  let router: jasmine.SpyObj<Router>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;

  const mockGame = {
    id: '1',
    name: 'Test Game',
    creatorName: 'Thibaut',
    maxParticipants: 49,
    status: 'CREATING',
    createdAt: new Date().toISOString(),
    participantCount: 1,
    canJoin: true
  };

  beforeEach(async () => {
    const gameServiceSpy = jasmine.createSpyObj('GameService', ['createGame']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    const snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    gameServiceSpy.createGame.and.returnValue(of(mockGame));

    TestBed.configureTestingModule({
      imports: [
        CreateGameComponent,
        ReactiveFormsModule,
        FormsModule,
        NoopAnimationsModule
      ],
      providers: [
        { provide: GameService, useValue: gameServiceSpy },
        { provide: Router, useValue: routerSpy },
      ]
    });

    TestBed.overrideProvider(MatSnackBar, { useValue: snackBarSpy });
    await TestBed.compileComponents();

    fixture = TestBed.createComponent(CreateGameComponent);
    component = fixture.componentInstance;
    gameService = TestBed.inject(GameService) as jasmine.SpyObj<GameService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    snackBar = TestBed.inject(MatSnackBar) as jasmine.SpyObj<MatSnackBar>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should initialize form with default configuration', () => {
      component.ngOnInit();
      
      expect(component.gameForm).toBeDefined();
      expect(component.useDefaultConfig).toBe(true);
      expect(component.gameForm.get('name')?.value).toBe('');
      expect(component.gameForm.get('maxParticipants')?.value).toBe(5);
      expect(component.gameForm.get('regionRules')?.value).toEqual({});
    });
  });

  describe('onDefaultConfigChange', () => {
    it('should apply default config when useDefaultConfig is true', () => {
      component.ngOnInit();
      component.useDefaultConfig = true;
      
      component.onDefaultConfigChange();
      
      expect(component.gameForm.get('maxParticipants')?.value).toBe(49);
      expect(component.gameForm.get('regionRules')?.value).toEqual({
        'EU': 7, 'NAC': 7, 'BR': 7, 'ASIA': 7, 'OCE': 7, 'NAW': 7, 'ME': 7
      });
    });

    it('should clear region rules when useDefaultConfig is false', () => {
      component.ngOnInit();
      component.useDefaultConfig = false;
      
      component.onDefaultConfigChange();
      
      expect(component.gameForm.get('maxParticipants')?.value).toBe(5);
      expect(component.gameForm.get('regionRules')?.value).toEqual({});
    });
  });

  describe('onSubmit', () => {
    it('should create game successfully with default configuration', () => {
      component.ngOnInit();
      component.gameForm.patchValue({
        name: 'Test Game',
        maxParticipants: 5,
        regionRules: {}
      });

      component.onSubmit();

      expect(gameService.createGame).toHaveBeenCalledWith(jasmine.objectContaining({
        name: 'Test Game',
        maxParticipants: 5,
        regionRules: {},
        isPrivate: false,
        autoStartDraft: true,
        draftTimeLimit: 300,
        autoPickDelay: 43200,
        currentSeason: 2025,
        description: jasmine.any(String)
      }));
      expect(snackBar.open).toHaveBeenCalledWith(
        jasmine.any(String),
        '',
        jasmine.objectContaining({ duration: 2000, panelClass: 'success-snackbar' })
      );
      expect(router.navigate).toHaveBeenCalledWith(['/games', '1'], { queryParams: { created: 'true' } });
    });

    it('should handle error when creating game fails', () => {
      const error = new Error('Network error');
      gameService.createGame.and.returnValue(throwError(() => error));
      
      component.ngOnInit();
      component.gameForm.patchValue({
        name: 'Test Game',
        maxParticipants: 5,
        regionRules: {}
      });

      component.onSubmit();

      expect(component.error).toContain('Impossible');
      expect(component.loading).toBe(false);
    });

    it('should not submit if form is invalid', () => {
      component.ngOnInit();
      component.gameForm.patchValue({
        name: '', // Invalid: required field empty
        maxParticipants: 49,
        regionRules: { 'EU': 7, 'NAC': 7, 'BR': 7, 'ASIA': 7, 'OCE': 7, 'NAW': 7, 'ME': 7 }
      });

      component.onSubmit();

      expect(gameService.createGame).not.toHaveBeenCalled();
    });
  });

  describe('addRegionRule', () => {
    it('should add region rule and update max participants', () => {
      component.ngOnInit();
      component.useDefaultConfig = false;
      component.gameForm.patchValue({ maxParticipants: 10 });

      component.addRegionRule('EU', 3);

      expect(component.gameForm.get('regionRules')?.value).toEqual({ 'EU': 3 });
      expect(component.gameForm.get('maxParticipants')?.value).toBe(3);
    });

    it('should update total participants when adding multiple rules', () => {
      component.ngOnInit();
      component.useDefaultConfig = false;
      component.gameForm.patchValue({ maxParticipants: 20 });

      component.addRegionRule('EU', 5);
      component.addRegionRule('NAC', 3);

      expect(component.gameForm.get('regionRules')?.value).toEqual({ 'EU': 5, 'NAC': 3 });
      expect(component.gameForm.get('maxParticipants')?.value).toBe(8);
    });
  });

  describe('removeRegionRule', () => {
    it('should remove region rule and update max participants', () => {
      component.ngOnInit();
      component.useDefaultConfig = false;
      component.gameForm.patchValue({
        maxParticipants: 10,
        regionRules: { 'EU': 5, 'NAC': 3 }
      });

      component.removeRegionRule('EU');

      expect(component.gameForm.get('regionRules')?.value).toEqual({ 'NAC': 3 });
      expect(component.gameForm.get('maxParticipants')?.value).toBe(3);
    });

    it('should set minimum max participants to 2 when removing all rules', () => {
      component.ngOnInit();
      component.useDefaultConfig = false;
      component.gameForm.patchValue({
        maxParticipants: 5,
        regionRules: { 'EU': 1 }
      });

      component.removeRegionRule('EU');

      expect(component.gameForm.get('regionRules')?.value).toEqual({});
      expect(component.gameForm.get('maxParticipants')?.value).toBe(2);
    });
  });

  describe('getAvailableRegions', () => {
    it('should return all available regions', () => {
      const regions = component.getAvailableRegions();
      
      expect(regions).toEqual(['EU', 'NAC', 'BR', 'ASIA', 'OCE', 'NAW', 'ME']);
    });
  });

  describe('getRegionLabel', () => {
    it('should return correct region labels', () => {
      expect(component.getRegionLabel('EU')).toBe('Europe');
      expect(component.getRegionLabel('NAC')).toBe('North America Central');
      expect(component.getRegionLabel('BR')).toBe('Brazil');
      expect(component.getRegionLabel('ASIA')).toBe('Asia');
      expect(component.getRegionLabel('OCE')).toBe('Oceania');
      expect(component.getRegionLabel('NAW')).toBe('North America West');
      expect(component.getRegionLabel('ME')).toBe('Middle East');
    });

    it('should return region code for unknown region', () => {
      expect(component.getRegionLabel('UNKNOWN')).toBe('UNKNOWN');
    });
  });

  describe('getDefaultConfigSummary', () => {
    it('should return correct default config summary', () => {
      const summary = component.getDefaultConfigSummary();
      
      expect(summary).toBe('49 joueurs répartis sur 7 régions (7 par région)');
    });
  });

  describe('getRegionRules', () => {
    it('should return current region rules', () => {
      component.ngOnInit();
      component.gameForm.patchValue({
        regionRules: { 'EU': 5, 'NAC': 3 }
      });

      const rules = component.getRegionRules();
      
      expect(rules).toEqual({ 'EU': 5, 'NAC': 3 });
    });
  });

  describe('getRegionRulesEntries', () => {
    it('should return region rules as entries', () => {
      component.ngOnInit();
      component.gameForm.patchValue({
        regionRules: { 'EU': 5, 'NAC': 3 }
      });

      const entries = component.getRegionRulesEntries();
      
      expect(entries).toEqual([['EU', 5], ['NAC', 3]]);
    });
  });

  describe('getTotalRegionPlayers', () => {
    it('should return total number of players from region rules', () => {
      component.ngOnInit();
      component.gameForm.patchValue({
        regionRules: { 'EU': 5, 'NAC': 3, 'BR': 2 }
      });

      const total = component.getTotalRegionPlayers();
      
      expect(total).toBe(10);
    });
  });

  describe('canAddRegionRule', () => {
    it('should return true when slots are available', () => {
      component.ngOnInit();
      component.gameForm.patchValue({
        maxParticipants: 10,
        regionRules: { 'EU': 5 }
      });

      const canAdd = component.canAddRegionRule();
      
      expect(canAdd).toBe(true);
    });

    it('should return false when no slots are available', () => {
      component.ngOnInit();
      component.gameForm.patchValue({
        maxParticipants: 5,
        regionRules: { 'EU': 5 }
      });

      const canAdd = component.canAddRegionRule();
      
      expect(canAdd).toBe(false);
    });
  });

  describe('getRemainingSlots', () => {
    it('should return correct number of remaining slots', () => {
      component.ngOnInit();
      component.gameForm.patchValue({
        maxParticipants: 10,
        regionRules: { 'EU': 5, 'NAC': 2 }
      });

      const remaining = component.getRemainingSlots();
      
      expect(remaining).toBe(3);
    });

    it('should return 0 when no slots remaining', () => {
      component.ngOnInit();
      component.gameForm.patchValue({
        maxParticipants: 5,
        regionRules: { 'EU': 5 }
      });

      const remaining = component.getRemainingSlots();
      
      expect(remaining).toBe(0);
    });
  });

  describe('onCancel', () => {
    it('should navigate back to games', () => {
      component.onCancel();
      
      expect(router.navigate).toHaveBeenCalledWith(['/games']);
    });
  });
});
