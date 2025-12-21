import { Injectable } from '@angular/core';

export enum LogLevel {
  DEBUG = 0,
  INFO = 1,
  WARN = 2,
  ERROR = 3
}

@Injectable({ providedIn: 'root' })
export class LoggerService {
  private currentLevel: LogLevel = LogLevel.INFO;

  setLevel(level: LogLevel): void {
    this.currentLevel = level;
  }

  debug(message: string, context?: unknown): void {
    this.log(LogLevel.DEBUG, message, context);
  }

  info(message: string, context?: unknown): void {
    this.log(LogLevel.INFO, message, context);
  }

  warn(message: string, context?: unknown): void {
    this.log(LogLevel.WARN, message, context);
  }

  error(message: string, context?: unknown): void {
    this.log(LogLevel.ERROR, message, context);
  }

  private log(level: LogLevel, message: string, context?: unknown): void {
    if (level < this.currentLevel) return;

    const timestamp = new Date().toISOString();
    const levelName = LogLevel[level];

    const url = typeof window !== 'undefined' ? window.location.href : 'n/a';
    const userAgent = typeof navigator !== 'undefined'
      ? navigator.userAgent.substring(0, 50)
      : 'n/a';

    const logEntry = {
      timestamp,
      level: levelName,
      message,
      context,
      url,
      userAgent
    };

    const prefix = `[${levelName}] ${timestamp} ${message}`;

    switch (level) {
      case LogLevel.DEBUG:
        console.debug(prefix, logEntry);
        break;
      case LogLevel.INFO:
        console.info(prefix, logEntry);
        break;
      case LogLevel.WARN:
        console.warn(prefix, logEntry);
        break;
      case LogLevel.ERROR:
        console.error(prefix, logEntry);
        break;
    }
  }
}

