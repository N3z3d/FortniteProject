import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { MaterialModule } from '../../shared/material/material.module';
import { GameRoutingModule } from './game-routing.module';

// Import des composants standalone
import { CreateGameComponent } from './create-game/create-game.component';
import { GameDetailComponent } from './game-detail/game-detail.component';

@NgModule({
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MaterialModule,
    GameRoutingModule,
    // Import des composants standalone
    CreateGameComponent,
    GameDetailComponent
  ]
})
export class GameModule { } 