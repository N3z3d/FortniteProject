import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, takeUntil } from 'rxjs';

import { TeamService } from '../../../core/services/team.service';
import { TranslationService } from '../../../core/services/translation.service';
import { formatPoints as formatPointsUtil } from '../../../shared/constants/theme.constants';

interface Player {
  id: string;
  nickname: string;
  region: string;
  tranche: string;
  points?: number;
}

interface TeamEditData {
  id: string;
  name: string;
  season: number;
  ownerUsername: string;
  totalScore: number;
  players: Array<{
    playerId: string;
    nickname: string;
    region: string;
    tranche: string;
  }>;
}

@Component({
  selector: 'app-team-edit',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDividerModule,
    MatTooltipModule
  ],
  templateUrl: './team-edit.component.html',
  styleUrls: ['./team-edit.component.scss']
})
export class TeamEditComponent implements OnInit, OnDestroy {
  teamForm!: FormGroup;
  teamId: string | null = null;
  gameId: string | null = null;
  loading = true;
  saving = false;
  team: TeamEditData | null = null;
  players: Player[] = [];

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly fb: FormBuilder,
    private readonly teamService: TeamService,
    private readonly snackBar: MatSnackBar,
    public readonly t: TranslationService
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe(params => {
      this.teamId = params['id'] || null;
      this.gameId = params['gameId'] || null;

      if (this.teamId) {
        this.loadTeam();
      } else {
        this.loading = false;
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initForm(): void {
    this.teamForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
      description: ['', [Validators.maxLength(200)]]
    });
  }

  private loadTeam(): void {
    this.loading = true;

    // Simuler le chargement d'une équipe
    setTimeout(() => {
      // Mock data
      const teamData: TeamEditData = {
        id: this.teamId!,
        name: 'Équipe Thibaut',
        season: 2025,
        ownerUsername: 'Thibaut',
        totalScore: 892041,
        players: [
          { playerId: '1', nickname: 'Mero', region: 'EU', tranche: 'T1' },
          { playerId: '2', nickname: 'Reet', region: 'NAW', tranche: 'T1' },
          { playerId: '3', nickname: 'Jahq', region: 'NAW', tranche: 'T2' },
          { playerId: '4', nickname: 'Kami', region: 'EU', tranche: 'T2' },
          { playerId: '5', nickname: 'Queasy', region: 'EU', tranche: 'T1' }
        ]
      };

      this.team = teamData;

      this.players = teamData.players.map(p => ({
        id: p.playerId,
        nickname: p.nickname,
        region: p.region,
        tranche: p.tranche,
        points: Math.floor(Math.random() * 300000) + 50000
      }));

      this.teamForm.patchValue({
        name: teamData.name,
        description: ''
      });

      this.loading = false;
    }, 800);
  }

  onSave(): void {
    if (this.teamForm.invalid) {
      this.snackBar.open(this.t.t('teams.edit.snackbar.formInvalid'), this.t.t('common.close'), {
        duration: 3000
      });
      return;
    }

    this.saving = true;

    // Simuler la sauvegarde
    setTimeout(() => {
      this.snackBar.open(this.t.t('teams.edit.snackbar.updated'), this.t.t('teams.edit.snackbar.viewAction'), {
        duration: 3000
      }).onAction().subscribe(() => {
        this.goBack();
      });

      this.saving = false;
    }, 1000);
  }

  onCancel(): void {
    this.goBack();
  }

  goBack(): void {
    if (this.gameId) {
      this.router.navigate(['/games', this.gameId, 'teams']);
    } else {
      this.router.navigate(['/games']);
    }
  }

  removePlayer(player: Player): void {
    const index = this.players.findIndex(p => p.id === player.id);
    if (index > -1) {
      this.players.splice(index, 1);
      this.snackBar.open(
        this.formatTemplate(this.t.t('teams.edit.snackbar.playerRemoved'), { player: player.nickname }),
        this.t.t('common.cancel'),
        {
          duration: 5000
        }
      ).onAction().subscribe(() => {
        // Restaurer le joueur
        this.players.push(player);
        this.snackBar.open(this.t.t('teams.edit.snackbar.playerRestored'), this.t.t('common.close'), {
          duration: 2000
        });
      });
    }
  }

  getRegionColor(region: string): string {
    const colors: { [key: string]: string } = {
      'EU': '#4CAF50',
      'NAW': '#2196F3',
      'NAC': '#FF9800',
      'BR': '#FFD700',
      'ASIA': '#E91E63',
      'OCE': '#9C27B0',
      'ME': '#FF5722'
    };
    return colors[region] || '#757575';
  }

  getTrancheLabel(tranche: string): string {
    const labels: { [key: string]: string } = {
      'T1': 'Tier 1 - Elite',
      'T2': 'Tier 2 - Pro',
      'T3': 'Tier 3 - Rising',
      'T4': 'Tier 4 - Amateur',
      'T5': 'Tier 5 - Rookie'
    };
    return labels[tranche] || tranche;
  }

  formatPoints(points: number): string {
    return formatPointsUtil(points);
  }

  get nameError(): string {
    const control = this.teamForm.get('name');
    if (control?.hasError('required')) {
      return this.t.t('teams.edit.validation.nameRequired');
    }
    if (control?.hasError('minlength')) {
      return this.t.t('teams.edit.validation.nameMinLength');
    }
    if (control?.hasError('maxlength')) {
      return this.t.t('teams.edit.validation.nameMaxLength');
    }
    return '';
  }

  getRosterSubtitle(): string {
    const count = this.players.length;
    const key = count === 1 ? 'teams.edit.rosterSubtitleSingle' : 'teams.edit.rosterSubtitleMultiple';
    return this.formatTemplate(this.t.t(key), { count });
  }

  getRemovePlayerTooltip(nickname: string): string {
    return this.formatTemplate(this.t.t('teams.edit.removePlayerTooltip'), { nickname });
  }

  private formatTemplate(template: string, params: Record<string, string | number>): string {
    return Object.entries(params).reduce((result, [key, value]) => {
      return result.replace(`{${key}}`, String(value));
    }, template);
  }

  trackByPlayerId(index: number, player: Player): string {
    return player.id;
  }
}
