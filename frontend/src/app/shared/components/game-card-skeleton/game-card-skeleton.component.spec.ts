import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { GameCardSkeletonComponent } from './game-card-skeleton.component';

describe('GameCardSkeletonComponent', () => {
    let component: GameCardSkeletonComponent;
    let fixture: ComponentFixture<GameCardSkeletonComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [GameCardSkeletonComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(GameCardSkeletonComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    describe('default values', () => {
        it('should have default count as 3', () => {
            expect(component.count).toBe(3);
        });
    });

    describe('items getter', () => {
        it('should return array with default count (3)', () => {
            expect(component.items.length).toBe(3);
            expect(component.items).toEqual([0, 1, 2]);
        });

        it('should return array with custom count', () => {
            component.count = 5;
            expect(component.items.length).toBe(5);
            expect(component.items).toEqual([0, 1, 2, 3, 4]);
        });

        it('should return empty array when count is 0', () => {
            component.count = 0;
            expect(component.items.length).toBe(0);
            expect(component.items).toEqual([]);
        });
    });

    describe('skeleton cards rendering', () => {
        it('should render correct number of skeleton cards', () => {
            component.count = 3;
            fixture.detectChanges();
            const cards = fixture.debugElement.queryAll(By.css('.game-card-skeleton'));
            expect(cards.length).toBe(3);
        });

        it('should update number of cards when count changes', () => {
            component.count = 5;
            fixture.detectChanges();
            const cards = fixture.debugElement.queryAll(By.css('.game-card-skeleton'));
            expect(cards.length).toBe(5);
        });

        it('should render no cards when count is 0', () => {
            component.count = 0;
            fixture.detectChanges();
            const cards = fixture.debugElement.queryAll(By.css('.game-card-skeleton'));
            expect(cards.length).toBe(0);
        });
    });

    describe('skeleton card structure', () => {
        beforeEach(() => {
            component.count = 1;
            fixture.detectChanges();
        });

        it('should contain header section', () => {
            const header = fixture.debugElement.query(By.css('.skeleton-header'));
            expect(header).toBeTruthy();
        });

        it('should contain icon in header', () => {
            const icon = fixture.debugElement.query(By.css('.skeleton-icon'));
            expect(icon).toBeTruthy();
        });

        it('should contain title group in header', () => {
            const titleGroup = fixture.debugElement.query(By.css('.skeleton-title-group'));
            expect(titleGroup).toBeTruthy();
        });

        it('should contain title and subtitle', () => {
            const title = fixture.debugElement.query(By.css('.skeleton-title'));
            const subtitle = fixture.debugElement.query(By.css('.skeleton-subtitle'));
            expect(title).toBeTruthy();
            expect(subtitle).toBeTruthy();
        });

        it('should contain body section', () => {
            const body = fixture.debugElement.query(By.css('.skeleton-body'));
            expect(body).toBeTruthy();
        });

        it('should contain 3 stats in body', () => {
            const stats = fixture.debugElement.queryAll(By.css('.skeleton-stat'));
            expect(stats.length).toBe(3);
        });

        it('should contain chip in body', () => {
            const chip = fixture.debugElement.query(By.css('.skeleton-chip'));
            expect(chip).toBeTruthy();
        });

        it('should contain footer section', () => {
            const footer = fixture.debugElement.query(By.css('.skeleton-footer'));
            expect(footer).toBeTruthy();
        });

        it('should contain buttons in footer', () => {
            const button = fixture.debugElement.query(By.css('.skeleton-button'));
            const buttonSmall = fixture.debugElement.query(By.css('.skeleton-button-small'));
            expect(button).toBeTruthy();
            expect(buttonSmall).toBeTruthy();
        });
    });

    describe('animation classes', () => {
        beforeEach(() => {
            component.count = 1;
            fixture.detectChanges();
        });

        it('should have animated class on icon', () => {
            const icon = fixture.debugElement.query(By.css('.skeleton-icon.animated'));
            expect(icon).toBeTruthy();
        });

        it('should have animated class on title', () => {
            const title = fixture.debugElement.query(By.css('.skeleton-title.animated'));
            expect(title).toBeTruthy();
        });

        it('should have animated class on subtitle', () => {
            const subtitle = fixture.debugElement.query(By.css('.skeleton-subtitle.animated'));
            expect(subtitle).toBeTruthy();
        });

        it('should have animated class on stats', () => {
            const animatedStats = fixture.debugElement.queryAll(By.css('.skeleton-stat.animated'));
            expect(animatedStats.length).toBe(3);
        });

        it('should have animated class on chip', () => {
            const chip = fixture.debugElement.query(By.css('.skeleton-chip.animated'));
            expect(chip).toBeTruthy();
        });

        it('should have animated class on buttons', () => {
            const button = fixture.debugElement.query(By.css('.skeleton-button.animated'));
            const buttonSmall = fixture.debugElement.query(By.css('.skeleton-button-small.animated'));
            expect(button).toBeTruthy();
            expect(buttonSmall).toBeTruthy();
        });
    });

    describe('multiple cards', () => {
        it('should have complete structure for each card', () => {
            component.count = 3;
            fixture.detectChanges();

            const cards = fixture.debugElement.queryAll(By.css('.game-card-skeleton'));
            cards.forEach(card => {
                expect(card.query(By.css('.skeleton-header'))).toBeTruthy();
                expect(card.query(By.css('.skeleton-body'))).toBeTruthy();
                expect(card.query(By.css('.skeleton-footer'))).toBeTruthy();
            });
        });
    });
});
