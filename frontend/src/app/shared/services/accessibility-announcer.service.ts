import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class AccessibilityAnnouncerService {
  private announcerElement: HTMLElement | null = null;

  constructor() {
    this.createAnnouncerElement();
  }

  /**
   * Creates a screen reader announcer element if it doesn't exist
   */
  private createAnnouncerElement(): void {
    if (this.announcerElement) {
      return;
    }

    this.announcerElement = document.createElement('div');
    this.announcerElement.setAttribute('aria-live', 'polite');
    this.announcerElement.setAttribute('aria-atomic', 'true');
    this.announcerElement.setAttribute('aria-hidden', 'false');
    this.announcerElement.setAttribute('class', 'sr-only accessibility-announcer');
    
    // Make it invisible but accessible to screen readers
    this.announcerElement.style.position = 'absolute';
    this.announcerElement.style.left = '-10000px';
    this.announcerElement.style.width = '1px';
    this.announcerElement.style.height = '1px';
    this.announcerElement.style.overflow = 'hidden';

    document.body.appendChild(this.announcerElement);
  }

  /**
   * Announces a message to screen readers with polite priority
   * @param message The message to announce
   */
  announce(message: string): void {
    this.announceWithPriority(message, 'polite');
  }

  /**
   * Announces a message to screen readers with assertive priority (interrupts current speech)
   * @param message The message to announce
   */
  announceUrgent(message: string): void {
    this.announceWithPriority(message, 'assertive');
  }

  /**
   * Announces a message with specified priority
   * @param message The message to announce
   * @param priority The announcement priority
   */
  private announceWithPriority(message: string, priority: 'polite' | 'assertive'): void {
    if (!this.announcerElement) {
      this.createAnnouncerElement();
    }

    if (!this.announcerElement || !message.trim()) {
      return;
    }

    // Set the priority
    this.announcerElement.setAttribute('aria-live', priority);
    
    // Clear previous message
    this.announcerElement.textContent = '';
    
    // Use setTimeout to ensure the screen reader picks up the change
    setTimeout(() => {
      if (this.announcerElement) {
        this.announcerElement.textContent = message.trim();
      }
    }, 100);
  }

  /**
   * Announces a successful action
   * @param action The action that was successful
   */
  announceSuccess(action: string): void {
    this.announce(`Succès: ${action}`);
  }

  /**
   * Announces an error
   * @param error The error message
   */
  announceError(error: string): void {
    this.announceUrgent(`Erreur: ${error}`);
  }

  /**
   * Announces a navigation change
   * @param pageName The name of the new page
   */
  announceNavigation(pageName: string): void {
    this.announce(`Navigation vers ${pageName}`);
  }

  /**
   * Announces loading state changes
   * @param isLoading Whether content is loading
   * @param context Optional context for what is loading
   */
  announceLoading(isLoading: boolean, context?: string): void {
    if (isLoading) {
      const message = context ? `Chargement de ${context}` : 'Chargement en cours';
      this.announce(message);
    } else {
      const message = context ? `${context} chargé` : 'Chargement terminé';
      this.announce(message);
    }
  }

  /**
   * Announces form validation errors
   * @param errors Array of error messages
   */
  announceFormErrors(errors: string[]): void {
    if (errors.length === 0) {
      return;
    }

    const message = errors.length === 1 
      ? `Erreur de validation: ${errors[0]}`
      : `${errors.length} erreurs de validation: ${errors.join(', ')}`;
    
    this.announceUrgent(message);
  }

  /**
   * Announces dynamic content changes
   * @param content Description of what changed
   */
  announceContentChange(content: string): void {
    this.announce(`Contenu mis à jour: ${content}`);
  }

  /**
   * Announces modal or dialog state changes
   * @param isOpen Whether the modal is opening or closing
   * @param modalName Name of the modal
   */
  announceModal(isOpen: boolean, modalName: string): void {
    const action = isOpen ? 'ouvert' : 'fermé';
    this.announce(`${modalName} ${action}`);
  }

  /**
   * Clears the announcer element
   */
  clear(): void {
    if (this.announcerElement) {
      this.announcerElement.textContent = '';
    }
  }

  /**
   * Destroys the announcer element (useful for cleanup)
   */
  destroy(): void {
    if (this.announcerElement && this.announcerElement.parentNode) {
      this.announcerElement.parentNode.removeChild(this.announcerElement);
      this.announcerElement = null;
    }
  }

  /**
   * Announces with polite priority (alias for announce)
   * @param message The message to announce
   */
  announcePolite(message: string): void {
    this.announce(message);
  }

  /**
   * Announces a technical error
   * @param error The error message
   */
  announceTechnicalError(error: string): void {
    this.announceUrgent(`Erreur technique: ${error}`);
  }

  /**
   * Announces error recovery
   * @param action The recovery action message
   */
  announceErrorRecovery(action: string): void {
    this.announce(`Récupération d'erreur: ${action}`);
  }

  /**
   * Announces game status changes
   * @param status The new game status
   * @param gameName Optional game name for context
   */
  announceGameStatusChange(status: string, gameName?: string): void {
    const message = gameName 
      ? `Statut du jeu "${gameName}": ${status}`
      : `Statut du jeu: ${status}`;
    this.announce(message);
  }
}