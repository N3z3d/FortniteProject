import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { CreateGameComponent } from '../../create-game/create-game.component';
import { GameService } from '../../services/game.service';

describe('CreateGameComponent', () => {
  let component: CreateGameComponent;
  let fixture: ComponentFixture<CreateGameComponent>;
  let gameService: jasmine.SpyObj<GameService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    const gameServiceSpy = jasmine.createSpyObj('GameService', ['createGame']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [
        ReactiveFormsModule,
        MatCardModule,
        MatFormFieldModule,
        MatInputModule,
        MatButtonModule,
        MatSelectModule,
        MatChipsModule,
        MatSnackBarModule,
        BrowserAnimationsModule
      ],
      declarations: [CreateGameComponent],
      providers: [
        { provide: GameService, useValue: gameServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    gameService = TestBed.inject(GameService) as jasmine.SpyObj<GameService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CreateGameComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize form with default values', () => {
    expect(component.gameForm).toBeTruthy();
    expect(component.gameForm.get('name')?.value).toBe('');
    expect(component.gameForm.get('maxParticipants')?.value).toBe(5);
    expect(component.gameForm.get('regionRules')?.value).toEqual({});
  });

  it('should validate required fields', () => {
    const nameControl = component.gameForm.get('name');
    const maxParticipantsControl = component.gameForm.get('maxParticipants');

    // Test name validation
    nameControl?.setValue('');
    expect(nameControl?.hasError('required')).toBeTruthy();

    nameControl?.setValue('ab'); // Too short
    expect(nameControl?.hasError('minlength')).toBeTruthy();

    nameControl?.setValue('Valid Game Name');
    expect(nameControl?.valid).toBeTruthy();

    // Test maxParticipants validation
    maxParticipantsControl?.setValue(1); // Too low
    expect(maxParticipantsControl?.hasError('min')).toBeTruthy();

    maxParticipantsControl?.setValue(25); // Too high
    expect(maxParticipantsControl?.hasError('max')).toBeTruthy();

    maxParticipantsControl?.setValue(8);
    expect(maxParticipantsControl?.valid).toBeTruthy();
  });

  it('should add region rule successfully', () => {
    component.selectedRegion = 'EU';
    component.selectedCount = 3;

    component.addRegionRule('EU', 3);

    const regionRules = component.gameForm.get('regionRules')?.value;
    expect(regionRules['EU']).toBe(3);
  });

  it('should remove region rule successfully', () => {
    // Add a rule first
    component.addRegionRule('EU', 3);
    expect(component.gameForm.get('regionRules')?.value['EU']).toBe(3);

    // Remove the rule
    component.removeRegionRule('EU');
    expect(component.gameForm.get('regionRules')?.value['EU']).toBeUndefined();
  });

  it('should get available regions', () => {
    const regions = component.getAvailableRegions();
    expect(regions).toContain('EU');
    expect(regions).toContain('NAC');
    expect(regions).toContain('BR');
    expect(regions).toContain('ASIA');
    expect(regions).toContain('OCE');
    expect(regions).toContain('NAW');
    expect(regions).toContain('ME');
  });

  it('should get region label correctly', () => {
    expect(component.getRegionLabel('EU')).toBe('Europe');
    expect(component.getRegionLabel('NAC')).toBe('North America Central');
    expect(component.getRegionLabel('BR')).toBe('Brazil');
    expect(component.getRegionLabel('ASIA')).toBe('Asia');
    expect(component.getRegionLabel('OCE')).toBe('Oceania');
    expect(component.getRegionLabel('NAW')).toBe('North America West');
    expect(component.getRegionLabel('ME')).toBe('Middle East');
    expect(component.getRegionLabel('UNKNOWN')).toBe('UNKNOWN');
  });

  it('should calculate total region players correctly', () => {
    component.addRegionRule('EU', 3);
    component.addRegionRule('NAC', 2);

    const total = component.getTotalRegionPlayers();
    expect(total).toBe(5);
  });

  it('should check if can add region rule', () => {
    component.gameForm.get('maxParticipants')?.setValue(5);
    
    // Add 3 players to EU
    component.addRegionRule('EU', 3);
    expect(component.canAddRegionRule()).toBeTruthy();

    // Add 2 more players to NAC
    component.addRegionRule('NAC', 2);
    expect(component.canAddRegionRule()).toBeFalsy(); // Total = 5, max = 5
  });

  it('should calculate remaining slots correctly', () => {
    component.gameForm.get('maxParticipants')?.setValue(10);
    component.addRegionRule('EU', 3);
    component.addRegionRule('NAC', 2);

    const remaining = component.getRemainingSlots();
    expect(remaining).toBe(5); // 10 - 3 - 2 = 5
  });

  it('should create game successfully', () => {
    const mockGame = {
      id: 'test-id',
      name: 'Test Game',
      creatorName: 'Test User',
      maxParticipants: 5,
      status: 'CREATING' as any,
      createdAt: new Date().toISOString(),
      participantCount: 1,
      canJoin: true
    };

    gameService.createGame.and.returnValue({
      subscribe: (callbacks: any) => {
        callbacks.next(mockGame);
        return { unsubscribe: () => {} };
      }
    } as any);

    component.gameForm.patchValue({
      name: 'Test Game',
      maxParticipants: 5
    });

    component.onSubmit();

    expect(gameService.createGame).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/games', 'test-id']);
  });

  it('should handle game creation error', () => {
    gameService.createGame.and.returnValue({
      subscribe: (callbacks: any) => {
        callbacks.error(new Error('Creation failed'));
        return { unsubscribe: () => {} };
      }
    } as any);

    component.gameForm.patchValue({
      name: 'Test Game',
      maxParticipants: 5
    });

    component.onSubmit();

    expect(component.error).toBe('Erreur lors de la création de la game');
    expect(component.loading).toBeFalsy();
  });

  it('should not submit if form is invalid', () => {
    component.gameForm.patchValue({
      name: '', // Invalid - empty
      maxParticipants: 5
    });

    component.onSubmit();

    expect(gameService.createGame).not.toHaveBeenCalled();
  });

  it('should navigate back on cancel', () => {
    component.onCancel();

    expect(router.navigate).toHaveBeenCalledWith(['/games']);
  });

  it('should get region rules entries', () => {
    component.addRegionRule('EU', 3);
    component.addRegionRule('NAC', 2);

    const entries = component.getRegionRulesEntries();
    expect(entries).toContain(['EU', 3]);
    expect(entries).toContain(['NAC', 2]);
  });
}); 