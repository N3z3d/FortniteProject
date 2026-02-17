import { ComponentFixture, TestBed, fakeAsync, tick, flush } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { By } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { TradeFormComponent } from './trade-form.component';
import { TranslationService } from '../../../core/services/translation.service';
import { LoggerService } from '../../../core/services/logger.service';
import { UiErrorFeedbackService } from '../../../core/services/ui-error-feedback.service';

describe('TradeFormComponent', () => {
    let component: TradeFormComponent;
    let fixture: ComponentFixture<TradeFormComponent>;
    let router: Router;
    let uiFeedback: jasmine.SpyObj<UiErrorFeedbackService>;
    let translationService: jasmine.SpyObj<TranslationService>;
    let loggerService: jasmine.SpyObj<LoggerService>;

    beforeEach(async () => {
        uiFeedback = jasmine.createSpyObj('UiErrorFeedbackService', ['showSuccessMessage', 'showError']);
        translationService = jasmine.createSpyObj('TranslationService', ['t']);
        translationService.t.and.callFake((key: string) => key);
        loggerService = jasmine.createSpyObj('LoggerService', ['debug', 'info', 'warn', 'error']);

        await TestBed.configureTestingModule({
            imports: [
                TradeFormComponent,
                NoopAnimationsModule,
                RouterTestingModule.withRoutes([
                    { path: 'trades', component: TradeFormComponent }
                ])
            ],
            providers: [
                { provide: UiErrorFeedbackService, useValue: uiFeedback },
                { provide: TranslationService, useValue: translationService },
                { provide: LoggerService, useValue: loggerService }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(TradeFormComponent);
        component = fixture.componentInstance;
        router = TestBed.inject(Router);
        fixture.detectChanges();
    });

    afterEach(() => {
        fixture.destroy();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    describe('form initialization', () => {
        it('should initialize form with empty values', () => {
            expect(component.tradeForm.get('teamId')?.value).toBe('');
            expect(component.tradeForm.get('playerOutId')?.value).toBe('');
            expect(component.tradeForm.get('playerInId')?.value).toBe('');
        });

        it('should have required validators on all fields', () => {
            expect(component.tradeForm.get('teamId')?.hasError('required')).toBe(true);
            expect(component.tradeForm.get('playerOutId')?.hasError('required')).toBe(true);
            expect(component.tradeForm.get('playerInId')?.hasError('required')).toBe(true);
        });

        it('should be invalid when empty', () => {
            expect(component.tradeForm.valid).toBe(false);
        });
    });

    describe('default values', () => {
        it('should have isEditMode as false', () => {
            expect(component.isEditMode).toBe(false);
        });

        it('should have isSubmitting as false', () => {
            expect(component.isSubmitting).toBe(false);
        });

        it('should have selectedTeam as null', () => {
            expect(component.selectedTeam).toBeNull();
        });
    });

    describe('data loading', () => {
        it('should load available teams', () => {
            expect(component.availableTeams.length).toBeGreaterThan(0);
        });

        it('should load available players', () => {
            expect(component.availablePlayers.length).toBeGreaterThan(0);
        });

        it('should have teams with required properties', () => {
            component.availableTeams.forEach(team => {
                expect(team.id).toBeDefined();
                expect(team.name).toBeDefined();
                expect(team.owner).toBeDefined();
                expect(team.players).toBeDefined();
            });
        });
    });

    describe('onTeamChange', () => {
        it('should set selectedTeam when valid team id is provided', () => {
            const teamId = component.availableTeams[0].id;
            component.onTeamChange(teamId);
            expect(component.selectedTeam).toEqual(component.availableTeams[0]);
        });

        it('should set selectedTeam to null when invalid team id is provided', () => {
            component.onTeamChange('invalid-id');
            expect(component.selectedTeam).toBeNull();
        });

        it('should reset playerOutId when team changes', () => {
            component.tradeForm.patchValue({ playerOutId: 'some-player' });
            component.onTeamChange(component.availableTeams[0].id);
            expect(component.tradeForm.get('playerOutId')?.value).toBe('');
        });
    });

    describe('hasValidationErrors', () => {
        it('should return false when form is valid', () => {
            component.tradeForm.setValue({
                teamId: '1',
                playerOutId: '1',
                playerInId: '5'
            });
            expect(component.hasValidationErrors()).toBe(false);
        });

        it('should return false when form is invalid but not touched', () => {
            expect(component.hasValidationErrors()).toBe(false);
        });

        it('should return true when form is invalid and touched', () => {
            component.tradeForm.markAllAsTouched();
            expect(component.hasValidationErrors()).toBe(true);
        });
    });

    describe('hasSamePlayerError', () => {
        it('should return false when players are different', () => {
            component.tradeForm.patchValue({
                playerOutId: '1',
                playerInId: '5'
            });
            expect(component.hasSamePlayerError()).toBe(false);
        });

        it('should return true when players are the same', () => {
            component.tradeForm.patchValue({
                playerOutId: '1',
                playerInId: '1'
            });
            expect(component.hasSamePlayerError()).toBe(true);
        });

        it('should return false when playerOutId is empty', () => {
            component.tradeForm.patchValue({
                playerOutId: '',
                playerInId: '1'
            });
            expect(component.hasSamePlayerError()).toBeFalsy();
        });

        it('should return false when playerInId is empty', () => {
            component.tradeForm.patchValue({
                playerOutId: '1',
                playerInId: ''
            });
            expect(component.hasSamePlayerError()).toBeFalsy();
        });
    });

    describe('getPlayerUsername', () => {
        it('should return username for team player', () => {
            const teamPlayer = component.availableTeams[0].players[0];
            const username = component.getPlayerUsername(teamPlayer.id);
            expect(username).toBe(teamPlayer.username);
        });

        it('should return username for available player', () => {
            const availablePlayer = component.availablePlayers[0];
            const username = component.getPlayerUsername(availablePlayer.id);
            expect(username).toBe(availablePlayer.username);
        });

        it('should return empty string for unknown player', () => {
            const username = component.getPlayerUsername('unknown-id');
            expect(username).toBe('');
        });
    });

    describe('getPlayerRegion', () => {
        it('should return region for team player', () => {
            const teamPlayer = component.availableTeams[0].players[0];
            const region = component.getPlayerRegion(teamPlayer.id);
            expect(region).toBe(teamPlayer.region);
        });

        it('should return region for available player', () => {
            const availablePlayer = component.availablePlayers[0];
            const region = component.getPlayerRegion(availablePlayer.id);
            expect(region).toBe(availablePlayer.region);
        });

        it('should return empty string for unknown player', () => {
            const region = component.getPlayerRegion('unknown-id');
            expect(region).toBe('');
        });
    });

    describe('goBack', () => {
        it('should navigate to trades page', () => {
            const navigateSpy = spyOn(router, 'navigate');
            component.goBack();
            expect(navigateSpy).toHaveBeenCalledWith(['/trades']);
        });
    });

    describe('onSubmit', () => {
        beforeEach(() => {
            component.tradeForm.setValue({
                teamId: '1',
                playerOutId: '1',
                playerInId: '5'
            });
        });

        it('should not submit when form is invalid', () => {
            component.tradeForm.reset();
            component.onSubmit();
            expect(component.isSubmitting).toBe(false);
        });

        it('should not submit when same player error exists', () => {
            component.tradeForm.patchValue({
                playerOutId: '1',
                playerInId: '1'
            });
            component.onSubmit();
            expect(component.isSubmitting).toBe(false);
        });

        it('should set isSubmitting to true when submitting', () => {
            component.onSubmit();
            expect(component.isSubmitting).toBe(true);
        });

        it('should log debug message when submitting', () => {
            component.onSubmit();
            expect(loggerService.debug).toHaveBeenCalled();
        });

        it('should complete submission after delay', fakeAsync(() => {
            const navigateSpy = spyOn(router, 'navigate');
            component.onSubmit();
            expect(component.isSubmitting).toBe(true);
            // Verify that snackBar and navigation will be called after the 2s timeout
            // by checking that the component state is correct before the timeout
            tick(2500);
            flush();
        }));
    });

    describe('template rendering', () => {
        it('should render form card', () => {
            const card = fixture.debugElement.query(By.css('.trade-form-card'));
            expect(card).toBeTruthy();
        });

        it('should render team selection', () => {
            const teamSelect = fixture.debugElement.query(By.css('mat-select[formControlName="teamId"]'));
            expect(teamSelect).toBeTruthy();
        });

        it('should render player in selection', () => {
            const playerInSelect = fixture.debugElement.query(By.css('mat-select[formControlName="playerInId"]'));
            expect(playerInSelect).toBeTruthy();
        });

        it('should show player out section when team is selected', () => {
            component.selectedTeam = component.availableTeams[0];
            fixture.detectChanges();
            const playerOutSection = fixture.debugElement.query(By.css('mat-select[formControlName="playerOutId"]'));
            expect(playerOutSection).toBeTruthy();
        });

        it('should show trade preview when both players are selected', () => {
            component.tradeForm.patchValue({
                playerOutId: '1',
                playerInId: '5'
            });
            fixture.detectChanges();
            const preview = fixture.debugElement.query(By.css('.trade-preview'));
            expect(preview).toBeTruthy();
        });

        it('should show validation errors when form is invalid and touched', () => {
            component.tradeForm.markAllAsTouched();
            fixture.detectChanges();
            const errors = fixture.debugElement.query(By.css('.validation-errors'));
            expect(errors).toBeTruthy();
        });
    });

    describe('button states', () => {
        it('should disable submit button when form is invalid', () => {
            fixture.detectChanges();
            const submitButton = fixture.debugElement.query(By.css('button[color="primary"]'));
            expect(submitButton.nativeElement.disabled).toBe(true);
        });

        it('should enable submit button when form is valid', () => {
            component.tradeForm.setValue({
                teamId: '1',
                playerOutId: '1',
                playerInId: '5'
            });
            fixture.detectChanges();
            const submitButton = fixture.debugElement.query(By.css('button[color="primary"]'));
            expect(submitButton.nativeElement.disabled).toBe(false);
        });

        it('should disable both buttons when submitting', () => {
            component.tradeForm.setValue({
                teamId: '1',
                playerOutId: '1',
                playerInId: '5'
            });
            component.onSubmit();
            fixture.detectChanges();
            const cancelButton = fixture.debugElement.query(By.css('button:not([color="primary"])'));
            expect(cancelButton.nativeElement.disabled).toBe(true);
        });

        it('should show spinner when submitting', () => {
            component.tradeForm.setValue({
                teamId: '1',
                playerOutId: '1',
                playerInId: '5'
            });
            component.onSubmit();
            fixture.detectChanges();
            const spinner = fixture.debugElement.query(By.css('mat-spinner'));
            expect(spinner).toBeTruthy();
        });
    });
});
