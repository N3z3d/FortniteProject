export interface JoinErrorDetails {
  message: string | null;
  code: string | null;
}

const INVITATION_CODE_REGEX = /^[A-Z0-9-]+$/;
const INVITATION_CODE_MAX_LENGTH = 32;

const ALREADY_IN_GAME_CODE = 'USER_ALREADY_IN_GAME';
const RATE_LIMIT_CODES = new Set(['RATE_LIMIT_EXCEEDED', 'SYS_004', 'TOO_MANY_REQUESTS']);
const INVALID_OR_UNAVAILABLE_CODES = new Set([
  'INVITATION_CODE_INVALID',
  'INVITATION_CODE_NOT_FOUND',
  'GAME_NOT_FOUND',
  'RESOURCE_NOT_FOUND',
  'VALIDATION_ERROR',
  'INVALID_INPUT_PARAMETERS'
]);

const RATE_LIMIT_PATTERNS = [
  'too many',
  'rate limit',
  'limite de taux',
  'trop de tentatives',
  'demasiados intentos',
  'muitas tentativas'
];

const ALREADY_IN_GAME_PATTERNS = [
  'already participating',
  'already in this game',
  'deja dans cette partie'
];

const INVALID_OR_UNAVAILABLE_PATTERNS = [
  'resource not found',
  'not found',
  'non trouve',
  'ressource non trouvee',
  'game not found',
  'partie introuvable',
  'invalid invitation code',
  'invalid input parameters',
  "code d'invitation invalide",
  'codigo de invitacion invalido',
  'codigo de convite invalido',
  'an unexpected error occurred',
  'une erreur inattendue est survenue',
  'ocurrio un error inesperado',
  'ocorreu um erro inesperado'
];

const MOJIBAKE_REPLACEMENTS: Array<[RegExp, string]> = [
  [/\u00c3\u00a9|\u00e3\u00a9/g, 'e'],
  [/\u00c3\u00a8|\u00e3\u00a8/g, 'e'],
  [/\u00c3\u00aa|\u00e3\u00aa/g, 'e'],
  [/\u00c3\u00ab|\u00e3\u00ab/g, 'e'],
  [/\u00c3\u00a1|\u00e3\u00a1/g, 'a'],
  [/\u00c3\u00a0|\u00e3\u00a0/g, 'a'],
  [/\u00c3\u00a2|\u00e3\u00a2/g, 'a'],
  [/\u00c3\u00a4|\u00e3\u00a4/g, 'a'],
  [/\u00c3\u00b3|\u00e3\u00b3/g, 'o'],
  [/\u00c3\u00b2|\u00e3\u00b2/g, 'o'],
  [/\u00c3\u00b4|\u00e3\u00b4/g, 'o'],
  [/\u00c3\u00b6|\u00e3\u00b6/g, 'o'],
  [/\u00c3\u00ba|\u00e3\u00ba/g, 'u'],
  [/\u00c3\u00b9|\u00e3\u00b9/g, 'u'],
  [/\u00c3\u00bb|\u00e3\u00bb/g, 'u'],
  [/\u00c3\u00bc|\u00e3\u00bc/g, 'u'],
  [/\u00c3\u00ad|\u00e3\u00ad/g, 'i'],
  [/\u00c3\u00ac|\u00e3\u00ac/g, 'i'],
  [/\u00c3\u00ae|\u00e3\u00ae/g, 'i'],
  [/\u00c3\u00af|\u00e3\u00af/g, 'i'],
  [/\u00c3\u00b1|\u00e3\u00b1/g, 'n'],
  [/\u00c3\u00a7|\u00e3\u00a7/g, 'c'],
  [/\u00c2/g, ''],
  [/\ufffd/g, '']
];

export function extractJoinErrorDetails(error: unknown): JoinErrorDetails {
  if (error instanceof Error) {
    return { message: error.message || null, code: null };
  }

  if (typeof error !== 'object' || error === null) {
    return { message: null, code: null };
  }

  const payload = error as {
    code?: string;
    message?: string;
    error?: { code?: string; message?: string };
  };

  return {
    message: payload.error?.message || payload.message || null,
    code: payload.error?.code || payload.code || null
  };
}

export function isInvitationCodeFormatValid(code: string): boolean {
  const normalizedCode = code.trim().toUpperCase();
  return (
    normalizedCode.length > 0 &&
    normalizedCode.length <= INVITATION_CODE_MAX_LENGTH &&
    INVITATION_CODE_REGEX.test(normalizedCode)
  );
}

export function resolveJoinErrorTranslationKey(
  details: JoinErrorDetails
):
  | 'games.joinDialog.alreadyInGame'
  | 'games.joinDialog.invalidOrUnavailableCode'
  | 'games.joinDialog.tooManyAttempts'
  | null {
  if (isRateLimitError(details)) {
    return 'games.joinDialog.tooManyAttempts';
  }

  if (isAlreadyInGameError(details)) {
    return 'games.joinDialog.alreadyInGame';
  }

  if (isInvalidCodeOrUnavailableGameError(details)) {
    return 'games.joinDialog.invalidOrUnavailableCode';
  }

  return null;
}

function isRateLimitError(details: JoinErrorDetails): boolean {
  if (RATE_LIMIT_CODES.has(normalizeCode(details.code))) {
    return true;
  }

  const normalizedMessage = normalizeMessage(details.message);
  return RATE_LIMIT_PATTERNS.some((pattern) => normalizedMessage.includes(pattern));
}

function isAlreadyInGameError(details: JoinErrorDetails): boolean {
  if (normalizeCode(details.code) === ALREADY_IN_GAME_CODE) {
    return true;
  }

  const normalizedMessage = normalizeMessage(details.message);
  return ALREADY_IN_GAME_PATTERNS.some((pattern) => normalizedMessage.includes(pattern));
}

function isInvalidCodeOrUnavailableGameError(details: JoinErrorDetails): boolean {
  if (INVALID_OR_UNAVAILABLE_CODES.has(normalizeCode(details.code))) {
    return true;
  }

  const normalizedMessage = normalizeMessage(details.message);
  return INVALID_OR_UNAVAILABLE_PATTERNS.some((pattern) => normalizedMessage.includes(pattern));
}

function normalizeCode(code: string | null): string {
  return (code || '').trim().toUpperCase();
}

function normalizeMessage(message: string | null): string {
  let normalized = (message || '').trim().toLowerCase();

  for (const [pattern, replacement] of MOJIBAKE_REPLACEMENTS) {
    normalized = normalized.replace(pattern, replacement);
  }

  return normalized
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/\s+/g, ' ')
    .trim();
}
