import { TestBed } from '@angular/core/testing';
import { GameSelectionService } from './game-selection.service';
import { Game } from '../../features/game/models/game.interface';

describe('GameSelectionService', () => {
  let service: GameSelectionService;

  const sampleGame: Game = {
    id: 'game-1',
    name: 'Test Game',
    creatorName: 'Creator',
    maxParticipants: 10,
    status: 'DRAFT',
    createdAt: '2025-01-01T00:00:00Z',
    participantCount: 1,
    canJoin: true
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [GameSelectionService]
    });

    service = TestBed.inject(GameSelectionService);
  });

  it('starts with no selected game', () => {
    expect(service.getSelectedGame()).toBeNull();
    expect(service.hasSelectedGame()).toBeFalse();
  });

  it('updates selection and observable when set', () => {
    const emissions: Array<Game | null> = [];
    const subscription = service.selectedGame$.subscribe((value) => {
      emissions.push(value);
    });

    service.setSelectedGame(sampleGame);

    expect(emissions).toEqual([null, sampleGame]);
    expect(service.hasSelectedGame()).toBeTrue();

    subscription.unsubscribe();
  });

  it('clears selection', () => {
    service.setSelectedGame(sampleGame);

    service.clearSelection();

    expect(service.getSelectedGame()).toBeNull();
    expect(service.hasSelectedGame()).toBeFalse();
  });
});
