import { Component, Inject, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslationService } from '../../../../core/services/translation.service';

export type InvitationCodeDuration = '24h' | '48h' | '7d' | 'permanent';
export type InvitationCodeDialogMode = 'generate' | 'regenerate';

export interface InvitationCodeDurationDialogData {
  defaultDuration?: InvitationCodeDuration;
  mode?: InvitationCodeDialogMode;
}

interface DurationOption {
  value: InvitationCodeDuration;
  titleKey: string;
  descriptionKey: string;
}

@Component({
  selector: 'app-invitation-code-duration-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  templateUrl: './invitation-code-duration-dialog.component.html',
  styleUrls: ['./invitation-code-duration-dialog.component.scss']
})
export class InvitationCodeDurationDialogComponent {
  public readonly t = inject(TranslationService);

  readonly durationOptions: DurationOption[] = [
    {
      value: '24h',
      titleKey: 'games.detail.regenerateDialog.option24hTitle',
      descriptionKey: 'games.detail.regenerateDialog.option24hDescription'
    },
    {
      value: '48h',
      titleKey: 'games.detail.regenerateDialog.option48hTitle',
      descriptionKey: 'games.detail.regenerateDialog.option48hDescription'
    },
    {
      value: '7d',
      titleKey: 'games.detail.regenerateDialog.option7dTitle',
      descriptionKey: 'games.detail.regenerateDialog.option7dDescription'
    },
    {
      value: 'permanent',
      titleKey: 'games.detail.regenerateDialog.optionPermanentTitle',
      descriptionKey: 'games.detail.regenerateDialog.optionPermanentDescription'
    }
  ];

  selectedDuration: InvitationCodeDuration;
  readonly mode: InvitationCodeDialogMode;

  constructor(
    private readonly dialogRef: MatDialogRef<InvitationCodeDurationDialogComponent, InvitationCodeDuration | undefined>,
    @Inject(MAT_DIALOG_DATA) data: InvitationCodeDurationDialogData | null
  ) {
    this.selectedDuration = data?.defaultDuration || 'permanent';
    this.mode = data?.mode || 'regenerate';
  }

  selectDuration(duration: InvitationCodeDuration): void {
    this.selectedDuration = duration;
  }

  onConfirm(): void {
    this.dialogRef.close(this.selectedDuration);
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  getTitleKey(): string {
    return this.mode === 'generate'
      ? 'games.detail.regenerateDialog.generateTitle'
      : 'games.detail.regenerateDialog.title';
  }

  getSubtitleKey(): string {
    return this.mode === 'generate'
      ? 'games.detail.regenerateDialog.generateSubtitle'
      : 'games.detail.regenerateDialog.subtitle';
  }

  getConfirmKey(): string {
    return this.mode === 'generate'
      ? 'games.detail.regenerateDialog.generateConfirm'
      : 'games.detail.regenerateDialog.confirm';
  }

  getTitleText(): string {
    return this.translateDialogText(
      this.getTitleKey(),
      'games.detail.regenerateDialog.title'
    );
  }

  getSubtitleText(): string {
    return this.translateDialogText(
      this.getSubtitleKey(),
      'games.detail.regenerateDialog.subtitle'
    );
  }

  getConfirmText(): string {
    return this.translateDialogText(
      this.getConfirmKey(),
      'games.detail.regenerateDialog.confirm'
    );
  }

  private translateDialogText(primaryKey: string, fallbackKey: string): string {
    const fallbackTranslation = this.t.t(fallbackKey);
    const safeFallback = fallbackTranslation === fallbackKey ? undefined : fallbackTranslation;
    return this.t.t(primaryKey, safeFallback);
  }
}
