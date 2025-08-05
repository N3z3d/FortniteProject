import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class AccessibilityAnnouncerService {
  
  constructor() {}

  /**
   * Annonce un message pour les lecteurs d'écran
   */
  announce(message: string): void {
    // Création d'un élément aria-live pour les annonces
    const announcement = document.createElement('div');
    announcement.setAttribute('aria-live', 'polite');
    announcement.setAttribute('aria-atomic', 'true');
    announcement.className = 'sr-only';
    announcement.textContent = message;
    
    document.body.appendChild(announcement);
    
    // Supprimer après un délai pour éviter l'accumulation
    setTimeout(() => {
      document.body.removeChild(announcement);
    }, 1000);
  }

  /**
   * Annonce un changement de statut de jeu
   */
  announceGameStatusChange(status: string, gameName?: string): void {
    const message = gameName 
      ? `Statut du jeu ${gameName} changé : ${status}`
      : `Statut de jeu changé : ${status}`;
    this.announce(message);
  }

  /**
   * Annonce une erreur
   */
  announceError(error: string): void {
    this.announce(`Erreur : ${error}`);
  }

  /**
   * Annonce un succès
   */
  announceSuccess(message: string): void {
    this.announce(`Succès : ${message}`);
  }

  /**
   * Annonce la navigation
   */
  announceNavigation(page: string): void {
    this.announce(`Navigation vers ${page}`);
  }
}