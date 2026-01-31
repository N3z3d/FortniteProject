import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { GameQueryService } from './game-query.service';
import { GameCommandService } from './game-command.service';
import {
  Game,
  CreateGameRequest,
  GameParticipant,
  DraftState,
  DraftStatistics,
  DraftHistoryEntry,
  InvitationCode
} from '../models/game.interface';

/**
 * CQRS Facade Service
 * Delegates to GameQueryService and GameCommandService
 * Provides a unified API for backward compatibility
 */
@Injectable({
  providedIn: 'root'
})
export class GameService {
  constructor(
    private readonly query: GameQueryService,
    private readonly command: GameCommandService
  ) { }

  // ==================== QUERY OPERATIONS (READ) ====================

  getAllGames(): Observable<Game[]> {
    return this.query.getAllGames();
  }

  getUserGames(): Observable<Game[]> {
    return this.query.getUserGames();
  }

  getAvailableGames(): Observable<Game[]> {
    return this.query.getAvailableGames();
  }

  getGameById(id: string): Observable<Game> {
    return this.query.getGameById(id);
  }

  getGameDetails(id: string): Observable<Game> {
    return this.query.getGameDetails(id);
  }

  validateInvitationCode(code: string): Observable<boolean> {
    return this.query.validateInvitationCode(code);
  }

  getDraftState(gameId: string): Observable<DraftState> {
    return this.query.getDraftState(gameId);
  }

  getDraftHistory(gameId: string): Observable<DraftHistoryEntry[]> {
    return this.query.getDraftHistory(gameId);
  }

  getDraftStatistics(gameId: string): Observable<DraftStatistics> {
    return this.query.getDraftStatistics(gameId);
  }

  getGameParticipants(gameId: string): Observable<GameParticipant[]> {
    return this.query.getGameParticipants(gameId);
  }

  canJoinGame(gameId: string): Observable<boolean> {
    return this.query.canJoinGame(gameId);
  }

  isGameHost(game: Game, userId: string): boolean {
    return this.query.isGameHost(game, userId);
  }

  getArchivedGameIds(): string[] {
    return this.query.getArchivedGameIds();
  }

  filterArchivedGames(games: Game[]): Game[] {
    return this.query.filterArchivedGames(games);
  }

  // ==================== COMMAND OPERATIONS (WRITE) ====================

  createGame(request: CreateGameRequest): Observable<Game> {
    return this.command.createGame(request);
  }

  joinGame(gameId: string): Observable<boolean> {
    return this.command.joinGame(gameId);
  }

  joinGameWithCode(invitationCode: string): Observable<Game> {
    return this.command.joinGameWithCode(invitationCode);
  }

  generateInvitationCode(gameId: string): Observable<InvitationCode> {
    return this.command.generateInvitationCode(gameId);
  }

  regenerateInvitationCode(gameId: string, duration: '24h' | '48h' | '7d' | 'permanent' = 'permanent'): Observable<Game> {
    return this.command.regenerateInvitationCode(gameId, duration);
  }

  renameGame(gameId: string, newName: string): Observable<Game> {
    return this.command.renameGame(gameId, newName);
  }

  deleteGame(gameId: string): Observable<boolean> {
    return this.command.deleteGame(gameId);
  }

  startDraft(gameId: string): Observable<boolean> {
    return this.command.startDraft(gameId);
  }

  finishDraft(gameId: string): Observable<Game> {
    return this.command.finishDraft(gameId);
  }

  archiveGame(gameId: string): Observable<boolean> {
    return this.command.archiveGame(gameId);
  }

  leaveGame(gameId: string): Observable<boolean> {
    return this.command.leaveGame(gameId);
  }

  initializeDraft(gameId: string): Observable<DraftState> {
    return this.command.initializeDraft(gameId);
  }

  makePlayerSelection(gameId: string, playerId: string): Observable<boolean> {
    return this.command.makePlayerSelection(gameId, playerId);
  }

  pauseDraft(gameId: string): Observable<boolean> {
    return this.command.pauseDraft(gameId);
  }

  resumeDraft(gameId: string): Observable<boolean> {
    return this.command.resumeDraft(gameId);
  }

  cancelDraft(gameId: string): Observable<boolean> {
    return this.command.cancelDraft(gameId);
  }
}
