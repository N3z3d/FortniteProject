import { Component, OnInit, ElementRef, ViewChildren, QueryList, AfterViewInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { PremiumInteractionsDirective, TooltipDirective, RevealOnScrollDirective, PulseDirective } from '../../directives/premium-interactions.directive';
import { PremiumInteractionsService } from '../../services/premium-interactions.service';
import { LoggerService } from '../../../core/services/logger.service';
import { TranslationService } from '../../../core/services/translation.service';
import { PlayerStatsService } from '../../../core/services/player-stats.service';

interface Region {
  id: string;
  code: string;
  nameKey: string;
  descriptionKey: string;
  icon: string;
  playerCount: number;
  topTeams: number;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    PremiumInteractionsDirective,
    TooltipDirective,
    RevealOnScrollDirective,
    PulseDirective
  ],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit, AfterViewInit {
  // Translation service exposed for template
  public readonly t = inject(TranslationService);

  @ViewChildren('heroButton') heroButtons!: QueryList<ElementRef>;
  @ViewChildren('regionCard') regionCards!: QueryList<ElementRef>;
  
  regions: Region[] = [
    {
      id: 'eu',
      code: 'EU',
      nameKey: 'home.regions.eu.name',
      descriptionKey: 'home.regions.eu.description',
      icon: '🏰',
      playerCount: 21,
      topTeams: 7
    },
    {
      id: 'nac',
      code: 'NAC',
      nameKey: 'home.regions.nac.name',
      descriptionKey: 'home.regions.nac.description',
      icon: '🗽',
      playerCount: 21,
      topTeams: 7
    },
    {
      id: 'naw',
      code: 'NAW',
      nameKey: 'home.regions.naw.name',
      descriptionKey: 'home.regions.naw.description',
      icon: '🌄',
      playerCount: 21,
      topTeams: 7
    },
    {
      id: 'br',
      code: 'BR',
      nameKey: 'home.regions.br.name',
      descriptionKey: 'home.regions.br.description',
      icon: '🏖️',
      playerCount: 21,
      topTeams: 7
    },
    {
      id: 'oce',
      code: 'OCE',
      nameKey: 'home.regions.oce.name',
      descriptionKey: 'home.regions.oce.description',
      icon: '🏄‍♂️',
      playerCount: 21,
      topTeams: 7
    },
    {
      id: 'me',
      code: 'ME',
      nameKey: 'home.regions.me.name',
      descriptionKey: 'home.regions.me.description',
      icon: '🏛️',
      playerCount: 21,
      topTeams: 7
    },
    {
      id: 'asia',
      code: 'ASIA',
      nameKey: 'home.regions.asia.name',
      descriptionKey: 'home.regions.asia.description',
      icon: '🏯',
      playerCount: 21,
      topTeams: 7
    }
  ];

  // Helper to get translated region name
  getRegionName(region: Region): string {
    return this.translationService.t(region.nameKey, region.code);
  }

  // Helper to get translated region description
  getRegionDescription(region: Region): string {
    return this.translationService.t(region.descriptionKey, '');
  }

  constructor(
    private readonly interactionsService: PremiumInteractionsService,
    private readonly logger: LoggerService,
    private readonly router: Router,
    private readonly translationService: TranslationService,
    private readonly playerStatsService: PlayerStatsService
  ) { }

  ngOnInit(): void {
    // Animations d'entrée retardées
    this.animateOnScroll();
    // Load dynamic player counts from API
    this.loadRegionPlayerCounts();
  }

  private loadRegionPlayerCounts(): void {
    this.playerStatsService.getPlayerStats().subscribe({
      next: (stats) => {
        if (stats.playersByRegion) {
          this.regions = this.regions.map(region => ({
            ...region,
            playerCount: stats.playersByRegion?.[region.code] || region.playerCount
          }));
          this.logger.debug('Home: region counts updated from API', stats.playersByRegion);
        }
      },
      error: (err) => {
        this.logger.warn('Home: failed to load player stats, using defaults', err);
      }
    });
  }

  ngAfterViewInit(): void {
    // Initialize premium interactions after view is ready
    this.initPremiumInteractions();
  }

  private animateOnScroll(): void {
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('animate-in');
        }
      });
    }, {
      threshold: 0.1
    });

    // Observer les sections pour les animations d'entrée
    setTimeout(() => {
      const sections = document.querySelectorAll('.regions, .features, .cta-section');
      sections.forEach(section => observer.observe(section));
    }, 100);
  }

  onRegionSelect(region: Region): void {
    this.logger.info('Home: region selected', { region: region.code });
    this.router.navigate(['/teams'], { queryParams: { region: region.code.toLowerCase() } });
  }

  onCreateTeam(): void {
    this.logger.info('Home: navigating to team creation');
    this.router.navigate(['/teams/create']);
  }

  onViewRules(): void {
    this.logger.info('Home: viewing rules');
    this.scrollToSection('rules');
  }

  onStartNow(): void {
    this.logger.info('Home: start now clicked');
    this.router.navigate(['/games']);
  }

  onBeginnersGuide(): void {
    this.logger.info('Home: beginners guide clicked');
    this.scrollToSection('features');
  }

  onLogin(): void {
    this.logger.info('Home: navigating to login');
    this.router.navigate(['/login']);
  }

  onViewLeaderboard(): void {
    this.logger.info('Home: navigating to leaderboard');
    this.router.navigate(['/leaderboard']);
  }

  // Smooth scroll vers les sections
  scrollToSection(sectionId: string): void {
    const element = document.getElementById(sectionId);
    if (element) {
      element.scrollIntoView({ 
        behavior: 'smooth',
        block: 'start'
      });
    }
  }

  // Gestion des liens de navigation
  onNavClick(section: string): void {
    this.scrollToSection(section.replace('#', ''));
  }

  // Premium interaction methods
  private initPremiumInteractions(): void {
    // Show welcome notification
    const welcomeMessage = this.translationService.t('home.welcome', 'Welcome to Fortnite Pro League!');
    this.interactionsService.showGamingNotification(welcomeMessage, 'info');
  }

  onRegionCardClick(region: Region): void {
    const regionName = this.getRegionName(region);
    const template = this.translationService.t('home.regionSelected', 'Region {name} selected!');
    this.interactionsService.showGamingNotification(
      template.replace('{name}', regionName),
      'success'
    );

    this.logger.info('Home: region card clicked', { region: region.code });
    this.onRegionSelect(region);
  }

  onHeroButtonClick(action: string): void {
    switch (action) {
      case 'create':
        this.interactionsService.showGamingNotification(
          this.translationService.t('home.creatingTeam', 'Creating team...'),
          'info'
        );
        this.onCreateTeam();
        break;
      case 'rules':
        this.interactionsService.showGamingNotification(
          this.translationService.t('home.loadingRules', 'Loading rules...'),
          'info'
        );
        this.onViewRules();
        break;
    }
  }
} 
