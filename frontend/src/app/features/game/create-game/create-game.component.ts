import { Component, OnInit, inject } from '@angular/core';
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
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';

import { finalize } from 'rxjs/operators';
import { GameService } from '../services/game.service';
import { CreateGameRequest } from '../models/game.interface';
import { UserGamesStore } from '../../../core/services/user-games.store';
import { TranslationService } from '../../../core/services/translation.service';
import { UiErrorFeedbackService } from '../../../core/services/ui-error-feedback.service';
import { LoggerService } from '../../../core/services/logger.service';
import { buildBalancedRegionRules } from './create-game-region-rules.util';

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
    MatTooltipModule,
    MatCheckboxModule,
    MatSlideToggleModule
  ],
  templateUrl: './create-game.component.html',
  styleUrls: ['./create-game.component.scss']
})
export class CreateGameComponent implements OnInit {
  private readonly DEFAULT_MAX_PARTICIPANTS = 5;
  private readonly DEFAULT_REGION_CONFIG =
    buildBalancedRegionRules(this.DEFAULT_MAX_PARTICIPANTS);

  public readonly t = inject(TranslationService);

  gameForm!: FormGroup;
  loading = false;
  error: string | null = null;
  selectedRegion = '';
  selectedCount = 1;
  useDefaultConfig = true;
  showAdvancedOptions = false; // Mode simplifie par defaut
  gameType = 'casual'; // Default game type

  constructor(
    private formBuilder: FormBuilder,
    private gameService: GameService,
    private router: Router,
    private userGamesStore: UserGamesStore,
    private readonly uiFeedback: UiErrorFeedbackService,
    private readonly logger: LoggerService
  ) { }

  ngOnInit(): void {
    this.initForm();
  }

  private initForm(): void {
    this.gameForm = this.formBuilder.group({
      name: ['', [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(30)
      ]],
      maxParticipants: [this.DEFAULT_MAX_PARTICIPANTS, [
        Validators.required,
        Validators.min(2),
        Validators.max(10)
      ]],
      regionRules: [{}], // Simplified - no complex region rules by default
      useDefaultConfig: [true],
      tranchesEnabled: [true]
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
    this.gameForm.patchValue({
      regionRules: this.DEFAULT_REGION_CONFIG,
      maxParticipants: this.DEFAULT_MAX_PARTICIPANTS
    });
  }

  private clearRegionRules(): void {
    this.gameForm.patchValue({
      regionRules: {},
      maxParticipants: this.DEFAULT_MAX_PARTICIPANTS
    });
  }

  private calculateTotalPlayers(regionRules: { [key: string]: number }): number {
    return Object.values(regionRules).reduce((sum, count) => sum + count, 0);
  }

  private resolveMaxParticipants(): number {
    const rawValue = this.showAdvancedOptions
      ? this.gameForm.value.maxParticipants
      : this.DEFAULT_MAX_PARTICIPANTS;
    return Number(rawValue);
  }

  private resolveRegionRules(maxParticipants: number): { [key: string]: number } {
    const formRegionRules = this.gameForm.value.regionRules ?? {};
    const hasExplicitRules = Object.keys(formRegionRules).length > 0;

    if (
      this.showAdvancedOptions &&
      hasExplicitRules &&
      this.calculateTotalPlayers(formRegionRules) === maxParticipants
    ) {
      return { ...formRegionRules };
    }

    return buildBalancedRegionRules(maxParticipants);
  }

  onSubmit(): void {
    if (this.gameForm.invalid) {
      return;
    }

    const maxParticipants = this.resolveMaxParticipants();
    const regionRules = this.resolveRegionRules(maxParticipants);

    const formData: CreateGameRequest = {
      name: this.gameForm.value.name,
      maxParticipants,
      description: `Game creee le ${new Date().toLocaleDateString()}`,
      isPrivate: false,
      autoStartDraft: true,
      draftTimeLimit: 300, // 5 minutes per pick
      autoPickDelay: 43200, // 12 hours for auto-pick
      currentSeason: new Date().getFullYear(),
      regionRules,
      tranchesEnabled: this.gameForm.value.tranchesEnabled ?? true
    };

    this.loading = true;
    this.error = null;

    this.gameService.createGame(formData).pipe(
      finalize(() => { this.loading = false; })
    ).subscribe({
      next: (game) => {
        this.userGamesStore.addGame(game);
        this.uiFeedback.showSuccessMessage(this.buildCreateSuccessMessage(game.invitationCode), 2000);
        this.router.navigate(['/games', game.id], {
          queryParams: { created: 'true' }
        });
      },
      error: (error) => {
        this.error = this.uiFeedback.showError(error, 'games.create.errorCreate', { duration: 5000 });
        this.logger.error('CreateGameComponent: failed to create game', {
          formName: this.gameForm?.value?.name,
          error
        });
      }
    });
  }

  addRegionRule(region: string, count: number): void {
    const regionRules = this.gameForm.get('regionRules')?.value || {};
    regionRules[region] = count;
    this.gameForm.patchValue({ regionRules });

    const totalPlayers = this.calculateTotalPlayers(regionRules);
    this.gameForm.patchValue({ maxParticipants: totalPlayers });
  }

  removeRegionRule(region: string): void {
    const regionRules = this.gameForm.get('regionRules')?.value || {};
    delete regionRules[region];
    this.gameForm.patchValue({ regionRules });

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
    const playersPerRegion = Object.values(this.DEFAULT_REGION_CONFIG)[0];
    return `${totalPlayers} joueurs repartis sur ${regions} regions (${playersPerRegion} par region)`;
  }

  private buildCreateSuccessMessage(invitationCode?: string): string {
    if (!invitationCode) {
      return 'Game creee ! Generez un code d\'invitation depuis la page de la partie.';
    }

    return `Game creee ! Code d'invitation : ${invitationCode}`;
  }
}
