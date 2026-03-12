import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AdminUserListComponent } from './admin-user-list.component';
import { AdminService } from '../services/admin.service';
import { AdminUserEntry } from '../models/admin.models';

const MOCK_USERS: AdminUserEntry[] = [
  {
    id: 'uuid-1',
    username: 'alice',
    email: 'alice@example.com',
    role: 'ADMIN',
    currentSeason: 2025,
    deleted: false
  },
  {
    id: 'uuid-2',
    username: 'bob',
    email: 'bob@example.com',
    role: 'USER',
    currentSeason: 2025,
    deleted: false
  },
  {
    id: 'uuid-3',
    username: 'charlie',
    email: 'charlie@example.com',
    role: 'SPECTATOR',
    currentSeason: 2025,
    deleted: true
  }
];

describe('AdminUserListComponent', () => {
  let component: AdminUserListComponent;
  let fixture: ComponentFixture<AdminUserListComponent>;
  let adminSpy: jasmine.SpyObj<AdminService>;

  beforeEach(async () => {
    adminSpy = jasmine.createSpyObj('AdminService', ['getUsers']);
    adminSpy.getUsers.and.returnValue(of(MOCK_USERS));

    await TestBed.configureTestingModule({
      imports: [AdminUserListComponent, NoopAnimationsModule],
      providers: [{ provide: AdminService, useValue: adminSpy }]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminUserListComponent);
    component = fixture.componentInstance;
  });

  it('calls getUsers on init', () => {
    fixture.detectChanges();
    expect(adminSpy.getUsers).toHaveBeenCalledTimes(1);
  });

  it('populates allUsers signal after successful load', () => {
    fixture.detectChanges();
    expect(component.allUsers()).toEqual(MOCK_USERS);
    expect(component.loading()).toBeFalse();
    expect(component.loadError).toBeFalse();
  });

  it('renders a row for each user when no filter applied', () => {
    fixture.detectChanges();
    const rows = fixture.nativeElement.querySelectorAll('mat-row, tr[mat-row]');
    expect(rows.length).toBe(3);
  });

  it('sets loadError true and loading false on error', () => {
    adminSpy.getUsers.and.returnValue(throwError(() => new Error('network')));
    fixture.detectChanges();
    expect(component.loadError).toBeTrue();
    expect(component.loading()).toBeFalse();
  });

  it('displays error state element on load failure', () => {
    adminSpy.getUsers.and.returnValue(throwError(() => new Error('network')));
    fixture.detectChanges();
    const error = fixture.nativeElement.querySelector('.user-list__error');
    expect(error).toBeTruthy();
  });

  it('calls loadUsers again when retry button clicked', () => {
    adminSpy.getUsers.and.returnValue(throwError(() => new Error('network')));
    fixture.detectChanges();
    const retryBtn = fixture.nativeElement.querySelector('button');
    expect(retryBtn).toBeTruthy();
    adminSpy.getUsers.and.returnValue(of(MOCK_USERS));
    retryBtn.click();
    fixture.detectChanges();
    expect(adminSpy.getUsers).toHaveBeenCalledTimes(2);
    expect(component.loadError).toBeFalse();
    expect(component.allUsers().length).toBe(3);
  });

  it('displays empty state when no users returned', () => {
    adminSpy.getUsers.and.returnValue(of([]));
    fixture.detectChanges();
    const empty = fixture.nativeElement.querySelector('.user-list__empty');
    expect(empty).toBeTruthy();
  });

  describe('filteredUsers', () => {
    beforeEach(() => fixture.detectChanges());

    it('returns all users when no filter applied', () => {
      expect(component.filteredUsers.length).toBe(3);
    });

    it('filters by username search (case-insensitive)', () => {
      component.searchControl.setValue('ALI');
      expect(component.filteredUsers.length).toBe(1);
      expect(component.filteredUsers[0].username).toBe('alice');
    });

    it('filters by email search', () => {
      component.searchControl.setValue('bob@');
      expect(component.filteredUsers.length).toBe(1);
      expect(component.filteredUsers[0].username).toBe('bob');
    });

    it('filters by role', () => {
      component.roleFilter.setValue('ADMIN');
      expect(component.filteredUsers.length).toBe(1);
      expect(component.filteredUsers[0].role).toBe('ADMIN');
    });

    it('filters deleted users only', () => {
      component.statusFilter.setValue('DELETED');
      expect(component.filteredUsers.length).toBe(1);
      expect(component.filteredUsers[0].username).toBe('charlie');
    });

    it('filters active users only', () => {
      component.statusFilter.setValue('ACTIVE');
      expect(component.filteredUsers.length).toBe(2);
    });

    it('combines search and role filters', () => {
      component.searchControl.setValue('example.com');
      component.roleFilter.setValue('USER');
      expect(component.filteredUsers.length).toBe(1);
      expect(component.filteredUsers[0].username).toBe('bob');
    });

    it('returns empty array when no users match combined filters', () => {
      component.roleFilter.setValue('ADMIN');
      component.statusFilter.setValue('DELETED');
      expect(component.filteredUsers.length).toBe(0);
    });
  });
});
