import { HttpErrorResponse } from '@angular/common/http';

export interface BackendErrorDetails {
  readonly message: string | null;
  readonly code: string | null;
}

const MOJIBAKE_PATTERN = /[ÃÂ\uFFFD]/;
const TECHNICAL_MESSAGE_PATTERNS = [
  /exception/i,
  /stack\s*trace/i,
  /\bat\s+[a-z0-9_.$]+\([^)]+\)/i,
  /\bjava\.[a-z0-9_.]+/i,
  /\borg\.[a-z0-9_.]+/i,
  /\bsql(state|exception|grammar)?\b/i
];
const MAX_USER_MESSAGE_LENGTH = 180;

export function extractBackendErrorDetails(error: HttpErrorResponse): BackendErrorDetails {
  const payload = error.error;
  if (!payload) {
    return { message: null, code: null };
  }

  if (typeof payload === 'string') {
    const trimmedPayload = payload.trim();
    if (!trimmedPayload) {
      return { message: null, code: null };
    }

    try {
      const parsedPayload = JSON.parse(trimmedPayload) as Record<string, unknown>;
      return {
        message: readMessageFromObject(parsedPayload) ?? trimmedPayload,
        code: readCodeFromObject(parsedPayload)
      };
    } catch {
      return { message: trimmedPayload, code: null };
    }
  }

  if (typeof payload === 'object') {
    const objectPayload = payload as Record<string, unknown>;
    return {
      message: readMessageFromObject(objectPayload),
      code: readCodeFromObject(objectPayload)
    };
  }

  return { message: null, code: null };
}

export function toSafeUserMessage(message: string | null): string | null {
  if (!message) {
    return null;
  }

  const trimmedMessage = message.trim();
  if (!trimmedMessage) {
    return null;
  }

  if (trimmedMessage.length > MAX_USER_MESSAGE_LENGTH) {
    return null;
  }

  if (MOJIBAKE_PATTERN.test(trimmedMessage)) {
    return null;
  }

  if (TECHNICAL_MESSAGE_PATTERNS.some((pattern) => pattern.test(trimmedMessage))) {
    return null;
  }

  return trimmedMessage;
}

function readMessageFromObject(payload: Record<string, unknown>): string | null {
  const directMessage = asString(payload['message']);
  if (directMessage) {
    return directMessage;
  }

  const directError = asString(payload['error']);
  if (directError) {
    return directError;
  }

  const nestedError = payload['error'];
  if (nestedError && typeof nestedError === 'object') {
    return asString((nestedError as Record<string, unknown>)['message']);
  }

  return null;
}

function readCodeFromObject(payload: Record<string, unknown>): string | null {
  const directCode = asString(payload['code']);
  if (directCode) {
    return directCode;
  }

  const nestedError = payload['error'];
  if (nestedError && typeof nestedError === 'object') {
    return asString((nestedError as Record<string, unknown>)['code']);
  }

  return null;
}

function asString(value: unknown): string | null {
  return typeof value === 'string' && value.trim() ? value.trim() : null;
}

