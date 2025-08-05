import { Component, OnInit, ElementRef, ViewChildren, QueryList, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PremiumInteractionsDirective, TooltipDirective, RevealOnScrollDirective, PulseDirective } from '../../directives/premium-interactions.directive';
import { PremiumInteractionsService } from '../../services/premium-interactions.service';

interface Region {
  id: string;
  code: string;
  name: string;
  description: string;
  icon: string;
  playerCount: number;
  topTeams: number;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule, 
    PremiumInteractionsDirective, 
    TooltipDirective, 
    RevealOnScrollDirective, 
    PulseDirective
  ],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit, AfterViewInit {
  
  @ViewChildren('heroButton') heroButtons!: QueryList<ElementRef>;
  @ViewChildren('regionCard') regionCards!: QueryList<ElementRef>;
  
  regions: Region[] = [
    {
      id: 'eu',
      code: 'EU',
      name: 'Europe',
      description: 'Le cœur stratégique du competitive. Des équipes disciplinées et une mécanique impeccable.',
      icon: '🏰',
      playerCount: 32,
      topTeams: 8
    },
    {
      id: 'nac',
      code: 'NAC',
      name: 'North America Central',
      description: 'L\'épicentre du gaming créatif. Innovation et spectacle garantis.',
      icon: '🗽',
      playerCount: 28,
      topTeams: 7
    },
    {
      id: 'naw',
      code: 'NAW',
      name: 'North America West',
      description: 'Terre des pionniers et des game-changers. Où naissent les méta.',
      icon: '🌄',
      playerCount: 25,
      topTeams: 6
    },
    {
      id: 'br',
      code: 'BR',
      name: 'Brazil',
      description: 'La passion à l\'état pur. Un style unique et une technique impressionnante.',
      icon: '🏖️',
      playerCount: 22,
      topTeams: 6
    },
    {
      id: 'oce',
      code: 'OCE',
      name: 'Oceania',
      description: 'Les guerriers des antipodes. Redoutables et imprévisibles.',
      icon: '🏄‍♂️',
      playerCount: 18,
      topTeams: 4
    },
    {
      id: 'me',
      code: 'ME',
      name: 'Middle East',
      description: 'Les diamants du désert. Talent émergent et ambition infinie.',
      icon: '🏛️',
      playerCount: 22,
      topTeams: 5
    },
    {
      id: 'asia',
      code: 'ASIA',
      name: 'Asia',
      description: 'La région de l\'innovation technologique. Précision et excellence.',
      icon: '🏯',
      playerCount: 20,
      topTeams: 5
    }
  ];

  constructor(private interactionsService: PremiumInteractionsService) { }

  ngOnInit(): void {
    // Animations d'entrée retardées
    this.animateOnScroll();
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
    console.log(`Région sélectionnée: ${region.name}`);
    // Ici on pourrait naviguer vers la page de sélection des joueurs
    // this.router.navigate(['/teams', region.code.toLowerCase()]);
  }

  onCreateTeam(): void {
    console.log('Création d\'équipe démarrée');
    // Navigation vers la page de création d\'équipe
    // this.router.navigate(['/teams/create']);
  }

  onViewRules(): void {
    console.log('Affichage des règles');
    // Navigation vers la page des règles
    // this.router.navigate(['/rules']);
  }

  // Méthodes pour les animations et interactions
  onHeroAction(action: string): void {
    switch(action) {
      case 'create':
        this.onCreateTeam();
        break;
      case 'rules':
        this.onViewRules();
        break;
      default:
        console.log(`Action: ${action}`);
    }
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
    this.interactionsService.showGamingNotification(
      'Bienvenue dans Fortnite Pro League ! 🎮', 
      'info'
    );
  }

  onRegionCardClick(region: Region): void {
    // Create particle explosion effect
    this.interactionsService.showGamingNotification(
      `Région ${region.name} sélectionnée !`, 
      'success'
    );
    
    // Navigate to region detail (placeholder)
    console.log(`Navigating to region: ${region.code}`);
  }

  onHeroButtonClick(action: string): void {
    switch (action) {
      case 'create':
        this.interactionsService.showGamingNotification(
          'Création d\'équipe en cours...', 
          'info'
        );
        this.onCreateTeam();
        break;
      case 'rules':
        this.interactionsService.showGamingNotification(
          'Chargement des règles...', 
          'info'
        );
        this.onViewRules();
        break;
    }
  }
} 