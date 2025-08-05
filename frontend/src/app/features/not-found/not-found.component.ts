import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';
import { Location } from '@angular/common';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule
  ],
  template: `
    <div class="not-found-container">
      <div class="not-found-content">
        <!-- 404 Hero Section -->
        <div class="error-hero">
          <div class="error-number">404</div>
          <mat-icon class="error-icon">sentiment_dissatisfied</mat-icon>
        </div>

        <!-- Error Message -->
        <mat-card class="error-card">
          <mat-card-header>
            <mat-card-title>Page introuvable</mat-card-title>
            <mat-card-subtitle>
              La page que vous recherchez n'existe pas ou a été déplacée
            </mat-card-subtitle>
          </mat-card-header>

          <mat-card-content>
            <div class="error-details">
              <p>Cela peut arriver pour plusieurs raisons :</p>
              <ul>
                <li>L'URL a été mal saisie</li>
                <li>La page a été supprimée ou déplacée</li>
                <li>Vous n'avez pas les permissions nécessaires</li>
                <li>Un lien obsolète vous a amené ici</li>
              </ul>
            </div>

            <div class="helpful-links">
              <h4>Que souhaitez-vous faire ?</h4>
              <div class="link-buttons">
                <button mat-raised-button color="primary" (click)="goHome()">
                  <mat-icon>home</mat-icon>
                  Accueil
                </button>
                
                <button mat-raised-button color="accent" (click)="goToGames()">
                  <mat-icon>sports_esports</mat-icon>
                  Mes Jeux
                </button>
                
                <button mat-button (click)="goBack()">
                  <mat-icon>arrow_back</mat-icon>
                  Page précédente
                </button>
              </div>
            </div>
          </mat-card-content>
        </mat-card>

        <!-- Fun Stats Section -->
        <div class="fun-stats">
          <div class="stat-item">
            <mat-icon>sports_esports</mat-icon>
            <span class="stat-number">147+</span>
            <span class="stat-label">Joueurs Fortnite</span>
          </div>
          
          <div class="stat-item">
            <mat-icon>group</mat-icon>
            <span class="stat-number">50+</span>
            <span class="stat-label">Games actives</span>
          </div>
          
          <div class="stat-item">
            <mat-icon>emoji_events</mat-icon>
            <span class="stat-number">∞</span>
            <span class="stat-label">Fun garanti</span>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .not-found-container {
      min-height: 100vh;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 2rem;
      position: relative;
      overflow: hidden;
    }

    .not-found-container::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background-image: url('data:image/svg+xml,<svg width="60" height="60" viewBox="0 0 60 60" xmlns="http://www.w3.org/2000/svg"><g fill="none" fill-rule="evenodd"><g fill="%23ffffff" fill-opacity="0.1"><circle cx="30" cy="30" r="2"/></g></g></svg>');
      animation: float 20s infinite linear;
    }

    @keyframes float {
      0% { transform: translateY(0px) translateX(0px); }
      50% { transform: translateY(-30px) translateX(15px); }
      100% { transform: translateY(-60px) translateX(0px); }
    }

    .not-found-content {
      max-width: 600px;
      width: 100%;
      text-align: center;
      z-index: 1;
    }

    .error-hero {
      margin-bottom: 2rem;
      position: relative;
    }

    .error-number {
      font-size: 8rem;
      font-weight: 900;
      background: linear-gradient(45deg, #ff6b6b, #4ecdc4);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
      margin: 0;
      line-height: 1;
      text-shadow: 0 0 50px rgba(255, 107, 107, 0.3);
    }

    .error-icon {
      font-size: 4rem;
      color: rgba(255, 255, 255, 0.8);
      margin-top: 1rem;
    }

    .error-card {
      background: rgba(255, 255, 255, 0.95);
      backdrop-filter: blur(10px);
      border-radius: 16px;
      margin-bottom: 2rem;
      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
    }

    .error-details ul {
      text-align: left;
      margin: 1rem 0;
      padding-left: 1rem;
    }

    .error-details li {
      margin: 0.5rem 0;
      color: rgba(0, 0, 0, 0.7);
    }

    .helpful-links {
      margin-top: 1.5rem;
    }

    .helpful-links h4 {
      margin-bottom: 1rem;
      color: rgba(0, 0, 0, 0.8);
    }

    .link-buttons {
      display: flex;
      flex-direction: column;
      gap: 1rem;
      align-items: center;
    }

    .link-buttons button {
      min-width: 200px;
    }

    .fun-stats {
      display: flex;
      justify-content: space-around;
      gap: 2rem;
      margin-top: 2rem;
    }

    .stat-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      color: rgba(255, 255, 255, 0.9);
      text-align: center;
    }

    .stat-item mat-icon {
      font-size: 2rem;
      margin-bottom: 0.5rem;
      color: #4ecdc4;
    }

    .stat-number {
      font-size: 1.5rem;
      font-weight: 700;
      color: #fff;
    }

    .stat-label {
      font-size: 0.8rem;
      opacity: 0.8;
      margin-top: 0.25rem;
    }

    @media (max-width: 768px) {
      .not-found-container {
        padding: 1rem;
      }
      
      .error-number {
        font-size: 6rem;
      }
      
      .fun-stats {
        flex-direction: column;
        gap: 1rem;
      }
      
      .link-buttons {
        width: 100%;
      }
      
      .link-buttons button {
        width: 100%;
        min-width: auto;
      }
    }

    @media (max-width: 480px) {
      .error-number {
        font-size: 4rem;
      }
      
      .error-card {
        margin: 0 0.5rem 1rem 0.5rem;
      }
    }
  `]
})
export class NotFoundComponent {
  constructor(
    private router: Router,
    private location: Location
  ) {}

  goHome(): void {
    this.router.navigate(['/']);
  }

  goToGames(): void {
    this.router.navigate(['/games']);
  }

  goBack(): void {
    // Try to go back in history, fallback to games if no history
    if (window.history.length > 1) {
      this.location.back();
    } else {
      this.router.navigate(['/games']);
    }
  }
}