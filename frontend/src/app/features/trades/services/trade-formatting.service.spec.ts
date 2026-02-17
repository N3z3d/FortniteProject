import { TestBed } from '@angular/core/testing';
import { TradeFormattingService } from './trade-formatting.service';

describe('TradeFormattingService', () => {
  let service: TradeFormattingService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [TradeFormattingService]
    });

    service = TestBed.inject(TradeFormattingService);
  });

  it('getBalanceDisplayClass returns class names based on sign', () => {
    expect(service.getBalanceDisplayClass(10)).toBe('positive');
    expect(service.getBalanceDisplayClass(-5)).toBe('negative');
    expect(service.getBalanceDisplayClass(0)).toBe('neutral');
  });

  it('getBalanceIcon returns icon names based on sign', () => {
    expect(service.getBalanceIcon(10)).toBe('trending_up');
    expect(service.getBalanceIcon(-5)).toBe('trending_down');
    expect(service.getBalanceIcon(0)).toBe('compare_arrows');
  });

  it('formatCurrency formats USD without decimals', () => {
    const formatted = service.formatCurrency(1234);

    expect(formatted).toContain('$');
    expect(formatted).toContain('1,234');
  });
});
