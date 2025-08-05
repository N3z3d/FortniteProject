import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class FocusManagementService {
  private previousFocusElement: HTMLElement | null = null;
  private focusStack: HTMLElement[] = [];

  constructor() {}

  /**
   * Sets focus to the specified element
   * @param element The element to focus
   * @param options Focus options
   */
  setFocus(element: HTMLElement | null, options?: FocusOptions): void {
    if (!element) {
      return;
    }

    // Store previous focus for restoration
    if (document.activeElement && document.activeElement !== document.body) {
      this.previousFocusElement = document.activeElement as HTMLElement;
    }

    element.focus(options);
  }

  /**
   * Alias for setFocus (for compatibility)
   * @param element The element to focus
   * @param options Focus options
   */
  focusElement(element: HTMLElement | null, options?: FocusOptions): void {
    this.setFocus(element, options);
  }

  /**
   * Sets focus to an element by selector
   * @param selector CSS selector
   * @param options Focus options
   */
  setFocusBySelector(selector: string, options?: FocusOptions): void {
    const element = document.querySelector(selector) as HTMLElement;
    this.setFocus(element, options);
  }

  /**
   * Sets focus to an element by ID
   * @param id Element ID
   * @param options Focus options
   */
  setFocusById(id: string, options?: FocusOptions): void {
    const element = document.getElementById(id);
    this.setFocus(element, options);
  }

  /**
   * Restores focus to the previously focused element
   */
  restoreFocus(): void {
    if (this.previousFocusElement) {
      this.setFocus(this.previousFocusElement);
      this.previousFocusElement = null;
    }
  }

  /**
   * Pushes current focus to stack and sets focus to new element
   * @param element The element to focus
   * @param options Focus options
   */
  pushFocus(element: HTMLElement | null, options?: FocusOptions): void {
    if (document.activeElement && document.activeElement !== document.body) {
      this.focusStack.push(document.activeElement as HTMLElement);
    }
    this.setFocus(element, options);
  }

  /**
   * Pops and restores focus from the stack
   */
  popFocus(): void {
    const previousElement = this.focusStack.pop();
    if (previousElement) {
      this.setFocus(previousElement);
    }
  }

  /**
   * Clears the focus stack
   */
  clearFocusStack(): void {
    this.focusStack = [];
  }

  /**
   * Traps focus within a container element
   * @param container The container to trap focus within
   * @returns Function to remove focus trap
   */
  trapFocus(container: HTMLElement): () => void {
    const focusableElements = this.getFocusableElements(container);
    
    if (focusableElements.length === 0) {
      return () => {};
    }

    const firstElement = focusableElements[0];
    const lastElement = focusableElements[focusableElements.length - 1];

    // Set focus to first element
    this.setFocus(firstElement);

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Tab') {
        if (event.shiftKey) {
          // Shift + Tab
          if (document.activeElement === firstElement) {
            event.preventDefault();
            this.setFocus(lastElement);
          }
        } else {
          // Tab
          if (document.activeElement === lastElement) {
            event.preventDefault();
            this.setFocus(firstElement);
          }
        }
      }
    };

    container.addEventListener('keydown', handleKeyDown);

    // Return cleanup function
    return () => {
      container.removeEventListener('keydown', handleKeyDown);
    };
  }

  /**
   * Gets all focusable elements within a container
   * @param container The container to search within
   * @returns Array of focusable elements
   */
  getFocusableElements(container: HTMLElement): HTMLElement[] {
    const focusableSelectors = [
      'a[href]',
      'button:not([disabled])',
      'input:not([disabled])',
      'select:not([disabled])',
      'textarea:not([disabled])',
      '[tabindex]:not([tabindex="-1"])',
      '[contenteditable="true"]'
    ].join(',');

    const elements = Array.from(container.querySelectorAll(focusableSelectors)) as HTMLElement[];
    
    return elements.filter(element => {
      return this.isElementVisible(element) && !this.isElementDisabled(element);
    });
  }

  /**
   * Checks if an element is visible
   * @param element The element to check
   * @returns True if element is visible
   */
  private isElementVisible(element: HTMLElement): boolean {
    const style = window.getComputedStyle(element);
    return style.display !== 'none' && 
           style.visibility !== 'hidden' && 
           style.opacity !== '0' &&
           element.offsetParent !== null;
  }

  /**
   * Checks if an element is disabled
   * @param element The element to check
   * @returns True if element is disabled
   */
  private isElementDisabled(element: HTMLElement): boolean {
    return element.hasAttribute('disabled') || 
           element.getAttribute('aria-disabled') === 'true';
  }

  /**
   * Sets focus to the first focusable element in a container
   * @param container The container to search within
   * @param options Focus options
   */
  focusFirstElement(container: HTMLElement, options?: FocusOptions): void {
    const focusableElements = this.getFocusableElements(container);
    if (focusableElements.length > 0) {
      this.setFocus(focusableElements[0], options);
    }
  }

  /**
   * Sets focus to the last focusable element in a container
   * @param container The container to search within
   * @param options Focus options
   */
  focusLastElement(container: HTMLElement, options?: FocusOptions): void {
    const focusableElements = this.getFocusableElements(container);
    if (focusableElements.length > 0) {
      this.setFocus(focusableElements[focusableElements.length - 1], options);
    }
  }

  /**
   * Manages focus for modal dialogs
   * @param modalElement The modal element
   * @returns Object with focus management methods
   */
  manageModalFocus(modalElement: HTMLElement): {
    trapFocus: () => () => void;
    restoreFocus: () => void;
  } {
    // Store current focus
    this.pushFocus(document.activeElement as HTMLElement);

    return {
      trapFocus: () => this.trapFocus(modalElement),
      restoreFocus: () => this.popFocus()
    };
  }

  /**
   * Announces focus changes to screen readers
   * @param element The focused element
   * @param customMessage Optional custom message
   */
  announceFocus(element: HTMLElement, customMessage?: string): void {
    const message = customMessage || this.getFocusAnnouncement(element);
    
    // Create temporary announcer if needed
    const announcer = document.createElement('div');
    announcer.setAttribute('aria-live', 'polite');
    announcer.style.position = 'absolute';
    announcer.style.left = '-10000px';
    announcer.textContent = message;
    
    document.body.appendChild(announcer);
    
    setTimeout(() => {
      document.body.removeChild(announcer);
    }, 1000);
  }

  /**
   * Gets appropriate announcement text for a focused element
   * @param element The focused element
   * @returns Announcement text
   */
  private getFocusAnnouncement(element: HTMLElement): string {
    const tagName = element.tagName.toLowerCase();
    const ariaLabel = element.getAttribute('aria-label');
    const title = element.getAttribute('title');
    const textContent = element.textContent?.trim();

    if (ariaLabel) {
      return ariaLabel;
    }

    if (title) {
      return title;
    }

    switch (tagName) {
      case 'button':
        return `Bouton ${textContent || 'sans libellé'}`;
      case 'input':
        const inputType = element.getAttribute('type') || 'text';
        return `Champ ${inputType} ${textContent || 'sans libellé'}`;
      case 'a':
        return `Lien ${textContent || 'sans libellé'}`;
      default:
        return textContent || `Élément ${tagName}`;
    }
  }
}