import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { UserContextService } from '../../core/services/user-context.service';
import { TranslationService, SupportedLanguage } from '../../core/services/translation.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatDividerModule,
    MatSlideToggleModule,
    MatFormFieldModule,
    MatSelectModule,
    MatSnackBarModule
  ],
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss']
})
export class SettingsComponent implements OnInit {
  // Notification settings
  emailNotifications = true;
  pushNotifications = false;
  gameReminders = true;
  draftAlerts = true;
  tradeNotifications = true;

  // Display settings
  theme = 'dark';
  language: SupportedLanguage = 'fr';

  // Game settings
  autoJoinDraft = false;
  showOnlineStatus = true;

  constructor(
    private userContextService: UserContextService,
    private router: Router,
    private snackBar: MatSnackBar,
    public t: TranslationService
  ) {}

  ngOnInit(): void {
    // Initialiser la langue depuis le service
    this.language = this.t.currentLanguage;
    // Load saved settings from local storage or backend
    this.loadSettings();
  }

  loadSettings(): void {
    // Load settings from localStorage or backend API
    const savedSettings = localStorage.getItem('userSettings');
    if (savedSettings) {
      const settings = JSON.parse(savedSettings);
      Object.assign(this, settings);
    }
    // Synchroniser avec le service de traduction
    this.language = this.t.currentLanguage;
  }

  onLanguageChange(): void {
    this.t.setLanguage(this.language);
  }

  saveSettings(): void {
    const settings = {
      emailNotifications: this.emailNotifications,
      pushNotifications: this.pushNotifications,
      gameReminders: this.gameReminders,
      draftAlerts: this.draftAlerts,
      tradeNotifications: this.tradeNotifications,
      theme: this.theme,
      language: this.language,
      autoJoinDraft: this.autoJoinDraft,
      showOnlineStatus: this.showOnlineStatus
    };

    // Save to localStorage (in real app, save to backend)
    localStorage.setItem('userSettings', JSON.stringify(settings));
    
    this.snackBar.open('Settings saved successfully!', 'Close', {
      duration: 3000
    });
  }

  resetSettings(): void {
    // Reset to default settings
    this.emailNotifications = true;
    this.pushNotifications = false;
    this.gameReminders = true;
    this.draftAlerts = true;
    this.tradeNotifications = true;
    this.theme = 'dark';
    this.language = 'fr';
    this.autoJoinDraft = false;
    this.showOnlineStatus = true;
    
    this.t.setLanguage(this.language);
    this.snackBar.open(this.t.t('settings.settingsReset'), this.t.t('common.close'), { duration: 3000 });
  }

  deleteAccount(): void {
    if (confirm('Are you sure you want to delete your account? This action cannot be undone.')) {
      // Implement account deletion logic
      console.log('Account deletion requested');
    }
  }

  exportData(): void {
    // Implement data export functionality
    console.log('Export data requested');
    this.snackBar.open('Data export started. You will receive an email when ready.', 'Close', {
      duration: 5000
    });
  }
}
