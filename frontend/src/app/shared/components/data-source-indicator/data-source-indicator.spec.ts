import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject } from 'rxjs';
import { DataSourceIndicator } from './data-source-indicator';
import { DataSourceStrategy, DataSourceType, DataSourceStatus } from '../../../core/strategies/data-source.strategy';

describe('DataSourceIndicator', () => {
  let component: DataSourceIndicator;
  let fixture: ComponentFixture<DataSourceIndicator>;
  let dataSourceStrategy: jasmine.SpyObj<DataSourceStrategy>;
  let statusSubject: BehaviorSubject<DataSourceStatus | null>;

  beforeEach(async () => {
    statusSubject = new BehaviorSubject<DataSourceStatus | null>(null);
    dataSourceStrategy = jasmine.createSpyObj('DataSourceStrategy', [], {
      currentSource$: statusSubject.asObservable()
    });

    await TestBed.configureTestingModule({
      imports: [DataSourceIndicator],
      providers: [
        { provide: DataSourceStrategy, useValue: dataSourceStrategy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DataSourceIndicator);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should subscribe to data source status on init', () => {
    expect(component.status).toBeNull();

    const mockStatus: DataSourceStatus = {
      type: DataSourceType.DATABASE,
      isAvailable: true,
      lastChecked: new Date(),
      message: 'Connected'
    };
    statusSubject.next(mockStatus);
    fixture.detectChanges();

    expect(component.status).toEqual(mockStatus);
  });

  it('should return badge-success for DATABASE', () => {
    component.status = {
      type: DataSourceType.DATABASE,
      isAvailable: true,
      lastChecked: new Date(),
      message: 'Connected'
    };
    expect(component.getBadgeClass()).toBe('badge-success');
  });

  it('should return badge-warning for MOCK', () => {
    component.status = {
      type: DataSourceType.MOCK,
      isAvailable: false,
      lastChecked: new Date(),
      message: 'Offline mode'
    };
    expect(component.getBadgeClass()).toBe('badge-warning');
  });

  it('should return badge-info for INITIALIZING', () => {
    component.status = {
      type: DataSourceType.INITIALIZING,
      isAvailable: false,
      lastChecked: new Date(),
      message: 'Initializing'
    };
    expect(component.getBadgeClass()).toBe('badge-info');
  });

  it('should return badge-error for ERROR', () => {
    component.status = {
      type: DataSourceType.ERROR,
      isAvailable: false,
      lastChecked: new Date(),
      message: 'Error'
    };
    expect(component.getBadgeClass()).toBe('badge-error');
  });

  it('should return badge-unknown when status is null', () => {
    component.status = null;
    expect(component.getBadgeClass()).toBe('badge-unknown');
  });

  it('should return correct icon for DATABASE', () => {
    component.status = {
      type: DataSourceType.DATABASE,
      isAvailable: true,
      lastChecked: new Date(),
      message: 'Connected'
    };
    expect(component.getIcon()).toBe('✅');
  });

  it('should return correct icon for MOCK', () => {
    component.status = {
      type: DataSourceType.MOCK,
      isAvailable: false,
      lastChecked: new Date(),
      message: 'Offline mode'
    };
    expect(component.getIcon()).toBe('⚠️');
  });

  it('should return correct icon for INITIALIZING', () => {
    component.status = {
      type: DataSourceType.INITIALIZING,
      isAvailable: false,
      lastChecked: new Date(),
      message: 'Initializing'
    };
    expect(component.getIcon()).toBe('⏳');
  });

  it('should return correct icon for ERROR', () => {
    component.status = {
      type: DataSourceType.ERROR,
      isAvailable: false,
      lastChecked: new Date(),
      message: 'Error'
    };
    expect(component.getIcon()).toBe('❌');
  });

  it('should return ❓ when status is null', () => {
    component.status = null;
    expect(component.getIcon()).toBe('❓');
  });

  it('should return correct display text for DATABASE', () => {
    component.status = {
      type: DataSourceType.DATABASE,
      isAvailable: true,
      lastChecked: new Date(),
      message: 'Connected'
    };
    expect(component.getDisplayText()).toBe('Base de données');
  });

  it('should return correct display text for MOCK', () => {
    component.status = {
      type: DataSourceType.MOCK,
      isAvailable: false,
      lastChecked: new Date(),
      message: 'Offline mode'
    };
    expect(component.getDisplayText()).toBe('Mode hors ligne');
  });

  it('should return Chargement when status is null', () => {
    component.status = null;
    expect(component.getDisplayText()).toBe('Chargement...');
  });

  it('should unsubscribe on destroy', () => {
    spyOn(component['destroy$'], 'next');
    spyOn(component['destroy$'], 'complete');

    component.ngOnDestroy();

    expect(component['destroy$'].next).toHaveBeenCalled();
    expect(component['destroy$'].complete).toHaveBeenCalled();
  });
});
