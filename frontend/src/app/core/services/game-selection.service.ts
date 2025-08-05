import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { Game } from '../../features/game/models/game.interface';

@Injectable({
  providedIn: 'root'
})
export class GameSelectionService {
  private selectedGameSubject = new BehaviorSubject<Game | null>(null);
  public selectedGame$: Observable<Game | null> = this.selectedGameSubject.asObservable();

  constructor() {}

  /**
   * Définit la game actuellement sélectionnée
   */
  setSelectedGame(game: Game | null): void {
    this.selectedGameSubject.next(game);
  }

  /**
   * Récupère la game actuellement sélectionnée
   */
  getSelectedGame(): Game | null {
    return this.selectedGameSubject.value;
  }

  /**
   * Vérifie si une game est sélectionnée
   */
  hasSelectedGame(): boolean {
    return this.selectedGameSubject.value !== null;
  }

  /**
   * Réinitialise la sélection
   */
  clearSelection(): void {
    this.selectedGameSubject.next(null);
  }
}