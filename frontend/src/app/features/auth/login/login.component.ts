import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router, ActivatedRoute } from '@angular/router';

import { UserContextService, UserProfile } from '../../../core/services/user-context.service';
import { AccessibilityAnnouncerService } from '../../../shared/services/accessibility-announcer.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="user-controlled-login">
      <!-- Hero Section -->
      <div class="hero-section">
        <div class="hero-content">
          <h1 class="game-title">Fortnite Fantasy</h1>
          <p class="hero-subtitle">Créez votre équipe de rêve et dominez la compétition</p>
        </div>
      </div>

      <!-- User Selection Login -->
      <div class="user-selection-login">
        <h2>{{ isSwitchingUser ? 'Changer d\'utilisateur' : 'Choisissez votre profil' }}</h2>
        <p class="login-subtitle">{{ isSwitchingUser ? 'Sélectionnez un autre utilisateur' : 'Sélectionnez votre utilisateur pour commencer' }}</p>
        
        <!-- User Profile Selection -->
        <fieldset class="user-selection-section" *ngIf="!isLoading">
          <legend class="sr-only">Select your user profile to login</legend>
          <button 
            *ngFor="let profile of availableProfiles; let i = index" 
            mat-fab 
            extended
            color="primary"
            class="user-profile-btn accessible-focus"
            (click)="selectUser(profile)"
            [attr.aria-label]="'Login as ' + profile.username"
            [attr.aria-describedby]="'profile-desc-' + i">
            <mat-icon aria-hidden="true">person</mat-icon>
            {{ profile.username }}
            <span [id]="'profile-desc-' + i" class="sr-only">
              User profile for {{ profile.username }}
            </span>
          </button>
        </fieldset>
        
        <!-- Loading State -->
        <div *ngIf="isLoading" class="user-loading" role="status" aria-live="polite">
          <mat-spinner diameter="48" aria-label="Loading indicator"></mat-spinner>
          <p id="loading-message">Connexion en cours...</p>
        </div>
        
        <!-- Alternative Login (Hidden by Default) -->
        <div class="alternative-login" *ngIf="showAlternative && !isLoading">
          <form [formGroup]="quickForm" (ngSubmit)="onQuickSubmit()" class="minimal-form" 
                role="form" aria-labelledby="alt-login-heading">
            <h3 id="alt-login-heading" class="sr-only">Alternative login form</h3>
            <mat-form-field appearance="outline" class="single-field">
              <mat-label>Email ou pseudo</mat-label>
              <input matInput formControlName="identifier" 
                     placeholder="Entrez votre identifiant"
                     aria-label="Email or username"
                     aria-required="true"
                     autocomplete="username">
              <mat-error *ngIf="quickForm.get('identifier')?.invalid && quickForm.get('identifier')?.touched">
                Un identifiant est requis
              </mat-error>
            </mat-form-field>
            <button mat-raised-button color="primary" type="submit" 
                    [disabled]="quickForm.invalid"
                    aria-label="Login with entered credentials">
              Connexion rapide
            </button>
          </form>
        </div>
        
        <!-- Toggle Alternative -->
        <button 
          mat-button 
          class="show-alternative"
          (click)="toggleAlternative()"
          *ngIf="!isLoading"
          [attr.aria-label]="showAlternative ? 'Hide alternative login form' : 'Show alternative login form'"
          [attr.aria-expanded]="showAlternative">
          {{ showAlternative ? 'Masquer' : 'Autre compte?' }}
        </button>
      </div>
      
      <!-- Quick Stats to Build Trust -->
      <div class="trust-indicators" *ngIf="!isLoading">
        <div class="stat-item">
          <span class="stat-number">500+</span>
          <span class="stat-label">Joueurs actifs</span>
        </div>
        <div class="stat-item">
          <span class="stat-number">50+</span>
          <span class="stat-label">Games en cours</span>
        </div>
        <div class="stat-item">
          <span class="stat-number">100%</span>
          <span class="stat-label">Gratuit</span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .user-controlled-login {
      min-height: 100vh;
      background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 2rem;
      position: relative;
      overflow: hidden;
    }

    .user-controlled-login::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background-image: url('data:image/svg+xml,<svg width="60" height="60" viewBox="0 0 60 60" xmlns="http://www.w3.org/2000/svg"><g fill="none" fill-rule="evenodd"><g fill="%23ffffff" fill-opacity="0.05"><circle cx="30" cy="30" r="2"/></g></g></svg>');
      animation: float 20s infinite linear;
    }

    @keyframes float {
      0% { transform: translateY(0px); }
      100% { transform: translateY(-60px); }
    }

    .hero-section {
      text-align: center;
      margin-bottom: 3rem;
      z-index: 1;
    }

    .game-title {
      font-size: 3.5rem;
      font-weight: 800;
      background: linear-gradient(45deg, #ff6b6b, #4ecdc4, #45b7d1);
      background-size: 200% 200%;
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
      animation: gradientShift 3s ease-in-out infinite alternate;
      margin: 0 0 1rem 0;
      text-shadow: 0 0 30px rgba(255, 107, 107, 0.3);
    }

    @keyframes gradientShift {
      0% { background-position: 0% 50%; }
      100% { background-position: 100% 50%; }
    }

    .hero-subtitle {
      font-size: 1.2rem;
      color: rgba(255, 255, 255, 0.8);
      margin: 0;
      font-weight: 300;
    }

    .user-selection-login {
      background: rgba(255, 255, 255, 0.1);
      backdrop-filter: blur(10px);
      border-radius: 20px;
      padding: 2.5rem;
      text-align: center;
      border: 1px solid rgba(255, 255, 255, 0.2);
      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
      z-index: 1;
      min-width: 450px;
      min-height: 300px;
    }

    .user-selection-login h2 {
      color: white;
      font-size: 1.8rem;
      margin: 0 0 0.5rem 0;
      font-weight: 600;
    }

    .login-subtitle {
      color: rgba(255, 255, 255, 0.7);
      margin: 0 0 2rem 0;
      font-size: 1rem;
    }

    .user-selection-section {
      border: none;
      margin: 0 0 1.5rem 0;
      padding: 0;
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1.5rem;
    }

    .user-selection-section legend {
      position: absolute;
      left: -10000px;
      width: 1px;
      height: 1px;
      overflow: hidden;
    }

    .user-profile-btn {
      min-height: 64px;
      font-size: 1.1rem;
      font-weight: 500;
      transition: all 0.3s ease;
      position: relative;
      overflow: hidden;
      border-radius: 12px;
    }

    .user-profile-btn:hover {
      transform: translateY(-3px) scale(1.02);
      box-shadow: 0 12px 25px rgba(33, 150, 243, 0.4);
    }

    .user-profile-btn:active {
      transform: translateY(0px) scale(0.98);
    }

    .user-profile-btn:focus-visible {
      outline: 3px solid #4ecdc4 !important;
      outline-offset: 2px !important;
      box-shadow: 0 0 0 5px rgba(78, 205, 196, 0.2) !important;
    }

    .user-profile-btn:focus:not(:focus-visible) {
      outline: none !important;
    }

    .user-loading {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 1.5rem;
      padding: 2rem;
      color: white;
    }

    .user-loading p {
      color: rgba(255, 255, 255, 0.8);
      margin: 0;
      font-size: 1.1rem;
      font-weight: 500;
    }

    .alternative-login {
      margin-top: 1.5rem;
      animation: slideDown 0.3s ease-out;
    }

    @keyframes slideDown {
      from {
        opacity: 0;
        transform: translateY(-10px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    .minimal-form {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .single-field {
      width: 100%;
    }

    .show-alternative {
      color: rgba(255, 255, 255, 0.6);
      font-size: 0.9rem;
      margin-top: 1rem;
    }

    .trust-indicators {
      display: flex;
      gap: 2rem;
      margin-top: 3rem;
      z-index: 1;
    }

    .stat-item {
      text-align: center;
      color: rgba(255, 255, 255, 0.8);
    }

    .stat-number {
      display: block;
      font-size: 1.5rem;
      font-weight: 700;
      color: #4ecdc4;
    }

    .stat-label {
      font-size: 0.8rem;
      opacity: 0.7;
    }

    @media (max-width: 768px) {
      .user-selection-login {
        min-width: auto;
        margin: 0 1rem;
        padding: 2rem;
      }
      
      .user-selection-section {
        grid-template-columns: 1fr;
      }
      
      .game-title {
        font-size: 2.5rem;
      }
      
      .trust-indicators {
        gap: 1rem;
      }
    }

    @media (max-width: 600px) {
      .user-selection-section {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class LoginComponent implements OnInit {
  quickForm: FormGroup;
  isLoading = false;
  showAlternative = false;
  availableProfiles: UserProfile[] = [];
  isSwitchingUser = false;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private userContextService: UserContextService,
    private accessibilityService: AccessibilityAnnouncerService
  ) {
    this.quickForm = this.fb.group({
      identifier: ['', [Validators.required]]
    });
  }

  ngOnInit(): void {
    this.loadAvailableProfiles();
    
    // Check if this is a user switch operation
    this.route.queryParams.subscribe(params => {
      this.isSwitchingUser = params['switchUser'] === 'true';
    });
    
    // Vérifier si l'utilisateur est déjà connecté
    const currentUser = this.userContextService.getCurrentUser();
    if (currentUser) {
      // Si déjà connecté, rediriger immédiatement
      this.router.navigate(['/games/join']);
      return;
    }
    
    // Tentative d'auto-connexion pour les utilisateurs récurrents (uniquement si pas en mode switch)
    if (!this.isSwitchingUser) {
      const autoLoggedUser = this.userContextService.attemptAutoLogin();
      if (autoLoggedUser) {
        console.log('🔐 Auto-connexion réussie pour:', autoLoggedUser.username);
        this.router.navigate(['/games/join'], { queryParams: { autoLogin: 'true', user: autoLoggedUser.username } });
        return;
      }
    }
    
    console.log('🔐 Page de login chargée - attente du choix utilisateur');
  }

  private loadAvailableProfiles(): void {
    this.availableProfiles = this.userContextService.getAvailableProfiles();
  }

  selectUser(profile: UserProfile): void {
    console.log('🔐 Utilisateur sélectionné:', profile.username);
    this.isLoading = true;
    
    // Announce the login attempt
    this.accessibilityService.announceLoading(true, `authentication for ${profile.username}`);
    
    // Connexion avec feedback visuel approprié
    setTimeout(() => {
      this.userContextService.login(profile);
      console.log('🔐 Connexion réussie pour:', profile.username);
      this.isLoading = false;
      
      // Announce successful login
      this.accessibilityService.announceSuccess(`Login successful for ${profile.username}`);
      this.accessibilityService.announceNavigation('Sélection de game');
      
      // Naviguer directement vers la sélection de game
      this.router.navigate(['/games/join'], { queryParams: { welcome: 'true', user: profile.username } });
    }, 800); // Temps suffisant pour voir le feedback
  }

  // Enhanced accessibility methods
  announceFormError(fieldName: string, error: string): void {
    this.accessibilityService.announceFormErrors([`${fieldName}: ${error}`]);
  }

  onFormError(): void {
    if (this.quickForm.get('identifier')?.invalid && this.quickForm.get('identifier')?.touched) {
      this.announceFormError('Identifiant', 'Un identifiant est requis');
    }
  }

  toggleAlternative(): void {
    this.showAlternative = !this.showAlternative;
  }

  onQuickSubmit(): void {
    if (this.quickForm.valid) {
      this.isLoading = true;
      const identifier = this.quickForm.get('identifier')?.value;
      
      // Announce login attempt
      this.accessibilityService.announceLoading(true, `login attempt for ${identifier}`);
      
      // Try to find matching profile or create default
      const matchingProfile = this.availableProfiles.find(p => 
        p.username.toLowerCase().includes(identifier.toLowerCase())
      ) || this.availableProfiles[0];
      
      setTimeout(() => {
        this.userContextService.login(matchingProfile);
        this.isLoading = false;
        this.accessibilityService.announceSuccess(`Login successful`);
        this.accessibilityService.announceNavigation('Sélection de game');
        this.router.navigate(['/games/join']);
      }, 800);
    } else {
      // Announce form validation errors
      this.onFormError();
    }
  }
}