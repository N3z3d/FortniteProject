import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { TeamList } from './team-list';

describe('TeamList', () => {
  let component: TeamList;
  let fixture: ComponentFixture<TeamList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TeamList],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TeamList);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
