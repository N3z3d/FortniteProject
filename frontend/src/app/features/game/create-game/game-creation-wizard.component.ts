import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatStepperModule } from '@angular/material/stepper';
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
import { MatRadioModule } from '@angular/material/radio';
import { MatSliderModule } from '@angular/material/slider';

import { GameService } from '../services/game.service';
import { CreateGameRequest } from '../models/game.interface';
import { TranslationService } from '../../../core/services/translation.service';

interface GameTemplate {
  id: string;
  name: string;
  description: string;
  icon: string;
  config: Partial<CreateGameRequest>;
  popular?: boolean;
}

@Component({
  selector: 'app-game-creation-wizard',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    MatStepperModule,
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
    MatCheckboxModule,
    MatRadioModule,
    MatSliderModule
  ],
  templateUrl: './game-creation-wizard.component.html',
  styleUrls: ['./game-creation-wizard.component.scss']
})
export class GameCreationWizardComponent implements OnInit {
  public readonly t = inject(TranslationService);

  // Form Groups for each step
  basicInfoForm!: FormGroup;
  rulesForm!: FormGroup;
  reviewForm!: FormGroup;

  loading = false;
  error: string | null = null;
  selectedTemplate: GameTemplate | null = null;

  // Game Templates - UX-001 requirement (using translation keys)
  getGameTemplates(): GameTemplate[] {
    return [
      {
        id: 'quick-play',
        name: this.t.t('games.wizard.templates.quickPlay'),
        description: this.t.t('games.wizard.templates.quickPlayDesc'),
        icon: 'flash_on',
        popular: true,
        config: {
          maxParticipants: 5,
          draftTimeLimit: 180, // 3 minutes
          autoPickDelay: 3600, // 1 hour
          isPrivate: false
        }
      },
      {
        id: 'championship',
        name: this.t.t('games.wizard.templates.championship'),
        description: this.t.t('games.wizard.templates.championshipDesc'),
        icon: 'emoji_events',
        popular: true,
        config: {
          maxParticipants: 10,
          draftTimeLimit: 300, // 5 minutes
          autoPickDelay: 86400, // 24 hours
          isPrivate: true
        }
      },
      {
        id: 'casual',
        name: this.t.t('games.wizard.templates.casualFun'),
        description: this.t.t('games.wizard.templates.casualFunDesc'),
        icon: 'sports_esports',
        config: {
          maxParticipants: 6,
          draftTimeLimit: 120, // 2 minutes
          autoPickDelay: 7200, // 2 hours
          isPrivate: false
        }
      },
      {
        id: 'custom',
        name: this.t.t('games.wizard.templates.custom'),
        description: this.t.t('games.wizard.templates.customDesc'),
        icon: 'tune',
        config: {
          maxParticipants: 5,
          draftTimeLimit: 300,
          autoPickDelay: 43200,
          isPrivate: false
        }
      }
    ];
  }

  // Keep a cached reference for performance
  private _cachedTemplates: GameTemplate[] = [];

  constructor(
    private formBuilder: FormBuilder,
    private gameService: GameService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.initForms();
  }

  private initForms(): void {
    // Step 1: Basic Information
    this.basicInfoForm = this.formBuilder.group({
      name: ['', [
        Validators.required,
        Validators.minLength(2),
        Validators.maxLength(50)
      ]],
      description: ['', [Validators.maxLength(200)]],
      template: ['quick-play'] // Default to Quick Play
    });

    // Step 2: Player Rules & Configuration
    this.rulesForm = this.formBuilder.group({
      maxParticipants: [5, [
        Validators.required,
        Validators.min(2),
        Validators.max(20)
      ]],
      draftTimeLimit: [180, [
        Validators.required,
        Validators.min(60),
        Validators.max(1800)
      ]],
      autoPickDelay: [3600, [
        Validators.required,
        Validators.min(300),
        Validators.max(172800)
      ]],
      isPrivate: [false],
      autoStartDraft: [true]
    });

    // Step 3: Review (read-only summary)
    this.reviewForm = this.formBuilder.group({});

    // Set default template on init
    this.onTemplateSelect(this.getGameTemplates()[0]);
  }

  onTemplateSelect(template: GameTemplate): void {
    this.selectedTemplate = template;
    
    // Update basic info
    this.basicInfoForm.patchValue({
      template: template.id
    });

    // Apply template configuration to rules
    if (template.config) {
      this.rulesForm.patchValue(template.config);
    }

    // Smart defaults for description if empty
    if (!this.basicInfoForm.get('description')?.value) {
      const smartDescription = this.generateSmartDescription(template);
      this.basicInfoForm.patchValue({
        description: smartDescription
      });
    }
  }

