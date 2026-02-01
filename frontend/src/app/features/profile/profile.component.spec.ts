import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { ProfileComponent } from './profile.component';
import { UserContextService, UserProfile } from '../../core/services/user-context.service';
import { TranslationService } from '../../core/services/translation.service';
import { EditProfileDialogComponent } from './edit-profile-dialog/edit-profile-dialog.component';
import { ChangePasswordDialogComponent } from './change-password-dialog/change-password-dialog.component';
import { UserStatsDialogComponent } from './user-stats-dialog/user-stats-dialog.component';

describe('ProfileComponent', () => {
  let component: ProfileComponent;
  let fixture: ComponentFixture<ProfileComponent>;
  let userContextService: jasmine.SpyObj<UserContextService>;
  let router: jasmine.SpyObj<Router>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;
  let translationService: jasmine.SpyObj<TranslationService>;

  const mockUser: UserProfile = {
    id: 'user1',
    username: 'testuser',
    email: 'test@example.com',
    role: 'Joueur'
  };

  beforeEach(async () => {
    userContextService = jasmine.createSpyObj('UserContextService', ['getCurrentUser', 'login', 'logout']);
    router = jasmine.createSpyObj('Router', ['navigate']);
    dialog = jasmine.createSpyObj('MatDialog', ['open']);
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    translationService = jasmine.createSpyObj('TranslationService', ['t']);
    translationService.t.and.callFake((key: string) => key);

    TestBed.configureTestingModule({
      imports: [ProfileComponent]
    });
    TestBed.overrideComponent(ProfileComponent, {
      set: {
        providers: [
          { provide: UserContextService, useValue: userContextService },
          { provide: Router, useValue: router },
          { provide: MatDialog, useValue: dialog },
          { provide: MatSnackBar, useValue: snackBar },
          { provide: TranslationService, useValue: translationService }
        ]
      }
    });
    await TestBed.compileComponents();

    fixture = TestBed.createComponent(ProfileComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load current user on init', () => {
    userContextService.getCurrentUser.and.returnValue(mockUser);

    component.ngOnInit();

    expect(component.currentUser).toEqual(mockUser);
  });

  it('should redirect to login when no user', () => {
    userContextService.getCurrentUser.and.returnValue(null);

    component.ngOnInit();

    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should open edit dialog and update user on success', () => {
    component.currentUser = mockUser;
    const updatedUser: UserProfile = { ...mockUser, username: 'newuser' };
    const dialogRef = jasmine.createSpyObj('MatDialogRef', ['afterClosed']);
    dialogRef.afterClosed.and.returnValue(of(updatedUser));
    dialog.open.and.returnValue(dialogRef);

    component.editProfile();

    expect(dialog.open).toHaveBeenCalledWith(EditProfileDialogComponent, {
      width: '500px',
      maxWidth: '95vw',
      data: { user: mockUser },
      panelClass: 'premium-dialog'
    });
    expect(component.currentUser).toEqual(updatedUser);
    expect(userContextService.login).toHaveBeenCalledWith(updatedUser);
    expect(snackBar.open).toHaveBeenCalled();
  });

  it('should not update user when edit dialog cancelled', () => {
    component.currentUser = mockUser;
    const dialogRef = jasmine.createSpyObj('MatDialogRef', ['afterClosed']);
    dialogRef.afterClosed.and.returnValue(of(undefined));
    dialog.open.and.returnValue(dialogRef);

    component.editProfile();

    expect(component.currentUser).toEqual(mockUser);
    expect(userContextService.login).not.toHaveBeenCalled();
  });

  it('should not open edit dialog when no user', () => {
    component.currentUser = null;

    component.editProfile();

    expect(dialog.open).not.toHaveBeenCalled();
  });

  it('should open change password dialog', () => {
    const dialogRef = jasmine.createSpyObj('MatDialogRef', ['afterClosed']);
    dialogRef.afterClosed.and.returnValue(of(true));
    dialog.open.and.returnValue(dialogRef);

    component.changePassword();

    expect(dialog.open).toHaveBeenCalledWith(ChangePasswordDialogComponent, {
      width: '450px',
      maxWidth: '95vw',
      panelClass: 'premium-dialog'
    });
    expect(snackBar.open).toHaveBeenCalled();
  });

  it('should not show snackbar when password change cancelled', () => {
    const dialogRef = jasmine.createSpyObj('MatDialogRef', ['afterClosed']);
    dialogRef.afterClosed.and.returnValue(of(false));
    dialog.open.and.returnValue(dialogRef);

    component.changePassword();

    expect(snackBar.open).not.toHaveBeenCalled();
  });

  it('should open stats dialog', () => {
    component.currentUser = mockUser;
    const dialogRef = jasmine.createSpyObj('MatDialogRef', ['afterClosed']);
    dialog.open.and.returnValue(dialogRef);

    component.viewStatistics();

    expect(dialog.open).toHaveBeenCalledWith(UserStatsDialogComponent, {
      width: '600px',
      maxWidth: '95vw',
      data: { user: mockUser },
      panelClass: 'premium-dialog'
    });
  });

  it('should not open stats dialog when no user', () => {
    component.currentUser = null;

    component.viewStatistics();

    expect(dialog.open).not.toHaveBeenCalled();
  });

  it('should logout and navigate to login', () => {
    component.logout();

    expect(userContextService.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should return admin icon for admin role', () => {
    component.currentUser = { ...mockUser, role: 'Admin' };
    expect(component.getRoleIcon()).toBe('admin_panel_settings');
  });

  it('should return moderator icon for moderator role', () => {
    component.currentUser = { ...mockUser, role: 'Modérateur' };
    expect(component.getRoleIcon()).toBe('verified_user');
  });

  it('should return player icon for joueur role', () => {
    component.currentUser = { ...mockUser, role: 'Joueur' };
    expect(component.getRoleIcon()).toBe('sports_esports');
  });

  it('should return default person icon for unknown role', () => {
    component.currentUser = { ...mockUser, role: 'Unknown' };
    expect(component.getRoleIcon()).toBe('person');
  });

  it('should return admin badge class for admin role', () => {
    component.currentUser = { ...mockUser, role: 'Admin' };
    expect(component.getRoleBadgeClass()).toBe('admin');
  });

  it('should return moderator badge class for moderator role', () => {
    component.currentUser = { ...mockUser, role: 'Modérateur' };
    expect(component.getRoleBadgeClass()).toBe('moderator');
  });

  it('should return player badge class for player role', () => {
    component.currentUser = { ...mockUser, role: 'Player' };
    expect(component.getRoleBadgeClass()).toBe('player');
  });

  it('should return user badge class for unknown role', () => {
    component.currentUser = { ...mockUser, role: undefined };
    expect(component.getRoleBadgeClass()).toBe('user');
  });
});
