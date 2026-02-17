import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { GameCreationWizardComponent } from './game-creation-wizard.component';
import { GameService } from '../services/game.service';
import { TranslationService } from '../../../core/services/translation.service';
import { CreateGameRequest, Game } from '../models/game.interface';
import { UiErrorFeedbackService } from '../../../core/services/ui-error-feedback.service';
import { LoggerService } from '../../../core/services/logger.service';

describe('GameCreationWizardComponent', () => {
  let component: GameCreationWizardComponent;
  let fixture: ComponentFixture<GameCreationWizardComponent>;
  let router: jasmine.SpyObj<Router>;
  let gameService: jasmine.SpyObj<GameService>;
  let uiFeedback: jasmine.SpyObj<UiErrorFeedbackService>;
  let translationService: jasmine.SpyObj<TranslationService>;
  let logger: jasmine.SpyObj<LoggerService>;

  const mockGame: Game = {
    id: 'game-123',
    name: 'Test Game',
    description: 'Test Description',
    creatorName: 'Thibaut',
    maxParticipants: 5,
    status: 'DRAFT',
    createdAt: new Date().toISOString(),
    participantCount: 1,
    canJoin: true
  };

  beforeEach(async () => {
    router = jasmine.createSpyObj('Router', ['navigate']);
    gameService = jasmine.createSpyObj('GameService', ['createGame']);
    uiFeedback = jasmine.createSpyObj('UiErrorFeedbackService', ['showSuccessMessage', 'showErrorMessage']);
    translationService = jasmine.createSpyObj('TranslationService', ['t']);
    logger = jasmine.createSpyObj('LoggerService', ['debug', 'info', 'warn', 'error']);

    translationService.t.and.callFake((key: string) => {
      const translations: Record<string, string> = {
        'games.wizard.minutesPerPick': '{n} minutes per pick',
        'games.wizard.smartDesc.quickPlay': 'Quick play game on {date} at {time}',
        'games.wizard.smartDesc.championship': 'Championship starting {date}',
        'games.wizard.smartDesc.casual': 'Casual game on {date}',
        'games.wizard.smartDesc.custom': 'Custom game on {date} at {time}',
        'games.wizard.successCreated': 'Game {name} created successfully',
        'games.wizard.viewGame': 'View Game'
      };
      return translations[key] || key;
    });

    await TestBed.configureTestingModule({
      imports: [GameCreationWizardComponent, ReactiveFormsModule]
    })
    .overrideProvider(Router, { useValue: router })
    .overrideProvider(GameService, { useValue: gameService })
    .overrideProvider(UiErrorFeedbackService, { useValue: uiFeedback })
    .overrideProvider(TranslationService, { useValue: translationService })
    .overrideProvider(LoggerService, { useValue: logger })
    .compileComponents();

    fixture = TestBed.createComponent(GameCreationWizardComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with default state', () => {
    expect(component.loading).toBeFalse();
    expect(component.error).toBeNull();
    expect(component.selectedTemplate).toBeNull();
  });

  it('should initialize forms on ngOnInit', () => {
    fixture.detectChanges();

    expect(component.basicInfoForm).toBeDefined();
    expect(component.rulesForm).toBeDefined();
    expect(component.reviewForm).toBeDefined();
  });

  it('should set default template on init', () => {
    fixture.detectChanges();

    expect(component.selectedTemplate).toBeTruthy();
    expect(component.selectedTemplate?.id).toBe('quick-play');
  });

  it('should return game templates', () => {
    fixture.detectChanges();
    const templates = component.getGameTemplates();

    expect(templates.length).toBe(4);
    expect(templates[0].id).toBe('quick-play');
    expect(templates[1].id).toBe('championship');
    expect(templates[2].id).toBe('casual');
    expect(templates[3].id).toBe('custom');
  });

  it('should filter popular templates', () => {
    fixture.detectChanges();
    const popular = component.getPopularTemplates();

    expect(popular.length).toBe(2);
    expect(popular.every(t => t.popular)).toBeTrue();
  });

  it('should update forms when template is selected', () => {
    fixture.detectChanges();
    const template = component.getGameTemplates()[1]; // championship

    component.onTemplateSelect(template);

    expect(component.selectedTemplate).toBe(template);
    expect(component.basicInfoForm.value.template).toBe('championship');
    expect(component.rulesForm.value.maxParticipants).toBe(10);
    expect(component.rulesForm.value.draftTimeLimit).toBe(300);
  });

  it('should generate smart description for templates', () => {
    fixture.detectChanges();
    const template = component.getGameTemplates()[0];

    component.onTemplateSelect(template);

    const description = component.basicInfoForm.value.description;
    expect(description).toBeTruthy();
    expect(description.length).toBeGreaterThan(0);
  });

  it('should validate basic info form', () => {
    fixture.detectChanges();

    expect(component.isStep1Valid()).toBeFalse();

    component.basicInfoForm.patchValue({ name: 'Test Game' });

    expect(component.isStep1Valid()).toBeTrue();
  });

  it('should validate rules form', () => {
    fixture.detectChanges();

    expect(component.isStep2Valid()).toBeTrue();

    component.rulesForm.patchValue({ maxParticipants: 1 }); // Below min

    expect(component.isStep2Valid()).toBeFalse();
  });

  it('should return name validation messages', () => {
    fixture.detectChanges();

    component.basicInfoForm.get('name')?.setValue('');
    component.basicInfoForm.get('name')?.markAsTouched();

    const message = component.getNameValidationMessage();
    expect(message).toBe('games.wizard.nameRequired');
  });

  it('should return participants validation messages', () => {
    fixture.detectChanges();

    component.rulesForm.get('maxParticipants')?.setValue(1);

    const message = component.getParticipantsValidationMessage();
    expect(message).toBe('games.wizard.minParticipants');
  });

  it('should format draft time limit label', () => {
    fixture.detectChanges();

    component.rulesForm.patchValue({ draftTimeLimit: 300 });

    const label = component.getDraftTimeLimitLabel();
    expect(label).toBe('5 minutes per pick');
  });

  it('should format auto pick delay label', () => {
    fixture.detectChanges();

    component.rulesForm.patchValue({ autoPickDelay: 7200 });

    const label = component.getAutoPickDelayLabel();
    expect(label).toContain('2h');
  });

  it('should calculate estimated duration', () => {
    fixture.detectChanges();

    const preview = component.getGamePreview();
    expect(preview.estimatedDuration).toBeTruthy();
    expect(preview.estimatedDuration).toMatch(/minutes|h/); // Can be in minutes or hours format
  });

  it('should create game preview with all data', () => {
    fixture.detectChanges();

    component.basicInfoForm.patchValue({ name: 'Test Game', description: 'Test Desc' });
    component.rulesForm.patchValue({ maxParticipants: 6 });

    const preview = component.getGamePreview();

    expect(preview.name).toBe('Test Game');
    expect(preview.description).toBe('Test Desc');
    expect(preview.maxParticipants).toBe(6);
    expect(preview.template).toBe(component.selectedTemplate);
  });

  it('should submit valid game and navigate on success', fakeAsync(() => {
    gameService.createGame.and.returnValue(of(mockGame));
    fixture.detectChanges();

    component.basicInfoForm.patchValue({ name: 'Test Game' });

    component.onSubmit();

    tick(); // Wait for observable completion

    expect(gameService.createGame).toHaveBeenCalled();
    expect(component.loading).toBeFalse(); // Loading completed
    expect(uiFeedback.showSuccessMessage).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(
      ['/games', 'game-123'],
      { queryParams: { created: 'true', template: 'quick-play' } }
    );
  }));

  it('should handle game creation error', fakeAsync(() => {
    fixture.detectChanges();

    component.basicInfoForm.patchValue({ name: 'Test Game' });
    gameService.createGame.and.returnValue(throwError(() => new Error('Creation failed')));

    component.onSubmit();

    tick();

    expect(component.loading).toBeFalse();
    expect(component.error).toBe('games.wizard.errorCreate');
    expect(logger.error).toHaveBeenCalledWith(
      'GameCreationWizardComponent: failed to create game',
      jasmine.objectContaining({
        formName: 'Test Game',
        selectedTemplateId: 'quick-play'
      })
    );
    expect(uiFeedback.showErrorMessage).toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  }));

  it('should not submit if forms are invalid', () => {
    fixture.detectChanges();

    component.basicInfoForm.patchValue({ name: '' }); // Invalid

    component.onSubmit();

    expect(gameService.createGame).not.toHaveBeenCalled();
  });

  it('should navigate to games on cancel', () => {
    fixture.detectChanges();

    component.onCancel();

    expect(router.navigate).toHaveBeenCalledWith(['/games']);
  });

  it('should initialize with default quick-play configuration', () => {
    fixture.detectChanges();

    expect(component.rulesForm.value.maxParticipants).toBe(5);
    expect(component.rulesForm.value.draftTimeLimit).toBe(180);
    expect(component.rulesForm.value.isPrivate).toBeFalse();
  });

  it('should validate name length constraints', () => {
    fixture.detectChanges();

    component.basicInfoForm.patchValue({ name: 'A' });
    expect(component.basicInfoForm.get('name')?.hasError('minlength')).toBeTrue();

    component.basicInfoForm.patchValue({ name: 'Valid Name' });
    expect(component.basicInfoForm.get('name')?.valid).toBeTrue();
  });
});
