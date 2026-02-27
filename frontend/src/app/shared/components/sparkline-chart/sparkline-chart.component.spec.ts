import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { signal } from '@angular/core';

import { SparklineChartComponent, RankSnapshot } from './sparkline-chart.component';
import { ResponsiveService } from '../../../core/services/responsive.service';

// ===== MOCK BaseChartDirective =====

@Component({ selector: '[baseChart]', standalone: true, template: '' })
class MockBaseChartDirective {}

// ===== FIXTURES =====

const SNAPSHOTS_UP: RankSnapshot[] = [
  { date: '2026-02-01', rank: 80 },
  { date: '2026-02-08', rank: 60 },
  { date: '2026-02-15', rank: 40 }, // rank improved (lower is better)
];

const SNAPSHOTS_DOWN: RankSnapshot[] = [
  { date: '2026-02-01', rank: 40 },
  { date: '2026-02-08', rank: 60 },
  { date: '2026-02-15', rank: 80 }, // rank degraded
];

const SNAPSHOTS_FLAT: RankSnapshot[] = [
  { date: '2026-02-01', rank: 50 },
  { date: '2026-02-15', rank: 50 },
];

describe('SparklineChartComponent', () => {
  let component: SparklineChartComponent;
  let fixture: ComponentFixture<SparklineChartComponent>;

  const mockResponsive = { hideSparkline: signal(false), isMobile: signal(false) };

  function createComponent(snapshots: RankSnapshot[] = SNAPSHOTS_UP, hideSparkline = false) {
    mockResponsive.hideSparkline = signal(hideSparkline);
    fixture.componentRef.setInput('snapshots', snapshots);
    fixture.detectChanges();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SparklineChartComponent, NoopAnimationsModule],
      providers: [
        { provide: ResponsiveService, useValue: mockResponsive },
      ],
    })
      .overrideComponent(SparklineChartComponent, {
        remove: { imports: [] },
        add: { imports: [MockBaseChartDirective] },
      })
      .compileComponents();

    fixture = TestBed.createComponent(SparklineChartComponent);
    component = fixture.componentInstance;
  });

  // ===== VISIBILITY =====

  it('should be hidden when snapshots has fewer than 2 entries', () => {
    createComponent([{ date: '2026-02-01', rank: 50 }]);

    const canvas = fixture.nativeElement.querySelector('.sparkline-canvas');
    expect(canvas).toBeNull();
  });

  it('should be shown when snapshots has 2 or more entries', () => {
    createComponent(SNAPSHOTS_UP);

    const host = fixture.nativeElement.querySelector('.sparkline-wrapper');
    expect(host).not.toBeNull();
  });

  it('should be hidden when snapshots is empty', () => {
    createComponent([]);

    const host = fixture.nativeElement.querySelector('.sparkline-wrapper');
    expect(host).toBeNull();
  });

  it('should be hidden when ResponsiveService.hideSparkline is true', () => {
    createComponent(SNAPSHOTS_UP, true);

    const host = fixture.nativeElement.querySelector('.sparkline-wrapper');
    expect(host).toBeNull();
  });

  // ===== TREND INDICATOR =====

  it('should show UP arrow when rank improved (final rank < initial rank)', () => {
    createComponent(SNAPSHOTS_UP);

    const arrow = fixture.nativeElement.querySelector('.trend-arrow--up');
    expect(arrow).not.toBeNull();
  });

  it('should show DOWN arrow when rank degraded (final rank > initial rank)', () => {
    createComponent(SNAPSHOTS_DOWN);

    const arrow = fixture.nativeElement.querySelector('.trend-arrow--down');
    expect(arrow).not.toBeNull();
  });

  it('should show flat indicator when rank is unchanged', () => {
    createComponent(SNAPSHOTS_FLAT);

    const flat = fixture.nativeElement.querySelector('.trend-arrow--flat');
    expect(flat).not.toBeNull();
  });

  // ===== ARIA =====

  it('should have aria-label "Rang en hausse" when trend is up', () => {
    createComponent(SNAPSHOTS_UP);

    const canvas = fixture.nativeElement.querySelector('[aria-label]');
    expect(canvas?.getAttribute('aria-label')).toContain('hausse');
  });

  it('should have aria-label "Rang en baisse" when trend is down', () => {
    createComponent(SNAPSHOTS_DOWN);

    const canvas = fixture.nativeElement.querySelector('[aria-label]');
    expect(canvas?.getAttribute('aria-label')).toContain('baisse');
  });

  // ===== FILTERED SNAPSHOTS =====

  it('should use last defaultDays snapshots when more are provided', () => {
    const many: RankSnapshot[] = Array.from({ length: 20 }, (_, i) => ({
      date: `2026-01-${String(i + 1).padStart(2, '0')}`,
      rank: 50 + i,
    }));
    component.defaultDays = 7;
    createComponent(many);

    expect(component.filteredSnapshots.length).toBe(7);
  });
});
