import {
  secureRandomFloat,
  secureRandomId,
  secureRandomInt,
  secureRandomIntInRange,
  secureRandomPick
} from './secure-random.util';

describe('secure-random.util', () => {
  it('returns float values between 0 (inclusive) and 1 (exclusive)', () => {
    for (let i = 0; i < 50; i++) {
      const value = secureRandomFloat();
      expect(value).toBeGreaterThanOrEqual(0);
      expect(value).toBeLessThan(1);
    }
  });

  it('returns integers in the expected exclusive range', () => {
    for (let i = 0; i < 50; i++) {
      const value = secureRandomInt(7);
      expect(Number.isInteger(value)).toBeTrue();
      expect(value).toBeGreaterThanOrEqual(0);
      expect(value).toBeLessThan(7);
    }
  });

  it('throws when secureRandomInt receives invalid maxExclusive', () => {
    expect(() => secureRandomInt(0)).toThrow();
    expect(() => secureRandomInt(-1)).toThrow();
    expect(() => secureRandomInt(1.5)).toThrow();
  });

  it('returns integers in the expected inclusive range', () => {
    for (let i = 0; i < 50; i++) {
      const value = secureRandomIntInRange(3, 9);
      expect(Number.isInteger(value)).toBeTrue();
      expect(value).toBeGreaterThanOrEqual(3);
      expect(value).toBeLessThanOrEqual(9);
    }
  });

  it('picks an item from the provided values', () => {
    const values = ['A', 'B', 'C'] as const;
    for (let i = 0; i < 20; i++) {
      expect(values).toContain(secureRandomPick(values));
    }
  });

  it('generates lowercase alphanumeric IDs with requested length', () => {
    const id = secureRandomId(12);
    expect(id).toMatch(/^[a-z0-9]{12}$/);
  });
});
