import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, inject, ViewChild, ElementRef, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CdkDragDrop, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { Observable, Subject, BehaviorSubject, combineLatest, of } from 'rxjs';
import { takeUntil, map, startWith, debounceTime, switchMap, tap } from 'rxjs/operators';
import { trigger, transition, style, animate, query, stagger, keyframes } from '@angular/animations';

import { MaterialModule } from '../../../../shared/material/material.module';
import { TradingService, TradeOffer, Player, Team } from '../../services/trading.service';
import { UserContextService } from '../../../../core/services/user-context.service';
import { NotificationService } from '../../../../shared/services/notification.service';

interface TradeProposalState {
  selectedTeam: Team | null;
  offeredPlayers: Player[];
  requestedPlayers: Player[];
  availablePlayers: Player[];
  targetTeamPlayers: Player[];
  tradeBalance: number;
  isValid: boolean;
}

@Component({
  selector: 'app-trade-proposal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MaterialModule],
  templateUrl: './trade-proposal.component.html',
  styleUrls: [
    './trade-proposal.component.scss',
    '../../styles/trading-theme.scss',
    '../../styles/trading-animations.scss'
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  animations: [
    trigger('slideInFromSide', [
      transition(':enter', [
        style({ transform: 'translateX(-100%)', opacity: 0 }),
        animate('0.6s cubic-bezier(0.25, 0.46, 0.45, 0.94)', 
          style({ transform: 'translateX(0)', opacity: 1 })
        )
      ])
    ]),
    trigger('playerCardDrag', [
      transition('idle => dragging', [
        animate('0.3s ease-out', 
          style({ 
            transform: 'scale(1.1) rotate(8deg)',
            zIndex: 1000,
            boxShadow: '0 20px 60px rgba(var(--gaming-primary-rgb), 0.4)'
          })
        )
      ]),
      transition('dragging => idle', [
        animate('0.3s cubic-bezier(0.68, -0.55, 0.265, 1.55)', 
          style({ 
            transform: 'scale(1) rotate(0deg)',
            zIndex: 'auto',
            boxShadow: 'var(--shadow-trading-card)'
          })
        )
      ])
    ]),
    trigger('tradeBalanceChange', [
      transition('* => positive', [
        animate('0.6s cubic-bezier(0.68, -0.55, 0.265, 1.55)', keyframes([
          style({ transform: 'scale(1)', color: 'var(--gaming-light)', offset: 0 }),
          style({ transform: 'scale(1.2)', color: 'var(--value-positive)', offset: 0.5 }),
          style({ transform: 'scale(1)', color: 'var(--value-positive)', offset: 1 })
        ]))
      ]),
      transition('* => negative', [
        animate('0.6s cubic-bezier(0.68, -0.55, 0.265, 1.55)', keyframes([
          style({ transform: 'scale(1)', color: 'var(--gaming-light)', offset: 0 }),
          style({ transform: 'scale(1.2)', color: 'var(--value-negative)', offset: 0.5 }),
          style({ transform: 'scale(1)', color: 'var(--value-negative)', offset: 1 })
        ]))
      ]),
      transition('* => neutral', [
        animate('0.4s ease-out', 
          style({ transform: 'scale(1)', color: 'var(--gaming-light)' })
        )
      ])
    ]),
    trigger('dropZoneActive', [
      transition(':enter', [
        style({ opacity: 0, transform: 'scale(0.9)' }),
        animate('0.3s ease-out', style({ opacity: 1, transform: 'scale(1)' }))
      ])
    ])
  ]
})
export class TradeProposalComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();
  private readonly searchSubject = new BehaviorSubject<string>('');
  
  // Injected services
  private readonly tradingService = inject(TradingService);
  public readonly userContextService = inject(UserContextService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly notificationService = inject(NotificationService);
  private readonly fb = inject(FormBuilder);
  private readonly cdr = inject(ChangeDetectorRef);

  @ViewChild('playerSearch', { static: false }) playerSearchInput?: ElementRef<HTMLInputElement>;

  // Form
  tradeForm: FormGroup;
  
  // Observable streams
  teams$: Observable<Team[]>;
  loading$: Observable<boolean>;
  error$: Observable<string | null>;

  // Component state
  private tradeStateSubject = new BehaviorSubject<TradeProposalState>({
    selectedTeam: null,
    offeredPlayers: [],
    requestedPlayers: [],
    availablePlayers: [],
    targetTeamPlayers: [],
    tradeBalance: 0,
    isValid: false
  });

  tradeState$ = this.tradeStateSubject.asObservable();

  // UI state
  isSubmitting = new BehaviorSubject<boolean>(false);
  searchQuery$ = this.searchSubject.asObservable();
  dragState = new BehaviorSubject<'idle' | 'dragging'>('idle');
  
  // Filtered players
  filteredAvailablePlayers$!: Observable<Player[]>;
  filteredTargetPlayers$!: Observable<Player[]>;

  // Trade validation
  tradeValidation$ = this.tradeState$.pipe(
    map(state => ({
      hasOfferedPlayers: state.offeredPlayers.length > 0,
      hasRequestedPlayers: state.requestedPlayers.length > 0,
      isBalanced: this.tradingService.isTradeBalanced(state.offeredPlayers, state.requestedPlayers),
      selectedTeam: state.selectedTeam !== null,
      balancePercentage: this.calculateBalancePercentage(state.offeredPlayers, state.requestedPlayers),
      isValid: state.isValid
    }))
  );

  // Drag and drop lists
  readonly OFFERED_LIST = 'offered-players';
  readonly REQUESTED_LIST = 'requested-players';
  readonly AVAILABLE_LIST = 'available-players';
  readonly TARGET_LIST = 'target-players';

  constructor() {
    // Initialize form
    this.tradeForm = this.fb.group({
      targetTeam: ['', Validators.required],
      message: ['', [Validators.maxLength(500)]],
      expiresIn: [72, [Validators.required, Validators.min(1), Validators.max(168)]] // Hours
    });

    // Initialize observable streams
    this.teams$ = this.tradingService.teams$;
    this.loading$ = this.tradingService.loading$;
    this.error$ = this.tradingService.error$;

    this.setupFilteredPlayers();
  }

  ngOnInit(): void {
    this.loadInitialData();
    this.setupFormSubscriptions();
    this.handleRouteParams();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.searchSubject.complete();
    this.tradeStateSubject.complete();
    this.isSubmitting.complete();
    this.dragState.complete();
  }

  private setupFilteredPlayers(): void {
    this.filteredAvailablePlayers$ = combineLatest([
      this.tradeState$,
      this.searchQuery$.pipe(startWith(''), debounceTime(300))
    ]).pipe(
      map(([state, searchQuery]) => {
        let players = state.availablePlayers;
        
        if (searchQuery.trim()) {
          const query = searchQuery.toLowerCase();
          players = players.filter(player => 
            player.name.toLowerCase().includes(query) ||
            player.region.toLowerCase().includes(query) ||
            player.position?.toLowerCase().includes(query)
          );
        }
        
        return players;
      })
    );

    this.filteredTargetPlayers$ = combineLatest([
      this.tradeState$,
      this.searchQuery$.pipe(startWith(''), debounceTime(300))
    ]).pipe(
      map(([state, searchQuery]) => {
        let players = state.targetTeamPlayers;
        
        if (searchQuery.trim()) {
          const query = searchQuery.toLowerCase();
          players = players.filter(player => 
            player.name.toLowerCase().includes(query) ||
            player.region.toLowerCase().includes(query) ||
            player.position?.toLowerCase().includes(query)
          );
        }
        
        return players;
      })
    );
  }

  private loadInitialData(): void {
    // Load teams for the current game
    const gameId = this.route.snapshot.paramMap.get('gameId');
    
    if (gameId) {
      this.tradingService.getTeams(gameId)
        .pipe(takeUntil(this.destroy$))
        .subscribe(teams => {
          // Load current user's team players
          const currentUserId = this.userContextService.getCurrentUser()?.id;
          const userTeam = teams.find(team => team.ownerId === currentUserId);
          
          if (userTeam) {
            this.updateTradeState({
              availablePlayers: userTeam.players
            });
          }
        });
    }
  }

  private setupFormSubscriptions(): void {
    // Watch target team selection
    this.tradeForm.get('targetTeam')?.valueChanges
      .pipe(
        takeUntil(this.destroy$),
        switchMap(teamId => {
          if (!teamId) return of(null);
          return this.teams$.pipe(
            map(teams => teams.find(team => team.id === teamId) || null)
          );
        })
      )
      .subscribe(selectedTeam => {
        this.updateTradeState({
          selectedTeam,
          targetTeamPlayers: selectedTeam?.players || [],
          requestedPlayers: [] // Clear requested players when changing teams
        });
      });
  }

  private handleRouteParams(): void {
    // Check if there's a specific team to trade with
    const targetTeamId = this.route.snapshot.queryParamMap.get('targetTeam');
    if (targetTeamId) {
      this.tradeForm.patchValue({ targetTeam: targetTeamId });
    }
  }

  // Template helpers
  minPercent(value: number): number {
    return Math.min(value ?? 0, 100);
  }

  abs(value: number): number {
    return Math.abs(value ?? 0);
  }

  // Drag and Drop Handlers
  onDragStarted(): void {
    this.dragState.next('dragging');
  }

  onDragEnded(): void {
    this.dragState.next('idle');
  }

  onDrop(event: CdkDragDrop<Player[]>): void {
    const { previousContainer, container, previousIndex, currentIndex } = event;

    if (previousContainer === container) {
      // Reordering within the same list
      moveItemInArray(container.data, previousIndex, currentIndex);
    } else {
      // Moving between lists
      const player = previousContainer.data[previousIndex];
      
      // Validate the move
      if (!this.canMovePlayer(player, previousContainer.id, container.id)) {
        this.showErrorMessage('This player cannot be moved to this location');
        return;
      }

      transferArrayItem(
        previousContainer.data,
        container.data,
        previousIndex,
        currentIndex
      );

      // Update trade state based on the move
      this.handlePlayerMove(player, previousContainer.id, container.id);
    }

    this.calculateTradeBalance();
    this.validateTrade();
    this.cdr.markForCheck();
  }

  private canMovePlayer(player: Player, fromList: string, toList: string): boolean {
    const state = this.tradeStateSubject.value;

    // Can't move to the same type of list
    if ((fromList === this.AVAILABLE_LIST || fromList === this.OFFERED_LIST) &&
        (toList === this.AVAILABLE_LIST || toList === this.OFFERED_LIST)) {
      return fromList !== toList;
    }

    if ((fromList === this.TARGET_LIST || fromList === this.REQUESTED_LIST) &&
        (toList === this.TARGET_LIST || toList === this.REQUESTED_LIST)) {
      return fromList !== toList;
    }

    // Can't move between different team pools
    if ((fromList === this.AVAILABLE_LIST || fromList === this.OFFERED_LIST) &&
        (toList === this.TARGET_LIST || toList === this.REQUESTED_LIST)) {
      return false;
    }

    if ((fromList === this.TARGET_LIST || fromList === this.REQUESTED_LIST) &&
        (toList === this.AVAILABLE_LIST || toList === this.OFFERED_LIST)) {
      return false;
    }

    return true;
  }

  private handlePlayerMove(player: Player, fromList: string, toList: string): void {
    // This method is called after the arrays have already been updated
    // We just need to trigger recalculation of the trade state
    const currentState = this.tradeStateSubject.value;
    
    // The arrays in currentState are already updated by the drag-drop operation
    // Just trigger validation and balance calculation
    this.updateTradeState(currentState);
  }

  // Search functionality
  onSearchChange(query: string): void {
    this.searchSubject.next(query);
  }

  // Player actions
  addToOffered(player: Player): void {
    const state = this.tradeStateSubject.value;
    const availableIndex = state.availablePlayers.indexOf(player);
    
    if (availableIndex > -1) {
      const newState = { ...state };
      newState.availablePlayers.splice(availableIndex, 1);
      newState.offeredPlayers.push(player);
      
      this.updateTradeState(newState);
    }
  }

  removeFromOffered(player: Player): void {
    const state = this.tradeStateSubject.value;
    const offeredIndex = state.offeredPlayers.indexOf(player);
    
    if (offeredIndex > -1) {
      const newState = { ...state };
      newState.offeredPlayers.splice(offeredIndex, 1);
      newState.availablePlayers.push(player);
      
      this.updateTradeState(newState);
    }
  }

  addToRequested(player: Player): void {
    const state = this.tradeStateSubject.value;
    const targetIndex = state.targetTeamPlayers.indexOf(player);
    
    if (targetIndex > -1) {
      const newState = { ...state };
      newState.targetTeamPlayers.splice(targetIndex, 1);
      newState.requestedPlayers.push(player);
      
      this.updateTradeState(newState);
    }
  }

  removeFromRequested(player: Player): void {
    const state = this.tradeStateSubject.value;
    const requestedIndex = state.requestedPlayers.indexOf(player);
    
    if (requestedIndex > -1) {
      const newState = { ...state };
      newState.requestedPlayers.splice(requestedIndex, 1);
      newState.targetTeamPlayers.push(player);
      
      this.updateTradeState(newState);
    }
  }

  // Trade submission
  async onSubmitTrade(): Promise<void> {
    if (!this.tradeForm.valid) {
      this.markFormGroupTouched(this.tradeForm);
      return;
    }

    const state = this.tradeStateSubject.value;
    if (!state.isValid) {
      this.showErrorMessage('Please complete the trade proposal');
      return;
    }

    this.isSubmitting.next(true);

    const formValue = this.tradeForm.value;
    const expiresAt = new Date();
    expiresAt.setHours(expiresAt.getHours() + formValue.expiresIn);

    const tradeOffer: Partial<TradeOffer> = {
      toTeamId: state.selectedTeam!.id,
      toUserId: state.selectedTeam!.ownerId,
      offeredPlayers: state.offeredPlayers,
      requestedPlayers: state.requestedPlayers,
      message: formValue.message || undefined,
      expiresAt,
      valueBalance: state.tradeBalance
    };

    try {
      const createdTrade = await this.tradingService.createTradeOffer(tradeOffer).toPromise();
      
      this.showSuccessMessage('Trade offer sent successfully!');
      this.triggerSuccessAnimation();
      
      // Navigate back to dashboard after a delay
      setTimeout(() => {
        this.router.navigate(['/trades']);
      }, 2000);
      
    } catch (error) {
      this.showErrorMessage('Failed to send trade offer. Please try again.');
    } finally {
      this.isSubmitting.next(false);
    }
  }

  // Utility methods
  private updateTradeState(updates: Partial<TradeProposalState>): void {
    const currentState = this.tradeStateSubject.value;
    const newState = { ...currentState, ...updates };
    
    // Calculate trade balance
    newState.tradeBalance = this.tradingService.calculateTradeBalance(
      newState.offeredPlayers, 
      newState.requestedPlayers
    );
    
    // Validate trade
    newState.isValid = this.validateTradeState(newState);
    
    this.tradeStateSubject.next(newState);
  }

  private validateTradeState(state: TradeProposalState): boolean {
    return (
      state.selectedTeam !== null &&
      state.offeredPlayers.length > 0 &&
      state.requestedPlayers.length > 0 &&
      this.tradeForm.valid
    );
  }

  private calculateTradeBalance(): void {
    const state = this.tradeStateSubject.value;
    const balance = this.tradingService.calculateTradeBalance(
      state.offeredPlayers,
      state.requestedPlayers
    );
    
    this.updateTradeState({ tradeBalance: balance });
  }

  private validateTrade(): void {
    const state = this.tradeStateSubject.value;
    const isValid = this.validateTradeState(state);
    
    this.updateTradeState({ isValid });
  }

  private calculateBalancePercentage(offered: Player[], requested: Player[]): number {
    if (offered.length === 0 && requested.length === 0) return 0;
    
    const offeredValue = offered.reduce((sum, p) => sum + p.marketValue, 0);
    const requestedValue = requested.reduce((sum, p) => sum + p.marketValue, 0);
    const totalValue = offeredValue + requestedValue;
    
    if (totalValue === 0) return 0;
    
    return Math.abs(offeredValue - requestedValue) / totalValue * 100;
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }

  private showSuccessMessage(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 4000,
      panelClass: ['success-snackbar'],
      horizontalPosition: 'end',
      verticalPosition: 'top'
    });
  }

  private showErrorMessage(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 4000,
      panelClass: ['error-snackbar'],
      horizontalPosition: 'end',
      verticalPosition: 'top'
    });
  }

  private triggerSuccessAnimation(): void {
    // Add celebration animation class to the component
    const element = document.querySelector('.trade-proposal-container');
    if (element) {
      element.classList.add('trade-submitted');
      setTimeout(() => {
        element.classList.remove('trade-submitted');
      }, 2000);
    }
  }

  // Template helper methods
  formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(value);
  }

  getBalanceDisplayClass(balance: number): string {
    if (balance > 0) return 'positive';
    if (balance < 0) return 'negative';
    return 'neutral';
  }

  getBalanceIcon(balance: number): string {
    if (balance > 0) return 'trending_up';
    if (balance < 0) return 'trending_down';
    return 'compare_arrows';
  }

  trackByPlayerId(index: number, player: Player): string {
    return player.id;
  }

  // Navigation
  onCancel(): void {
    this.router.navigate(['/trades']);
  }

  onClearTrade(): void {
    const state = this.tradeStateSubject.value;
    
    // Move all offered players back to available
    const newAvailable = [...state.availablePlayers, ...state.offeredPlayers];
    
    // Move all requested players back to target team
    const newTargetPlayers = [...state.targetTeamPlayers, ...state.requestedPlayers];
    
    this.updateTradeState({
      offeredPlayers: [],
      requestedPlayers: [],
      availablePlayers: newAvailable,
      targetTeamPlayers: newTargetPlayers
    });

    this.tradeForm.patchValue({
      message: '',
      expiresIn: 72
    });
  }
}