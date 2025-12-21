import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { LoggerService } from '../../../core/services/logger.service';

interface Player {
  id: string;
  username: string;
  region: string;
  isAvailable: boolean;
}

interface Team {
  id: string;
  name: string;
  owner: string;
  players: Player[];
}

@Component({
  selector: 'app-trade-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  template: `
    <div class="trade-form-container">
      <mat-card class="trade-form-card">
        <mat-card-header>
          <mat-card-title>
            <mat-icon>swap_horiz</mat-icon>
            {{ isEditMode ? 'Modifier Trade' : 'Nouveau Trade' }}
          </mat-card-title>
          <mat-card-subtitle>
            Créez un échange de joueurs entre équipes
          </mat-card-subtitle>
        </mat-card-header>

        <mat-card-content>
          <form [formGroup]="tradeForm" (ngSubmit)="onSubmit()">
            
            <!-- Team Selection -->
            <div class="form-section">
              <h3>
                <mat-icon>group</mat-icon>
                Équipe concernée
              </h3>
              
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Sélectionner l'équipe</mat-label>
                <mat-select formControlName="teamId" (selectionChange)="onTeamChange($event.value)">
                  <mat-option *ngFor="let team of availableTeams" [value]="team.id">
                    {{ team.name }} ({{ team.owner }})
                  </mat-option>
                </mat-select>
              </mat-form-field>
            </div>

            <!-- Player Out Selection -->
            <div class="form-section" *ngIf="selectedTeam">
              <h3>
                <mat-icon color="warn">person_remove</mat-icon>
                Joueur à échanger (sortant)
              </h3>
              
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Joueur à faire sortir</mat-label>
                <mat-select formControlName="playerOutId">
                  <mat-option *ngFor="let player of selectedTeam.players" [value]="player.id">
                    {{ player.username }} ({{ player.region }})
                  </mat-option>
                </mat-select>
              </mat-form-field>
            </div>

            <!-- Player In Selection -->
            <div class="form-section">
              <h3>
                <mat-icon color="primary">person_add</mat-icon>
                Joueur à récupérer (entrant)
              </h3>
              
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Joueur à faire entrer</mat-label>
                <mat-select formControlName="playerInId">
                  <mat-option *ngFor="let player of availablePlayers" [value]="player.id">
                    {{ player.username }} ({{ player.region }})
                  </mat-option>
                </mat-select>
              </mat-form-field>
            </div>

            <!-- Trade Preview -->
            <div class="trade-preview" *ngIf="tradeForm.get('playerOutId')?.value && tradeForm.get('playerInId')?.value">
              <h3>
                <mat-icon>preview</mat-icon>
                Aperçu du trade
              </h3>
              
              <div class="preview-content">
                <div class="preview-flow">
                  <div class="player-preview player-out">
                    <div class="player-info">
                      <strong>{{ getPlayerUsername(tradeForm.get('playerOutId')?.value) }}</strong>
                      <span class="region">{{ getPlayerRegion(tradeForm.get('playerOutId')?.value) }}</span>
                      <span class="status out">Sortant</span>
                    </div>
                  </div>
                  
                  <mat-icon class="trade-arrow">swap_horiz</mat-icon>
                  
                  <div class="player-preview player-in">
                    <div class="player-info">
                      <strong>{{ getPlayerUsername(tradeForm.get('playerInId')?.value) }}</strong>
                      <span class="region">{{ getPlayerRegion(tradeForm.get('playerInId')?.value) }}</span>
                      <span class="status in">Entrant</span>
                    </div>
                  </div>
                </div>
                
                <div class="team-info" *ngIf="selectedTeam">
                  <p><strong>Équipe:</strong> {{ selectedTeam.name }}</p>
                  <p><strong>Propriétaire:</strong> {{ selectedTeam.owner }}</p>
                </div>
              </div>
            </div>

            <!-- Validation Errors -->
            <div class="validation-errors" *ngIf="hasValidationErrors()">
              <mat-icon color="warn">warning</mat-icon>
              <div class="error-messages">
                <p *ngIf="tradeForm.get('teamId')?.hasError('required')">
                  Veuillez sélectionner une équipe
                </p>
                <p *ngIf="tradeForm.get('playerOutId')?.hasError('required')">
                  Veuillez sélectionner un joueur sortant
                </p>
                <p *ngIf="tradeForm.get('playerInId')?.hasError('required')">
                  Veuillez sélectionner un joueur entrant
                </p>
                <p *ngIf="hasSamePlayerError()">
                  Le joueur sortant et entrant ne peuvent pas être identiques
                </p>
              </div>
            </div>
          </form>
        </mat-card-content>

        <mat-card-actions>
          <button mat-button (click)="goBack()" [disabled]="isSubmitting">
            <mat-icon>arrow_back</mat-icon>
            Annuler
          </button>
          
          <button 
            mat-raised-button 
            color="primary" 
            (click)="onSubmit()"
            [disabled]="!tradeForm.valid || isSubmitting || hasSamePlayerError()">
            <mat-spinner diameter="20" *ngIf="isSubmitting"></mat-spinner>
            <mat-icon *ngIf="!isSubmitting">{{ isEditMode ? 'save' : 'add' }}</mat-icon>
            {{ isSubmitting ? 'Création...' : (isEditMode ? 'Sauvegarder' : 'Créer Trade') }}
          </button>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`
    .trade-form-container {
      padding: 2rem;
      max-width: 800px;
      margin: 0 auto;
    }

    .trade-form-card {
      width: 100%;
    }

    .form-section {
      margin: 2rem 0;
    }

    .form-section h3 {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin-bottom: 1rem;
      color: rgba(0, 0, 0, 0.8);
    }

    .full-width {
      width: 100%;
    }

    .trade-preview {
      margin: 2rem 0;
      padding: 1.5rem;
      background-color: #f5f5f5;
      border-radius: 12px;
      border: 2px solid #e0e0e0;
    }

    .trade-preview h3 {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin-bottom: 1rem;
      color: rgba(0, 0, 0, 0.8);
    }

    .preview-content {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .preview-flow {
      display: flex;
      align-items: center;
      gap: 2rem;
      justify-content: center;
    }

    .player-preview {
      padding: 1rem;
      border-radius: 8px;
      text-align: center;
      min-width: 150px;
    }

    .player-out {
      background-color: #ffebee;
      border: 2px solid #f44336;
    }

    .player-in {
      background-color: #e3f2fd;
      border: 2px solid #2196f3;
    }

    .player-info {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }

    .player-info strong {
      font-size: 1.1rem;
    }

    .region {
      color: rgba(0, 0, 0, 0.6);
      font-size: 0.9rem;
      font-style: italic;
    }

    .status {
      padding: 0.25rem 0.5rem;
      border-radius: 12px;
      font-size: 0.8rem;
      font-weight: 500;
      color: white;
    }

    .status.out {
      background-color: #f44336;
    }

    .status.in {
      background-color: #2196f3;
    }

    .trade-arrow {
      font-size: 2rem;
      height: 2rem;
      width: 2rem;
      color: #4caf50;
    }

    .team-info {
      text-align: center;
      padding: 1rem;
      background-color: white;
      border-radius: 8px;
      border: 1px solid #e0e0e0;
    }

    .team-info p {
      margin: 0.25rem 0;
    }

    .validation-errors {
      display: flex;
      align-items: flex-start;
      gap: 0.5rem;
      padding: 1rem;
      background-color: #ffebee;
      border-radius: 8px;
      border-left: 4px solid #f44336;
      margin: 1rem 0;
    }

    .error-messages p {
      margin: 0.25rem 0;
      color: #d32f2f;
      font-size: 0.9rem;
    }

    mat-card-header mat-icon {
      margin-right: 0.5rem;
    }

    mat-card-actions {
      display: flex;
      justify-content: space-between;
      gap: 1rem;
    }

    mat-card-actions button {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    @media (max-width: 768px) {
      .trade-form-container {
        padding: 1rem;
      }

      .preview-flow {
        flex-direction: column;
        gap: 1rem;
      }

      .trade-arrow {
        transform: rotate(90deg);
      }

      .player-preview {
        width: 100%;
      }

      mat-card-actions {
        flex-direction: column;
      }
    }
  `]
})
export class TradeFormComponent implements OnInit {
  tradeForm: FormGroup;
  isEditMode = false;
  isSubmitting = false;
  availableTeams: Team[] = [];
  availablePlayers: Player[] = [];
  selectedTeam: Team | null = null;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private snackBar: MatSnackBar,
    private logger: LoggerService
  ) {
    this.tradeForm = this.fb.group({
      teamId: ['', Validators.required],
      playerOutId: ['', Validators.required],
      playerInId: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.loadData();
  }

  private loadData(): void {
    // Load mock data
    this.availableTeams = this.getMockTeams();
    this.availablePlayers = this.getMockAvailablePlayers();
  }

  private getMockTeams(): Team[] {
    return [
      {
        id: '1',
        name: 'Team Alpha',
        owner: 'Thibaut',
        players: [
          { id: '1', username: 'Ninja', region: 'NA-EAST', isAvailable: true },
          { id: '2', username: 'Tfue', region: 'NA-WEST', isAvailable: true }
        ]
      },
      {
        id: '2',
        name: 'Team Beta',
        owner: 'Marcel',
        players: [
          { id: '3', username: 'Bugha', region: 'NA-EAST', isAvailable: true },
          { id: '4', username: 'Aqua', region: 'EU', isAvailable: true }
        ]
      }
    ];
  }

  private getMockAvailablePlayers(): Player[] {
    return [
      { id: '5', username: 'Mongraal', region: 'EU', isAvailable: true },
      { id: '6', username: 'Benjyfishy', region: 'EU', isAvailable: true },
      { id: '7', username: 'Clix', region: 'NA-EAST', isAvailable: true },
      { id: '8', username: 'Unknown', region: 'NA-WEST', isAvailable: true }
    ];
  }

  onTeamChange(teamId: string): void {
    this.selectedTeam = this.availableTeams.find(team => team.id === teamId) || null;
    // Reset player out selection when team changes
    this.tradeForm.patchValue({ playerOutId: '' });
  }

  onSubmit(): void {
    if (this.tradeForm.valid && !this.hasSamePlayerError()) {
      this.isSubmitting = true;
      
      const tradeData = this.tradeForm.value;
      this.logger.debug('TradeForm: creating trade', tradeData);
      
      // Simulate API call
      setTimeout(() => {
        this.isSubmitting = false;
        this.snackBar.open('Trade créé avec succès!', 'Fermer', {
          duration: 3000,
          panelClass: ['success-snackbar']
        });
        this.router.navigate(['/trades']);
      }, 2000);
    }
  }

  goBack(): void {
    this.router.navigate(['/trades']);
  }

  hasValidationErrors(): boolean {
    return this.tradeForm.invalid && this.tradeForm.touched;
  }

  hasSamePlayerError(): boolean {
    const playerOutId = this.tradeForm.get('playerOutId')?.value;
    const playerInId = this.tradeForm.get('playerInId')?.value;
    return playerOutId && playerInId && playerOutId === playerInId;
  }

  getPlayerUsername(playerId: string): string {
    const allPlayers = [
      ...this.availableTeams.flatMap(team => team.players),
      ...this.availablePlayers
    ];
    return allPlayers.find(player => player.id === playerId)?.username || '';
  }

  getPlayerRegion(playerId: string): string {
    const allPlayers = [
      ...this.availableTeams.flatMap(team => team.players),
      ...this.availablePlayers
    ];
    return allPlayers.find(player => player.id === playerId)?.region || '';
  }
}
