import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import { Game, GameParticipant } from '../models/game.interface';
import { GameApiMapper } from '../mappers/game-api.mapper';
import { CsvDataService } from './csv-data.service';

/**
 * Service responsable de la gestion des données de games
 * Respecte le principe de Single Responsibility
 * Utilise uniquement les données de la base de données
 */
@Injectable({
  providedIn: 'root'
})
export class GameDataService {
  private readonly apiUrl = `${environment.apiUrl}/api`;

  constructor(
    private http: HttpClient,
    private csvDataService: CsvDataService
  ) { }

  /**
   * Récupère une game par son ID depuis la BDD
   * @param gameId - ID de la game
   * @returns Observable<Game> - Game formatée
   */
  getGameById(gameId: string): Observable<Game> {
    if (!gameId || gameId.trim() === '') {
      return throwError(() => new Error('Game ID is required'));
    }

    return this.http.get<any>(`${this.apiUrl}/games/${gameId}`)
      .pipe(
        map(apiResponse => {
          try {
            return GameApiMapper.mapApiResponseToGame(apiResponse);
          } catch (mappingError) {
            console.error('Error mapping game data:', mappingError);
            throw new Error('Failed to process game data from server');
          }
        }),
        catchError(this.handleHttpError)
      );
  }

  /**
   * Récupère les participants d'une game depuis la BDD
   * @param gameId - ID de la game
   * @returns Observable<GameParticipant[]> - Liste des participants
   */
  getGameParticipants(gameId: string): Observable<GameParticipant[]> {
    if (!gameId || gameId.trim() === '') {
      return throwError(() => new Error('Game ID is required'));
    }

    // PHASE 1A: PRODUCTION READY - Real API calls replacing mock data
    return this.http.get<any[]>(`${this.apiUrl}/games/${gameId}/participants`)
      .pipe(
        map(apiParticipants => {
          try {
            if (!Array.isArray(apiParticipants)) {
              console.warn('API returned non-array response for participants');
              return [];
            }
            return GameApiMapper.mapApiParticipants(apiParticipants);
          } catch (mappingError) {
            console.error('Error mapping participants data:', mappingError);
            return []; // Retourner un tableau vide en cas d'erreur
          }
        }),
        catchError(this.handleHttpError)
      );
  }

  /**
   * Récupère les games de l'utilisateur depuis la BDD
   * @returns Observable<Game[]> - Liste des games
   */
  getUserGames(): Observable<Game[]> {
    return this.http.get<any[]>(`${this.apiUrl}/games/my-games`)
      .pipe(
        map(apiGames => {
          if (!Array.isArray(apiGames)) {
            console.warn('API returned non-array response for user games');
            return [];
          }

          return apiGames.map(apiGame => {
            try {
              return GameApiMapper.mapApiResponseToGame(apiGame);
            } catch (mappingError) {
              console.error('Error mapping game:', apiGame.id, mappingError);
              return null;
            }
          }).filter(game => game !== null) as Game[];
        }),
        catchError(this.handleHttpError)
      );
  }

  /**
   * Vérifie si une game existe et est accessible
   * @param gameId - ID de la game à vérifier
   * @returns Observable<boolean>
   */
  verifyGameExists(gameId: string): Observable<boolean> {
    if (!gameId || gameId.trim() === '') {
      return throwError(() => new Error('Game ID is required'));
    }

    return this.getGameById(gameId)
      .pipe(
        map(() => true),
        catchError(() => {
          return throwError(() => new Error('Game not found or not accessible'));
        })
      );
  }

  /**
   * Calcule les statistiques d'une game
   * @param game - Game pour laquelle calculer les stats
   * @returns Object avec les statistiques
   */
  calculateGameStatistics(game: Game) {
    if (!game) {
      return {
        fillPercentage: 0,
        availableSlots: 0,
        isNearlyFull: false,
        canAcceptMoreParticipants: false
      };
    }

    const fillPercentage = GameApiMapper.calculateFillPercentage(game);
    const availableSlots = game.maxParticipants - game.participantCount;
    const isNearlyFull = fillPercentage >= 80;
    const canAcceptMoreParticipants = game.canJoin && availableSlots > 0;

    return {
      fillPercentage,
      availableSlots,
      isNearlyFull,
      canAcceptMoreParticipants
    };
  }

  /**
   * Valide les données d'une game
   * @param game - Game à valider
   * @returns Object avec le résultat de validation
   */
  validateGameData(game: Game) {
    const errors: string[] = [];

    if (!game.id) errors.push('Game ID is missing');
    if (!game.name || game.name.trim() === '') errors.push('Game name is required');
    if (!game.creatorName) errors.push('Creator name is missing');
    if (game.maxParticipants <= 0) errors.push('Max participants must be greater than 0');
    if (game.participantCount < 0) errors.push('Participant count cannot be negative');
    if (game.participantCount > game.maxParticipants) errors.push('Participant count exceeds maximum');

    return {
      isValid: errors.length === 0,
      errors
    };
  }

  /**
   * Gestion centralisée des erreurs HTTP
   * @param error - Erreur HTTP
   * @returns Observable avec erreur formatée
   */
  private handleHttpError(error: any): Observable<never> {
    let errorMessage = 'Une erreur est survenue lors de la communication avec le serveur';
    
    if (error.status === 404) {
      errorMessage = 'Ressource non trouvée';
    } else if (error.status === 401) {
      errorMessage = 'Accès non autorisé';
    } else if (error.status === 403) {
      errorMessage = 'Accès interdit';
    } else if (error.status === 500) {
      errorMessage = 'Erreur interne du serveur';
    } else if (error.error?.message) {
      errorMessage = error.error.message;
    }

    console.error('GameDataService HTTP Error:', {
      status: error.status,
      message: errorMessage,
      originalError: error
    });

    return throwError(() => new Error(errorMessage));
  }
}