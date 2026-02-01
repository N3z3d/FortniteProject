import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import { Router } from '@angular/router';
import { TradeHistoryComponent, TradeHistoryItem } from './trade-history.component';
import { TranslationService } from '../../../core/services/translation.service';

describe('TradeHistoryComponent', () => {
  let component: TradeHistoryComponent;
  let fixture: ComponentFixture<TradeHistoryComponent>;
  let router: jasmine.SpyObj<Router>;
  let translationService: jasmine.SpyObj<TranslationService>;

  beforeEach(async () => {
    router = jasmine.createSpyObj('Router', ['navigate']);
    translationService = jasmine.createSpyObj('TranslationService', ['t']);
    translationService.t.and.callFake((key: string, fallback?: string) => fallback || key);

    await TestBed.configureTestingModule({
      imports: [TradeHistoryComponent],
      providers: [
        FormBuilder,
        { provide: Router, useValue: router },
        { provide: TranslationService, useValue: translationService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TradeHistoryComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with loading state', () => {
    expect(component.isLoading).toBeTrue();
    expect(component.allTrades).toEqual([]);
    expect(component.filteredTrades).toEqual([]);
  });

  it('should initialize filters form', () => {
    expect(component.filtersForm).toBeDefined();
    expect(component.filtersForm.get('status')).toBeDefined();
    expect(component.filtersForm.get('team')).toBeDefined();
    expect(component.filtersForm.get('player')).toBeDefined();
  });

  it('should have correct displayed columns', () => {
    expect(component.displayedColumns).toEqual(['status', 'details', 'team', 'createdAt', 'completedAt', 'actions']);
  });

  it('should load trade history on init', fakeAsync(() => {
    fixture.detectChanges();

    expect(component.isLoading).toBeTrue();

    tick(1000);

    expect(component.isLoading).toBeFalse();
    expect(component.allTrades.length).toBeGreaterThan(0);
    expect(component.filteredTrades.length).toBe(component.allTrades.length);
  }));

  it('should load mock trade history data', fakeAsync(() => {
    fixture.detectChanges();
    tick(1000);

    expect(component.allTrades.length).toBe(3);
    expect(component.allTrades[0].status).toBe('COMPLETED');
    expect(component.allTrades[1].status).toBe('PENDING');
    expect(component.allTrades[2].status).toBe('CANCELLED');
  }));

  it('should filter trades by status', fakeAsync(() => {
    fixture.detectChanges();
    tick(1000);

    component.filtersForm.patchValue({ status: 'PENDING' });
    component.applyFilters();

    expect(component.filteredTrades.length).toBe(1);
    expect(component.filteredTrades[0].status).toBe('PENDING');
  }));

  it('should filter trades by team', fakeAsync(() => {
    fixture.detectChanges();
    tick(1000);

    component.filtersForm.patchValue({ team: '1' });
    component.applyFilters();

    expect(component.filteredTrades.length).toBe(1);
    expect(component.filteredTrades[0].team.id).toBe('1');
  }));

  it('should filter trades by player name', fakeAsync(() => {
    fixture.detectChanges();
    tick(1000);

    component.filtersForm.patchValue({ player: 'Ninja' });
    component.applyFilters();

    expect(component.filteredTrades.length).toBe(1);
    expect(component.filteredTrades[0].playerOut.username).toBe('Ninja');
  }));

  it('should filter by player in name', fakeAsync(() => {
    fixture.detectChanges();
    tick(1000);

    component.filtersForm.patchValue({ player: 'Aqua' });
    component.applyFilters();

    expect(component.filteredTrades.length).toBe(1);
    expect(component.filteredTrades[0].playerIn.username).toBe('Aqua');
  }));

  it('should filter case-insensitively', fakeAsync(() => {
    fixture.detectChanges();
    tick(1000);

    component.filtersForm.patchValue({ player: 'ninja' });
    component.applyFilters();

    expect(component.filteredTrades.length).toBe(1);
  }));

  it('should combine multiple filters', fakeAsync(() => {
    fixture.detectChanges();
    tick(1000);

    component.filtersForm.patchValue({
      status: 'COMPLETED',
      team: '1'
    });
    component.applyFilters();

    expect(component.filteredTrades.length).toBe(1);
    expect(component.filteredTrades[0].status).toBe('COMPLETED');
    expect(component.filteredTrades[0].team.id).toBe('1');
  }));

  it('should clear filters', fakeAsync(() => {
    fixture.detectChanges();
    tick(1000);

    component.filtersForm.patchValue({ status: 'PENDING' });
    component.applyFilters();

    expect(component.filteredTrades.length).toBe(1);

    component.clearFilters();

    expect(component.filtersForm.value.status).toBeNull();
    expect(component.filteredTrades.length).toBe(component.allTrades.length);
  }));

  it('should detect active filters', fakeAsync(() => {
    fixture.detectChanges();
    tick(1000);

    expect(component.hasActiveFilters()).toBeFalse();

    component.filtersForm.patchValue({ status: 'PENDING' });
    expect(component.hasActiveFilters()).toBeTrue();

    component.filtersForm.patchValue({ status: null, team: '1' });
    expect(component.hasActiveFilters()).toBeTrue();

    component.filtersForm.reset();
    expect(component.hasActiveFilters()).toBeFalse();
  }));

  it('should navigate to trade detail', () => {
    component.viewTrade('123');

    expect(router.navigate).toHaveBeenCalledWith(['/trades', '123']);
  });

  it('should navigate to create new trade', () => {
    component.createNewTrade();

    expect(router.navigate).toHaveBeenCalledWith(['/trades/new']);
  });

  it('should navigate back to trades list', () => {
    component.goBack();

    expect(router.navigate).toHaveBeenCalledWith(['/trades']);
  });

  it('should get status color for PENDING', () => {
    expect(component.getStatusColor('PENDING')).toBe('accent');
  });

  it('should get status color for COMPLETED', () => {
    expect(component.getStatusColor('COMPLETED')).toBe('primary');
  });

  it('should get status color for CANCELLED', () => {
    expect(component.getStatusColor('CANCELLED')).toBe('warn');
  });

  it('should get status color for unknown status', () => {
    expect(component.getStatusColor('UNKNOWN')).toBe('');
  });

  it('should get status label from translation', () => {
    translationService.t.and.returnValue('Pending');

    const label = component.getStatusLabel('PENDING');

    expect(label).toBe('Pending');
    expect(translationService.t).toHaveBeenCalledWith('trades.status.pending', 'PENDING');
  });

  it('should get status label for uppercase status', () => {
    const label = component.getStatusLabel('COMPLETED');

    expect(translationService.t).toHaveBeenCalledWith('trades.status.completed', 'COMPLETED');
  });

  it('should get total trades count', fakeAsync(() => {
    fixture.detectChanges();
    tick(1000);

    expect(component.getTotalTrades()).toBe(3);
  }));

  it('should get completed trades count', fakeAsync(() => {
    fixture.detectChanges();
    tick(1000);

    expect(component.getCompletedTrades()).toBe(1);
  }));

  it('should get pending trades count', fakeAsync(() => {
    fixture.detectChanges();
    tick(1000);

    expect(component.getPendingTrades()).toBe(1);
  }));

  it('should get cancelled trades count', fakeAsync(() => {
    fixture.detectChanges();
    tick(1000);

    expect(component.getCancelledTrades()).toBe(1);
  }));

  it('should have available teams', () => {
    expect(component.availableTeams.length).toBe(3);
    expect(component.availableTeams[0].name).toBe('Team Alpha');
  });

  it('should return empty filtered trades when no trades loaded', () => {
    component.allTrades = [];

    component.applyFilters();

    expect(component.filteredTrades).toEqual([]);
  });

  it('should handle filters with no matching trades', fakeAsync(() => {
    fixture.detectChanges();
    tick(1000);

    component.filtersForm.patchValue({ player: 'NonExistentPlayer' });
    component.applyFilters();

    expect(component.filteredTrades.length).toBe(0);
  }));
});
