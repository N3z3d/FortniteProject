import { Injectable } from '@angular/core';
import { Player } from './trading.service';

@Injectable({
  providedIn: 'root'
})
export class TradeValidationService {
  validateTradeProposal(
    selectedTeam: unknown | null,
    offeredPlayers: Player[],
    requestedPlayers: Player[],
    isFormValid: boolean
  ): boolean {
    return (
      selectedTeam !== null &&
      offeredPlayers.length > 0 &&
      requestedPlayers.length > 0 &&
      isFormValid
    );
  }

  canMovePlayer(
    player: Player,
    fromList: string,
    toList: string,
    listTypes: {
      OFFERED_LIST: string;
      REQUESTED_LIST: string;
      AVAILABLE_LIST: string;
      TARGET_LIST: string;
    }
  ): boolean {
    if (
      (fromList === listTypes.AVAILABLE_LIST || fromList === listTypes.OFFERED_LIST) &&
      (toList === listTypes.AVAILABLE_LIST || toList === listTypes.OFFERED_LIST)
    ) {
      return fromList !== toList;
    }

    if (
      (fromList === listTypes.TARGET_LIST || fromList === listTypes.REQUESTED_LIST) &&
      (toList === listTypes.TARGET_LIST || toList === listTypes.REQUESTED_LIST)
    ) {
      return fromList !== toList;
    }

    if (
      (fromList === listTypes.AVAILABLE_LIST || fromList === listTypes.OFFERED_LIST) &&
      (toList === listTypes.TARGET_LIST || toList === listTypes.REQUESTED_LIST)
    ) {
      return false;
    }

    if (
      (fromList === listTypes.TARGET_LIST || fromList === listTypes.REQUESTED_LIST) &&
      (toList === listTypes.AVAILABLE_LIST || toList === listTypes.OFFERED_LIST)
    ) {
      return false;
    }

    return true;
  }
}
