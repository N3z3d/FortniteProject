import { HttpErrorResponse } from '@angular/common/http';

import {
  extractBackendErrorDetails,
  toSafeUserMessage
} from './user-facing-error-message.util';

describe('user-facing-error-message.util', () => {
  it('extracts message and code from object payload', () => {
    const error = new HttpErrorResponse({
      status: 409,
      error: { message: 'User already in game', code: 'USER_ALREADY_IN_GAME' }
    });

    expect(extractBackendErrorDetails(error)).toEqual({
      message: 'User already in game',
      code: 'USER_ALREADY_IN_GAME'
    });
  });

  it('extracts message from json string payload', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: '{"message":"Invalid invitation code","code":"INVITATION_CODE_INVALID"}'
    });

    expect(extractBackendErrorDetails(error)).toEqual({
      message: 'Invalid invitation code',
      code: 'INVITATION_CODE_INVALID'
    });
  });

  it('returns null for mojibake and technical strings', () => {
    expect(toSafeUserMessage('Ressource non trouvÃ©e')).toBeNull();
    expect(toSafeUserMessage('java.lang.IllegalStateException: boom')).toBeNull();
  });

  it('returns trimmed message for safe content', () => {
    expect(toSafeUserMessage('  Invitation code expired  ')).toBe('Invitation code expired');
  });
});
