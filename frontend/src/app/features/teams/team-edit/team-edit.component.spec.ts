import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { of } from 'rxjs';

import { TeamEditComponent } from './team-edit.component';
import { TeamService } from '../../../core/services/team.service';
import { TranslationService } from '../../../core/services/translation.service';
import { UiErrorFeedbackService } from '../../../core/services/ui-error-feedback.service';

describe('TeamEditComponent', () => {
  let component: TeamEditComponent;
  let fixture: ComponentFixture<TeamEditComponent>;
  let router: jasmine.SpyObj<Router>;
  let uiFeedback: jasmine.SpyObj<UiErrorFeedbackService>;
  let teamService: jasmine.SpyObj<TeamService>;
  let translationService: jasmine.SpyObj<TranslationService>;
  let activatedRoute: { params: any };

  beforeEach(async () => {
    router = jasmine.createSpyObj('Router', ['navigate']);
    uiFeedback = jasmine.createSpyObj('UiErrorFeedbackService', [
      'showErrorFromKey',
      'showSuccessWithAction',
      'showInfoWithAction',
      'showSuccessFromKey'
    ]);
    teamService = jasmine.createSpyObj('TeamService', ['getTeam', 'updateTeam']);
    translationService = jasmine.createSpyObj('TranslationService', ['t']);
    translationService.t.and.callFake((key: string) => key);

    activatedRoute = {
      params: of({ id: '1', gameId: 'game1' })
    };

    await TestBed.configureTestingModule({
      imports: [TeamEditComponent, ReactiveFormsModule]
    })
      .overrideProvider(Router, { useValue: router })
      .overrideProvider(ActivatedRoute, { useValue: activatedRoute })
      .overrideProvider(UiErrorFeedbackService, { useValue: uiFeedback })
      .overrideProvider(TeamService, { useValue: teamService })
      .overrideProvider(TranslationService, { useValue: translationService })
      .compileComponents();

    fixture = TestBed.createComponent(TeamEditComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize form with validators', () => {
    expect(component.teamForm).toBeDefined();
    expect(component.teamForm.get('name')?.hasError('required')).toBeTrue();
  });

  it('should initialize with loading state', () => {
    expect(component.loading).toBeTrue();
    expect(component.saving).toBeFalse();
    expect(component.team).toBeNull();
  });

  it('should load team from route params', fakeAsync(() => {
    fixture.detectChanges();
    tick();

    expect(component.teamId).toBe('1');
    expect(component.gameId).toBe('game1');
    expect(component.loading).toBeTrue();
  }));

  it('should load mock team data after timeout', fakeAsync(() => {
    fixture.detectChanges();
    tick(800);

    expect(component.loading).toBeFalse();
    expect(component.team).not.toBeNull();
    expect(component.team?.name).toBe('Équipe Thibaut');
    expect(component.players.length).toBe(5);
    expect(component.teamForm.value.name).toBe('Équipe Thibaut');
  }));

  it('should not load team if no teamId in route', fakeAsync(() => {
    activatedRoute.params = of({});
    fixture.detectChanges();
    tick();

    expect(component.teamId).toBeNull();
    expect(component.loading).toBeFalse();
    expect(component.team).toBeNull();
  }));

  it('should show error when saving invalid form', () => {
    component.teamForm.get('name')?.setValue('');

    component.onSave();

    expect(uiFeedback.showErrorFromKey).toHaveBeenCalledWith('teams.edit.snackbar.formInvalid', 3000);
    expect(component.saving).toBeFalse();
  });

  it('should save valid form', fakeAsync(() => {
    component.teamForm.get('name')?.setValue('New Team Name');
    component.teamForm.get('description')?.setValue('Description');

    component.onSave();

    expect(component.saving).toBeTrue();
    tick(1000);

    expect(component.saving).toBeFalse();
    expect(uiFeedback.showSuccessWithAction).toHaveBeenCalledWith(
      'teams.edit.snackbar.updated',
      'teams.edit.snackbar.viewAction',
      jasmine.any(Function),
      3000
    );
  }));

  it('should navigate back on save action click', fakeAsync(() => {
    component.teamForm.get('name')?.setValue('Valid Name');
    component.gameId = 'game1';

    component.onSave();
    tick(1000);

    const onAction = uiFeedback.showSuccessWithAction.calls.mostRecent().args[2] as () => void;
    onAction();

    expect(router.navigate).toHaveBeenCalledWith(['/games', 'game1', 'teams']);
  }));

  it('should navigate back on cancel', () => {
    component.gameId = 'game1';

    component.onCancel();

    expect(router.navigate).toHaveBeenCalledWith(['/games', 'game1', 'teams']);
  });

  it('should navigate to games if no gameId', () => {
    component.gameId = null;

    component.goBack();

    expect(router.navigate).toHaveBeenCalledWith(['/games']);
  });

  it('should remove player and show undo option', fakeAsync(() => {
    const player = { id: '1', nickname: 'Mero', region: 'EU', tranche: 'T1', points: 100000 };
    const translations: Record<string, string> = {
      'teams.edit.snackbar.playerRemoved': '{player} retire',
      'common.cancel': 'common.cancel'
    };
    translationService.t.and.callFake((key: string) => translations[key] || key);

    fixture.detectChanges();
    tick(1000);

    component.players = [player];
    component.removePlayer(player);

    expect(component.players.length).toBe(0);
    expect(uiFeedback.showInfoWithAction).toHaveBeenCalled();
  }));

  it('should restore player on undo', fakeAsync(() => {
    const player = { id: '1', nickname: 'Mero', region: 'EU', tranche: 'T1', points: 100000 };
    const translations: Record<string, string> = {
      'teams.edit.snackbar.playerRemoved': '{player} retire',
      'common.cancel': 'common.cancel',
      'teams.edit.snackbar.playerRestored': 'teams.edit.snackbar.playerRestored',
      'common.close': 'common.close'
    };
    translationService.t.and.callFake((key: string) => translations[key] || key);

    fixture.detectChanges();
    tick(1000);

    component.players = [player];
    component.removePlayer(player);
    expect(component.players.length).toBe(0);

    const undoAction = uiFeedback.showInfoWithAction.calls.mostRecent().args[2] as () => void;
    undoAction();
    tick();

    expect(component.players.length).toBe(1);
    expect(component.players[0]).toBe(player);
    expect(uiFeedback.showSuccessFromKey).toHaveBeenCalledWith('teams.edit.snackbar.playerRestored', 2000);
  }));

  it('should return correct region color', () => {
    expect(component.getRegionColor('EU')).toBe('#4CAF50');
    expect(component.getRegionColor('NAW')).toBe('#2196F3');
    expect(component.getRegionColor('BR')).toBe('#FFD700');
    expect(component.getRegionColor('UNKNOWN')).toBe('#757575');
  });

  it('should return correct tranche label', () => {
    expect(component.getTrancheLabel('T1')).toBe('Tier 1 - Elite');
    expect(component.getTrancheLabel('T2')).toBe('Tier 2 - Pro');
    expect(component.getTrancheLabel('T5')).toBe('Tier 5 - Rookie');
    expect(component.getTrancheLabel('UNKNOWN')).toBe('UNKNOWN');
  });

  it('should format points correctly', () => {
    const formatted = component.formatPoints(123456);
    expect(formatted).toBeDefined();
    expect(typeof formatted).toBe('string');
  });

  it('should return name required error', () => {
    component.teamForm.get('name')?.setValue('');
    component.teamForm.get('name')?.markAsTouched();

    expect(component.nameError).toBe('teams.edit.validation.nameRequired');
  });

  it('should return name minlength error', () => {
    component.teamForm.get('name')?.setValue('ab');
    component.teamForm.get('name')?.markAsTouched();

    expect(component.nameError).toBe('teams.edit.validation.nameMinLength');
  });

  it('should return name maxlength error', () => {
    component.teamForm.get('name')?.setValue('a'.repeat(51));
    component.teamForm.get('name')?.markAsTouched();

    expect(component.nameError).toBe('teams.edit.validation.nameMaxLength');
  });

  it('should return roster subtitle for single player', () => {
    component.players = [{ id: '1', nickname: 'Player', region: 'EU', tranche: 'T1' }];
    translationService.t.and.returnValue('{count} joueur');

    const subtitle = component.getRosterSubtitle();

    expect(subtitle).toContain('1');
  });

  it('should return roster subtitle for multiple players', () => {
    component.players = [
      { id: '1', nickname: 'Player1', region: 'EU', tranche: 'T1' },
      { id: '2', nickname: 'Player2', region: 'NAW', tranche: 'T2' }
    ];
    translationService.t.and.returnValue('{count} joueurs');

    const subtitle = component.getRosterSubtitle();

    expect(subtitle).toContain('2');
  });

  it('should get remove player tooltip', () => {
    translationService.t.and.returnValue('Retirer {nickname}');

    const tooltip = component.getRemovePlayerTooltip('Mero');

    expect(tooltip).toContain('Mero');
  });

  it('should track players by id', () => {
    const player = { id: 'player1', nickname: 'Test', region: 'EU', tranche: 'T1' };

    const result = component.trackByPlayerId(0, player);

    expect(result).toBe('player1');
  });

  it('should unsubscribe on destroy', () => {
    const destroySpy = spyOn(component['destroy$'], 'next');
    const completeSpy = spyOn(component['destroy$'], 'complete');

    component.ngOnDestroy();

    expect(destroySpy).toHaveBeenCalled();
    expect(completeSpy).toHaveBeenCalled();
  });
});
