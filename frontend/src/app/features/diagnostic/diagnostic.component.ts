import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

interface DiagnosticInfo {
  label: string;
  value: string;
  status: 'success' | 'warning' | 'error';
  details?: string;
}

@Component({
  selector: 'app-diagnostic',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatListModule,
    MatDividerModule,
    MatProgressBarModule
  ],
  template: `
    <div class="diagnostic-container">
      <mat-card class="diagnostic-card">
        <mat-card-header>
          <mat-card-title>
            <mat-icon>bug_report</mat-icon>
            Diagnostic Système
          </mat-card-title>
          <mat-card-subtitle>
            Vérification des composants de l'application
          </mat-card-subtitle>
        </mat-card-header>

        <mat-card-content>
          <div class="diagnostic-progress" *ngIf="isRunning">
            <mat-progress-bar mode="indeterminate"></mat-progress-bar>
            <p>Diagnostic en cours...</p>
          </div>

          <mat-list *ngIf="!isRunning && diagnostics.length > 0">
            <mat-list-item *ngFor="let diagnostic of diagnostics">
              <mat-icon matListItemIcon [ngClass]="getIconClass(diagnostic.status)">
                {{ getIcon(diagnostic.status) }}
              </mat-icon>
              <div matListItemTitle>{{ diagnostic.label }}</div>
              <div matListItemLine>{{ diagnostic.value }}</div>
              <div matListItemLine *ngIf="diagnostic.details" class="diagnostic-details">
                {{ diagnostic.details }}
              </div>
            </mat-list-item>
            <mat-divider *ngIf="diagnostics.length > 0"></mat-divider>
          </mat-list>

          <div class="diagnostic-summary" *ngIf="!isRunning && diagnostics.length > 0">
            <p>
              <strong>Résumé:</strong> 
              {{ getSuccessCount() }} OK, 
              {{ getWarningCount() }} Avertissements, 
              {{ getErrorCount() }} Erreurs
            </p>
          </div>
        </mat-card-content>

        <mat-card-actions>
          <button mat-raised-button color="primary" (click)="runDiagnostic()" [disabled]="isRunning">
            <mat-icon>refresh</mat-icon>
            {{ isRunning ? 'Diagnostic en cours...' : 'Relancer diagnostic' }}
          </button>
          <button mat-button (click)="goBack()">
            <mat-icon>arrow_back</mat-icon>
            Retour
          </button>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`
    .diagnostic-container {
      padding: 2rem;
      max-width: 800px;
      margin: 0 auto;
    }

    .diagnostic-card {
      width: 100%;
    }

    .diagnostic-progress {
      text-align: center;
      padding: 2rem 0;
    }

    .diagnostic-progress p {
      margin-top: 1rem;
      color: rgba(0, 0, 0, 0.6);
    }

    .diagnostic-details {
      font-size: 0.8rem;
      color: rgba(0, 0, 0, 0.5);
      font-style: italic;
    }

    .diagnostic-summary {
      margin-top: 1rem;
      padding: 1rem;
      background-color: #f5f5f5;
      border-radius: 4px;
    }

    .success {
      color: #4caf50;
    }

    .warning {
      color: #ff9800;
    }

    .error {
      color: #f44336;
    }

    mat-card-header mat-icon {
      margin-right: 0.5rem;
    }

    mat-card-actions {
      display: flex;
      gap: 1rem;
    }
  `]
})
export class DiagnosticComponent implements OnInit {
  diagnostics: DiagnosticInfo[] = [];
  isRunning = false;

  constructor(
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.runDiagnostic();
  }

  async runDiagnostic(): Promise<void> {
    this.isRunning = true;
    this.diagnostics = [];

    try {
      // Test 1: Frontend Environment
      this.diagnostics.push({
        label: 'Environment Frontend',
        value: environment.production ? 'Production' : 'Développement',
        status: 'success'
      });

      // Test 2: API Base URL
      this.diagnostics.push({
        label: 'URL API configurée',
        value: environment.apiBaseUrl || 'Non configurée',
        status: environment.apiBaseUrl ? 'success' : 'error',
        details: environment.apiBaseUrl ? undefined : 'API URL manquante dans environment'
      });

      // Test 3: Local Storage
      const storageTest = this.testLocalStorage();
      this.diagnostics.push(storageTest);

      // Test 4: Session Storage
      const sessionTest = this.testSessionStorage();
      this.diagnostics.push(sessionTest);

      // Test 5: Backend Connectivity
      if (environment.apiBaseUrl) {
        const backendTest = await this.testBackendConnectivity();
        this.diagnostics.push(backendTest);
      }

      // Test 6: Browser Features
      const browserTest = this.testBrowserFeatures();
      this.diagnostics.push(browserTest);

    } catch (error) {
      this.diagnostics.push({
        label: 'Erreur Diagnostic',
        value: 'Erreur inattendue',
        status: 'error',
        details: error instanceof Error ? error.message : 'Erreur inconnue'
      });
    } finally {
      this.isRunning = false;
    }
  }

  private testLocalStorage(): DiagnosticInfo {
    try {
      const testKey = 'diagnostic-test';
      localStorage.setItem(testKey, 'test');
      localStorage.removeItem(testKey);
      return {
        label: 'Local Storage',
        value: 'Fonctionnel',
        status: 'success'
      };
    } catch (error) {
      return {
        label: 'Local Storage',
        value: 'Non disponible',
        status: 'error',
        details: 'Local Storage nécessaire pour l\'application'
      };
    }
  }

  private testSessionStorage(): DiagnosticInfo {
    try {
      const testKey = 'diagnostic-session-test';
      sessionStorage.setItem(testKey, 'test');
      sessionStorage.removeItem(testKey);
      return {
        label: 'Session Storage',
        value: 'Fonctionnel',
        status: 'success'
      };
    } catch (error) {
      return {
        label: 'Session Storage',
        value: 'Non disponible',
        status: 'error',
        details: 'Session Storage nécessaire pour l\'authentification'
      };
    }
  }

  private async testBackendConnectivity(): Promise<DiagnosticInfo> {
    try {
      const response = await this.http.get(`${environment.apiBaseUrl}/actuator/health`).toPromise();
      
      return {
        label: 'Connectivité Backend',
        value: 'Connecté',
        status: 'success',
        details: `API disponible sur ${environment.apiBaseUrl}`
      };
    } catch (error) {
      return {
        label: 'Connectivité Backend',
        value: 'Non disponible',
        status: 'warning',
        details: `Impossible de contacter ${environment.apiBaseUrl}`
      };
    }
  }

  private testBrowserFeatures(): DiagnosticInfo {
    const features = [];
    
    if (typeof fetch === 'function') features.push('Fetch API');
    if ('serviceWorker' in navigator) features.push('Service Worker');
    if (typeof WebSocket !== 'undefined') features.push('WebSocket');
    if (typeof Promise !== 'undefined') features.push('Promises');
    
    return {
      label: 'Fonctionnalités Navigateur',
      value: `${features.length} disponibles`,
      status: features.length >= 3 ? 'success' : 'warning',
      details: features.join(', ')
    };
  }

  getIcon(status: string): string {
    switch (status) {
      case 'success': return 'check_circle';
      case 'warning': return 'warning';
      case 'error': return 'error';
      default: return 'help';
    }
  }

  getIconClass(status: string): string {
    return status;
  }

  getSuccessCount(): number {
    return this.diagnostics.filter(d => d.status === 'success').length;
  }

  getWarningCount(): number {
    return this.diagnostics.filter(d => d.status === 'warning').length;
  }

  getErrorCount(): number {
    return this.diagnostics.filter(d => d.status === 'error').length;
  }

  goBack(): void {
    this.router.navigate(['/games']);
  }
}