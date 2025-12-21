import { TestBed } from '@angular/core/testing';

import { LoggerService, LogLevel } from './logger.service';

describe('LoggerService', () => {
  let service: LoggerService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(LoggerService);
  });

  it('does not log debug messages when level is INFO', () => {
    spyOn(console, 'debug');

    service.debug('debug message');

    expect(console.debug).not.toHaveBeenCalled();
  });

  it('logs info messages with a structured entry', () => {
    const infoSpy = spyOn(console, 'info');

    service.info('hello', { user: 'Thibaut' });

    expect(infoSpy).toHaveBeenCalled();

    const [prefix, entry] = infoSpy.calls.mostRecent().args as [string, any];
    expect(prefix).toContain('[INFO]');
    expect(prefix).toContain('hello');
    expect(entry.level).toBe('INFO');
    expect(entry.message).toBe('hello');
    expect(entry.context).toEqual({ user: 'Thibaut' });
    expect(entry.timestamp).toMatch(/^\d{4}-\d{2}-\d{2}T/);
    expect(typeof entry.url).toBe('string');
  });

  it('logs debug messages after setting level to DEBUG', () => {
    const debugSpy = spyOn(console, 'debug');
    service.setLevel(LogLevel.DEBUG);

    service.debug('debug enabled');

    expect(debugSpy).toHaveBeenCalled();
  });
});

