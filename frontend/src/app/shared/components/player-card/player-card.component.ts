import { Component, Input, Output, EventEmitter, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';

import { AvailablePlayer } from '../../../features/draft/models/draft.interface';
import { ResponsiveService } from '../../../core/services/responsive.service';

export interface RankSnapshot {
  date: string;
  rank: number;
}

@Component({
  selector: 'app-player-card',
  standalone: true,
  imports: [CommonModule, MatButtonModule],
  templateUrl: './player-card.component.html',
  styleUrls: ['./player-card.component.scss'],
})
export class PlayerCardComponent {
  private readonly responsive = inject(ResponsiveService);

  @Input() player!: AvailablePlayer;
  @Input() mode: 'draft' | 'browse' = 'draft';
  @Input() selected = false;
  @Input() taken = false;
  @Input() recommended = false;
  @Input() snapshots: RankSnapshot[] = [];

  @Output() readonly cardSelected = new EventEmitter<AvailablePlayer>();
  @Output() readonly reportPlayer = new EventEmitter<AvailablePlayer>();

  get showSparklineSlot(): boolean {
    return (
      this.mode === 'browse' &&
      this.snapshots.length >= 2 &&
      !this.responsive.hideSparkline()
    );
  }

  get ariaLabel(): string {
    let state = '';
    if (this.taken) {
      state = 'joueur pris';
    } else if (this.recommended) {
      state = 'recommandÃ©';
    } else if (this.selected) {
      state = 'sÃ©lectionnÃ©';
    }
    return `${this.player?.username ?? ''}${state ? ' â€” ' + state : ''}`;
  }

  onSelect(): void {
    if (!this.taken) {
      this.cardSelected.emit(this.player);
    }
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.onSelect();
    }
  }

  onReport(event: MouseEvent): void {
    event.stopPropagation();
    this.reportPlayer.emit(this.player);
  }
}

