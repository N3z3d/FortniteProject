import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { Router, ActivatedRoute } from '@angular/router';

import { UserContextService, UserProfile } from '../../../core/services/user-context.service';
import { TranslationService, SupportedLanguage } from '../../../core/services/translation.service';
import { LoggerService } from '../../../core/services/logger.service';
import { AccessibilityAnnouncerService } from '../../../shared/services/accessibility-announcer.service';

const toSvgDataUri = (svg: string): string => `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;

const FR_FLAG_SVG = toSvgDataUri(
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 3 2"><rect width="1" height="2" fill="#0055A4"/><rect x="1" width="1" height="2" fill="#FFFFFF"/><rect x="2" width="1" height="2" fill="#EF4135"/></svg>'
);
const EN_FLAG_SVG = toSvgDataUri(
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 14"><rect width="20" height="14" fill="#012169"/><path d="M0 0 L8 5.6 L6.6 7 L0 2.4 Z M20 0 L12 5.6 L13.4 7 L20 2.4 Z M0 14 L8 8.4 L6.6 7 L0 11.6 Z M20 14 L12 8.4 L13.4 7 L20 11.6 Z" fill="#FFFFFF"/><rect width="20" height="2.4" y="5.8" fill="#FFFFFF"/><rect width="2.4" height="14" x="8.8" fill="#FFFFFF"/><rect width="20" height="1.2" y="6.4" fill="#C8102E"/><rect width="1.2" height="14" x="9.4" fill="#C8102E"/></svg>'
);
const ES_FLAG_SVG = toSvgDataUri(
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 14"><rect width="20" height="14" fill="#C60B1E"/><rect width="20" height="7" y="3.5" fill="#FFC400"/></svg>'
);
const PT_FLAG_SVG = toSvgDataUri(
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 14"><rect width="20" height="14" fill="#FF0000"/><rect width="8" height="14" fill="#006600"/><circle cx="8" cy="7" r="2.2" fill="#FFCC00"/></svg>'
);

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
    MatProgressSpinnerModule,
    MatSelectModule
  ],
  template: `
    <div class="user-controlled-login">
      <!-- Language Selector -->
      <nav class="language-selector" [attr.aria-label]="t.t('loginPage.selectLanguage')">
        <mat-form-field appearance="outline" class="lang-select-field">
          <mat-label>{{ t.t('loginPage.selectLanguage') }}</mat-label>
          <mat-select
            [value]="t.currentLanguage"
            (selectionChange)="switchLanguage($event.value)"
            [attr.aria-label]="t.t('loginPage.selectLanguage')">
            <mat-select-trigger>
              <span class="lang-trigger">
                <img class="lang-flag-icon" [src]="currentLanguageOption.flagAsset" alt="" aria-hidden="true" loading="lazy" decoding="async">
                <span class="lang-label">{{ currentLanguageOption.label }}</span>
              </span>
            </mat-select-trigger>
            <mat-option *ngFor="let lang of languages" [value]="lang.code">
              <span class="lang-option">
                <img class="lang-flag-icon" [src]="lang.flagAsset" alt="" aria-hidden="true" loading="lazy" decoding="async">
                <span class="lang-label">{{ lang.label }}</span>
              </span>
            </mat-option>
          </mat-select>
        </mat-form-field>
      </nav>

      <!-- Hero Section -->
      <div class="hero-section">
        <div class="hero-content">
          <h1 class="game-title">Fortnite Fantasy</h1>
          <p class="hero-subtitle">{{ t.t('loginPage.heroSubtitle') }}</p>
        </div>
      </div>

      <!-- User Selection Login -->
      <div class="user-selection-login">
        <h2>{{ isSwitchingUser ? t.t('loginPage.switchUser') : t.t('loginPage.chooseProfile') }}</h2>
        <p class="login-subtitle">{{ isSwitchingUser ? t.t('loginPage.switchUserSubtitle') : t.t('loginPage.selectUserSubtitle') }}</p>

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

        <!-- Login Error -->
        <div *ngIf="loginError" class="login-error" role="alert" aria-live="assertive">
          {{ loginError }}
        </div>

        <!-- Loading State -->
        <div *ngIf="isLoading" class="user-loading" role="status" aria-live="polite">
          <mat-spinner diameter="48" aria-label="Loading indicator"></mat-spinner>
          <p id="loading-message">{{ t.t('loginPage.connecting') }}</p>
        </div>

        <!-- Alternative Login (Hidden by Default) -->
        <div class="alternative-login" *ngIf="showAlternative && !isLoading">
          <form [formGroup]="quickForm" (ngSubmit)="onQuickSubmit()" class="minimal-form"
                role="form" aria-labelledby="alt-login-heading">
            <h3 id="alt-login-heading" class="sr-only">Alternative login form</h3>
            <mat-form-field appearance="outline" class="single-field">
              <mat-label>{{ t.t('loginPage.emailOrUsername') }}</mat-label>
              <input matInput formControlName="identifier"
                     [placeholder]="t.t('loginPage.enterIdentifier')"
                     aria-label="Email or username"
                     aria-required="true"
                     autocomplete="username">
              <mat-error *ngIf="quickForm.get('identifier')?.invalid && quickForm.get('identifier')?.touched">
                {{ t.t('loginPage.identifierRequired') }}
              </mat-error>
            </mat-form-field>
            <button mat-raised-button color="primary" type="submit"
                    [disabled]="quickForm.invalid"
                    aria-label="Login with entered credentials">
              {{ t.t('loginPage.quickLogin') }}
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
          {{ showAlternative ? t.t('loginPage.hide') : t.t('loginPage.otherAccount') }}
        </button>
      </div>

      <!-- Quick Stats to Build Trust -->
      <div class="trust-indicators" *ngIf="!isLoading">
        <div class="stat-item">
          <span class="stat-number">500+</span>
          <span class="stat-label">{{ t.t('loginPage.activePlayers') }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-number">50+</span>
          <span class="stat-label">{{ t.t('loginPage.ongoingGames') }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-number">100%</span>
          <span class="stat-label">{{ t.t('loginPage.free') }}</span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .language-selector {
      position: absolute;
      top: 1.5rem;
      right: 1.5rem;
      z-index: 10;
      min-width: 170px;
    }

    .lang-select-field {
      width: 210px;
    }

    .lang-trigger,
    .lang-option {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .lang-flag-icon {
      width: 20px;
      height: 14px;
      border-radius: 2px;
      border: 1px solid rgba(255, 255, 255, 0.3);
      object-fit: cover;
      flex-shrink: 0;
    }

    .lang-label {
      font-size: 0.9rem;
    }

    :host ::ng-deep .lang-select-field .mat-mdc-text-field-wrapper {
      background: rgba(0, 0, 0, 0.5);
      border-radius: 10px;
      border: 1px solid rgba(201, 169, 98, 0.35);
    }

    :host ::ng-deep .lang-select-field .mat-mdc-form-field-focus-overlay {
      background-color: rgba(201, 169, 98, 0.1);
    }

    :host ::ng-deep .lang-select-field .mat-mdc-select-value,
    :host ::ng-deep .lang-select-field .mat-mdc-select-arrow,
    :host ::ng-deep .lang-select-field .mat-mdc-floating-label {
      color: rgba(255, 255, 255, 0.92) !important;
    }

    .user-controlled-login {
      min-height: 100vh;
      background: #0d0d0d;
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
      background-image: url('data:image/svg+xml,<svg width="60" height="60" viewBox="0 0 60 60" xmlns="http://www.w3.org/2000/svg"><g fill="none" fill-rule="evenodd"><g fill="%23c9a962" fill-opacity="0.03"><circle cx="30" cy="30" r="2"/></g></g></svg>');
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
      color: #c9a962;
      font-family: 'Orbitron', sans-serif;
      text-transform: uppercase;
      letter-spacing: 4px;
      margin: 0 0 1rem 0;
      text-shadow: 0 0 30px rgba(201, 169, 98, 0.3);
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
      background: #1a1a1a;
      border-radius: 16px;
      padding: 2.5rem;
      text-align: center;
      border: 1px solid #333333;
      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
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
      box-shadow: 0 12px 25px rgba(201, 169, 98, 0.4);
    }

    .user-profile-btn:active {
      transform: translateY(0px) scale(0.98);
    }

    .user-profile-btn:focus-visible {
      outline: 3px solid #c9a962 !important;
      outline-offset: 2px !important;
      box-shadow: 0 0 0 5px rgba(201, 169, 98, 0.2) !important;
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

    .login-error {
      color: #f44336;
      font-size: 0.9rem;
      margin: 0.5rem 0 1rem 0;
      padding: 0.75rem 1rem;
      border: 1px solid rgba(244, 67, 54, 0.4);
      border-radius: 8px;
      background: rgba(244, 67, 54, 0.1);
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
      color: #c9a962;
    }

    .stat-label {
      font-size: 0.8rem;
      opacity: 0.7;
    }

    @media (max-width: 768px) {
      .language-selector {
        top: 1rem;
        right: 1rem;
      }

      .lang-select-field {
        width: 180px;
      }

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
  loginError: string | null = null;

  readonly languages: { code: SupportedLanguage; flagAsset: string; label: string }[] = [
    { code: 'fr', flagAsset: FR_FLAG_SVG, label: 'Fran\u00e7ais' },
    { code: 'en', flagAsset: EN_FLAG_SVG, label: 'English' },
    { code: 'es', flagAsset: ES_FLAG_SVG, label: 'Espa\u00f1ol' },
    { code: 'pt', flagAsset: PT_FLAG_SVG, label: 'Portugu\u00eas' }
  ];

  get currentLanguageOption(): { code: SupportedLanguage; flagAsset: string; label: string } {
    return this.languages.find(lang => lang.code === this.t.currentLanguage) ?? this.languages[0];
  }

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private userContextService: UserContextService,
    public readonly t: TranslationService,
    private logger: LoggerService,
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

    const returnUrl = this.getSafeReturnUrl();

    // Vérifier si l'utilisateur est déjà connecté
    const currentUser = this.userContextService.getCurrentUser();
    if (currentUser) {
      // Si déjà connecté, rediriger immédiatement
      this.router.navigateByUrl(returnUrl);
      return;
    }

    // Tentative d'auto-connexion pour les utilisateurs récurrents (uniquement si pas en mode switch)
    if (!this.isSwitchingUser) {
      const autoLoggedUser = this.userContextService.attemptAutoLogin();
      if (autoLoggedUser) {
        this.logger.info('Login: auto-login succeeded', { username: autoLoggedUser.username });

        if (returnUrl === '/games') {
          this.router.navigate(['/games'], { queryParams: { autoLogin: 'true', user: autoLoggedUser.username } });
        } else {
          this.router.navigateByUrl(returnUrl);
        }
        return;
      }
    }

    this.logger.info('Login: page loaded');
  }

  private loadAvailableProfiles(): void {
    this.availableProfiles = this.userContextService.getAvailableProfiles();
  }

  selectUser(profile: UserProfile): void {
    this.logger.info('Login: user selected', { username: profile.username });
    this.isLoading = true;
    this.loginError = null;

    // Announce the login attempt
    this.accessibilityService.announceLoading(true, `authentication for ${profile.username}`);

    this.userContextService.login(profile).subscribe({
      next: () => {
        this.logger.info('Login: login succeeded', { username: profile.username });
        this.isLoading = false;

        // Announce successful login
        this.accessibilityService.announceSuccess(`Login successful for ${profile.username}`);
        this.accessibilityService.announceNavigation('Sélection de game');

        const returnUrl = this.getSafeReturnUrl();
        this.logger.info('Login: redirecting after login', { returnUrl });

        if (returnUrl === '/games') {
          this.router.navigate(['/games'], { queryParams: { welcome: 'true', user: profile.username } });
        } else {
          this.router.navigateByUrl(returnUrl);
        }
      },
      error: (err) => {
        this.logger.error('Login: login failed', err);
        this.isLoading = false;
        this.loginError = err?.error?.message ?? this.t.t('loginPage.loginError', 'Erreur de connexion. Vérifiez que le serveur est disponible.');
        this.accessibilityService.announceFormErrors([this.loginError!]);
      }
    });
  }

  private getSafeReturnUrl(): string {
    const returnUrl = this.route.snapshot.queryParams['returnUrl'];
    if (typeof returnUrl !== 'string' || returnUrl.trim().length === 0) {
      return '/games';
    }

    const isSafeInternalPath = returnUrl.startsWith('/') && !returnUrl.startsWith('//');
    return isSafeInternalPath ? returnUrl : '/games';
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

  switchLanguage(lang: SupportedLanguage): void {
    this.t.setLanguage(lang);
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

      this.userContextService.login(matchingProfile).subscribe({
        next: () => {
          this.isLoading = false;
          this.accessibilityService.announceSuccess(`Login successful`);
          this.accessibilityService.announceNavigation('Sélection de game');
          this.router.navigate(['/games']);
        },
        error: (err) => {
          this.isLoading = false;
          this.loginError = err?.error?.message ?? this.t.t('loginPage.loginError', 'Erreur de connexion. Vérifiez que le serveur est disponible.');
          this.accessibilityService.announceFormErrors([this.loginError!]);
        }
      });
    } else {
      // Announce form validation errors
      this.onFormError();
    }
  }
}
