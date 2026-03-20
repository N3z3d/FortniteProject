import { ErrorHandler, Injectable, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { LoggerService } from './logger.service';

/**
 * Global Angular error handler.
 * Catches unhandled errors and logs them.
 * HTTP 500 errors from the backend are already handled by the HTTP interceptor.
 */
@Injectable()
export class GlobalErrorHandlerService implements ErrorHandler {
  private readonly logger = inject(LoggerService);

  handleError(error: unknown): void {
    if (error instanceof HttpErrorResponse) {
      // HTTP errors are handled by the auth/error interceptors — just log
      this.logger.error('GlobalErrorHandler: unhandled HTTP error', { status: error.status, url: error.url });
      return;
    }

    this.logger.error('GlobalErrorHandler: unhandled error', { error });

    // Re-throw so Zone.js still reports it to the console in dev
    console.error('Unhandled error:', error);
  }
}
