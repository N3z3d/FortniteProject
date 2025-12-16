import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-game-card-skeleton',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="game-card-skeleton" *ngFor="let item of items">
      <div class="skeleton-header">
        <div class="skeleton-icon animated"></div>
        <div class="skeleton-title-group">
          <div class="skeleton skeleton-title animated"></div>
          <div class="skeleton skeleton-subtitle animated"></div>
        </div>
      </div>
      <div class="skeleton-body">
        <div class="skeleton-stats">
          <div class="skeleton-stat animated" *ngFor="let stat of [1,2,3]"></div>
        </div>
        <div class="skeleton skeleton-chip animated"></div>
      </div>
      <div class="skeleton-footer">
        <div class="skeleton skeleton-button animated"></div>
        <div class="skeleton skeleton-button-small animated"></div>
      </div>
    </div>
  `,
  styles: [`
    :host {
      display: contents;
    }

    .game-card-skeleton {
      background: rgba(30, 30, 40, 0.6);
      border-radius: 16px;
      border: 1px solid rgba(255, 255, 255, 0.08);
      padding: 20px;
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .skeleton {
      background: linear-gradient(
        90deg,
        rgba(255, 255, 255, 0.04) 0%,
        rgba(255, 255, 255, 0.08) 50%,
        rgba(255, 255, 255, 0.04) 100%
      );
      background-size: 200% 100%;
      border-radius: 4px;
    }

    .skeleton.animated {
      animation: shimmer 1.5s infinite ease-in-out;
    }

    @keyframes shimmer {
      0% { background-position: 200% 0; }
      100% { background-position: -200% 0; }
    }

    .skeleton-header {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .skeleton-icon {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      background: linear-gradient(135deg, rgba(201, 169, 98, 0.1), rgba(201, 169, 98, 0.05));
      background-size: 200% 100%;
    }

    .skeleton-title-group {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .skeleton-title {
      height: 20px;
      width: 70%;
    }

    .skeleton-subtitle {
      height: 14px;
      width: 50%;
    }

    .skeleton-body {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .skeleton-stats {
      display: flex;
      gap: 16px;
    }

    .skeleton-stat {
      width: 60px;
      height: 32px;
      border-radius: 8px;
      background: linear-gradient(
        90deg,
        rgba(255, 255, 255, 0.04) 0%,
        rgba(255, 255, 255, 0.08) 50%,
        rgba(255, 255, 255, 0.04) 100%
      );
      background-size: 200% 100%;
    }

    .skeleton-chip {
      width: 80px;
      height: 24px;
      border-radius: 12px;
    }

    .skeleton-footer {
      display: flex;
      gap: 12px;
      justify-content: flex-end;
      padding-top: 12px;
      border-top: 1px solid rgba(255, 255, 255, 0.06);
    }

    .skeleton-button {
      width: 100px;
      height: 36px;
      border-radius: 8px;
    }

    .skeleton-button-small {
      width: 36px;
      height: 36px;
      border-radius: 8px;
    }
  `]
})
export class GameCardSkeletonComponent {
  @Input() count = 3;

  get items(): number[] {
    return Array.from({ length: this.count }, (_, i) => i);
  }
}
