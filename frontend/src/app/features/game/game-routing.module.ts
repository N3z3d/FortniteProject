import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

// Composants (à créer)
import { GameListComponent } from './game-list/game-list.component';
import { CreateGameComponent } from './create-game/create-game.component';
import { GameCreationWizardComponent } from './create-game/game-creation-wizard.component';
import { JoinGameComponent } from './join-game/join-game.component';
import { GameDetailComponent } from './game-detail/game-detail.component';

const routes: Routes = [
  { path: '', component: GameListComponent },
  { path: 'create', component: GameCreationWizardComponent }, // UX-001: New wizard interface
  { path: 'create-legacy', component: CreateGameComponent }, // Keep old version as fallback
  { path: 'join', component: JoinGameComponent },
  { path: ':id', component: GameDetailComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class GameRoutingModule { } 