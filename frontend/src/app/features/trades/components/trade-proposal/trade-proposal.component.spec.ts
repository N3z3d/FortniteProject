import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { BehaviorSubject, of } from 'rxjs';

import { TradeProposalComponent } from './trade-proposal.component';
import { TradingService } from '../../services/trading.service';
import { UserContextService } from '../../../../core/services/user-context.service';
import { NotificationService } from '../../../../shared/services/notification.service';
import { TranslationService } from '../../../../core/services/translation.service';

describe('TradeProposalComponent (i18n)', () => {
  let fixture: ComponentFixture<TradeProposalComponent>;
  let translationService: TranslationService;
  let loadingSubject: BehaviorSubject<boolean>;
  let errorSubject: BehaviorSubject<string | null>;

  beforeEach(async () => {
    localStorage.removeItem('app_language');

    loadingSubject = new BehaviorSubject<boolean>(false);
    errorSubject = new BehaviorSubject<string | null>(null);

    const tradingServiceSpy = jasmine.createSpyObj<TradingService>(
      'TradingService',
      ['getTeams', 'calculateTradeBalance', 'isTradeBalanced', 'createTradeOffer'],
      {
        teams$: of([]),
        loading$: loadingSubject.asObservable(),
        error$: errorSubject.asObservable()
      }
    );

    tradingServiceSpy.getTeams.and.returnValue(of([]));
    tradingServiceSpy.calculateTradeBalance.and.returnValue(0);
    tradingServiceSpy.isTradeBalanced.and.returnValue(true);
    tradingServiceSpy.createTradeOffer.and.returnValue(of({} as any));

    const userContextSpy = jasmine.createSpyObj<UserContextService>('UserContextService', ['getCurrentUser']);
    userContextSpy.getCurrentUser.and.returnValue({ id: 'user-1', username: 'testuser', email: 'test@test.com' });

    const snackBarSpy = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    const dialogSpy = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    const routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [TradeProposalComponent, NoopAnimationsModule],
      providers: [
        { provide: TradingService, useValue: tradingServiceSpy },
        { provide: UserContextService, useValue: userContextSpy },
        { provide: NotificationService, useValue: {} },
        { provide: MatSnackBar, useValue: snackBarSpy },
        { provide: MatDialog, useValue: dialogSpy },
        { provide: Router, useValue: routerSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            parent: { params: of({}) },
            snapshot: { queryParamMap: { get: () => null } }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TradeProposalComponent);
    translationService = TestBed.inject(TranslationService);
  });

  it('affiche le titre en francais par defaut', () => {
    fixture.detectChanges();

    const title = fixture.nativeElement.querySelector('.proposal-title') as HTMLElement;
    expect(title?.textContent).toContain(translationService.t('trades.proposal.title'));
  });

  it('affiche le titre en anglais quand la langue est en', () => {
    translationService.setLanguage('en');
    fixture.detectChanges();

    const title = fixture.nativeElement.querySelector('.proposal-title') as HTMLElement;
    expect(title?.textContent).toContain('Create Trade Proposal');
  });

  it('affiche le titre d\'erreur quand une erreur est presente', () => {
    errorSubject.next('Erreur');
    fixture.detectChanges();

    const errorTitle = fixture.nativeElement.querySelector('.error-container h3') as HTMLElement;
    expect(errorTitle?.textContent).toContain(translationService.t('trades.proposal.errorTitle'));
  });

  it('affiche le message de chargement quand loading$ est true', () => {
    loadingSubject.next(true);
    fixture.detectChanges();

    const loadingText = fixture.nativeElement.querySelector('.loading-container p') as HTMLElement;
    expect(loadingText?.textContent).toContain(translationService.t('trades.proposal.loading.teamsAndPlayers'));
  });
});
