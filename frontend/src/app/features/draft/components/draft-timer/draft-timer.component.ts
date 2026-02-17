import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

import { TranslationService } from '../../../../core/services/translation.service';
import { GameParticipant } from '../../models/draft.interface';

@Component({
  selector: 'app-draft-timer',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './draft-timer.component.html',
  styleUrls: ['./draft-timer.component.scss']
})
export class DraftTimerComponent {
  public readonly t = inject(TranslationService);

  @Input() currentPlayer: GameParticipant | null = null;
  @Input() formattedTime: string | null = null;

  get hasTimer(): boolean {
    return Boolean(this.currentPlayer?.timeRemaining);
  }
}
