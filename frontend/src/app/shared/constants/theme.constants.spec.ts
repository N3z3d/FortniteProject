import { getRegionColor, getTrancheColor, formatPoints, THEME_COLORS, NUMBER_FORMAT } from './theme.constants';

describe('theme.constants', () => {
  describe('getRegionColor', () => {
    it('should return correct color for known regions', () => {
      expect(getRegionColor('EU')).toBe('var(--color-region-eu)');
      expect(getRegionColor('NAW')).toBe('var(--color-region-naw)');
      expect(getRegionColor('NAC')).toBe('var(--color-admin-warning)');
      expect(getRegionColor('BR')).toBe('var(--color-region-brazil)');
      expect(getRegionColor('ASIA')).toBe('var(--color-region-asia)');
      expect(getRegionColor('OCE')).toBe('var(--color-region-oce)');
      expect(getRegionColor('ME')).toBe('var(--color-region-mena)');
    });

    it('should return default gray color for unknown regions', () => {
      expect(getRegionColor('UNKNOWN')).toBe('var(--color-neutral)');
      expect(getRegionColor('')).toBe('var(--color-neutral)');
      expect(getRegionColor('INVALID')).toBe('var(--color-neutral)');
    });
  });

  describe('getTrancheColor', () => {
    it('should return correct color for valid tranche indices', () => {
      expect(getTrancheColor(1)).toBe('var(--color-achievement)');
      expect(getTrancheColor(2)).toBe('rgb(192 192 192)');
      expect(getTrancheColor(3)).toBe('rgb(205 127 50)');
      expect(getTrancheColor(4)).toBe('var(--color-region-eu)');
      expect(getTrancheColor(5)).toBe('var(--color-admin-info)');
    });

    it('should return default gray color for invalid tranche indices', () => {
      expect(getTrancheColor(0)).toBe('var(--color-neutral)');
      expect(getTrancheColor(6)).toBe('var(--color-neutral)');
      expect(getTrancheColor(99)).toBe('var(--color-neutral)');
      expect(getTrancheColor(-1)).toBe('var(--color-neutral)');
    });
  });

  describe('formatPoints', () => {
    it('should format points in millions with M suffix', () => {
      expect(formatPoints(1000000)).toBe('1.00M');
      expect(formatPoints(2500000)).toBe('2.50M');
      expect(formatPoints(1234567)).toBe('1.23M');
    });

    it('should format points in thousands with K suffix', () => {
      expect(formatPoints(1000)).toBe('1.0K');
      expect(formatPoints(1500)).toBe('1.5K');
      expect(formatPoints(10500)).toBe('10.5K');
      expect(formatPoints(999999)).toBe('1000.0K');
    });

    it('should return plain string for values under thousand', () => {
      expect(formatPoints(0)).toBe('0');
      expect(formatPoints(500)).toBe('500');
      expect(formatPoints(999)).toBe('999');
    });

    it('should handle edge case of exactly million threshold', () => {
      expect(formatPoints(1000000)).toBe('1.00M');
    });

    it('should handle edge case of exactly thousand threshold', () => {
      expect(formatPoints(1000)).toBe('1.0K');
    });
  });

  describe('THEME_COLORS constants', () => {
    it('should have primary colors defined', () => {
      expect(THEME_COLORS.primaryCyan).toBe('var(--color-info)');
      expect(THEME_COLORS.primaryGold).toBe('var(--color-action)');
      expect(THEME_COLORS.primaryOrange).toBe('var(--color-region-brazil)');
      expect(THEME_COLORS.primaryGreen).toBe('var(--color-success)');
    });

    it('should have status colors defined', () => {
      expect(THEME_COLORS.success).toBeDefined();
      expect(THEME_COLORS.error).toBeDefined();
      expect(THEME_COLORS.info).toBeDefined();
      expect(THEME_COLORS.warning).toBeDefined();
    });

    it('should have region colors for all regions', () => {
      expect(Object.keys(THEME_COLORS.regions).length).toBe(7);
      expect(THEME_COLORS.regions['EU']).toBeDefined();
      expect(THEME_COLORS.regions['NAW']).toBeDefined();
    });

    it('should have 5 tranche colors', () => {
      expect(THEME_COLORS.tranches.length).toBe(5);
    });
  });

  describe('NUMBER_FORMAT constants', () => {
    it('should have correct thresholds', () => {
      expect(NUMBER_FORMAT.millionThreshold).toBe(1000000);
      expect(NUMBER_FORMAT.thousandThreshold).toBe(1000);
    });

    it('should have correct suffixes', () => {
      expect(NUMBER_FORMAT.millionSuffix).toBe('M');
      expect(NUMBER_FORMAT.thousandSuffix).toBe('K');
    });
  });
});
