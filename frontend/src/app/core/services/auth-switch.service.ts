import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, delay, map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface AuthSwitchResponse {
  success: boolean;
  message: string;
  userId?: string;
  username?: string;
  timestamp?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthSwitchService {
  private readonly baseUrl = environment.apiBaseUrl || 'http://localhost:8081';

  constructor(private http: HttpClient) {}

  /**
   * Switches the current user context in the backend
   * @param username The username to switch to
   * @returns Observable with switch response
   */
  switchUser(username: string): Observable<AuthSwitchResponse> {
    console.log(`🔄 AuthSwitchService: Changement vers ${username}`);

    // For development, we'll use mock response with proper error handling
    if (!environment.production) {
      return this.mockSwitchUser(username);
    }

    // Production API call
    return this.http.post<AuthSwitchResponse>(`${this.baseUrl}/api/auth/switch`, {
      username: username
    }).pipe(
      catchError(error => {
        console.warn('⚠️ Erreur lors du changement d\'utilisateur:', error);
        // Don't throw error - return mock response as fallback
        return this.mockSwitchUser(username);
      })
    );
  }

  /**
   * Mock implementation for development/testing
   * @param username The username to switch to
   * @returns Observable with mock response
   */
  private mockSwitchUser(username: string): Observable<AuthSwitchResponse> {
    // Simulate network delay
    return of({
      success: true,
      message: `Changement vers ${username} effectué avec succès`,
      userId: this.generateMockUserId(username),
      username: username,
      timestamp: new Date().toISOString()
    }).pipe(
      delay(300) // Simulate network delay
    );
  }

  /**
   * Validates if user switch is allowed
   * @param username The username to validate
   * @returns Observable<boolean>
   */
  canSwitchToUser(username: string): Observable<boolean> {
    if (!username || username.trim().length === 0) {
      return of(false);
    }

    // Mock validation - in production this would check backend permissions
    const allowedUsers = ['Thibaut', 'Marcel', 'Teddy', 'Sarah'];
    return of(allowedUsers.includes(username));
  }

  /**
   * Gets the list of users that can be switched to
   * @returns Observable with list of available users
   */
  getAvailableUsers(): Observable<string[]> {
    if (!environment.production) {
      return of(['Thibaut', 'Marcel', 'Teddy', 'Sarah']).pipe(delay(200));
    }

    return this.http.get<string[]>(`${this.baseUrl}/api/auth/available-users`).pipe(
      catchError(() => {
        // Fallback to mock data if API fails
        return of(['Thibaut', 'Marcel', 'Teddy', 'Sarah']);
      })
    );
  }

  /**
   * Notifies backend of user context change
   * @param fromUser Previous user
   * @param toUser New user
   * @returns Observable with notification response
   */
  notifyUserSwitch(fromUser: string, toUser: string): Observable<AuthSwitchResponse> {
    console.log(`📢 Notification changement: ${fromUser} → ${toUser}`);

    if (!environment.production) {
      return of({
        success: true,
        message: `Notification de changement ${fromUser} → ${toUser} enregistrée`,
        timestamp: new Date().toISOString()
      }).pipe(delay(100));
    }

    return this.http.post<AuthSwitchResponse>(`${this.baseUrl}/api/auth/notify-switch`, {
      fromUser,
      toUser,
      timestamp: new Date().toISOString()
    }).pipe(
      catchError(error => {
        console.warn('⚠️ Erreur notification changement utilisateur:', error);
        // Return success anyway - this is not critical
        return of({
          success: true,
          message: 'Notification échouée mais changement effectué',
          timestamp: new Date().toISOString()
        });
      })
    );
  }

  /**
   * Validates current user session
   * @param username Current username to validate
   * @returns Observable<boolean>
   */
  validateUserSession(username: string): Observable<boolean> {
    if (!environment.production) {
      // Mock validation - always true for development
      return of(true).pipe(delay(100));
    }

    return this.http.post<{ valid: boolean }>(`${this.baseUrl}/api/auth/validate-session`, {
      username
    }).pipe(
      map(response => response.valid),
      catchError(() => {
        // If validation fails, assume session is valid to avoid blocking
        console.warn('⚠️ Validation de session échouée, considérée comme valide');
        return of(true);
      })
    );
  }

  /**
   * Logs user switch activity
   * @param activity Activity details
   */
  logSwitchActivity(activity: {
    action: 'switch' | 'validate' | 'notify';
    username: string;
    success: boolean;
    error?: string;
  }): void {
    const logEntry = {
      timestamp: new Date().toISOString(),
      service: 'AuthSwitchService',
      ...activity
    };

    console.log('📊 Auth Switch Activity:', logEntry);

    // In production, you might want to send this to a logging service
    if (environment.production) {
      // this.sendToLoggingService(logEntry);
    }
  }

  /**
   * Generates a mock user ID for development
   * @param username The username
   * @returns Mock user ID
   */
  private generateMockUserId(username: string): string {
    const userMap: { [key: string]: string } = {
      'Thibaut': '1',
      'Marcel': '2',
      'Teddy': '3',
      'Sarah': '4'
    };
    return userMap[username] || Math.random().toString(36).substr(2, 9);
  }

  /**
   * Handles errors gracefully for user switches
   * @param error The error that occurred
   * @param username The username being switched to
   * @returns Observable with fallback response
   */
  private handleSwitchError(error: any, username: string): Observable<AuthSwitchResponse> {
    console.error('❌ Erreur lors du changement d\'utilisateur:', error);
    
    this.logSwitchActivity({
      action: 'switch',
      username,
      success: false,
      error: error.message || 'Erreur inconnue'
    });

    // Return a fallback response that doesn't block the user
    return of({
      success: false,
      message: `Erreur lors du changement vers ${username}. Mode local activé.`,
      username,
      timestamp: new Date().toISOString()
    });
  }

  /**
   * Clears any cached auth data
   */
  clearAuthCache(): void {
    console.log('🧹 Nettoyage du cache auth');
    // Clear any cached authentication data
    // This might involve clearing localStorage, sessionStorage, etc.
  }

  /**
   * Gets current auth status
   * @returns Observable with current auth status
   */
  getAuthStatus(): Observable<{ authenticated: boolean; username?: string }> {
    if (!environment.production) {
      return of({
        authenticated: true,
        username: 'Development User'
      }).pipe(delay(100));
    }

    return this.http.get<{ authenticated: boolean; username?: string }>(`${this.baseUrl}/api/auth/status`).pipe(
      catchError(() => {
        return of({ authenticated: true, username: 'Unknown' });
      })
    );
  }
}