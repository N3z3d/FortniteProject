import { Game, GameParticipant } from '../models/game.interface';

/**
 * Mapper pour transformer les réponses API en objets métier
 * Respecte le principe de Single Responsibility
 */
export class GameApiMapper {
  
  /**
   * Mappe une réponse API Game vers l'interface Game du front-end
   * @param apiResponse - Réponse brute de l'API
   * @returns Game formaté pour le front-end
   */
  static mapApiResponseToGame(apiResponse: any): Game {
    if (!apiResponse) {
      throw new Error('API response is null or undefined');
    }

    return {
      id: apiResponse.id,
      name: apiResponse.name,
      creatorName: apiResponse.creatorUsername || apiResponse.creatorName || 'Créateur inconnu',
      maxParticipants: apiResponse.maxParticipants || 0,
      status: apiResponse.status,
      createdAt: this.validateAndFormatDate(apiResponse.createdAt),
      participantCount: apiResponse.currentParticipantCount || apiResponse.participantCount || 0,
      canJoin: this.determineCanJoin(apiResponse),
      invitationCode: apiResponse.invitationCode,
      draftRules: apiResponse.draftRules,
      regionRules: apiResponse.regionRules
    };
  }

  /**
   * Mappe un tableau de participants API vers GameParticipant[]
   * @param apiParticipants - Tableau de participants de l'API
   * @returns GameParticipant[] formaté
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
      lastSelectionTime: participant.lastSelectionTime ? 
        this.validateAndFormatDate(participant.lastSelectionTime) : undefined,
      isCurrentTurn: participant.isCurrentTurn || false,
      timeRemaining: participant.timeRemaining
    }));
  }

  /**
   * Valide et formate une date string
   * @param dateString - String de date à valider
   * @returns Date string formatée ou string d'erreur
   */
  private static validateAndFormatDate(dateString: string | null | undefined): string {
    if (!dateString) {
      return new Date().toISOString(); // Date par défaut
    }

    try {
      const date = new Date(dateString);
      if (isNaN(date.getTime())) {
        console.warn(`Invalid date string: ${dateString}`);
        return new Date().toISOString();
      }
      return date.toISOString();
    } catch (error) {
      console.error(`Error parsing date: ${dateString}`, error);
      return new Date().toISOString();
    }
  }

  /**
   * Détermine si un utilisateur peut rejoindre la game
   * @param apiResponse - Réponse API
   * @returns boolean indiquant si on peut rejoindre
   */
  private static determineCanJoin(apiResponse: any): boolean {
    // Priorité aux propriétés explicites de l'API
    if (apiResponse.availableToJoin !== undefined) {
      return apiResponse.availableToJoin;
    }
    
    if (apiResponse.canJoin !== undefined) {
      return apiResponse.canJoin;
    }

    // Logique de fallback basée sur l'état de la game
    const currentCount = apiResponse.currentParticipantCount || apiResponse.participantCount || 0;
    const maxParticipants = apiResponse.maxParticipants || 0;
    const isActive = !apiResponse.cancelled && !apiResponse.finished;
    
    return isActive && currentCount < maxParticipants && apiResponse.status !== 'FINISHED';
  }

  /**
   * Calcule le pourcentage de remplissage d'une game
   * @param game - Objet Game
   * @returns Pourcentage (0-100)
   */
  static calculateFillPercentage(game: Game): number {
    if (!game || game.maxParticipants === 0) {
      return 0;
    }
    
    return Math.round((game.participantCount / game.maxParticipants) * 100);
  }

  /**
   * Formate une date relative (il y a X minutes/heures/jours)
   * @param dateString - String de date ISO
   * @returns String formatée relative
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
    } catch (error) {
      console.error('Error formatting relative time:', error);
      return 'Date invalide';
    }
  }
}