import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { AuthSwitchService } from '../../../core/services/auth-switch.service';
import { UserContextService, UserProfile } from '../../../core/services/user-context.service';

@Component({
  selector: 'app-dev-user-switch',
  standalone: true,
  imports: [CommonModule, MatSelectModule, MatFormFieldModule],
  template: `
    <mat-form-field appearance="fill" class="dev-switch">
      <mat-label>Profil</mat-label>
      <mat-select [value]="current" (selectionChange)="onChange($event.value)" [disabled]="switching">
        <mat-option *ngFor="let u of users" [value]="u">
          {{ u.username }}{{ u.id === current?.id ? ' (actuel)' : '' }}
        </mat-option>
      </mat-select>
      <mat-hint *ngIf="switching">Changement en cours...</mat-hint>
    </mat-form-field>
  `,
  styles: [`.dev-switch { min-width: 120px; margin-right:1rem;}`]
})
export class DevUserSwitchComponent {
  users: UserProfile[];
  current: UserProfile | null;
  switching = false;

  constructor(
    private authSwitch: AuthSwitchService,
    private userContext: UserContextService
  ) {
    this.users = this.userContext.getAvailableProfiles();
    this.current = this.userContext.getCurrentUser();
  }

  onChange(user: UserProfile) {
    if (user.id === this.current?.id) return; // Pas de changement nécessaire
    
    console.log(`Changement de profil vers: ${user.username}`);
    this.switching = true;
    
    // 1. Mettre à jour le contexte utilisateur
    this.userContext.login(user);
    
    // 2. Appel API backend en arrière-plan (optionnel)
    this.authSwitch.switchUser(user.username).subscribe({
      next: () => console.log(`API backend notifiée du changement vers ${user.username}`),
      error: (err) => console.warn('Erreur API backend (ignorée):', err)
    });
    
    // 3. Rechargement immédiat de la page
    console.log('Rechargement de la page...');
    setTimeout(() => {
      location.reload();
    }, 100); // Petit délai pour laisser le localStorage se mettre à jour
  }
} 