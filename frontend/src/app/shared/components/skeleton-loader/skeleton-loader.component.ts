import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type SkeletonType = 'text' | 'title' | 'avatar' | 'thumbnail' | 'card' | 'button' | 'chip';

@Component({
  selector: 'app-skeleton-loader',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="skeleton-container" [ngClass]="containerClass">
      <!-- Text skeleton -->
      <ng-container *ngIf="type === 'text'">
        <div class="skeleton skeleton-text"
             *ngFor="let line of lines"
             [style.width]="getLineWidth(line)"
             [class.animated]="animated">
        </div>
      </ng-container>

      <!-- Title skeleton -->
      <ng-container *ngIf="type === 'title'">
        <div class="skeleton skeleton-title" [class.animated]="animated"></div>
      </ng-container>

      <!-- Avatar skeleton -->
      <ng-container *ngIf="type === 'avatar'">
        <div class="skeleton skeleton-avatar"
             [style.width.px]="size"
             [style.height.px]="size"
             [class.animated]="animated">
        </div>
      </ng-container>

      <!-- Thumbnail skeleton -->
      <ng-container *ngIf="type === 'thumbnail'">
        <div class="skeleton skeleton-thumbnail"
             [style.width.px]="width || 200"
             [style.height.px]="height || 120"
             [class.animated]="animated">
        </div>
      </ng-container>

      <!-- Card skeleton -->
      <ng-container *ngIf="type === 'card'">
        <div class="skeleton-card" [class.animated]="animated">
          <div class="skeleton skeleton-card-header"></div>
          <div class="skeleton skeleton-card-body">
            <div class="skeleton skeleton-text" style="width: 80%"></div>
            <div class="skeleton skeleton-text" style="width: 60%"></div>
            <div class="skeleton skeleton-text" style="width: 70%"></div>
          </div>
        </div>
      </ng-container>

      <!-- Button skeleton -->
      <ng-container *ngIf="type === 'button'">
        <div class="skeleton skeleton-button"
             [style.width.px]="width || 120"
             [class.animated]="animated">
        </div>
      </ng-container>

      <!-- Chip skeleton -->
      <ng-container *ngIf="type === 'chip'">
        <div class="skeleton skeleton-chip" [class.animated]="animated"></div>
      </ng-container>
    </div>
  `,
  styles: [`
    .skeleton-container {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .skeleton-container.inline {
      flex-direction: row;
      align-items: center;
    }

    .skeleton {
      background: linear-gradient(
        90deg,
        rgba(255, 255, 255, 0.05) 0%,
        rgba(255, 255, 255, 0.1) 50%,
        rgba(255, 255, 255, 0.05) 100%
      );
      background-size: 200% 100%;
      border-radius: 4px;
    }

    .skeleton.animated {
      animation: skeleton-shimmer 1.5s infinite ease-in-out;
    }

    @keyframes skeleton-shimmer {
      0% {
        background-position: 200% 0;
      }
      100% {
        background-position: -200% 0;
      }
    }

    .skeleton-text {
      height: 16px;
      border-radius: 4px;
    }

    .skeleton-title {
      height: 28px;
      width: 60%;
      border-radius: 6px;
    }

    .skeleton-avatar {
      border-radius: 50%;
      flex-shrink: 0;
    }

    .skeleton-thumbnail {
      border-radius: 8px;
    }

    .skeleton-card {
      background: rgba(255, 255, 255, 0.03);
      border-radius: 12px;
      border: 1px solid rgba(255, 255, 255, 0.08);
      overflow: hidden;
    }

    .skeleton-card-header {
      height: 120px;
      border-radius: 0;
    }

    .skeleton-card-body {
      padding: 16px;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .skeleton-button {
      height: 40px;
      border-radius: 8px;
    }

    .skeleton-chip {
      width: 80px;
      height: 24px;
      border-radius: 12px;
    }
  `]
})
export class SkeletonLoaderComponent {
  @Input() type: SkeletonType = 'text';
  @Input() count = 1;
  @Input() animated = true;
  @Input() size = 40;
  @Input() width?: number;
  @Input() height?: number;
  @Input() inline = false;

  get lines(): number[] {
    return Array.from({ length: this.count }, (_, i) => i);
  }

  get containerClass(): string {
    return this.inline ? 'inline' : '';
  }

  getLineWidth(index: number): string {
    // Variation de largeur pour un effet plus naturel
    const widths = ['100%', '85%', '70%', '90%', '60%'];
    return widths[index % widths.length];
  }
}
