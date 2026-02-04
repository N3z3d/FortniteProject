import { AppError, isAppError, AppErrorDetails } from './app-error';

describe('AppError', () => {
  it('should create an AppError with all details', () => {
    const details: AppErrorDetails = {
      code: 'AUTH_001',
      status: 401,
      requestId: 'req-123',
      source: 'auth-service'
    };

    const error = new AppError('Unauthorized', details);

    expect(error).toBeInstanceOf(Error);
    expect(error).toBeInstanceOf(AppError);
    expect(error.name).toBe('AppError');
    expect(error.message).toBe('Unauthorized');
    expect(error.code).toBe('AUTH_001');
    expect(error.status).toBe(401);
    expect(error.requestId).toBe('req-123');
    expect(error.source).toBe('auth-service');
  });

  it('should create an AppError with minimal details', () => {
    const details: AppErrorDetails = {
      code: 'ERR_001'
    };

    const error = new AppError('Generic error', details);

    expect(error.code).toBe('ERR_001');
    expect(error.status).toBeUndefined();
    expect(error.requestId).toBeUndefined();
    expect(error.source).toBeUndefined();
  });

  it('should maintain prototype chain', () => {
    const error = new AppError('Test', { code: 'TEST_001' });

    expect(Object.getPrototypeOf(error)).toBe(AppError.prototype);
  });
});

describe('isAppError', () => {
  it('should return true for AppError instances', () => {
    const error = new AppError('Test error', { code: 'TEST' });

    expect(isAppError(error)).toBe(true);
  });

  it('should return false for regular Error instances', () => {
    const error = new Error('Regular error');

    expect(isAppError(error)).toBe(false);
  });

  it('should return false for non-Error objects', () => {
    expect(isAppError({})).toBe(false);
    expect(isAppError('string')).toBe(false);
    expect(isAppError(null)).toBe(false);
    expect(isAppError(undefined)).toBe(false);
    expect(isAppError(123)).toBe(false);
  });
});
