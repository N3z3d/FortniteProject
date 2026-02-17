import { LeaderboardApiMapper } from './leaderboard-api.mapper';

describe('LeaderboardApiMapper', () => {
  it('returns empty list when api response is null', () => {
    expect(LeaderboardApiMapper.mapApiResponseToLeaderboardEntries(null)).toEqual([]);
  });

  it('returns empty list when api response payload is not an array', () => {
    const response = { entries: { invalid: true } };

    expect(LeaderboardApiMapper.mapApiResponseToLeaderboardEntries(response)).toEqual([]);
  });

  it('maps entries from API response', () => {
    const response = {
      entries: [
        {
          teamId: 'team-1',
          teamName: 'Equipe Alpha',
          ownerName: 'thibaut',
          totalPoints: 150,
          rank: 1,
          players: []
        }
      ]
    };

    const mapped = LeaderboardApiMapper.mapApiResponseToLeaderboardEntries(response);

    expect(mapped).toHaveSize(1);
    expect(mapped[0]).toEqual(jasmine.objectContaining({
      teamId: 'team-1',
      teamName: 'Equipe Alpha',
      ownerName: 'thibaut',
      totalPoints: 150,
      rank: 1
    }));
  });
});
