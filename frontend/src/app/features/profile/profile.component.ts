import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatListModule } from '@angular/material/list';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { UserContextService, UserProfile } from '../../core/services/user-context.service';
import { TranslationService } from '../../core/services/translation.service';
import { Router } from '@angular/router';
import { EditProfileDialogComponent } from './edit-profile-dialog/edit-profile-dialog.component';
import { ChangePasswordDialogComponent } from './change-password-dialog/change-password-dialog.component';
import { UserStatsDialogComponent } from './user-stats-dialog/user-stats-dialog.component';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatDividerModule,
    MatListModule,
    MatDialogModule,
    MatSnackBarModule
  ],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {
  currentUser: UserProfile | null = null;

  constructor(
    private readonly userContextService: UserContextService,
    private readonly router: Router,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar,
    public readonly t: TranslationService
  ) {}

  ngOnInit(): void {
    this.currentUser = this.userContextService.getCurrentUser();
    if (!this.currentUser) {
      this.router.navigate(['/login']);
    }
  }

  editProfile(): void {
    if (!this.currentUser) return;

    const dialogRef = this.dialog.open(EditProfileDialogComponent, {
      width: '500px',
      maxWidth: '95vw',
      data: { user: this.currentUser },
      panelClass: 'premium-dialog'
    });

    dialogRef.afterClosed().subscribe((result: UserProfile | undefined) => {
      if (result) {
        this.currentUser = result;
        this.userContextService.login(result);
        this.snackBar.open('Profil mis à jour !', 'Fermer', { duration: 3000 });
      }
    });
  }

  changePassword(): void {
    const dialogRef = this.dialog.open(ChangePasswordDialogComponent, {
      width: '450px',
      maxWidth: '95vw',
      panelClass: 'premium-dialog'
    });

    dialogRef.afterClosed().subscribe((success: boolean) => {
      if (success) {
        this.snackBar.open('Mot de passe modifié avec succès !', 'Fermer', { duration: 3000 });
      }
    });
  }

  viewStatistics(): void {
    if (!this.currentUser) return;

    this.dialog.open(UserStatsDialogComponent, {
      width: '600px',
      maxWidth: '95vw',
      data: { user: this.currentUser },
      panelClass: 'premium-dialog'
    });
  }

  logout(): void {
    this.userContextService.logout();
    this.router.navigate(['/login']);
  }

  getRoleIcon(): string {
    const role = this.currentUser?.role?.toLowerCase() || '';

    if (role.includes('admin')) {
      return 'admin_panel_settings';
    } else if (role.includes('modérat') || role.includes('moderat')) {
      return 'verified_user';
    } else if (role.includes('joueur') || role.includes('player')) {
      return 'sports_esports';
    }

    return 'person';
  }

  getRoleBadgeClass(): string {
    const role = this.currentUser?.role?.toLowerCase() || '';

    if (role.includes('admin')) {
      return 'admin';
    } else if (role.includes('modérat') || role.includes('moderat')) {
      return 'moderator';
    } else if (role.includes('joueur') || role.includes('player')) {
      return 'player';
    }

    return 'user';
  }
}