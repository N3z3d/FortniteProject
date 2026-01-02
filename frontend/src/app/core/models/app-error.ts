export interface AppErrorDetails {
  code: string;
  status?: number;
  requestId?: string;
  source?: string;
}

export class AppError extends Error {
  readonly code: string;
  readonly status?: number;
  readonly requestId?: string;
  readonly source?: string;

  constructor(message: string, details: AppErrorDetails) {
    super(message);
    this.name = 'AppError';
    this.code = details.code;
    this.status = details.status;
    this.requestId = details.requestId;
    this.source = details.source;
    Object.setPrototypeOf(this, AppError.prototype);
  }
}

export const isAppError = (error: unknown): error is AppError => error instanceof AppError;
