import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { HomeComponent } from './home.component';
import { PremiumInteractionsService } from '../../services/premium-interactions.service';
import { LoggerService } from '../../../core/services/logger.service';
import { TranslationService } from '../../../core/services/translation.service';
import { PlayerStatsService } from '../../../core/services/player-stats.service';

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;
  let router: Router;
  let interactionsService: jasmine.SpyObj<PremiumInteractionsService>;
  let logger: jasmine.SpyObj<LoggerService>;
  let translationService: jasmine.SpyObj<TranslationService>;
  let playerStatsService: jasmine.SpyObj<PlayerStatsService>;

  beforeEach(async () => {
    interactionsService = jasmine.createSpyObj('PremiumInteractionsService', [
      'showGamingNotification',
      'initMagneticButton',
      'initRevealOnScroll',
      'initPulse',
      'initTooltip'
    ]);
    logger = jasmine.createSpyObj('LoggerService', ['info', 'debug', 'warn']);
    translationService = jasmine.createSpyObj('TranslationService', ['t']);
    translationService.t.and.callFake((key: string, fallback?: string) => fallback || key);
    playerStatsService = jasmine.createSpyObj('PlayerStatsService', ['getPlayerStats']);
    playerStatsService.getPlayerStats.and.returnValue(of({ totalPlayers: 147, playersByRegion: {} }));

    await TestBed.configureTestingModule({
      imports: [HomeComponent, RouterTestingModule],
      providers: [
        { provide: PremiumInteractionsService, useValue: interactionsService },
        { provide: LoggerService, useValue: logger },
        { provide: TranslationService, useValue: translationService },
        { provide: PlayerStatsService, useValue: playerStatsService }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with 7 regions', () => {
    expect(component.regions.length).toBe(7);
    expect(component.regions[0].code).toBe('EU');
    expect(component.regions[6].code).toBe('ASIA');
  });

  it('should load region player counts on init', fakeAsync(() => {
    const mockStats = {
      totalPlayers: 147,
      playersByRegion: {
        'EU': 35,
        'NAW': 28,
        'ASIA': 42
      }
    };
    playerStatsService.getPlayerStats.and.returnValue(of(mockStats));

    fixture.detectChanges();
    tick();

    expect(playerStatsService.getPlayerStats).toHaveBeenCalled();
    expect(component.regions.find(r => r.code === 'EU')?.playerCount).toBe(35);
    expect(component.regions.find(r => r.code === 'NAW')?.playerCount).toBe(28);
    expect(component.regions.find(r => r.code === 'ASIA')?.playerCount).toBe(42);
    expect(logger.debug).toHaveBeenCalledWith('Home: region counts updated from API', mockStats.playersByRegion);
  }));

  it('should handle player stats error gracefully', fakeAsync(() => {
    playerStatsService.getPlayerStats.and.returnValue(throwError(() => new Error('API error')));

    fixture.detectChanges();
    tick();

    expect(logger.warn).toHaveBeenCalledWith('Home: failed to load player stats, using defaults', jasmine.any(Error));
    expect(component.regions[0].playerCount).toBe(21); // Default value
  }));

  it('should show welcome notification after view init', () => {
    translationService.t.and.returnValue('Welcome to Fortnite Pro League!');

    fixture.detectChanges();

    expect(interactionsService.showGamingNotification).toHaveBeenCalledWith(
      'Welcome to Fortnite Pro League!',
      'info'
    );
  });

  it('should navigate to teams with region filter on region select', () => {
    const region = component.regions[0]; // EU

    component.onRegionSelect(region);

    expect(logger.info).toHaveBeenCalledWith('Home: region selected', { region: 'EU' });
    expect(router.navigate).toHaveBeenCalledWith(['/teams'], { queryParams: { region: 'eu' } });
  });

  it('should navigate to team creation', () => {
    component.onCreateTeam();

    expect(logger.info).toHaveBeenCalledWith('Home: navigating to team creation');
    expect(router.navigate).toHaveBeenCalledWith(['/teams/create']);
  });

  it('should scroll to rules section on view rules', () => {
    spyOn(component, 'scrollToSection');

    component.onViewRules();

    expect(logger.info).toHaveBeenCalledWith('Home: viewing rules');
    expect(component.scrollToSection).toHaveBeenCalledWith('rules');
  });

  it('should navigate to games on start now', () => {
    component.onStartNow();

    expect(logger.info).toHaveBeenCalledWith('Home: start now clicked');
    expect(router.navigate).toHaveBeenCalledWith(['/games']);
  });

  it('should scroll to features on beginners guide', () => {
    spyOn(component, 'scrollToSection');

    component.onBeginnersGuide();

    expect(logger.info).toHaveBeenCalledWith('Home: beginners guide clicked');
    expect(component.scrollToSection).toHaveBeenCalledWith('features');
  });

  it('should navigate to login', () => {
    component.onLogin();

    expect(logger.info).toHaveBeenCalledWith('Home: navigating to login');
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should navigate to leaderboard', () => {
    component.onViewLeaderboard();

    expect(logger.info).toHaveBeenCalledWith('Home: navigating to leaderboard');
    expect(router.navigate).toHaveBeenCalledWith(['/leaderboard']);
  });

  it('should scroll to section smoothly', () => {
    const mockElement = document.createElement('div');
    spyOn(mockElement, 'scrollIntoView');
    spyOn(document, 'getElementById').and.returnValue(mockElement);

    component.scrollToSection('features');

    expect(document.getElementById).toHaveBeenCalledWith('features');
    expect(mockElement.scrollIntoView).toHaveBeenCalledWith({
      behavior: 'smooth',
      block: 'start'
    });
  });

  it('should not crash if section not found', () => {
    spyOn(document, 'getElementById').and.returnValue(null);

    expect(() => component.scrollToSection('invalid-section')).not.toThrow();
  });

  it('should handle nav click by removing hash', () => {
    spyOn(component, 'scrollToSection');

    component.onNavClick('#features');

    expect(component.scrollToSection).toHaveBeenCalledWith('features');
  });

  it('should show notification and navigate on region card click', () => {
    const region = component.regions[1]; // NAC
    translationService.t.and.returnValues('North America Central', 'Region {name} selected!');

    component.onRegionCardClick(region);

    expect(interactionsService.showGamingNotification).toHaveBeenCalledWith(
      'Region North America Central selected!',
      'success'
    );
    expect(logger.info).toHaveBeenCalledWith('Home: region card clicked', { region: 'NAC' });
    expect(router.navigate).toHaveBeenCalledWith(['/teams'], { queryParams: { region: 'nac' } });
  });

  it('should handle hero button click for create action', () => {
    translationService.t.and.returnValue('Creating team...');

    component.onHeroButtonClick('create');

    expect(interactionsService.showGamingNotification).toHaveBeenCalledWith('Creating team...', 'info');
    expect(router.navigate).toHaveBeenCalledWith(['/teams/create']);
  });

  it('should handle hero button click for rules action', () => {
    spyOn(component, 'scrollToSection');
    translationService.t.and.returnValue('Loading rules...');

    component.onHeroButtonClick('rules');

    expect(interactionsService.showGamingNotification).toHaveBeenCalledWith('Loading rules...', 'info');
    expect(component.scrollToSection).toHaveBeenCalledWith('rules');
  });

  it('should get translated region name', () => {
    const region = component.regions[0];
    translationService.t.and.returnValue('Europe');

    const name = component.getRegionName(region);

    expect(name).toBe('Europe');
    expect(translationService.t).toHaveBeenCalledWith('home.regions.eu.name', 'EU');
  });

  it('should get translated region description', () => {
    const region = component.regions[0];
    translationService.t.and.returnValue('European servers');

    const description = component.getRegionDescription(region);

    expect(description).toBe('European servers');
    expect(translationService.t).toHaveBeenCalledWith('home.regions.eu.description', '');
  });
});
