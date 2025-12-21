import { Game, GameParticipant } from '../../features/game/models/game.interface';

export const MOCK_GAMES: Game[] = [
  {
    id: 'a1234567-8901-2345-6789-012345678901',
    name: 'Game Demo de Thibaut',
    description: 'Game de demonstration en mode developpement',
    status: 'ACTIVE',
    maxParticipants: 8,
    participantCount: 3,
    createdAt: '2025-08-01T10:00:00Z',
    creatorName: 'Thibaut',
    canJoin: true,
    invitationCode: 'DEMO2025'
  },
  {
    id: '880e8400-e29b-41d4-a716-446655440000',
    name: 'Tournoi Test',
    description: 'Tournoi de test avec donnees de fallback',
    status: 'DRAFT',
    maxParticipants: 12,
    participantCount: 8,
    createdAt: '2025-08-05T12:00:00Z',
    creatorName: 'System',
    canJoin: true,
    invitationCode: 'TEST2025'
  }
];

export const MOCK_GAME_PARTICIPANTS: Record<string, GameParticipant[]> = {
  'a1234567-8901-2345-6789-012345678901': [
    {
      id: '5764d97a-b1d4-449b-8127-d449b4d2ede6',
      username: 'Thibaut',
      joinedAt: '2025-08-01T10:05:00Z',
      isCreator: true,
      draftOrder: 1
    },
    {
      id: '713f500c-dacd-4856-ba5f-eeb03f0e1219',
      username: 'Teddy',
      joinedAt: '2025-08-01T10:06:00Z',
      draftOrder: 2
    },
    {
      id: '7daa2318-6110-49f2-944b-2129fd57d732',
      username: 'Marcel',
      joinedAt: '2025-08-01T10:07:00Z',
      draftOrder: 3
    }
  ],
  '880e8400-e29b-41d4-a716-446655440000': [
    {
      id: '11111111-1111-1111-1111-111111111111',
      username: 'System',
      joinedAt: '2025-08-05T12:01:00Z',
      isCreator: true,
      draftOrder: 1
    },
    {
      id: '22222222-2222-2222-2222-222222222222',
      username: 'Alpha',
      joinedAt: '2025-08-05T12:02:00Z',
      draftOrder: 2
    },
    {
      id: '33333333-3333-3333-3333-333333333333',
      username: 'Bravo',
      joinedAt: '2025-08-05T12:03:00Z',
      draftOrder: 3
    }
  ]
};
