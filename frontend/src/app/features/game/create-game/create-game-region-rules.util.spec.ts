import { ACTIVE_GAME_REGIONS, buildBalancedRegionRules, buildSingleRegionRules } from './create-game-region-rules.util';

describe('buildBalancedRegionRules', () => {
  it('builds explicit region rules for participant counts below the region count', () => {
    expect(buildBalancedRegionRules(5)).toEqual({
      EU: 1,
      NAC: 1,
      BR: 1,
      ASIA: 1,
      OCE: 1
    });
  });

  it('distributes remaining players evenly across active regions', () => {
    expect(buildBalancedRegionRules(8)).toEqual({
      EU: 2,
      NAC: 1,
      BR: 1,
      ASIA: 1,
      OCE: 1,
      NAW: 1,
      ME: 1
    });
  });

  it('never allocates outside the supported region list', () => {
    const supportedRegions = new Set(ACTIVE_GAME_REGIONS);

    expect(Object.keys(buildBalancedRegionRules(20)).every(region => supportedRegions.has(region as any))).toBeTrue();
  });

  it('rejects unsupported participant counts', () => {
    expect(() => buildBalancedRegionRules(0)).toThrowError(RangeError);
    expect(() => buildBalancedRegionRules(21)).toThrowError(RangeError);
    expect(() => buildBalancedRegionRules(2.5)).toThrowError(RangeError);
  });

  it('can build a single-region ruleset for scenario fixtures', () => {
    expect(buildSingleRegionRules(3, 'EU')).toEqual({ EU: 3 });
    expect(buildSingleRegionRules(2, 'NAW')).toEqual({ NAW: 2 });
  });
});
