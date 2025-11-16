import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface UserProfile {
  id: string;
  username: string;
  email: string;
  role?: string;
  lastLoginDate?: Date;
  browserFingerprint?: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserContextService {
  private readonly STORAGE_KEY = 'currentUser';
  private readonly LAST_USER_KEY = 'lastUser';
  private readonly AUTO_LOGIN_KEY = 'autoLogin';
  private readonly userChangedSubject = new BehaviorSubject<UserProfile | null>(null);

  public readonly userChanged$: Observable<UserProfile | null> = this.userChangedSubject.asObservable();

  constructor() {
    this.initializeFromStorage();
  }

  getAvailableProfiles(): UserProfile[] {
    return [
      { id: '1', username: 'Thibaut', email: 'thibaut@test.com' },
      { id: '2', username: 'Marcel', email: 'marcel@test.com' },
      { id: '3', username: 'Teddy', email: 'teddy@test.com' },
      { id: '4', username: 'Sarah', email: 'sarah@test.com' }
    ];
  }

  getCurrentUser(): UserProfile | null {
    const storedUser = sessionStorage.getItem(this.STORAGE_KEY);
    return storedUser ? JSON.parse(storedUser) : null;
  }

  login(user: UserProfile): void {
    // Add browser fingerprint and last login date
    const enrichedUser = {
      ...user,
      lastLoginDate: new Date(),
      browserFingerprint: this.generateBrowserFingerprint()
    };
    
    sessionStorage.setItem(this.STORAGE_KEY, JSON.stringify(enrichedUser));
    localStorage.setItem(this.LAST_USER_KEY, JSON.stringify(enrichedUser));
    localStorage.setItem(this.AUTO_LOGIN_KEY, 'true');
    
    this.userChangedSubject.next(enrichedUser);
  }

  logout(): void {
    sessionStorage.removeItem(this.STORAGE_KEY);
    this.userChangedSubject.next(null);
  }

  isLoggedIn(): boolean {
    return this.getCurrentUser() !== null;
  }

  getUserById(id: string): UserProfile | undefined {
    return this.getAvailableProfiles().find(profile => profile.id === id);
  }

  getUserByUsername(username: string): UserProfile | undefined {
    return this.getAvailableProfiles().find(profile => profile.username === username);
  }

  private initializeFromStorage(): void {
    const currentUser = this.getCurrentUser();
    if (currentUser) {
      this.userChangedSubject.next(currentUser);
    }
  }

  // Auto-login functionality
  isAutoLoginEnabled(): boolean {
    return localStorage.getItem(this.AUTO_LOGIN_KEY) === 'true';
  }

  getLastUser(): UserProfile | null {
    const lastUser = localStorage.getItem(this.LAST_USER_KEY);
    return lastUser ? JSON.parse(lastUser) : null;
  }

  attemptAutoLogin(): UserProfile | null {
    if (!this.isAutoLoginEnabled()) {
      return null;
    }

    const lastUser = this.getLastUser();
    if (!lastUser) {
      return null;
    }

    // Check if browser fingerprint matches
    const currentFingerprint = this.generateBrowserFingerprint();
    if (lastUser.browserFingerprint === currentFingerprint) {
      // Auto login the user
      this.login(lastUser);
      return lastUser;
    }

    return null;
  }

  disableAutoLogin(): void {
    localStorage.removeItem(this.AUTO_LOGIN_KEY);
  }

  // Browser fingerprinting (simple implementation)
  private generateBrowserFingerprint(): string {
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');
    ctx!.textBaseline = 'top';
    ctx!.font = '14px Arial';
    ctx!.fillText('Browser fingerprint', 2, 2);
    
    const fingerprint = [
      navigator.userAgent,
      navigator.language,
      screen.width + 'x' + screen.height,
      screen.colorDepth,
      new Date().getTimezoneOffset(),
      canvas.toDataURL()
    ].join('|');
    
    // Simple hash function
    let hash = 0;
    for (let i = 0; i < fingerprint.length; i++) {
      const char = fingerprint.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash; // Convert to 32-bit integer
    }
    
    return hash.toString(36);
  }
} 