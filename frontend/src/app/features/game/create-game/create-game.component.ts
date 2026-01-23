import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatCheckboxModule } from '@angular/material/checkbox';

import { GameService } from '../services/game.service';
import { CreateGameRequest } from '../models/game.interface';
import { UserGamesStore } from '../../../core/services/user-games.store';

@Component({
  selector: 'app-create-game',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatCheckboxModule
  ],
  templateUrl: './create-game.component.html',
  styleUrls: ['./create-game.component.scss']
})
export class CreateGameComponent implements OnInit {
  gameForm!: FormGroup;
  loading = false;
  error: string | null = null;
  selectedRegion = '';
  selectedCount = 1;
  useDefaultConfig = true;
  showAdvancedOptions = false; // Mode simplifié par défaut
  gameType = 'casual'; // Default game type

  // Configuration par défaut : 7 joueurs par région
  private readonly DEFAULT_REGION_CONFIG = {
    'EU': 7,
    'NAC': 7,
    'BR': 7,
    'ASIA': 7,
    'OCE': 7,
    'NAW': 7,
    'ME': 7
  };

  constructor(
    private formBuilder: FormBuilder,
    private gameService: GameService,
    private router: Router,
    private snackBar: MatSnackBar,
    private userGamesStore: UserGamesStore
  ) { }

  ngOnInit(): void {
    this.initForm();
  }

  private initForm(): void {
    this.gameForm = this.formBuilder.group({
      name: ['', [
        Validators.required,
        Validators.minLength(2),
        Validators.maxLength(30)
      ]],
      maxParticipants: [5, [
        Validators.required,
        Validators.min(2),
        Validators.max(10)
      ]],
      regionRules: [{}], // Simplified - no complex region rules by default
      useDefaultConfig: [true]
    });
  }

  onDefaultConfigChange(): void {
    if (this.useDefaultConfig) {
      this.applyDefaultConfig();
    } else {
      this.clearRegionRules();
    }
  }

  private applyDefaultConfig(): void {
    const totalPlayers = Object.values(this.DEFAULT_REGION_CONFIG).reduce((sum, count) => sum + count, 0);
    
    this.gameForm.patchValue({
      regionRules: this.DEFAULT_REGION_CONFIG,
      maxParticipants: totalPlayers
    });
  }

  private clearRegionRules(): void {
    this.gameForm.patchValue({
      regionRules: {},
      maxParticipants: 5
    });
  }

  private calculateTotalPlayers(regionRules: { [key: string]: number }): number {
    return Object.values(regionRules).reduce((sum, count) => sum + (count as number), 0);
  }

  onSubmit(): void {
    if (this.gameForm.invalid) {
      return;
    }

    this.loading = true;
    this.error = null;

    // Ultra-simplified game creation with smart defaults
    const formData: CreateGameRequest = {
      name: this.gameForm.value.name,
      maxParticipants: this.showAdvancedOptions ? this.gameForm.value.maxParticipants : 5,
      description: `Game créée le ${new Date().toLocaleDateString()}`,
      isPrivate: false,
      autoStartDraft: true,
      draftTimeLimit: 300, // 5 minutes per pick
      autoPickDelay: 43200, // 12 hours for auto-pick
      currentSeason: 2025,
      regionRules: {} // Simplified - no complex region rules for now
    };

    this.gameService.createGame(formData).subscribe({
      next: (game) => {
        this.loading = false;
        // Add the new game to the store so sidebar refreshes immediately
        this.userGamesStore.addGame(game);
        this.snackBar.open('🎉 Game créée ! Invitation envoyée', '', {
          duration: 2000,
          panelClass: 'success-snackbar'
        });
        // Navigate directly to the game to start playing
        this.router.navigate(['/games', game.id], {
          queryParams: { created: 'true' }
        });
      },
      error: (error) => {
        this.loading = false;
        this.error = 'Impossible de créer la game. Réessayez ?';
        console.error('Error creating game:', error);
      }
    });
  }

  addRegionRule(region: string, count: number): void {
    const regionRules = this.gameForm.get('regionRules')?.value || {};
    regionRules[region] = count;
    this.gameForm.patchValue({ regionRules });
    
    // Mettre à jour le nombre total de participants
    const totalPlayers = this.calculateTotalPlayers(regionRules);
    this.gameForm.patchValue({ maxParticipants: totalPlayers });
  }

  removeRegionRule(region: string): void {
    const regionRules = this.gameForm.get('regionRules')?.value || {};
    delete regionRules[region];
    this.gameForm.patchValue({ regionRules });
    
    // Mettre à jour le nombre total de participants
    const totalPlayers = this.calculateTotalPlayers(regionRules);
    this.gameForm.patchValue({ maxParticipants: Math.max(totalPlayers, 2) });
  }

  getAvailableRegions(): string[] {
    return ['EU', 'NAC', 'BR', 'ASIA', 'OCE', 'NAW', 'ME'];
  }

  getRegionLabel(region: string): string {
    const labels: { [key: string]: string } = {
      'EU': 'Europe',
      'NAC': 'North America Central',
      'BR': 'Brazil',
      'ASIA': 'Asia',
      'OCE': 'Oceania',
      'NAW': 'North America West',
      'ME': 'Middle East'
    };
    return labels[region] || region;
  }

  onCancel(): void {
    this.router.navigate(['/games']);
  }

  getRegionRules(): { [key: string]: number } {
    return this.gameForm.get('regionRules')?.value || {};
  }

  getRegionRulesEntries(): [string, number][] {
    return Object.entries(this.getRegionRules());
  }

  getTotalRegionPlayers(): number {
    return Object.values(this.getRegionRules()).reduce((sum, count) => sum + count, 0);
  }

  canAddRegionRule(): boolean {
    const maxParticipants = this.gameForm.get('maxParticipants')?.value || 0;
    const totalRegionPlayers = this.getTotalRegionPlayers();
    return totalRegionPlayers < maxParticipants;
  }

  getRemainingSlots(): number {
    const maxParticipants = this.gameForm.get('maxParticipants')?.value || 0;
    const totalRegionPlayers = this.getTotalRegionPlayers();
    return Math.max(0, maxParticipants - totalRegionPlayers);
  }

  getDefaultConfigSummary(): string {
    const totalPlayers = Object.values(this.DEFAULT_REGION_CONFIG).reduce((sum, count) => sum + count, 0);
    const regions = Object.keys(this.DEFAULT_REGION_CONFIG).length;
    return `${totalPlayers} joueurs répartis sur ${regions} régions (7 par région)`;
  }
} 