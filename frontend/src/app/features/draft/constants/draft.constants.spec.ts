import {
  FILTER_OPTIONS,
  PERFORMANCE_CONFIG,
  STATUS_COLORS,
  UI_CONFIG,
} from './draft.constants';

describe('draft.constants color tokens', () => {
  function expectNoHex(values: string[]): void {
    for (const value of values) {
      expect(value).not.toMatch(/#[0-9a-f]{3,8}\b/i);
    }
  }

  it('uses region tokens for filter option colors', () => {
    const regionColors = FILTER_OPTIONS.REGIONS.map(region => region.color);
    for (const color of regionColors) {
      expect(color).toMatch(/^var\(--color-region-[a-z-]+\)$/);
    }
  });

  it('uses CSS variables for performance colors', () => {
    const performanceColors = Object.values(PERFORMANCE_CONFIG.PERFORMANCE_COLORS);
    for (const color of performanceColors) {
      expect(color).toMatch(/^var\(--color-[a-z-]+\)$/);
    }
    expectNoHex(performanceColors);
  });

  it('uses CSS variables for UI theme colors and status colors', () => {
    const themeColors = Object.values(UI_CONFIG.THEME_COLORS);
    const statusColors = Object.values(STATUS_COLORS);

    for (const color of [...themeColors, ...statusColors]) {
      expect(color).toMatch(/^var\(--color-[a-z-]+\)$/);
    }

    expectNoHex([...themeColors, ...statusColors]);
  });
});