  private generateSmartDescription(template: GameTemplate): string {
    const currentDate = new Date().toLocaleDateString();
    const time = new Date().toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });

    switch (template.id) {
      case 'quick-play':
        return this.t.t('games.wizard.smartDesc.quickPlay').replace('{date}', currentDate).replace('{time}', time);
      case 'championship':
        return this.t.t('games.wizard.smartDesc.championship').replace('{date}', currentDate);
      case 'casual':
        return this.t.t('games.wizard.smartDesc.casual').replace('{date}', currentDate);
      default:
        return this.t.t('games.wizard.smartDesc.custom').replace('{date}', currentDate).replace('{time}', time);
    }
  }

  // Real-time validation feedback - UX-001 requirement
  getNameValidationMessage(): string {
    const nameControl = this.basicInfoForm.get('name');
    if (nameControl?.hasError('required')) {
      return this.t.t('games.wizard.nameRequired');
    }
    if (nameControl?.hasError('minlength')) {
      return this.t.t('games.wizard.minChars');
    }
    if (nameControl?.hasError('maxlength')) {
      return this.t.t('games.wizard.maxChars');
    }
    return '';
  }

  getParticipantsValidationMessage(): string {
    const participantsControl = this.rulesForm.get('maxParticipants');
    if (participantsControl?.hasError('min')) {
      return this.t.t('games.wizard.minParticipants');
    }
    if (participantsControl?.hasError('max')) {
      return this.t.t('games.wizard.maxParticipants');
    }
    return '';
  }

  getDraftTimeLimitLabel(): string {
    const minutes = Math.floor((this.rulesForm.get('draftTimeLimit')?.value || 0) / 60);
    return this.t.t('games.wizard.minutesPerPick').replace('{n}', minutes.toString());
  }

  getAutoPickDelayLabel(): string {
    const seconds = this.rulesForm.get('autoPickDelay')?.value || 0;
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    
    if (hours > 0) {
      return `${hours}h${minutes > 0 ? ` ${minutes}m` : ''}`;
    }
    return `${minutes} minutes`;
  }

  // Step validation
  isStep1Valid(): boolean {
    return this.basicInfoForm.valid;
  }

  isStep2Valid(): boolean {
    return this.rulesForm.valid;
  }

  // Game preview data for step 3
  getGamePreview(): any {
    return {
      ...this.basicInfoForm.value,
      ...this.rulesForm.value,
      template: this.selectedTemplate,
      estimatedDuration: this.calculateEstimatedDuration(),
      createdAt: new Date()
    };
  }

  private calculateEstimatedDuration(): string {
    const maxParticipants = this.rulesForm.get('maxParticipants')?.value || 5;
    const playersPerTeam = 5; // Standard
    const draftTimeLimit = this.rulesForm.get('draftTimeLimit')?.value || 180;
    
    const totalPicks = maxParticipants * playersPerTeam;
    const estimatedMinutes = Math.ceil(totalPicks * (draftTimeLimit / 60));
    
    if (estimatedMinutes > 60) {
      const hours = Math.floor(estimatedMinutes / 60);
      const minutes = estimatedMinutes % 60;
      return `~${hours}h${minutes > 0 ? ` ${minutes}m` : ''}`;
    }
    return `~${estimatedMinutes} minutes`;
  }

  onSubmit(): void {
    if (!this.isStep1Valid() || !this.isStep2Valid()) {
      return;
    }

    this.loading = true;
    this.error = null;

    const gameData: CreateGameRequest = {
      name: this.basicInfoForm.value.name,
      description: this.basicInfoForm.value.description,
      maxParticipants: this.rulesForm.value.maxParticipants,
      isPrivate: this.rulesForm.value.isPrivate,
      autoStartDraft: this.rulesForm.value.autoStartDraft,
      draftTimeLimit: this.rulesForm.value.draftTimeLimit,
      autoPickDelay: this.rulesForm.value.autoPickDelay,
      currentSeason: 2025,
      regionRules: {} // Simplified for now - can be enhanced later
    };

    this.gameService.createGame(gameData).subscribe({
      next: (game) => {
        this.loading = false;
        this.snackBar.open(
          `🎉 ${this.t.t('games.wizard.successCreated').replace('{name}', game.name)}`,
          this.t.t('games.wizard.viewGame'),
          {
            duration: 5000,
            panelClass: 'success-snackbar'
          }
        );

        // Navigate to the created game
        this.router.navigate(['/games', game.id], {
          queryParams: {
            created: 'true',
            template: this.selectedTemplate?.id
          }
        });
      },
      error: (error) => {
        this.loading = false;
        this.error = this.t.t('games.wizard.errorCreate');
        console.error('Game creation error:', error);

        this.snackBar.open(
          `❌ ${this.t.t('games.wizard.errorCreation')}`,
          this.t.t('games.wizard.retry'),
          {
            duration: 5000,
            panelClass: 'error-snackbar'
          }
        );
      }
    });
  }

  onCancel(): void {
    this.router.navigate(['/games']);
  }

  // Smart defaults helpers
  getPopularTemplates(): GameTemplate[] {
    return this.getGameTemplates().filter(t => t.popular);
  }

  getAllTemplates(): GameTemplate[] {
    return this.getGameTemplates();
  }
}