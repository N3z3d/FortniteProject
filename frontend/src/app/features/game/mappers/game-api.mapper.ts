import { Game, GameParticipant } from '../models/game.interface';

/**
 * Mapper for API responses to frontend game models.
 */
export class GameApiMapper {

  /**
   * Maps a game API response to the frontend Game shape.
   */
  static mapApiResponseToGame(apiResponse: any): Game {
    if (!apiResponse) {
      throw new Error('API response is null or undefined');
    }

    return {
      id: apiResponse.id,
      name: apiResponse.name,
      creatorId: apiResponse.creatorId,
      creatorName: this.resolveCreatorName(apiResponse),
      maxParticipants: apiResponse.maxParticipants || 0,
      status: apiResponse.status,
      createdAt: this.validateAndFormatDate(apiResponse.createdAt),
      participantCount: apiResponse.currentParticipantCount || apiResponse.participantCount || 0,
      canJoin: this.determineCanJoin(apiResponse),
      invitationCode: apiResponse.invitationCode,
      participants: this.mapGameParticipants(apiResponse),
      draftRules: apiResponse.draftRules,
      regionRules: apiResponse.regionRules
    };
  }

  /**
   * Maps API participants to frontend participants.
   */
  static mapApiParticipants(apiParticipants: any[]): GameParticipant[] {
    if (!Array.isArray(apiParticipants)) {
      return [];
    }

    return apiParticipants.map((participant, index) => ({
      id: participant.userId || participant.id || `participant-${index}`,
      username: participant.username,
      joinedAt: this.validateAndFormatDate(participant.joinedAt || new Date().toISOString()),
      isCreator: participant.isCreator || false,
      draftOrder: participant.draftOrder || (index + 1),
      selectedPlayers: participant.selectedPlayers || [],
      lastSelectionTime: participant.lastSelectionTime
        ? this.validateAndFormatDate(participant.lastSelectionTime)
        : undefined,
      isCurrentTurn: participant.isCurrentTurn || false,
      timeRemaining: participant.timeRemaining
    }));
  }

  /**
   * Validates and normalizes date strings.
   */
  private static validateAndFormatDate(dateString: string | null | undefined): string {
    if (!dateString) {
      return new Date().toISOString();
    }

    try {
      const date = new Date(dateString);
      if (isNaN(date.getTime())) {
        return new Date().toISOString();
      }
      return date.toISOString();
    } catch {
      return new Date().toISOString();
    }
  }

  /**
   * Determines if the game can be joined.
   */
  private static determineCanJoin(apiResponse: any): boolean {
    if (apiResponse.availableToJoin !== undefined) {
      return apiResponse.availableToJoin;
    }

    if (apiResponse.canJoin !== undefined) {
      return apiResponse.canJoin;
    }

    const currentCount = apiResponse.currentParticipantCount || apiResponse.participantCount || 0;
    const maxParticipants = apiResponse.maxParticipants || 0;
    const isActive = !apiResponse.cancelled && !apiResponse.finished;

    return isActive && currentCount < maxParticipants && apiResponse.status !== 'FINISHED';
  }

  /**
   * Maps participants from a game payload.
   * Supports both array payload and backend map payload { userId: username }.
   */
  private static mapGameParticipants(apiResponse: any): GameParticipant[] | undefined {
    const participants = apiResponse?.participants;
    if (!participants) {
      return undefined;
    }

    if (Array.isArray(participants)) {
      return this.mapApiParticipants(participants);
    }

    if (typeof participants !== 'object') {
      return undefined;
    }

    const creatorId = typeof apiResponse.creatorId === 'string' ? apiResponse.creatorId : '';
    const createdAt = this.validateAndFormatDate(apiResponse.createdAt || new Date().toISOString());

    return Object.entries(participants)
      .filter(([id, username]) => typeof id === 'string' && typeof username === 'string')
      .map(([id, username]) => ({
        id,
        username: username as string,
        joinedAt: createdAt,
        isCreator: id === creatorId
      }));
  }

  /**
   * Resolves creator display name with deterministic fallbacks.
   */
  private static resolveCreatorName(apiResponse: any): string {
    const creatorUsername = apiResponse.creatorUsername;
    if (typeof creatorUsername === 'string' && creatorUsername.trim() !== '') {
      return creatorUsername;
    }

    const creatorName = apiResponse.creatorName;
    if (typeof creatorName === 'string' && creatorName.trim() !== '') {
      return creatorName;
    }

    const creatorId = apiResponse.creatorId;
    const participants = apiResponse.participants;
    if (
      typeof creatorId === 'string' &&
      creatorId.trim() !== '' &&
      participants &&
      typeof participants === 'object' &&
      !Array.isArray(participants)
    ) {
      const creatorParticipantName = participants[creatorId];
      if (typeof creatorParticipantName === 'string' && creatorParticipantName.trim() !== '') {
        return creatorParticipantName;
      }
    }

    return 'Créateur inconnu';
  }

  /**
   * Calculates game fill percentage.
   */
  static calculateFillPercentage(game: Game): number {
    if (!game || game.maxParticipants === 0) {
      return 0;
    }

    return Math.round((game.participantCount / game.maxParticipants) * 100);
  }

  /**
   * Formats an ISO date into a relative french label.
   */
  static formatRelativeTime(dateString: string): string {
    try {
      const date = new Date(dateString);

      if (isNaN(date.getTime())) {
        return 'Date invalide';
      }

      const now = new Date();
      const diffInMinutes = Math.floor((now.getTime() - date.getTime()) / (1000 * 60));

      if (diffInMinutes < 1) return 'À l\'instant';
      if (diffInMinutes < 60) return `Il y a ${diffInMinutes} min`;

      const diffInHours = Math.floor(diffInMinutes / 60);
      if (diffInHours < 24) return `Il y a ${diffInHours}h`;

      const diffInDays = Math.floor(diffInHours / 24);
      if (diffInDays < 7) return `Il y a ${diffInDays}j`;

      return date.toLocaleDateString('fr-FR');
    } catch {
      return 'Date invalide';
    }
  }
}
