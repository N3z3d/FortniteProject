export type TradeDetailStatus = 'PENDING' | 'COMPLETED' | 'CANCELLED';

export interface TradeDetailStats {
  kills: number;
  wins: number;
  kd: number;
}

export interface TradeDetailPlayer {
  id: string;
  username: string;
  region: string;
  stats?: TradeDetailStats;
}

export interface TradeDetailTeam {
  id: string;
  name: string;
  owner: string;
}

export interface TradeDetail {
  id: string;
  playerOut: TradeDetailPlayer;
  playerIn: TradeDetailPlayer;
  team: TradeDetailTeam;
  createdAt: Date;
  completedAt?: Date;
  status: TradeDetailStatus;
  reason?: string;
}
