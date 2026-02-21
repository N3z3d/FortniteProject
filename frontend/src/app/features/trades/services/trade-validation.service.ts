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
    _player: Player,
    fromList: string,
    toList: string,
    listTypes: {
      OFFERED_LIST: string;
      REQUESTED_LIST: string;
      AVAILABLE_LIST: string;
      TARGET_LIST: string;
    }
  ): boolean {
    const sourceGroup = this.resolveListGroup(fromList, listTypes);
    const targetGroup = this.resolveListGroup(toList, listTypes);

    if (sourceGroup === 'offered' && targetGroup === 'offered') {
      return fromList !== toList;
    }

    if (sourceGroup === 'requested' && targetGroup === 'requested') {
      return fromList !== toList;
    }

    if (sourceGroup !== null && targetGroup !== null && sourceGroup !== targetGroup) {
      return false;
    }

    return true;
  }

  private resolveListGroup(
    listName: string,
    listTypes: {
      OFFERED_LIST: string;
      REQUESTED_LIST: string;
      AVAILABLE_LIST: string;
      TARGET_LIST: string;
    }
  ): 'offered' | 'requested' | null {
    if (listName === listTypes.AVAILABLE_LIST || listName === listTypes.OFFERED_LIST) {
      return 'offered';
    }
    if (listName === listTypes.TARGET_LIST || listName === listTypes.REQUESTED_LIST) {
      return 'requested';
    }
    return null;
  }
}
