export const ACTIVE_GAME_REGIONS = ['EU', 'NAC', 'BR', 'ASIA', 'OCE', 'NAW', 'ME'] as const;

const MAX_TOTAL_REGION_PLAYERS = 20;

function assertValidParticipantCount(maxParticipants: number): void {
  if (!Number.isInteger(maxParticipants) || maxParticipants < 1 || maxParticipants > MAX_TOTAL_REGION_PLAYERS) {
    throw new RangeError('maxParticipants must be an integer between 1 and 20');
  }
}

export function buildBalancedRegionRules(maxParticipants: number): Record<string, number> {
  assertValidParticipantCount(maxParticipants);

  const regionRules: Record<string, number> = {};

  for (let index = 0; index < maxParticipants; index += 1) {
    const region = ACTIVE_GAME_REGIONS[index % ACTIVE_GAME_REGIONS.length];
    regionRules[region] = (regionRules[region] ?? 0) + 1;
  }

  return regionRules;
}

export function buildSingleRegionRules(maxParticipants: number, region = ACTIVE_GAME_REGIONS[0]): Record<string, number> {
  assertValidParticipantCount(maxParticipants);

  if (!ACTIVE_GAME_REGIONS.includes(region as typeof ACTIVE_GAME_REGIONS[number])) {
    throw new RangeError(`region must be one of: ${ACTIVE_GAME_REGIONS.join(', ')}`);
  }

  return { [region]: maxParticipants };
}
