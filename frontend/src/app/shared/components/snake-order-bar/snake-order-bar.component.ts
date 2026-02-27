import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ResponsiveService } from '../../../core/services/responsive.service';

export interface SnakeParticipant {
  id: string;
  username: string;
}

/** Step in pixels between avatar centers (avatar 36px + gap 4px). */
const AVATAR_STEP_PX = 40;

@Component({
  selector: 'app-snake-order-bar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './snake-order-bar.component.html',
  styleUrls: ['./snake-order-bar.component.scss'],
})
export class SnakeOrderBarComponent {
  private readonly responsive = inject(ResponsiveService);

  @Input() participants: SnakeParticipant[] = [];
  @Input() currentIndex = 0;
  @Input() regionLabel?: string;

  get currentParticipant(): SnakeParticipant | null {
    return this.participants[this.currentIndex] ?? null;
  }

  get showAvatarMode(): boolean {
    return this.participants.length <= 6 && !this.responsive.isMobile();
  }

  get position(): number {
    return this.currentIndex + 1;
  }

  get total(): number {
    return this.participants.length;
  }

  get ariaLabel(): string {
    const p = this.currentParticipant;
    if (!p) return '';
    return `Tour de ${p.username}, position ${this.position} sur ${this.total}`;
  }

  get cursorOffset(): string {
    return `${this.currentIndex * AVATAR_STEP_PX}px`;
  }

  getInitials(username: string): string {
    return username.slice(0, 2).toUpperCase();
  }
}
