import { Component } from '@angular/core';
import { RouterModule, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { UserContextService } from './core/services/user-context.service';
import { environment } from '../environments/environment';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule
  ],
  template: `
    <!-- Skip Links pour l'accessibilité -->
    <div class="skip-links">
      <a href="#main-content" class="skip-link sr-only sr-only-focusable">
        Aller au contenu principal
      </a>
    </div>

    <!-- Région live pour les annonces -->
    <div aria-live="polite" aria-atomic="true" class="live-region" id="announcements">
    </div>

    <!-- Contenu principal - Laisse la navigation à main-layout -->
    <main role="main" id="main-content" class="content">
      <router-outlet></router-outlet>
    </main>
  `,
  styles: [`
    .content {
      min-height: 100vh;
    }

    .skip-links {
      position: absolute;
      top: -40px;
      left: 6px;
      z-index: 9999;
    }

    .skip-link {
      position: absolute;
      top: -40px;
      left: 6px;
      color: white;
      padding: 8px;
      text-decoration: none;
      background: #000;
    }

    .skip-link:focus {
      top: 6px;
    }

    .live-region {
      position: absolute;
      left: -10000px;
      width: 1px;
      height: 1px;
      overflow: hidden;
    }
  `]
})
export class AppComponent {
  constructor(
    private userContextService: UserContextService,
    private router: Router
  ) {}

  isDevelopment(): boolean {
    return !environment.production;
  }
} 